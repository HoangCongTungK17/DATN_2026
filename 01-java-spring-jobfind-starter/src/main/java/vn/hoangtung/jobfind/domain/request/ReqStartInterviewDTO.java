package vn.hoangtung.jobfind.domain.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqStartInterviewDTO {
    private String jobPosition; // "Java Backend Developer"
    private String level; // "Junior", "Mid", "Senior"
    private int totalQuestions; // 5 hoặc 10
}
