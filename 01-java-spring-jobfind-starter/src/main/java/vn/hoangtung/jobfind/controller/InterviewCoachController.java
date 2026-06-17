package vn.hoangtung.jobfind.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.request.ReqAnswerDTO;
import vn.hoangtung.jobfind.domain.request.ReqStartInterviewDTO;
import vn.hoangtung.jobfind.domain.response.ResultPaginationDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResAiTaskSubmittedDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResInterviewQuestionDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResInterviewSummaryDTO;
import vn.hoangtung.jobfind.service.AiRateLimitService;
import vn.hoangtung.jobfind.service.AiTaskExecutorService;
import vn.hoangtung.jobfind.service.InterviewCoachService;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;
import vn.hoangtung.jobfind.util.constant.AiTaskTypeEnum;

@RestController
@RequestMapping("/api/v1/ai/interview")
public class InterviewCoachController {

    private final InterviewCoachService interviewCoachService;
    private final AiTaskExecutorService aiTaskExecutorService;
    private final AiRateLimitService aiRateLimitService;
    private final ObjectMapper objectMapper;

    public InterviewCoachController(
            InterviewCoachService interviewCoachService,
            AiTaskExecutorService aiTaskExecutorService,
            AiRateLimitService aiRateLimitService,
            ObjectMapper objectMapper) {
        this.interviewCoachService = interviewCoachService;
        this.aiTaskExecutorService = aiTaskExecutorService;
        this.aiRateLimitService = aiRateLimitService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/start")
    @ApiMessage("Start interview session")
    public ResponseEntity<ResAiTaskSubmittedDTO> startInterview(
            @Valid @RequestBody ReqStartInterviewDTO req,
            HttpServletRequest request) {
        aiRateLimitService.checkTaskLimit(rateLimitKey(request), AiTaskTypeEnum.INTERVIEW_START.name());
        User currentUser = aiTaskExecutorService.getCurrentUserOrThrow();
        String inputJson = buildJsonSafe(
                "jobPosition", req.getJobPosition(),
                "level", req.getLevel(),
                "totalQuestions", req.getTotalQuestions());

        ResAiTaskSubmittedDTO result = aiTaskExecutorService.submitTask(
                AiTaskTypeEnum.INTERVIEW_START, inputJson, currentUser);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @PostMapping("/answer")
    @ApiMessage("Submit interview answer")
    public ResponseEntity<ResAiTaskSubmittedDTO> submitAnswer(
            @Valid @RequestBody ReqAnswerDTO req,
            HttpServletRequest request) {
        aiRateLimitService.checkTaskLimit(rateLimitKey(request), AiTaskTypeEnum.INTERVIEW_ANSWER.name());
        User currentUser = aiTaskExecutorService.getCurrentUserOrThrow();
        String inputJson = buildJsonSafe(
                "sessionId", req.getSessionId(),
                "answer", req.getAnswer());

        ResAiTaskSubmittedDTO result = aiTaskExecutorService.submitTask(
                AiTaskTypeEnum.INTERVIEW_ANSWER, inputJson, currentUser);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @GetMapping("/question/{sessionId}")
    @ApiMessage("Get current interview question")
    public ResponseEntity<ResInterviewQuestionDTO> getCurrentQuestion(
            @PathVariable long sessionId) {
        ResInterviewQuestionDTO result = interviewCoachService.getCurrentQuestion(sessionId);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/summary/{sessionId}")
    @ApiMessage("Get interview summary")
    public ResponseEntity<ResInterviewSummaryDTO> getSessionSummary(
            @PathVariable long sessionId) {
        ResInterviewSummaryDTO result = interviewCoachService.getSessionSummary(sessionId);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/history")
    @ApiMessage("Get interview history")
    public ResponseEntity<ResultPaginationDTO> getHistory(Pageable pageable) {
        ResultPaginationDTO result = interviewCoachService.getInterviewHistory(pageable);
        return ResponseEntity.ok().body(result);
    }

    private String buildJsonSafe(Object... keyValuePairs) {
        try {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            for (int i = 0; i < keyValuePairs.length; i += 2) {
                map.put(String.valueOf(keyValuePairs[i]), keyValuePairs[i + 1]);
            }
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Cannot serialize AI task input: " + e.getMessage(), e);
        }
    }

    private String rateLimitKey(HttpServletRequest request) {
        return SecurityUtil.getCurrentUserLogin()
                .map(email -> "user:" + email)
                .orElse("ip:" + request.getRemoteAddr());
    }
}
