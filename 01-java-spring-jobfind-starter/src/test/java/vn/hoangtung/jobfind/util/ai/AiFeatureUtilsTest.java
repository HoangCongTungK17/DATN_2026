package vn.hoangtung.jobfind.util.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Skill;
import vn.hoangtung.jobfind.util.ai.AiFeatureUtils.ChatIntent;
import vn.hoangtung.jobfind.util.ai.AiFeatureUtils.CvSignalProfile;
import vn.hoangtung.jobfind.util.ai.AiFeatureUtils.MatchBreakdown;
import vn.hoangtung.jobfind.util.ai.AiFeatureUtils.SemanticMatchSignal;
import vn.hoangtung.jobfind.util.ai.ParsedCv;
import vn.hoangtung.jobfind.util.ai.ParsedCv.ContactInfo;
import vn.hoangtung.jobfind.util.ai.ParsedCv.DocumentStats;
import vn.hoangtung.jobfind.util.ai.ParsedCv.EducationEntry;
import vn.hoangtung.jobfind.util.ai.ParsedCv.SkillEvidence;
import vn.hoangtung.jobfind.util.ai.ParsedCv.WorkEntry;
import vn.hoangtung.jobfind.util.constant.LevelEnum;

class AiFeatureUtilsTest {

    @Test
    void parseChatIntent_shouldExtractSkillSalaryAndLocation() {
        ChatIntent intent = AiFeatureUtils.parseChatIntent(
                "Tìm việc Java backend ở Hà Nội lương từ 25 triệu",
                List.of("Hà Nội", "TP Hồ Chí Minh"),
                Set.of("Java", "Spring Boot", "React"));

        assertEquals("Hà Nội", intent.location());
        assertEquals(25_000_000, intent.minSalary());
        assertTrue(intent.requestedSkills().contains("Java"));
        assertTrue(intent.keywords().contains("backend"));
    }

    @Test
    void extractKnownSkills_shouldResolveAliasesAndRespectTokenBoundaries() {
        Set<String> skills = AiFeatureUtils.extractKnownSkills(
                "Worked with ReactJS, K8s, Postgres, ML pipelines and MongoDB.",
                Set.of("React", "Kubernetes", "PostgreSQL", "Machine Learning", "MongoDB", "Go"));

        assertTrue(skills.contains("React"));
        assertTrue(skills.contains("Kubernetes"));
        assertTrue(skills.contains("PostgreSQL"));
        assertTrue(skills.contains("Machine Learning"));
        assertTrue(skills.contains("MongoDB"));
        assertFalse(skills.contains("Go"));
    }

    @Test
    void analyzeCv_shouldDetectSignalsAndLevel() {
        String cvText = """
                NGUYEN VAN A
                LinkedIn: linkedin.com/in/nguyenvana
                GitHub: github.com/nguyenvana

                SKILLS
                Java, Spring Boot, Docker, PostgreSQL

                EXPERIENCE
                3 years backend developer.
                Built REST APIs and reduced response time by 35%.

                EDUCATION
                Bachelor of Information Technology
                """;

        CvSignalProfile profile = AiFeatureUtils.analyzeCv(
                cvText,
                Set.of("Java", "Spring Boot", "Docker", "PostgreSQL"));

        assertTrue(profile.hasSkillsSection());
        assertTrue(profile.hasExperienceSection());
        assertTrue(profile.hasEducationSection());
        assertTrue(profile.hasGithub());
        assertTrue(profile.hasLinkedin());
        assertTrue(profile.hasMetrics());
        assertTrue(profile.detectedSkills().contains("Java"));
        assertEquals("MIDDLE", profile.inferredLevel());
    }

    @Test
    void analyzeCv_shouldUseStructuredParsedCvSignals() {
        ParsedCv parsedCv = new ParsedCv(
                new ContactInfo("Nguyen Van A", "a@example.com", "0900000000", "Ha Noi",
                        List.of("github.com/nguyenvana")),
                "Backend Developer",
                "Backend engineer focused on Spring Boot services.",
                List.of(new SkillEvidence("Kubernetes", "devops", List.of("K8s"), "K8s")),
                List.of(new WorkEntry(
                        "FPT Software",
                        "Backend Developer",
                        "2021-01",
                        "2024-01",
                        36,
                        List.of("Reduced API latency by 35% with caching."),
                        List.of("Kubernetes"),
                        true)),
                List.of(),
                List.of(new EducationEntry("HUST", "Bachelor", "Information Technology", "", "", "")),
                List.of(),
                List.of(),
                List.of("github.com/nguyenvana"),
                new DocumentStats(2, 400, 4, 1, 7, List.of("summary", "experience", "education")),
                List.of());

        CvSignalProfile profile = AiFeatureUtils.analyzeCv(
                parsedCv,
                "Backend developer without explicit section headers.",
                Set.of("Kubernetes"));

        assertTrue(profile.detectedSkills().contains("Kubernetes"));
        assertTrue(profile.hasExperienceSection());
        assertTrue(profile.hasEducationSection());
        assertTrue(profile.hasGithub());
        assertTrue(profile.hasMetrics());
        assertEquals("MIDDLE", profile.inferredLevel());
    }

