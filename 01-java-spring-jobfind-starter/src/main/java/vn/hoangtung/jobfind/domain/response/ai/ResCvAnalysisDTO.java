package vn.hoangtung.jobfind.domain.response.ai;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResCvAnalysisDTO {
    private long id;
    private String fileName;

    // Điểm số
    private int overallScore;
    private int formatScore;
    private int contentScore;
    private int keywordScore;
    private int impactScore;

    // Kết quả chi tiết
    private String summary;
    private List<String> strengths;
    private List<Suggestion> suggestions;

    private Instant createdAt;

    // Inner class cho mỗi gợi ý
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String category; // FORMAT, CONTENT, KEYWORD, IMPACT
        private String priority; // HIGH, MEDIUM, LOW
        private String issue; // Vấn đề
        private String suggestion; // Gợi ý cải thiện
    }
}
