package vn.hoangtung.jobfind.domain.response.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResInterviewQuestionDTO {
    private long sessionId;
    private int questionNumber; // Câu thứ mấy (1, 2, 3...)
    private int totalQuestions; // Tổng số câu
    private String question; // Nội dung câu hỏi
    private String category; // TECHNICAL, BEHAVIORAL, SYSTEM_DESIGN
    private String difficulty; // EASY, MEDIUM, HARD
}
