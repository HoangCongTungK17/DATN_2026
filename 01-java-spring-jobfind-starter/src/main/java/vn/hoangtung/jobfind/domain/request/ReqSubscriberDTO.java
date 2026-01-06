package vn.hoangtung.jobfind.domain.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqSubscriberDTO {
    private Long id; // Changed to Long to handle optional values

    @NotBlank(message = "email không được để trống")
    private String email;

    @NotBlank(message = "name không được để trống")
    private String name;

    private List<SkillId> skills;

    @Getter
    @Setter
    public static class SkillId {
        private Long id; // Changed to Long to handle string/null values
    }
}
