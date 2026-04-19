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
public class ResInterviewSummaryDTO {
    private long sessionId;
    private String jobPosition;
    private String level;
    private int overallScore;
    private String finalSummary;
    private List<QuestionResult> questions;
    private Instant createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResult {
        private int questionNumber;
        private String question;
        private String answer;
        private int score;
        private String feedback;
    }
}
