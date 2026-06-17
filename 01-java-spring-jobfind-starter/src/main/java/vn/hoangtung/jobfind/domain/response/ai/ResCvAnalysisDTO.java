package vn.hoangtung.jobfind.domain.response.ai;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.hoangtung.jobfind.util.ai.ParsedCv;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResCvAnalysisDTO {
    private long id;
    private String fileName;

    private int overallScore;
    private int formatScore;
    private int contentScore;
    private int keywordScore;
    private int impactScore;

    private String summary;
    private List<String> strengths;
    private List<Suggestion> suggestions;
    private List<String> detectedSkills;
    private ParsedCv parsedCv;
    private String analysisVersion;
    private boolean cached;

    private Instant createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String category;
        private String priority;
        private String issue;
        private String suggestion;
    }
}
