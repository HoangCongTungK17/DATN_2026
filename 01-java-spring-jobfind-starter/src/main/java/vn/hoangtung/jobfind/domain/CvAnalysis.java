package vn.hoangtung.jobfind.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import vn.hoangtung.jobfind.util.SecurityUtil;

@Entity
@Table(name = "cv_analyses")
@Getter
@Setter
public class CvAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thông tin file CV
    private String fileName; // Tên file: "NguyenVanA_CV.pdf"
    private String fileUrl; // Đường dẫn file đã upload

    // Điểm số (0 - 100)
    private int overallScore; // Điểm tổng
    private int formatScore; // Điểm định dạng/bố cục
    private int contentScore; // Điểm nội dung
    private int keywordScore; // Điểm từ khóa
    private int impactScore; // Điểm tác động/thành tựu

    // Kết quả chi tiết từ AI
    @Column(columnDefinition = "MEDIUMTEXT")
    private String summary; // Nhận xét tổng quan
    @Column(columnDefinition = "JSON")
    private String strengths; // Điểm mạnh (JSON array)
    @Column(columnDefinition = "JSON")
    private String suggestions; // Gợi ý cải thiện (JSON array)
    @Column(columnDefinition = "MEDIUMTEXT")
    private String rawAiResponse; // Lưu nguyên response từ AI (debug)

    // Quan hệ: CV này thuộc user nào
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

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
