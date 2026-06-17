package vn.hoangtung.jobfind.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "cv_analyses", indexes = {
        @Index(name = "idx_cv_analysis_user_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_cv_analysis_cache", columnList = "user_id, content_hash, prompt_version")
})
@Getter
@Setter
public class CvAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileUrl;

    private int overallScore;
    private int formatScore;
    private int contentScore;
    private int keywordScore;
    private int impactScore;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String summary;

    @Column(columnDefinition = "JSON")
    private String strengths;

    @Column(columnDefinition = "JSON")
    private String suggestions;

    @Column(columnDefinition = "JSON")
    private String detectedSkills;

    @Column(columnDefinition = "JSON")
    private String parsedCv;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String rawAiResponse;

    private String contentHash;
    private String promptVersion;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdBy = SecurityUtil.getCurrentUserLogin().orElse("");
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtil.getCurrentUserLogin().orElse("");
        this.updatedAt = Instant.now();
    }
}
