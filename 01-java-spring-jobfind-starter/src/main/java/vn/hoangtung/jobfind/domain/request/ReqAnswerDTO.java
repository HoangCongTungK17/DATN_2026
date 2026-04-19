package vn.hoangtung.jobfind.domain.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqAnswerDTO {
    private long sessionId;
    private String answer; // Câu trả lời của user
}
