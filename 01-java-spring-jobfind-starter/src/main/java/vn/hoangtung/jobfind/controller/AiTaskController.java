package vn.hoangtung.jobfind.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.response.ai.ResAiTaskDTO;
import vn.hoangtung.jobfind.service.AiTaskExecutorService;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/ai/tasks")
public class AiTaskController {

    private final AiTaskExecutorService aiTaskExecutorService;

    public AiTaskController(AiTaskExecutorService aiTaskExecutorService) {
        this.aiTaskExecutorService = aiTaskExecutorService;
    }

    /**
     * Polling: lấy trạng thái + kết quả task.
     * GET /api/v1/ai/tasks/{taskId}
     */
    @GetMapping("/{taskId}")
    @ApiMessage("Lấy trạng thái task AI")
    public ResponseEntity<ResAiTaskDTO> getTaskStatus(@PathVariable long taskId) {
        User currentUser = aiTaskExecutorService.getCurrentUserOrThrow();
        ResAiTaskDTO result = aiTaskExecutorService.getTaskStatus(taskId, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * SSE stream: gửi trạng thái task realtime cho frontend.
     * EventSource không gửi được Authorization header, nên endpoint này nhận
     * access_token qua query param và tự kiểm tra quyền truy cập task.
     * GET /api/v1/ai/tasks/{taskId}/stream?access_token=...
     */
    @GetMapping(value = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiMessage("Theo dõi trạng thái task AI bằng SSE")
    public SseEmitter streamTaskStatus(
            @PathVariable long taskId,
            @RequestParam("access_token") String accessToken) {
        User currentUser = aiTaskExecutorService.resolveUserFromAccessToken(accessToken);
        return aiTaskExecutorService.streamTaskStatus(taskId, currentUser);
    }

    /**
     * Hủy task đang xử lý.
     * POST /api/v1/ai/tasks/{taskId}/cancel
     */
    @PostMapping("/{taskId}/cancel")
    @ApiMessage("Hủy task AI")
    public ResponseEntity<Void> cancelTask(@PathVariable long taskId) {
        User currentUser = aiTaskExecutorService.getCurrentUserOrThrow();
        aiTaskExecutorService.cancelTask(taskId, currentUser);
        return ResponseEntity.ok().build();
    }
}
