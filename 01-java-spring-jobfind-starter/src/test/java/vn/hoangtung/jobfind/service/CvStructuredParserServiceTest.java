package vn.hoangtung.jobfind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.Test;

import vn.hoangtung.jobfind.util.ai.ParsedCv;

class CvStructuredParserServiceTest {

    @Test
    void parse_shouldReturnDeterministicFallbackWhenAiFails() {
        AiGatewayService aiGatewayService = mock(AiGatewayService.class);
        when(aiGatewayService.callText(anyString(), eq("CV-Structured-Parser"), nullable(String.class),
                any(Duration.class)))
                .thenThrow(new RuntimeException("offline"));

        CvStructuredParserService parser = new CvStructuredParserService(aiGatewayService);
        ParsedCv parsedCv = parser.parse(
                """
                        Nguyen Van A
                        Email: a@example.com
                        Phone: 0900000000
                        GitHub: github.com/nguyenvana

                        SKILLS
                        Java, Spring Boot, PostgreSQL

                        EXPERIENCE
                        - Built REST APIs with Spring Boot.
                        - Reduced response time by 30%.

                        EDUCATION
                        Bachelor of Information Technology
                        """,
                Set.of("Java", "Spring Boot", "PostgreSQL", "Docker"),
                2);

        assertEquals("a@example.com", parsedCv.contact().email());
        assertEquals(2, parsedCv.documentStats().pageCount());
        assertTrue(parsedCv.skillNames().contains("Java"));
        assertTrue(parsedCv.hasMetrics());
        assertFalse(parsedCv.experience().isEmpty());
        assertFalse(parsedCv.education().isEmpty());
    }

    @Test
    void parse_shouldCanonicalizeFallbackSkillAliases() {
        AiGatewayService aiGatewayService = mock(AiGatewayService.class);
        when(aiGatewayService.callText(anyString(), eq("CV-Structured-Parser"), nullable(String.class),
                any(Duration.class)))
                .thenThrow(new RuntimeException("offline"));

        CvStructuredParserService parser = new CvStructuredParserService(aiGatewayService);
        ParsedCv parsedCv = parser.parse(
                """
                        Nguyen Van B

                        SKILLS
                        ReactJS, K8s, Postgres, MongoDB

                        EXPERIENCE
                        - Built admin dashboards and deployment pipelines.
                        """,
                Set.of("React", "Kubernetes", "PostgreSQL", "MongoDB", "Go"),
                1);

        assertTrue(parsedCv.skillNames().contains("React"));
        assertTrue(parsedCv.skillNames().contains("Kubernetes"));
        assertTrue(parsedCv.skillNames().contains("PostgreSQL"));
        assertTrue(parsedCv.skillNames().contains("MongoDB"));
        assertFalse(parsedCv.skillNames().contains("Go"));
    }
}
