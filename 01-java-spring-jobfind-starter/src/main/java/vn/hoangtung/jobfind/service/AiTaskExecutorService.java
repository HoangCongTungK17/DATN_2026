package vn.hoangtung.jobfind.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import vn.hoangtung.jobfind.domain.AiTask;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.request.ReqAnswerDTO;
import vn.hoangtung.jobfind.domain.request.ReqStartInterviewDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResAiTaskDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResAiTaskSubmittedDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResAnswerFeedbackDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResCvAnalysisDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResCvMatchDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResInterviewQuestionDTO;
import vn.hoangtung.jobfind.repository.AiTaskRepository;
import vn.hoangtung.jobfind.repository.UserRepository;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.constant.AiTaskStatusEnum;
import vn.hoangtung.jobfind.util.constant.AiTaskTypeEnum;

@Service
public class AiTaskExecutorService {

    private static final int DEFAULT_MAX_RETRIES = 2;
    private static final int POLL_INTERVAL_MILLIS = 1500;
    private static final Duration SSE_TIMEOUT = Duration.ofMinutes(10);

    private final AiTaskRepository aiTaskRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final CvDoctorService cvDoctorService;
    private final InterviewCoachService interviewCoachService;
    private final AIService aiService;
    private final ThreadPoolTaskExecutor aiTaskExecutor;
    private final JwtDecoder jwtDecoder;

    private final ConcurrentMap<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, CopyOnWriteArrayList<SseEmitter>> taskEmitters = new ConcurrentHashMap<>();

    public AiTaskExecutorService(
            AiTaskRepository aiTaskRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            CvDoctorService cvDoctorService,
            InterviewCoachService interviewCoachService,
            AIService aiService,
            @Qualifier("aiTaskExecutor") ThreadPoolTaskExecutor aiTaskExecutor,
            JwtDecoder jwtDecoder) {
        this.aiTaskRepository = aiTaskRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.cvDoctorService = cvDoctorService;
        this.interviewCoachService = interviewCoachService;
        this.aiService = aiService;
        this.aiTaskExecutor = aiTaskExecutor;
        this.jwtDecoder = jwtDecoder;
    }

