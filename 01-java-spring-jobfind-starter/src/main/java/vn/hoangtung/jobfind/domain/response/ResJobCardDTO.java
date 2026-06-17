package vn.hoangtung.jobfind.domain.response;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import vn.hoangtung.jobfind.util.constant.LevelEnum;

/**
 * DTO cho danh sách job công khai (public listing).
 * Chỉ phơi bày các trường an toàn với khách vãng lai, không lộ createdBy/updatedBy
 * và không serialize toàn bộ entity Job (tránh rò rỉ thông tin nội bộ).
 */
@Getter
@Setter
public class ResJobCardDTO {
    private long id;
    private String name;
    private String location;
    private double salary;
    private int quantity;
    private LevelEnum level;
    private Instant startDate;
    private Instant endDate;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private CompanyCard company;
    private List<SkillCard> skills;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class CompanyCard {
        private long id;
        private String name;
        private String logo;
        private String address;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class SkillCard {
        private long id;
        private String name;
    }
}
