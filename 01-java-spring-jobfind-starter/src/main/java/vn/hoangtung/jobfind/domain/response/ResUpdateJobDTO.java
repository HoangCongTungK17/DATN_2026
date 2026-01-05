package vn.hoangtung.jobfind.domain.response;

import lombok.Getter;
import lombok.Setter;
import vn.hoangtung.jobfind.util.constant.LevelEnum;
import java.util.List;
import java.time.Instant;

@Getter
@Setter
public class ResUpdateJobDTO {
    private long id;
    private String name;

    private String location;

    private double salary;

    private int quantity;

    private LevelEnum level;

    private Instant startDate;
    private Instant endDate;
    private boolean isActive;

    private List<String> skills;

    private Instant updatedAt;
    private String updatedBy;

}