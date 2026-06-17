package vn.hoangtung.jobfind.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Skill;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    List<Job> findBySkillsIn(List<Skill> skills);

    @Query("""
            select distinct j
            from Job j
            left join fetch j.skills
            left join fetch j.company
            where j.active = true
            """)
    List<Job> findActiveJobsWithSkillsAndCompany();
}
