package vn.hoangtung.jobfind.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqGoogleLoginDTO {
    @NotBlank(message = "Google credential khong duoc de trong")
    private String credential;
}
