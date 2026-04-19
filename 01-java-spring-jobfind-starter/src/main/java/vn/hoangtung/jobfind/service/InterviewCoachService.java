package vn.hoangtung.jobfind.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import vn.hoangtung.jobfind.domain.InterviewSession;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.request.ReqAnswerDTO;
import vn.hoangtung.jobfind.domain.request.ReqStartInterviewDTO;
import vn.hoangtung.jobfind.domain.response.ResultPaginationDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResAnswerFeedbackDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResInterviewQuestionDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResInterviewSummaryDTO;
import vn.hoangtung.jobfind.repository.InterviewSessionRepository;
import vn.hoangtung.jobfind.repository.UserRepository;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.constant.InterviewStatusEnum;

@Service
public class InterviewCoachService {

    private final ChatModel chatModel;
    private final InterviewSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InterviewCoachService(ChatModel chatModel,
            InterviewSessionRepository sessionRepository,
            UserRepository userRepository) {
        this.chatModel = chatModel;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    // =============================================
    // WHITELIST: Các vị trí IT hợp lệ
    // =============================================
    private static final List<String> VALID_IT_POSITIONS = List.of(
            "Java Backend Developer", "Node.js Backend Developer", "Python Backend Developer",
            ".NET Developer", "React Frontend Developer", "Angular Frontend Developer",
            "Vue.js Frontend Developer", "Fullstack Developer", "React Native Developer",
            "Flutter Developer", "iOS Developer (Swift)", "Android Developer (Kotlin)",
            "DevOps Engineer", "Cloud Engineer (AWS/Azure)", "Data Engineer",
            "Data Scientist / AI Engineer", "QA / Tester", "Business Analyst (IT)",
            "System Administrator", "Database Administrator", "Cyber Security Engineer",
            "Project Manager (IT)", "UI/UX Designer", "Embedded / IoT Engineer"
    );

    // =============================================
    // PROMPT: Sinh câu hỏi phỏng vấn
    // =============================================
    private static final String GENERATE_QUESTION_PROMPT = """
            Bạn là nhà tuyển dụng IT chuyên nghiệp với 10+ năm kinh nghiệm phỏng vấn.
            BẮT BUỘC: Bạn CHỈ phỏng vấn cho các vị trí CÔNG NGHỆ THÔNG TIN (IT).
            Nhiệm vụ: Tạo 1 câu hỏi phỏng vấn kỹ thuật IT chất lượng cao.

            Vị trí IT: {jobPosition}
            Level: {level}
            Câu hỏi số: {questionNumber}/{totalQuestions}
            Các câu hỏi đã hỏi: {previousQuestions}

            === YÊU CẦU ===
            Trả về DUY NHẤT JSON (không thêm text nào khác):

            {
              "question": "<câu hỏi phỏng vấn IT bằng tiếng Việt>",
              "category": "<TECHNICAL hoặc BEHAVIORAL hoặc SYSTEM_DESIGN>",
              "difficulty": "<EASY hoặc MEDIUM hoặc HARD>"
            }

            === QUY TẮC SINH CÂU HỎI ===
            1. Câu hỏi PHẢI liên quan đến lĩnh vực IT/phần mềm/công nghệ
            2. KHÔNG trùng với các câu hỏi đã hỏi
            3. Độ khó tăng dần:
               - Câu 1-2: EASY (kiến thức nền tảng IT)
               - Câu 3-4: MEDIUM (ứng dụng thực tế)
               - Câu 5+: HARD (kiến trúc, trade-offs)
            4. Phân bố category:
               - 60% TECHNICAL: hỏi về công nghệ, code, algorithms, frameworks
               - 20% BEHAVIORAL: tình huống làm việc nhóm IT, xử lý xung đột kỹ thuật
               - 20% SYSTEM_DESIGN: thiết kế hệ thống, scalability, infrastructure
            5. Câu hỏi PHẢI liên quan trực tiếp đến vị trí và level:
               - FRESHER/JUNIOR: hỏi kiến thức cơ bản, khái niệm, syntax
                 VD: "OOP là gì? Giải thích 4 tính chất", "REST vs SOAP khác gì?"
               - MIDDLE: hỏi kinh nghiệm thực tế, best practices, debugging
                 VD: "Bạn xử lý N+1 query problem như thế nào?", "Giải thích SOLID principles"
               - SENIOR: hỏi kiến trúc, trade-offs, leadership, mentoring
                 VD: "Thiết kế hệ thống real-time chat phục vụ 1M users", "Microservices vs Monolith khi nào chọn cái nào?"
            6. Câu hỏi phải cụ thể, không quá chung chung
            7. Trả lời bằng tiếng Việt
            """;

    // =============================================
    // PROMPT: Tạo tổng kết phiên phỏng vấn bằng AI
    // =============================================
    private static final String SUMMARY_PROMPT = """
            Bạn là chuyên gia tuyển dụng IT. Hãy viết nhận xét tổng kết phiên phỏng vấn.

            Vị trí: {jobPosition}
            Level: {level}
            Điểm trung bình: {overallScore}/100

            Chi tiết từng câu:
            {questionsDetail}

            === YÊU CẦU ===
            Viết 1 đoạn nhận xét tổng kết 4-6 câu bằng tiếng Việt, bao gồm:
            1. Đánh giá tổng quan năng lực ứng viên cho vị trí IT này
            2. Điểm mạnh nổi bật (1-2 điểm)
            3. Điểm cần cải thiện (1-2 điểm)
            4. Lời khuyên cụ thể để nâng cao năng lực cho vị trí {jobPosition}

            CHỈ trả về đoạn văn nhận xét, KHÔNG trả về JSON.
            """;

    // =============================================
    // PROMPT: Đánh giá câu trả lời
    // =============================================
    private static final String EVALUATE_ANSWER_PROMPT = """
            Bạn là nhà tuyển dụng IT chuyên nghiệp với 10+ năm kinh nghiệm.
            Nhiệm vụ: Đánh giá câu trả lời phỏng vấn một cách công bằng và hữu ích.

            Vị trí: {jobPosition}
            Level: {level}
            Câu hỏi: {question}
            Câu trả lời của ứng viên: {answer}

            === YÊU CẦU ===
            Trả về DUY NHẤT JSON (không thêm text nào khác):

            {
              "score": <số nguyên 0-100>,
              "feedback": "<nhận xét 3-4 câu: điểm tốt + điểm cần cải thiện>",
              "betterAnswer": "<câu trả lời mẫu hoàn chỉnh 4-6 câu>"
            }

            === TIÊU CHÍ CHẤM ĐIỂM ===
            - 85-100: Xuất sắc — chính xác, đầy đủ, có ví dụ thực tế và giải thích sâu
            - 70-84: Tốt — đúng hướng, có chi tiết nhưng có thể bổ sung thêm
            - 55-69: Khá — hiểu đúng ý chính nhưng thiếu chi tiết hoặc ví dụ
            - 40-54: Trung bình — hiểu cơ bản nhưng thiếu nhiều ý quan trọng
            - 25-39: Yếu — trả lời mơ hồ, không rõ ràng, thiếu nhiều
            - 10-24: Kém — sai nhiều hoặc chỉ nói qua loa
            - 0-9: Sai hoàn toàn hoặc không liên quan

            === QUY TẮC ĐÁNH GIÁ ===
            - feedback PHẢI chỉ ra CẢ điểm tốt VÀ điểm cần cải thiện
            - Nếu câu trả lời ngắn (< 50 từ), trừ điểm vì thiếu chi tiết
            - Nếu câu trả lời có ví dụ thực tế, cộng điểm
            - Đánh giá theo đúng level: cùng câu trả lời, Junior được điểm cao hơn Senior
            - betterAnswer phải là câu trả lời MẪU hoàn chỉnh phù hợp level {level}:
              + JUNIOR: giải thích khái niệm rõ ràng + 1 ví dụ đơn giản
              + MIDDLE: giải thích + best practices + ví dụ từ dự án thực tế
              + SENIOR: phân tích trade-offs + kiến trúc + kinh nghiệm leadership
            - betterAnswer phải chi tiết hơn câu trả lời của ứng viên
            - Trả lời bằng tiếng Việt
            - CHỈ trả về JSON, KHÔNG thêm text bên ngoài
            """;

    // =============================================
    // 1. BẮT ĐẦU PHIÊN PHỎNG VẤN
    // =============================================
    public ResInterviewQuestionDTO startInterview(ReqStartInterviewDTO req) {
        // 0. Validate vị trí IT
        validateITPosition(req.getJobPosition());

        // 1. Lấy user hiện tại
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập");
        }

        // 2. Tạo session mới
        InterviewSession session = new InterviewSession();
        session.setJobPosition(req.getJobPosition());
        session.setLevel(req.getLevel());
        session.setTotalQuestions(req.getTotalQuestions() > 0 ? req.getTotalQuestions() : 5);
        session.setCurrentQuestion(1);
        session.setStatus(InterviewStatusEnum.IN_PROGRESS);
        session.setQuestionsData("[]"); // JSON array rỗng
        session.setUser(currentUser);
        session = sessionRepository.save(session);

        // 3. Sinh câu hỏi đầu tiên
        String question = generateQuestion(session, 1);

        // 4. Parse câu hỏi từ AI
        try {
            JsonNode qNode = objectMapper.readTree(extractJson(question));

            // Lưu câu hỏi vào questionsData
            List<QuestionData> questions = new ArrayList<>();
            QuestionData qd = new QuestionData();
            qd.questionNumber = 1;
            qd.question = qNode.path("question").asText();
            qd.category = qNode.path("category").asText("TECHNICAL");
            qd.difficulty = qNode.path("difficulty").asText("MEDIUM");
            qd.answer = "";
            qd.score = 0;
            qd.feedback = "";
            qd.betterAnswer = "";
            questions.add(qd);

            session.setQuestionsData(objectMapper.writeValueAsString(questions));
            sessionRepository.save(session);

            // 5. Trả về DTO
            ResInterviewQuestionDTO dto = new ResInterviewQuestionDTO();
            dto.setSessionId(session.getId());
            dto.setQuestionNumber(1);
            dto.setTotalQuestions(session.getTotalQuestions());
            dto.setQuestion(qd.question);
            dto.setCategory(qd.category);
            dto.setDifficulty(qd.difficulty);
            return dto;

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo câu hỏi: " + e.getMessage());
        }
    }

