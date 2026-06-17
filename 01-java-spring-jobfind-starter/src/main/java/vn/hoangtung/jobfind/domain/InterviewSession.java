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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.constant.InterviewStatusEnum;

@Entity
@Table(name = "interview_sessions", indexes = {
        @Index(name = "idx_interview_user_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_interview_status", columnList = "status")
})
@Getter
@Setter
public class InterviewSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Version
    private long version;

    // Thông tin phiên phỏng vấn
    private String jobPosition; // "Java Backend Developer"
    private String level; // "Junior", "Mid", "Senior"
    private int totalQuestions; // Tổng số câu hỏi (mặc định 5)
    private int currentQuestion; // Câu hỏi hiện tại (0 = chưa bắt đầu)

    // Điểm số
    private int overallScore; // Điểm tổng (0-100)

    // Trạng thái
    @Enumerated(EnumType.STRING)
    private InterviewStatusEnum status; // IN_PROGRESS, COMPLETED, CANCELLED

    // Lưu toàn bộ Q&A dưới dạng JSON
    @Column(columnDefinition = "JSON")
    private String questionsData; // JSON array: [{question, answer, score, feedback}]

    // Nhận xét tổng kết phiên phỏng vấn
    @Column(columnDefinition = "MEDIUMTEXT")
    private String finalSummary;

    // Quan hệ: phiên phỏng vấn thuộc user nào
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        this.updatedAt = Instant.now();
    }
}
