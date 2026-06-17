package vn.hoangtung.jobfind.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.constant.AiTaskStatusEnum;
import vn.hoangtung.jobfind.util.constant.AiTaskTypeEnum;

@Entity
@Table(name = "ai_tasks", indexes = {
        @Index(name = "idx_ai_task_user", columnList = "user_id"),
        @Index(name = "idx_ai_task_status_next_retry", columnList = "status, next_retry_at"),
        @Index(name = "idx_ai_task_status_started", columnList = "status, started_at")
})
@Getter
@Setter
public class AiTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Enumerated(EnumType.STRING)
    private AiTaskTypeEnum taskType;

    @Enumerated(EnumType.STRING)
    private AiTaskStatusEnum status;

    @Column(columnDefinition = "JSON")
    private String inputData;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String resultData;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private int retryCount;
    private int maxRetries;
    private int progress;
    private int timeoutSeconds;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant nextRetryAt;
    private Instant lastHeartbeatAt;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdBy = SecurityUtil.getCurrentUserLogin().orElse("");
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = AiTaskStatusEnum.PENDING;
        }
        if (this.timeoutSeconds <= 0) {
            this.timeoutSeconds = 120;
        }
        if (this.maxRetries <= 0) {
            this.maxRetries = 2;
        }
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtil.getCurrentUserLogin().orElse("");
        this.updatedAt = Instant.now();
    }
}
