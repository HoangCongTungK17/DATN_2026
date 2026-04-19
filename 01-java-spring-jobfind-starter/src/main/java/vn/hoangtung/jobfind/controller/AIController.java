package vn.hoangtung.jobfind.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoangtung.jobfind.service.AIService;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/sync")
    @ApiMessage("Sync All Jobs to Vector DB")
    public ResponseEntity<String> syncData() {
        String result = aiService.syncJobData();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/chat")
    @ApiMessage("Chat with AI Agent")
    public ResponseEntity<String> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        if (message.isBlank()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập câu hỏi");
        }
        String response = aiService.chat(message);
        return ResponseEntity.ok(response);
    }
}