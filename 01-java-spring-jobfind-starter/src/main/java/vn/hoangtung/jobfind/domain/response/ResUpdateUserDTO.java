package vn.hoangtung.jobfind.domain.response;

import lombok.Getter;
import lombok.Setter;
import vn.hoangtung.jobfind.util.constant.GenderEnum;
import java.time.Instant;

@Getter
@Setter
public class ResUpdateUserDTO {
    private long id;
    private String name;
    private GenderEnum gender;
    private String address;
    private int age;
    private Instant updatedAt;
    private CompanyUser company;

    @Getter
    @Setter
    public static class CompanyUser {
        private long id;
        private String name;
    }

}