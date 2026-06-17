package vn.hoangtung.jobfind.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.hoangtung.jobfind.domain.Resume;
import vn.hoangtung.jobfind.domain.User;

public interface ResumeRepository extends JpaRepository<Resume, Long>, JpaSpecificationExecutor<Resume> {

    Optional<Resume> findTopByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserIdAndJobId(long userId, long jobId);
}
