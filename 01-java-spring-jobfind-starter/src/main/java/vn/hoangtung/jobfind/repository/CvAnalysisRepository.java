package vn.hoangtung.jobfind.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.hoangtung.jobfind.domain.CvAnalysis;
import vn.hoangtung.jobfind.domain.User;

public interface CvAnalysisRepository extends JpaRepository<CvAnalysis, Long>,
        JpaSpecificationExecutor<CvAnalysis> {

    Page<CvAnalysis> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUser(User user);

    Optional<CvAnalysis> findTopByUserAndContentHashAndPromptVersionOrderByCreatedAtDesc(
            User user,
            String contentHash,
            String promptVersion);
}