    public ResAiTaskSubmittedDTO submitTask(AiTaskTypeEnum taskType, String inputDataJson, User user) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để sử dụng tác vụ AI bất đồng bộ");
        }

        AiTask task = new AiTask();
        task.setTaskType(taskType);
        task.setStatus(AiTaskStatusEnum.PENDING);
        task.setInputData(inputDataJson);
        task.setUser(user);
        task.setRetryCount(0);
        task.setMaxRetries(DEFAULT_MAX_RETRIES);
        task.setProgress(0);
        task.setTimeoutSeconds(resolveTimeout(taskType));
        task.setLastHeartbeatAt(Instant.now());
        task = aiTaskRepository.save(task);

        try {
            enqueueTask(task.getId());
        } catch (TaskRejectedException ex) {
            task.setStatus(AiTaskStatusEnum.FAILED);
            task.setErrorMessage("Hàng đợi AI đang đầy, vui lòng thử lại sau");
            task.setCompletedAt(Instant.now());
            aiTaskRepository.save(task);
            emitTaskUpdate(task);
            throw new IllegalStateException(task.getErrorMessage(), ex);
        }

        String statusUrl = "/api/v1/ai/tasks/" + task.getId();
        String streamUrl = statusUrl + "/stream";
        return new ResAiTaskSubmittedDTO(
                task.getId(),
                taskType.name(),
                AiTaskStatusEnum.PENDING.name(),
                "Task đã được đưa vào hàng đợi xử lý",
                statusUrl,
                streamUrl,
                POLL_INTERVAL_MILLIS);
    }

    public ResAiTaskDTO getTaskStatus(long taskId, User user) {
        AiTask task = findTaskForUser(taskId, user);
        return mapToDto(task);
    }

    public void cancelTask(long taskId, User user) {
        AiTask task = findTaskForUser(taskId, user);
        if (isTerminal(task.getStatus())) {
            throw new IllegalArgumentException("Task đã kết thúc nên không thể hủy");
        }

        task.setStatus(AiTaskStatusEnum.CANCELLED);
        task.setProgress(0);
        task.setCompletedAt(Instant.now());
        task.setCancelledAt(Instant.now());
        task.setErrorMessage("Task đã bị hủy bởi người dùng");
        task.setLastHeartbeatAt(Instant.now());
        task = aiTaskRepository.save(task);

        Future<?> future = runningTasks.remove(taskId);
        if (future != null) {
            future.cancel(true);
        }
        emitTaskUpdate(task);
    }

    public SseEmitter streamTaskStatus(long taskId, User user) {
        AiTask task = findTaskForUser(taskId, user);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT.toMillis());
        CopyOnWriteArrayList<SseEmitter> emitters = taskEmitters.computeIfAbsent(
                taskId,
                ignored -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(taskId, emitter));
        emitter.onTimeout(() -> {
            removeEmitter(taskId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> removeEmitter(taskId, emitter));

        sendTaskEvent(emitter, mapToDto(task));
        if (isTerminal(task.getStatus())) {
            emitter.complete();
            removeEmitter(taskId, emitter);
        }
        return emitter;
    }

    public User resolveUserFromAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Thiếu access token cho SSE stream");
        }
        String token = accessToken.replaceFirst("(?i)^Bearer\\s+", "").trim();
        Jwt jwt = jwtDecoder.decode(token);
        User user = userRepository.findByEmail(jwt.getSubject());
        if (user == null) {
            throw new IllegalArgumentException("Access token không khớp người dùng nào");
        }
        return user;
    }

    public User getCurrentUserOrThrow() {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để sử dụng tính năng này");
        }
        return user;
    }

    private void enqueueTask(long taskId) {
        Future<?> future = aiTaskExecutor.submit(() -> runTask(taskId));
        runningTasks.put(taskId, future);
    }

    private void runTask(long taskId) {
        AiTask task = aiTaskRepository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != AiTaskStatusEnum.PENDING) {
            return;
        }

        setTaskSecurityContext(task);
        try {
            task = markProcessing(task);
            String resultJson = dispatchTask(task);

            task = aiTaskRepository.findById(taskId).orElse(null);
            if (task == null || isTerminal(task.getStatus())) {
                return;
            }

            task.setStatus(AiTaskStatusEnum.COMPLETED);
            task.setResultData(resultJson);
            task.setProgress(100);
            task.setCompletedAt(Instant.now());
            task.setLastHeartbeatAt(Instant.now());
            task = aiTaskRepository.save(task);
            emitTaskUpdate(task);
        } catch (CancellationException ex) {
            markCancelledIfStillActive(taskId, "Task đã bị hủy");
        } catch (Exception ex) {
            handleExecutionFailure(taskId, ex);
        } finally {
            runningTasks.remove(taskId);
            SecurityContextHolder.clearContext();
        }
    }

    private String dispatchTask(AiTask task) throws Exception {
        JsonNode input = objectMapper.readTree(task.getInputData());
        long taskId = task.getId();

        return switch (task.getTaskType()) {
            case CHAT -> handleChat(taskId, input);
            case CV_ANALYZE -> handleCvAnalyze(taskId, input);
            case CV_MATCH -> handleCvMatch(taskId, input);
            case INTERVIEW_START -> handleInterviewStart(taskId, input);
            case INTERVIEW_ANSWER -> handleInterviewAnswer(taskId, input);
        };
    }

    private String handleChat(long taskId, JsonNode input) throws Exception {
        String message = input.path("message").asText("");
        if (message.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập câu hỏi");
        }

        updateProgress(taskId, 30);
        String result = aiService.chat(message);
        updateProgress(taskId, 90);
        return objectMapper.writeValueAsString(result);
    }

    private String handleCvAnalyze(long taskId, JsonNode input) throws Exception {
        String tempFilePath = input.path("tempFilePath").asText("");
        if (tempFilePath.isBlank()) {
            throw new IllegalArgumentException("Thiếu đường dẫn file CV");
        }

        updateProgress(taskId, 30);
        ResCvAnalysisDTO result = cvDoctorService.analyzeCVFromPath(tempFilePath);
        updateProgress(taskId, 90);
        return objectMapper.writeValueAsString(result);
    }

    private String handleCvMatch(long taskId, JsonNode input) throws Exception {
        long resumeId = input.path("resumeId").asLong(0);
        if (resumeId <= 0) {
            throw new IllegalArgumentException("resumeId không hợp lệ");
        }

        updateProgress(taskId, 30);
        ResCvMatchDTO result = cvDoctorService.matchCvWithJob(resumeId);
        updateProgress(taskId, 90);
        return objectMapper.writeValueAsString(result);
    }

    private String handleInterviewStart(long taskId, JsonNode input) throws Exception {
        ReqStartInterviewDTO req = new ReqStartInterviewDTO();
        req.setJobPosition(input.path("jobPosition").asText(""));
        req.setLevel(input.path("level").asText(""));
        req.setTotalQuestions(input.path("totalQuestions").asInt(5));

        updateProgress(taskId, 30);
        ResInterviewQuestionDTO result = interviewCoachService.startInterview(req);
        updateProgress(taskId, 90);
        return objectMapper.writeValueAsString(result);
    }

    private String handleInterviewAnswer(long taskId, JsonNode input) throws Exception {
        ReqAnswerDTO req = new ReqAnswerDTO();
        req.setSessionId(input.path("sessionId").asLong(0));
        req.setAnswer(input.path("answer").asText(""));

        updateProgress(taskId, 30);
        ResAnswerFeedbackDTO result = interviewCoachService.submitAnswer(req);
        updateProgress(taskId, 90);
        return objectMapper.writeValueAsString(result);
    }

    @Scheduled(fixedRate = 15000)
    public void checkTimeouts() {
        List<AiTask> processingTasks = aiTaskRepository.findByStatusAndStartedAtBefore(
                AiTaskStatusEnum.PROCESSING,
                Instant.now());

        for (AiTask task : processingTasks) {
            if (task.getStartedAt() == null || task.getTimeoutSeconds() <= 0) {
                continue;
            }

            Instant deadline = task.getStartedAt().plusSeconds(task.getTimeoutSeconds());
            if (deadline.isBefore(Instant.now())) {
                task.setStatus(AiTaskStatusEnum.TIMEOUT);
                task.setErrorMessage("Task đã vượt quá thời gian cho phép (" + task.getTimeoutSeconds() + "s)");
                task.setCompletedAt(Instant.now());
                task.setLastHeartbeatAt(Instant.now());
                task = aiTaskRepository.save(task);

                Future<?> future = runningTasks.remove(task.getId());
                if (future != null) {
                    future.cancel(true);
                }
                emitTaskUpdate(task);
            }
        }
    }

    @Scheduled(fixedRate = 10000)
    public void enqueueRetryingTasks() {
        List<AiTask> retryingTasks = aiTaskRepository.findByStatusAndNextRetryAtBefore(
                AiTaskStatusEnum.RETRYING,
                Instant.now());

        for (AiTask task : retryingTasks) {
            if (task.getRetryCount() >= task.getMaxRetries()) {
                task.setStatus(AiTaskStatusEnum.FAILED);
                task.setCompletedAt(Instant.now());
                task.setLastHeartbeatAt(Instant.now());
                task = aiTaskRepository.save(task);
                emitTaskUpdate(task);
                continue;
            }

            task.setStatus(AiTaskStatusEnum.PENDING);
            task.setRetryCount(task.getRetryCount() + 1);
            task.setProgress(0);
            task.setStartedAt(null);
            task.setCompletedAt(null);
            task.setNextRetryAt(null);
            task.setLastHeartbeatAt(Instant.now());
            task = aiTaskRepository.save(task);
            emitTaskUpdate(task);

            try {
                enqueueTask(task.getId());
            } catch (TaskRejectedException ex) {
                task.setStatus(AiTaskStatusEnum.RETRYING);
                task.setErrorMessage("Hàng đợi AI đang đầy, sẽ thử lại sau");
                task.setNextRetryAt(Instant.now().plusSeconds(15));
                task = aiTaskRepository.save(task);
                emitTaskUpdate(task);
            }
        }
    }

    private AiTask markProcessing(AiTask task) {
        task.setStatus(AiTaskStatusEnum.PROCESSING);
        task.setStartedAt(Instant.now());
        task.setCompletedAt(null);
        task.setProgress(10);
        task.setLastHeartbeatAt(Instant.now());
        task = aiTaskRepository.save(task);
        emitTaskUpdate(task);
        return task;
    }

    private void updateProgress(long taskId, int progress) {
        AiTask task = aiTaskRepository.findById(taskId)
                .orElseThrow(() -> new CancellationException("Task không còn tồn tại"));
        if (isTerminal(task.getStatus())) {
            throw new CancellationException("Task đã kết thúc");
        }
        task.setProgress(Math.max(0, Math.min(99, progress)));
        task.setLastHeartbeatAt(Instant.now());
        task = aiTaskRepository.save(task);
        emitTaskUpdate(task);
    }

    private void handleExecutionFailure(long taskId, Exception ex) {
        AiTask task = aiTaskRepository.findById(taskId).orElse(null);
        if (task == null || isTerminal(task.getStatus())) {
            return;
        }

        String message = ex.getMessage() != null ? ex.getMessage() : "Lỗi không xác định";
        task.setErrorMessage(message);
        task.setProgress(0);
        task.setLastHeartbeatAt(Instant.now());

        if (task.getRetryCount() < task.getMaxRetries()) {
            task.setStatus(AiTaskStatusEnum.RETRYING);
            task.setNextRetryAt(Instant.now().plusSeconds(retryBackoffSeconds(task.getRetryCount())));
            task.setCompletedAt(null);
        } else {
            task.setStatus(AiTaskStatusEnum.FAILED);
            task.setCompletedAt(Instant.now());
        }

        task = aiTaskRepository.save(task);
        emitTaskUpdate(task);
    }

    private void markCancelledIfStillActive(long taskId, String message) {
        AiTask task = aiTaskRepository.findById(taskId).orElse(null);
        if (task == null || isTerminal(task.getStatus())) {
            return;
        }
        task.setStatus(AiTaskStatusEnum.CANCELLED);
        task.setErrorMessage(message);
        task.setCompletedAt(Instant.now());
        task.setCancelledAt(Instant.now());
        task.setLastHeartbeatAt(Instant.now());
        task = aiTaskRepository.save(task);
        emitTaskUpdate(task);
    }

    private AiTask findTaskForUser(long taskId, User user) {
        return aiTaskRepository.findByIdAndUser(taskId, user)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy task hoặc bạn không có quyền truy cập"));
    }

    private ResAiTaskDTO mapToDto(AiTask task) {
        ResAiTaskDTO dto = new ResAiTaskDTO();
        dto.setTaskId(task.getId());
        dto.setTaskType(task.getTaskType().name());
        dto.setStatus(task.getStatus().name());
        dto.setProgress(task.getProgress());
        dto.setRetryCount(task.getRetryCount());
        dto.setMaxRetries(task.getMaxRetries());
        dto.setTimeoutSeconds(task.getTimeoutSeconds());
        dto.setRetryable(task.getStatus() == AiTaskStatusEnum.RETRYING);
        dto.setTerminal(isTerminal(task.getStatus()));
        dto.setErrorMessage(task.getErrorMessage());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setStartedAt(task.getStartedAt());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setNextRetryAt(task.getNextRetryAt());
        dto.setUpdatedAt(task.getUpdatedAt());

        if (task.getResultData() != null && task.getStatus() == AiTaskStatusEnum.COMPLETED) {
            try {
                dto.setResult(objectMapper.readTree(task.getResultData()));
            } catch (Exception ignored) {
                dto.setResult(task.getResultData());
            }
        }
        return dto;
    }

    private void emitTaskUpdate(AiTask task) {
        CopyOnWriteArrayList<SseEmitter> emitters = taskEmitters.get(task.getId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        ResAiTaskDTO dto = mapToDto(task);
        for (SseEmitter emitter : emitters) {
            sendTaskEvent(emitter, dto);
        }

        if (isTerminal(task.getStatus())) {
            for (SseEmitter emitter : emitters) {
                emitter.complete();
            }
            taskEmitters.remove(task.getId());
        }
    }

    private void sendTaskEvent(SseEmitter emitter, ResAiTaskDTO dto) {
        try {
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(dto));
        } catch (IOException | IllegalStateException ex) {
            emitter.completeWithError(ex);
        }
    }

    private void removeEmitter(long taskId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = taskEmitters.get(taskId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            taskEmitters.remove(taskId);
        }
    }

    private void setTaskSecurityContext(AiTask task) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        if (task.getUser() != null && task.getUser().getEmail() != null) {
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    task.getUser().getEmail(),
                    null,
                    List.of()));
        }
        SecurityContextHolder.setContext(context);
    }

    private boolean isTerminal(AiTaskStatusEnum status) {
        return status == AiTaskStatusEnum.COMPLETED
                || status == AiTaskStatusEnum.FAILED
                || status == AiTaskStatusEnum.CANCELLED
                || status == AiTaskStatusEnum.TIMEOUT;
    }

    private long retryBackoffSeconds(int retryCount) {
        return Math.min(60, 5L * (retryCount + 1));
    }

    private int resolveTimeout(AiTaskTypeEnum taskType) {
        return switch (taskType) {
            case CHAT -> 60;
            case CV_ANALYZE, CV_MATCH, INTERVIEW_START, INTERVIEW_ANSWER -> 120;
        };
    }
}
