package vn.hoangtung.jobfind.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.hoangtung.jobfind.domain.request.ReqAnswerDTO;
import vn.hoangtung.jobfind.domain.request.ReqStartInterviewDTO;
import vn.hoangtung.jobfind.domain.response.ResultPaginationDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResAnswerFeedbackDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResInterviewQuestionDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResInterviewSummaryDTO;
import vn.hoangtung.jobfind.service.InterviewCoachService;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/ai/interview")
public class InterviewCoachController {

    private final InterviewCoachService interviewCoachService;

    public InterviewCoachController(InterviewCoachService interviewCoachService) {
        this.interviewCoachService = interviewCoachService;
    }

    /**
     * API 1: Bắt đầu phiên phỏng vấn mới
     * POST /api/v1/ai/interview/start
     * Body: { "jobPosition": "Java Backend", "level": "Junior", "totalQuestions": 5
     * }
     */
    @PostMapping("/start")
    @ApiMessage("Bắt đầu phiên phỏng vấn")
    public ResponseEntity<ResInterviewQuestionDTO> startInterview(
            @RequestBody ReqStartInterviewDTO req) {
        ResInterviewQuestionDTO result = interviewCoachService.startInterview(req);
        return ResponseEntity.ok().body(result);
    }

    /**
     * API 2: Gửi câu trả lời → nhận feedback + câu hỏi tiếp
     * POST /api/v1/ai/interview/answer
     * Body: { "sessionId": 1, "answer": "..." }
     */
    @PostMapping("/answer")
    @ApiMessage("Gửi câu trả lời phỏng vấn")
    public ResponseEntity<ResAnswerFeedbackDTO> submitAnswer(
            @RequestBody ReqAnswerDTO req) {
        ResAnswerFeedbackDTO result = interviewCoachService.submitAnswer(req);
        return ResponseEntity.ok().body(result);
    }

    /**
     * API 3: Lấy câu hỏi hiện tại (nếu refresh trang)
     * GET /api/v1/ai/interview/question/{sessionId}
     */
    @GetMapping("/question/{sessionId}")
    @ApiMessage("Lấy câu hỏi hiện tại")
    public ResponseEntity<ResInterviewQuestionDTO> getCurrentQuestion(
            @PathVariable long sessionId) {
        ResInterviewQuestionDTO result = interviewCoachService.getCurrentQuestion(sessionId);
        return ResponseEntity.ok().body(result);
    }

    /**
     * API 4: Xem tổng kết phiên phỏng vấn
     * GET /api/v1/ai/interview/summary/{sessionId}
     */
    @GetMapping("/summary/{sessionId}")
    @ApiMessage("Xem tổng kết phiên phỏng vấn")
    public ResponseEntity<ResInterviewSummaryDTO> getSessionSummary(
            @PathVariable long sessionId) {
        ResInterviewSummaryDTO result = interviewCoachService.getSessionSummary(sessionId);
        return ResponseEntity.ok().body(result);
    }

    /**
     * API 5: Lịch sử phỏng vấn
     * GET /api/v1/ai/interview/history
     */
    @GetMapping("/history")
    @ApiMessage("Lịch sử phỏng vấn")
    public ResponseEntity<ResultPaginationDTO> getHistory(Pageable pageable) {
        ResultPaginationDTO result = interviewCoachService.getInterviewHistory(pageable);
        return ResponseEntity.ok().body(result);
    }
}
