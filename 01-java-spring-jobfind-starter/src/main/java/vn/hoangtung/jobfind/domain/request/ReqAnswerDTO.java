package vn.hoangtung.jobfind.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqAnswerDTO {
    @Positive(message = "sessionId không hợp lệ")
    private long sessionId;

    @NotBlank(message = "Vui lòng nhập câu trả lời")
    @Size(min = 10, max = 2000, message = "Câu trả lời phải có từ 10 đến 2000 ký tự")
    private String answer; // Câu trả lời của user
}