    @Test
    void computeMatch_shouldReturnExplainableBreakdown() {
        Job job = new Job();
        job.setName("Java Backend Developer");
        job.setDescription("Build microservices, Spring Boot APIs, Docker deployment");
        job.setLevel(LevelEnum.JUNIOR);
        job.setSalary(20_000_000);
        job.setLocation("Hà Nội");
        job.setActive(true);
        job.setSkills(List.of(skill("Java"), skill("Spring Boot"), skill("Docker"), skill("Kafka")));

        String cvText = """
                Java Backend Engineer
                Skills: Java, Spring Boot, Docker, PostgreSQL
                2 years experience building backend services for internal products.
                Improved API latency by 20%.
                Education: Bachelor of Information Technology.
                Strong teamwork, collaboration and communication.
                """;

        MatchBreakdown breakdown = AiFeatureUtils.computeMatch(
                job,
                cvText,
                Set.of("Java", "Spring Boot", "Docker", "Kafka", "PostgreSQL"));

        assertTrue(breakdown.finalScore() >= 70);
        assertTrue(breakdown.matchedSkills().contains("Java"));
        assertTrue(breakdown.missingSkills().contains("Kafka"));
        assertFalse(breakdown.evidence().isEmpty());
    }

    @Test
    void computeMatch_shouldCollapseScoreForUnrelatedDocument() {
        Job job = new Job();
        job.setName("Java Backend Developer");
        job.setDescription("Build microservices, Spring Boot APIs, Docker deployment");
        job.setLevel(LevelEnum.JUNIOR);
        job.setSalary(20_000_000);
        job.setLocation("Hà Nội");
        job.setActive(true);
        job.setSkills(List.of(skill("Java"), skill("Spring Boot"), skill("Docker"), skill("Kafka")));

        // Tài liệu hoàn toàn không liên quan tới IT (nội quy trường học, có cả từ "ai"
        // và tín hiệu bằng cấp "đại học" để bẫy các điểm sàn cũ).
        String unrelatedText = """
                QUY DINH CUA NHA TRUONG
                Sinh vien phai tuan thu noi quy ky tuc xa va gio giac len lop.
                Moi truong hop vi pham se bi xu ly theo quy che dai hoc hien hanh.
                Khong ai duoc tu y roi khoi truong trong gio hoc.
                """;

        MatchBreakdown breakdown = AiFeatureUtils.computeMatch(
                job,
                unrelatedText,
                Set.of("Java", "Spring Boot", "Docker", "Kafka"));

        assertTrue(breakdown.finalScore() <= 20,
                "Tài liệu không liên quan phải đạt điểm rất thấp, nhưng nhận được " + breakdown.finalScore());
        assertTrue(breakdown.matchedSkills().isEmpty());
    }

    @Test
    void computeMatch_shouldMatchCanonicalSkillAliases() {
        Job job = new Job();
        job.setName("Platform Frontend Engineer");
        job.setDescription("Build React applications deployed on Kubernetes with PostgreSQL data services");
        job.setLevel(LevelEnum.JUNIOR);
        job.setSkills(List.of(skill("React"), skill("Kubernetes"), skill("PostgreSQL")));

        String cvText = """
                Frontend Engineer
                Skills: ReactJS, K8s, Postgres
                2 years building dashboards and deployment workflows.
                """;

        MatchBreakdown breakdown = AiFeatureUtils.computeMatch(
                job,
                cvText,
                Set.of("React", "Kubernetes", "PostgreSQL"));

        assertEquals(100, breakdown.skillMatchScore());
        assertTrue(breakdown.matchedSkills().contains("React"));
        assertTrue(breakdown.matchedSkills().contains("Kubernetes"));
        assertTrue(breakdown.matchedSkills().contains("PostgreSQL"));
        assertTrue(breakdown.missingSkills().isEmpty());
    }

    @Test
    void computeMatch_shouldBlendSemanticSignalWhenAvailable() {
        Job job = new Job();
        job.setName("Cloud Backend Engineer");
        job.setDescription("Build distributed backend services on AWS and Kubernetes");
        job.setLevel(LevelEnum.JUNIOR);
        job.setSalary(25_000_000);
        job.setSkills(List.of(skill("Java"), skill("Spring Boot"), skill("AWS"), skill("Kubernetes")));

        String cvText = """
                Backend Engineer
                Skills: Java, Spring Boot, Docker
                2 years building backend services and cloud deployment pipelines.
                """;

        MatchBreakdown deterministic = AiFeatureUtils.computeMatch(
                job,
                cvText,
                Set.of("Java", "Spring Boot", "AWS", "Kubernetes", "Docker"));
        MatchBreakdown semantic = AiFeatureUtils.computeMatch(
                job,
                null,
                cvText,
                Set.of("Java", "Spring Boot", "AWS", "Kubernetes", "Docker"),
                new SemanticMatchSignal(100, true, 1, List.of("Semantic match rank #1")));

        assertTrue(semantic.semanticAvailable());
        assertEquals(100, semantic.semanticMatchScore());
        assertEquals(1, semantic.semanticRank());
        assertTrue(semantic.finalScore() >= deterministic.finalScore());
        assertTrue(semantic.evidence().contains("Semantic match rank #1"));
    }

    @Test
    void interviewBlueprint_shouldShiftCategoryAndDifficulty() {
        assertEquals("TECHNICAL", AiFeatureUtils.expectedInterviewCategory(1, 5, "JUNIOR"));
        assertEquals("BEHAVIORAL", AiFeatureUtils.expectedInterviewCategory(5, 5, "JUNIOR"));
        assertEquals("SYSTEM_DESIGN", AiFeatureUtils.expectedInterviewCategory(5, 5, "SENIOR"));
        assertEquals("EASY", AiFeatureUtils.expectedInterviewDifficulty("JUNIOR", 1));
        assertEquals("HARD", AiFeatureUtils.expectedInterviewDifficulty("SENIOR", 3));
    }

    private Skill skill(String name) {
        Skill skill = new Skill();
        skill.setName(name);
        return skill;
    }

}
