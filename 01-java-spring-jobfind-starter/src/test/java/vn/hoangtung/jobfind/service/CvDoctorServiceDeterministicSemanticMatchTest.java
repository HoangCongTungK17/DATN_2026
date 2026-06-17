package vn.hoangtung.jobfind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Skill;
import vn.hoangtung.jobfind.util.ai.AiFeatureUtils.SemanticMatchSignal;
import vn.hoangtung.jobfind.util.constant.LevelEnum;

class CvDoctorServiceDeterministicSemanticMatchTest {

    @Test
    void computeDeterministicSemanticMatch_shouldReturnStableScoreForSameCvAndJob() {
        Job job = new Job();
        job.setName("Backend Java Developer");
        job.setLevel(LevelEnum.JUNIOR);
        job.setDescription("Build Spring Boot microservices with Docker and PostgreSQL");
        job.setSkills(List.of(skill("Java"), skill("Spring Boot"), skill("Docker")));

        String cvText = """
                Java Backend Engineer
                Skills: Java, Spring Boot, Docker, PostgreSQL
                Built microservices and REST APIs for internal products.
                """;

        SemanticMatchSignal first = CvDoctorService.computeDeterministicSemanticMatch(job, null, cvText);
        SemanticMatchSignal second = CvDoctorService.computeDeterministicSemanticMatch(job, null, cvText);

        assertTrue(first.available());
        assertNull(first.rank());
        assertEquals(first.score(), second.score());
        assertEquals(first.evidence(), second.evidence());
        assertTrue(first.score() >= 60);
        assertTrue(String.join(" ", first.evidence()).contains("java"));
    }

    @Test
    void computeDeterministicSemanticMatch_shouldBeUnavailableWhenTextIsMissing() {
        SemanticMatchSignal signal = CvDoctorService.computeDeterministicSemanticMatch(null, null, "");

        assertFalse(signal.available());
        assertEquals(0, signal.score());
        assertNull(signal.rank());
    }

    private Skill skill(String name) {
        Skill skill = new Skill();
        skill.setName(name);
        return skill;
    }
}
