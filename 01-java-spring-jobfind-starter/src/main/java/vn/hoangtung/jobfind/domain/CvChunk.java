package vn.hoangtung.jobfind.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cv_chunks", indexes = {
        @Index(name = "idx_cv_chunk_resume", columnList = "resume_id"),
        @Index(name = "idx_cv_chunk_analysis", columnList = "cv_analysis_id"),
        @Index(name = "idx_cv_chunk_user_hash", columnList = "user_id, content_hash"),
        @Index(name = "idx_cv_chunk_vector_id", columnList = "vector_id")
})
@Getter
@Setter
public class CvChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_analysis_id")
    private CvAnalysis cvAnalysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "vector_id", nullable = false, length = 180)
    private String vectorId;

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Column(name = "chunk_type", length = 40)
    private String chunkType;

    private int chunkIndex;
    private int wordCount;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String text;

    private Instant createdAt;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdAt = Instant.now();
    }
}
