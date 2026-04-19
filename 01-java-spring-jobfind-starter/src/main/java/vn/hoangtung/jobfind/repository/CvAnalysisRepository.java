package vn.hoangtung.jobfind.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.hoangtung.jobfind.domain.CvAnalysis;
import vn.hoangtung.jobfind.domain.User;

public interface CvAnalysisRepository extends JpaRepository<CvAnalysis, Long>,
        JpaSpecificationExecutor<CvAnalysis> {

    // Tìm tất cả phân tích CV của 1 user (mới nhất trước)
    Page<CvAnalysis> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Đếm số lần phân tích của 1 user
    long countByUser(User user);
}
