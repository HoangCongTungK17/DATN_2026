package vn.hoangtung.jobfind.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import vn.hoangtung.jobfind.repository.CvChunkRepository;
import vn.hoangtung.jobfind.util.ai.ParsedCv;
import vn.hoangtung.jobfind.util.ai.ParsedCv.ContactInfo;
import vn.hoangtung.jobfind.util.ai.ParsedCv.DocumentStats;
import vn.hoangtung.jobfind.util.ai.ParsedCv.EducationEntry;
import vn.hoangtung.jobfind.util.ai.ParsedCv.ProjectEntry;
import vn.hoangtung.jobfind.util.ai.ParsedCv.SkillEvidence;
import vn.hoangtung.jobfind.util.ai.ParsedCv.WorkEntry;

class CvVectorServiceTest {

    @Test
    void chunkCv_shouldCreateStructuredChunksAndMaskSensitiveContactData() {
        CvVectorService service = new CvVectorService(
                mock(VectorStore.class),
                mock(CvChunkRepository.class),
                mock(AiGatewayService.class));

        ParsedCv parsedCv = new ParsedCv(
                new ContactInfo("Nguyen Van A", "a@example.com", "0900000000", "Ha Noi", List.of()),
                "Java Backend Developer",
                "Built REST APIs with Spring Boot and MySQL.",
                List.of(
                        new SkillEvidence("Java", "backend", List.of(), "Java"),
                        new SkillEvidence("Spring Boot", "backend", List.of(), "Spring Boot")),
                List.of(new WorkEntry(
                        "Acme",
                        "Backend Developer",
                        "2024-01",
                        "present",
                        18,
                        List.of("Built payment APIs with Spring Boot.", "Reduced response time by 30%."),
                        List.of("Java", "Spring Boot", "MySQL"),
                        true)),
                List.of(new ProjectEntry(
                        "Job portal",
                        "Backend",
                        "Recruitment platform",
                        List.of("Implemented JWT authentication."),
                        List.of("Spring Boot", "Redis"),
                        false)),
                List.of(new EducationEntry("HUST", "Bachelor", "IT", "2020", "2024", "3.4")),
                List.of("AWS Cloud Practitioner"),
                List.of("English"),
                List.of(),
                new DocumentStats(2, 260, 5, 3, 11, List.of("skills", "experience", "projects")),
                List.of());

        List<CvVectorService.CvChunkDraft> chunks = service.chunkCv(
                parsedCv,
                "Contact a@example.com or 0900000000 for details.");

        assertTrue(chunks.stream().anyMatch(chunk -> "CV_PROFILE".equals(chunk.chunkType())));
        assertTrue(chunks.stream().anyMatch(chunk -> "CV_SKILLS".equals(chunk.chunkType())));
        assertTrue(chunks.stream().anyMatch(chunk -> "CV_EXPERIENCE".equals(chunk.chunkType())));
        assertTrue(chunks.stream().anyMatch(chunk -> "CV_PROJECT".equals(chunk.chunkType())));
        assertFalse(chunks.stream().map(CvVectorService.CvChunkDraft::text).anyMatch(text -> text.contains("a@example.com")));
        assertFalse(chunks.stream().map(CvVectorService.CvChunkDraft::text).anyMatch(text -> text.contains("0900000000")));
    }

    @Test
    void filterExpressions_shouldSupportUnderscoreMetadataKeysWithoutStringParsing() {
        Filter.Expression docTypeFilter = CvVectorService.docTypeFilterExpression(CvVectorService.DOC_TYPE_JOB);
        Filter.Expression resumeFilter = CvVectorService.resumeChunkFilterExpression(42L);

        assertNotNull(docTypeFilter);
        assertNotNull(resumeFilter);
        assertTrue(docTypeFilter.toString().contains("doc_type"));
        assertTrue(resumeFilter.toString().contains("resume_id"));
    }
}
