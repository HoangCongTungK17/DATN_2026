package vn.hoangtung.jobfind.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqAnswerDTO {
    private long sessionId;

    @NotBlank(message = "Vui lòng nhập câu trả lời")
    @Size(min = 10, message = "Câu trả lời quá ngắn (tối thiểu 10 ký tự)")
    private String answer; // Câu trả lời của user
}
