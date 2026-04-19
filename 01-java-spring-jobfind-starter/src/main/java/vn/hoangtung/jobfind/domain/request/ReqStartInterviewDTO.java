package vn.hoangtung.jobfind.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqStartInterviewDTO {
    @NotBlank(message = "Vui lòng chọn vị trí ứng tuyển")
    private String jobPosition; // "Java Backend Developer"

    @NotBlank(message = "Vui lòng chọn level")
    private String level; // "Junior", "Mid", "Senior"

    private int totalQuestions; // 5 hoặc 10
}
