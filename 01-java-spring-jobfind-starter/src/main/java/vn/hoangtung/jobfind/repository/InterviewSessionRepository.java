package vn.hoangtung.jobfind.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import vn.hoangtung.jobfind.domain.InterviewSession;
import vn.hoangtung.jobfind.domain.User;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    Page<InterviewSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from InterviewSession s where s.id = :id")
    Optional<InterviewSession> findByIdForUpdate(@Param("id") long id);
}