    // =============================================
    // 2. GỬI CÂU TRẢ LỜI + NHẬN FEEDBACK
    // =============================================
    public ResAnswerFeedbackDTO submitAnswer(ReqAnswerDTO req) {
        // 1. Lấy session và kiểm tra quyền sở hữu
        InterviewSession session = sessionRepository.findById(req.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên phỏng vấn"));
        validateSessionOwnership(session);

        if (session.getStatus() != InterviewStatusEnum.IN_PROGRESS) {
            throw new IllegalArgumentException("Phiên phỏng vấn đã kết thúc");
        }

        try {
            // 2. Lấy danh sách câu hỏi hiện tại
            List<QuestionData> questions = objectMapper.readValue(
                    session.getQuestionsData(), new TypeReference<List<QuestionData>>() {
                    });

            int currentQ = session.getCurrentQuestion();
            QuestionData currentQuestion = questions.get(currentQ - 1);

            // Kiểm tra xem câu hỏi đã được trả lời chưa (chống ghi đè)
            if (currentQuestion.answer != null && !currentQuestion.answer.isEmpty()) {
                throw new IllegalArgumentException(
                        "Câu hỏi #" + currentQ + " đã được trả lời rồi. Không thể gửi lại.");
            }

            // 3. Lưu câu trả lời
            currentQuestion.answer = req.getAnswer();

            // 4. Gọi AI đánh giá
            String evalResult = evaluateAnswer(session, currentQuestion.question, req.getAnswer());
            JsonNode evalNode = objectMapper.readTree(extractJson(evalResult));

            currentQuestion.score = evalNode.path("score").asInt(0);
            currentQuestion.feedback = evalNode.path("feedback").asText("");
            currentQuestion.betterAnswer = evalNode.path("betterAnswer").asText("");

            // 5. Cập nhật questions data
            questions.set(currentQ - 1, currentQuestion);
            session.setQuestionsData(objectMapper.writeValueAsString(questions));

            boolean isLastQuestion = currentQ >= session.getTotalQuestions();

            // 6. Tạo DTO response
            ResAnswerFeedbackDTO feedback = new ResAnswerFeedbackDTO();
            feedback.setSessionId(session.getId());
            feedback.setQuestionNumber(currentQ);
            feedback.setScore(currentQuestion.score);
            feedback.setFeedback(currentQuestion.feedback);
            feedback.setBetterAnswer(currentQuestion.betterAnswer);
            feedback.setLastQuestion(isLastQuestion);

            if (isLastQuestion) {
                // Kết thúc phỏng vấn
                int totalScore = questions.stream().mapToInt(q -> q.score).sum() / questions.size();
                session.setOverallScore(totalScore);
                session.setStatus(InterviewStatusEnum.COMPLETED);

                // Tạo nhận xét tổng kết bằng AI
                String aiSummary = generateAISummary(session, questions, totalScore);
                session.setFinalSummary(aiSummary);
            } else {
                // Sinh câu hỏi tiếp theo
                session.setCurrentQuestion(currentQ + 1);

                String nextQ = generateQuestion(session, currentQ + 1);
                JsonNode nextNode = objectMapper.readTree(extractJson(nextQ));

                QuestionData nextQd = new QuestionData();
                nextQd.questionNumber = currentQ + 1;
                nextQd.question = nextNode.path("question").asText();
                nextQd.category = nextNode.path("category").asText("TECHNICAL");
                nextQd.difficulty = nextNode.path("difficulty").asText("MEDIUM");
                nextQd.answer = "";
                nextQd.score = 0;
                nextQd.feedback = "";
                nextQd.betterAnswer = "";
                questions.add(nextQd);

                session.setQuestionsData(objectMapper.writeValueAsString(questions));
                // Trả nextQuestion kèm trong feedback
                ResInterviewQuestionDTO nextQuestionDTO = new ResInterviewQuestionDTO();
                nextQuestionDTO.setSessionId(session.getId());
                nextQuestionDTO.setQuestionNumber(currentQ + 1);
                nextQuestionDTO.setTotalQuestions(session.getTotalQuestions());
                nextQuestionDTO.setQuestion(nextQd.question);
                nextQuestionDTO.setCategory(nextQd.category);
                nextQuestionDTO.setDifficulty(nextQd.difficulty);
                feedback.setNextQuestion(nextQuestionDTO);

            }

            sessionRepository.save(session);
            return feedback;

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xử lý câu trả lời: " + e.getMessage());
        }
    }

    // =============================================
    // 3. LẤY CÂU HỎI HIỆN TẠI (nếu cần)
    // =============================================
    public ResInterviewQuestionDTO getCurrentQuestion(long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên phỏng vấn"));
        validateSessionOwnership(session);

        try {
            List<QuestionData> questions = objectMapper.readValue(
                    session.getQuestionsData(), new TypeReference<List<QuestionData>>() {
                    });

            QuestionData current = questions.get(session.getCurrentQuestion() - 1);

            ResInterviewQuestionDTO dto = new ResInterviewQuestionDTO();
            dto.setSessionId(session.getId());
            dto.setQuestionNumber(session.getCurrentQuestion());
            dto.setTotalQuestions(session.getTotalQuestions());
            dto.setQuestion(current.question);
            dto.setCategory(current.category);
            dto.setDifficulty(current.difficulty);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy câu hỏi: " + e.getMessage());
        }
    }

    // =============================================
    // 4. XEM TỔNG KẾT PHIÊN PHỎNG VẤN
    // =============================================
    public ResInterviewSummaryDTO getSessionSummary(long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên phỏng vấn"));
        validateSessionOwnership(session);

        try {
            List<QuestionData> questions = objectMapper.readValue(
                    session.getQuestionsData(), new TypeReference<List<QuestionData>>() {
                    });

            ResInterviewSummaryDTO dto = new ResInterviewSummaryDTO();
            dto.setSessionId(session.getId());
            dto.setJobPosition(session.getJobPosition());
            dto.setLevel(session.getLevel());
            dto.setOverallScore(session.getOverallScore());
            dto.setFinalSummary(session.getFinalSummary());
            dto.setCreatedAt(session.getCreatedAt());

            List<ResInterviewSummaryDTO.QuestionResult> results = new ArrayList<>();
            for (QuestionData q : questions) {
                results.add(new ResInterviewSummaryDTO.QuestionResult(
                        q.questionNumber, q.question, q.answer, q.score, q.feedback));
            }
            dto.setQuestions(results);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy tổng kết: " + e.getMessage());
        }
    }

    // =============================================
    // 5. LỊCH SỬ PHỎNG VẤN
    // =============================================
    public ResultPaginationDTO getInterviewHistory(Pageable pageable) {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập");
        }

        Page<InterviewSession> page = sessionRepository
                .findByUserOrderByCreatedAtDesc(currentUser, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(page.getTotalPages());
        mt.setTotal(page.getTotalElements());
        rs.setMeta(mt);

        // Convert → DTO tóm tắt
        List<ResInterviewSummaryDTO> list = page.getContent().stream()
                .map(s -> {
                    ResInterviewSummaryDTO dto = new ResInterviewSummaryDTO();
                    dto.setSessionId(s.getId());
                    dto.setJobPosition(s.getJobPosition());
                    dto.setLevel(s.getLevel());
                    dto.setOverallScore(s.getOverallScore());
                    dto.setCreatedAt(s.getCreatedAt());
                    return dto;
                }).toList();

        rs.setResult(list);
        return rs;
    }

    // =============================================
    // HELPER: Sinh câu hỏi bằng AI
    // =============================================
    private String generateQuestion(InterviewSession session, int questionNumber) {
        // Lấy danh sách câu hỏi đã hỏi
        String previousQuestions = "Chưa có";
        try {
            List<QuestionData> questions = objectMapper.readValue(
                    session.getQuestionsData(), new TypeReference<List<QuestionData>>() {
                    });
            if (!questions.isEmpty()) {
                previousQuestions = questions.stream()
                        .map(q -> q.question)
                        .reduce((a, b) -> a + " | " + b)
                        .orElse("Chưa có");
            }
        } catch (Exception ignored) {
        }

        String promptText = GENERATE_QUESTION_PROMPT
                .replace("{jobPosition}", session.getJobPosition())
                .replace("{level}", session.getLevel())
                .replace("{questionNumber}", String.valueOf(questionNumber))
                .replace("{totalQuestions}", String.valueOf(session.getTotalQuestions()))
                .replace("{previousQuestions}", previousQuestions);

        System.out.println(">>> [Interview] Sinh câu hỏi #" + questionNumber);
        return callAiWithRetry(promptText, "Interview-Question");
    }

    // =============================================
    // HELPER: Đánh giá câu trả lời bằng AI
    // =============================================
    private String evaluateAnswer(InterviewSession session, String question, String answer) {
        String promptText = EVALUATE_ANSWER_PROMPT
                .replace("{jobPosition}", session.getJobPosition())
                .replace("{level}", session.getLevel())
                .replace("{question}", question)
                .replace("{answer}", answer);

        System.out.println(">>> [Interview] Đánh giá câu trả lời");
        return callAiWithRetry(promptText, "Interview-Evaluate");
    }

    // =============================================
    // HELPER: Gọi AI với retry logic (xử lý rate limit)
    // =============================================
    private String callAiWithRetry(String promptText, String context) {
        int maxRetries = 2;
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return chatModel.call(new Prompt(promptText)).getResult().getOutput().getContent();
            } catch (Exception e) {
                lastError = e;
                System.out.println(">>> [" + context + "] ⚠️ Lần " + attempt + " thất bại: " + e.getMessage());
                if (attempt < maxRetries) {
                    System.out.println(">>> [" + context + "] ⏳ Đợi 5 giây rồi thử lại...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        throw new RuntimeException("AI không phản hồi sau " + maxRetries + " lần thử: " + lastError.getMessage());
    }

    // =============================================
    // HELPER: Trích xuất JSON từ response AI
    // =============================================
    private String extractJson(String response) {
        if (response == null)
            return "{}";
        response = response.trim();

        // Tìm JSON block trong markdown
        int mdStart = response.indexOf("```json");
        if (mdStart != -1) {
            int jsonStart = response.indexOf("\n", mdStart) + 1;
            int jsonEnd = response.indexOf("```", jsonStart);
            if (jsonEnd != -1)
                return response.substring(jsonStart, jsonEnd).trim();
        }

        mdStart = response.indexOf("```");
        if (mdStart != -1) {
            int jsonStart = response.indexOf("\n", mdStart) + 1;
            int jsonEnd = response.indexOf("```", jsonStart);
            if (jsonEnd != -1)
                return response.substring(jsonStart, jsonEnd).trim();
        }

        // Tìm JSON trực tiếp
        int braceStart = response.indexOf('{');
        int braceEnd = response.lastIndexOf('}');
        if (braceStart != -1 && braceEnd > braceStart) {
            return response.substring(braceStart, braceEnd + 1);
        }

        return response;
    }

    // =============================================
    // HELPER: Validate vị trí IT
    // =============================================
    private void validateITPosition(String jobPosition) {
        if (jobPosition == null || jobPosition.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn vị trí ứng tuyển");
        }
        boolean isValid = VALID_IT_POSITIONS.stream()
                .anyMatch(pos -> pos.equalsIgnoreCase(jobPosition.trim()));
        if (!isValid) {
            throw new IllegalArgumentException(
                    "JobFind chỉ hỗ trợ phỏng vấn cho các vị trí IT. Vui lòng chọn vị trí IT từ danh sách.");
        }
    }

    // =============================================
    // HELPER: Kiểm tra quyền sở hữu session
    // =============================================
    private void validateSessionOwnership(InterviewSession session) {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null || session.getUser().getId() != currentUser.getId()) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập phiên phỏng vấn này");
        }
    }

    // =============================================
    // HELPER: Tạo nhận xét tổng kết bằng AI
    // =============================================
    private String generateAISummary(InterviewSession session, List<QuestionData> questions, int totalScore) {
        try {
            // Build chi tiết từng câu
            StringBuilder detail = new StringBuilder();
            for (QuestionData q : questions) {
                detail.append(String.format("Câu %d (Điểm: %d/100): %s\n", q.questionNumber, q.score, q.question));
                detail.append(String.format("  Trả lời: %s\n", q.answer));
                detail.append(String.format("  Nhận xét: %s\n\n", q.feedback));
            }

            String promptText = SUMMARY_PROMPT
                    .replace("{jobPosition}", session.getJobPosition())
                    .replace("{level}", session.getLevel())
                    .replace("{overallScore}", String.valueOf(totalScore))
                    .replace("{questionsDetail}", detail.toString());

            System.out.println(">>> [Interview] Tạo tổng kết AI...");
            return callAiWithRetry(promptText, "Interview-Summary");
        } catch (Exception e) {
            // Fallback nếu AI lỗi
            System.out.println(">>> [Interview] ⚠️ AI Summary lỗi, dùng fallback: " + e.getMessage());
            return "Phiên phỏng vấn cho vị trí " + session.getJobPosition() + " (" + session.getLevel()
                    + ") hoàn thành. Điểm trung bình: " + totalScore + "/100. "
                    + "Hãy tiếp tục luyện tập để cải thiện kỹ năng phỏng vấn IT!";
        }
    }

    // =============================================
    // INNER CLASS: Cấu trúc dữ liệu câu hỏi (lưu JSON)
    // =============================================
    public static class QuestionData {
        public int questionNumber;
        public String question;
        public String category;
        public String difficulty;
        public String answer;
        public int score;
        public String feedback;
        public String betterAnswer;
    }
}
