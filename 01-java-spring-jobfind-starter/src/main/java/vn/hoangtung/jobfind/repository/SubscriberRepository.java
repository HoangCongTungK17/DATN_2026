package vn.hoangtung.jobfind.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import vn.hoangtung.jobfind.domain.Subscriber;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long>,
        JpaSpecificationExecutor<Subscriber> {
    boolean existsByEmail(String email);

    Subscriber findByEmail(String email);

    @Query("""
            select distinct s
            from Subscriber s
            left join fetch s.skills
            where s.email = :email
            """)
    Subscriber findByEmailWithSkills(@Param("email") String email);
}
