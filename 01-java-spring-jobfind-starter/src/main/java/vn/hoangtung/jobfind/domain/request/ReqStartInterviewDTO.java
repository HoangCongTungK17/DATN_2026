package vn.hoangtung.jobfind.domain.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqStartInterviewDTO {
    @NotBlank(message = "Vui lòng chọn vị trí ứng tuyển")
    private String jobPosition;

    @NotBlank(message = "Vui lòng chọn level")
    private String level;

    @Min(value = 3, message = "Số câu hỏi tối thiểu là 3")
    @Max(value = 10, message = "Số câu hỏi tối đa là 10")
    private int totalQuestions = 5;
}
