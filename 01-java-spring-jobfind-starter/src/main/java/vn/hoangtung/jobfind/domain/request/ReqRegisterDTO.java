package vn.hoangtung.jobfind.domain.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.hoangtung.jobfind.util.constant.GenderEnum;

@Getter
@Setter
public class ReqRegisterDTO {

    @NotBlank(message = "email must not be blank")
    @Email(message = "email is invalid")
    private String email;

    @NotBlank(message = "password must not be blank")
    @Size(min = 6, max = 100, message = "password must be between 6 and 100 characters")
    private String password;

    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @Min(value = 0, message = "age must be positive")
    @Max(value = 120, message = "age is invalid")
    private int age;

    private GenderEnum gender;

    @Size(max = 255, message = "address must be at most 255 characters")
    private String address;
}
