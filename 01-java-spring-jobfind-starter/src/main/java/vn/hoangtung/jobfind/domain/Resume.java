package vn.hoangtung.jobfind.domain;

import java.time.Instant;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.constant.ResumeStateEnum;

@Entity
@Table(name = "resumes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_resume_user_job", columnNames = { "user_id", "job_id" })
}, indexes = {
        @Index(name = "idx_resume_user_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_resume_job", columnList = "job_id"),
        @Index(name = "idx_resume_email", columnList = "email")
})
@Getter
@Setter
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank(message = "email không được để trống")
    private String email;

    @NotBlank(message = "url không được để trống (upload cv chưa thành công)")
    String url;

    @Enumerated(EnumType.STRING)
    private ResumeStateEnum status;

    private Instant createdAt;
    private Instant updatedAt;

    private String createdBy;
    private String updatedBy;

    // AI CV-JD Matching fields
    private Integer aiMatchScore;

    @Column(columnDefinition = "TEXT")
    private String aiMatchSummary;

    @Column(columnDefinition = "TEXT")
    private String aiMatchDetails;

    private boolean vectorized;
    private Instant vectorizedAt;
    private String cvVectorContentHash;
    private Integer cvChunkCount;

    @Column(columnDefinition = "TEXT")
    private String cvVectorError;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent() == true
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent() == true
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        this.updatedAt = Instant.now();
    }

}
