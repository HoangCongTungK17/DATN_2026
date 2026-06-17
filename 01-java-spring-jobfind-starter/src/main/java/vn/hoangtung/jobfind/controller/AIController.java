package vn.hoangtung.jobfind.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.response.ai.ResAiTaskSubmittedDTO;
import vn.hoangtung.jobfind.service.AIService;
import vn.hoangtung.jobfind.service.AiRateLimitService;
import vn.hoangtung.jobfind.service.AiTaskExecutorService;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;
import vn.hoangtung.jobfind.util.constant.AiTaskTypeEnum;
import vn.hoangtung.jobfind.util.error.PermissionException;

@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private static final int MAX_CHAT_MESSAGE_LENGTH = 1000;

    private final AIService aiService;
    private final AiTaskExecutorService aiTaskExecutorService;
    private final AiRateLimitService aiRateLimitService;
    private final ObjectMapper objectMapper;

    public AIController(
            AIService aiService,
            AiTaskExecutorService aiTaskExecutorService,
            AiRateLimitService aiRateLimitService,
            ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.aiTaskExecutorService = aiTaskExecutorService;
        this.aiRateLimitService = aiRateLimitService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/sync")
    @ApiMessage("Sync All Jobs to Vector DB")
    public ResponseEntity<String> syncData() throws PermissionException {
        User currentUser = aiTaskExecutorService.getCurrentUserOrThrow();
        if (!isAdmin(currentUser)) {
            throw new PermissionException("Chỉ admin được đồng bộ dữ liệu job lên vector DB.");
        }
        String result = aiService.syncJobData();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/chat")
    @ApiMessage("Chat with AI Agent")
    public ResponseEntity<String> chat(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String message = body == null ? "" : body.getOrDefault("message", "");
        validateChatMessage(message);
        aiRateLimitService.checkChatLimit(rateLimitKey(request));

        String response = aiService.chat(message);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat/async")
    @ApiMessage("Chat with AI Agent async")
    public ResponseEntity<ResAiTaskSubmittedDTO> chatAsync(@RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String message = body == null ? "" : body.getOrDefault("message", "");
        validateChatMessage(message);
        aiRateLimitService.checkTaskLimit(rateLimitKey(request), AiTaskTypeEnum.CHAT.name());

        User currentUser = aiTaskExecutorService.getCurrentUserOrThrow();
        String inputJson;
        try {
            inputJson = objectMapper.writeValueAsString(Map.of("message", message));
        } catch (Exception e) {
            throw new RuntimeException("Không thể serialize nội dung chat", e);
        }

        ResAiTaskSubmittedDTO result = aiTaskExecutorService.submitTask(
                AiTaskTypeEnum.CHAT,
                inputJson,
                currentUser);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    private void validateChatMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập câu hỏi");
        }
        if (message.length() > MAX_CHAT_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Câu hỏi quá dài. Vui lòng giới hạn dưới 1000 ký tự.");
        }
    }

    private boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getName() != null
                && user.getRole().getName().toUpperCase().contains("ADMIN");
    }

    private String rateLimitKey(HttpServletRequest request) {
        return SecurityUtil.getCurrentUserLogin()
                .map(email -> "user:" + email)
                .orElse("ip:" + request.getRemoteAddr());
    }
}
