package vn.hoangtung.jobfind.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.hoangtung.jobfind.domain.CvChunk;

@Repository
public interface CvChunkRepository extends JpaRepository<CvChunk, Long> {

    List<CvChunk> findByResumeId(long resumeId);

    List<CvChunk> findByResumeIdAndContentHash(long resumeId, String contentHash);

    List<CvChunk> findByCvAnalysisId(long cvAnalysisId);

    void deleteByResumeId(long resumeId);

    void deleteByCvAnalysisId(long cvAnalysisId);
}
