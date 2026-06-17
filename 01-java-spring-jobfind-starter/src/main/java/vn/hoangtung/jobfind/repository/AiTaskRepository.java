package vn.hoangtung.jobfind.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.hoangtung.jobfind.domain.AiTask;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.util.constant.AiTaskStatusEnum;

@Repository
public interface AiTaskRepository extends JpaRepository<AiTask, Long> {

    Optional<AiTask> findByIdAndUser(long id, User user);

    List<AiTask> findByStatusAndNextRetryAtBefore(AiTaskStatusEnum status, Instant cutoff);

    List<AiTask> findByStatusAndStartedAtBefore(AiTaskStatusEnum status, Instant cutoff);
}
