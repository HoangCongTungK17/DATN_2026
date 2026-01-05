package vn.hoangtung.jobfind.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/chat")
    @ApiMessage("Chat with AI Agent")
    public ResponseEntity<String> chat(@RequestParam String message) {
        String response = aiService.chat(message);
        return ResponseEntity.ok(response);
    }
}