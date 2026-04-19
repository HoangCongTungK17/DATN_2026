package vn.hoangtung.jobfind.domain.response.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.hoangtung.jobfind.domain.response.ai.ResInterviewQuestionDTO;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResAnswerFeedbackDTO {
    private long sessionId;
    private int questionNumber;
    private int score; // 0-100
    private String feedback; // Nhận xét AI
    private String betterAnswer; // Câu trả lời mẫu tốt hơn
    private boolean isLastQuestion; // true nếu đã hết câu hỏi
    private ResInterviewQuestionDTO nextQuestion; // Câu hỏi tiếp theo (null nếu là câu cuối)

}
