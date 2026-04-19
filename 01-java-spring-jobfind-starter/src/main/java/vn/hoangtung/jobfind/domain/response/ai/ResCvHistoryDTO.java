package vn.hoangtung.jobfind.domain.response.ai;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResCvHistoryDTO {
    private long id;
    private String fileName;
    private int overallScore;
    private Instant createdAt;
}
