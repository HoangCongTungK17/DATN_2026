package vn.hoangtung.jobfind.domain.response.ai;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResAiTaskDTO {
    private long taskId;
    private String taskType;
    private String status;
    private int progress;
    private int retryCount;
    private int maxRetries;
    private int timeoutSeconds;
    private boolean retryable;
    private boolean terminal;
    private Object result;
    private String errorMessage;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant nextRetryAt;
    private Instant updatedAt;
}
