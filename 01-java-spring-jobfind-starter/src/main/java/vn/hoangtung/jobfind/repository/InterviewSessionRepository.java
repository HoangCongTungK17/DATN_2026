package vn.hoangtung.jobfind.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import vn.hoangtung.jobfind.domain.InterviewSession;
import vn.hoangtung.jobfind.domain.User;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    // Tìm lịch sử phỏng vấn của user (mới nhất trước)
    Page<InterviewSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Đếm số phiên phỏng vấn của user
    long countByUser(User user);
}
