package vn.hoangtung.jobfind.domain.response.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResAiTaskSubmittedDTO {
    private long taskId;
    private String taskType;
    private String status;
    private String message;
    private String statusUrl;
    private String streamUrl;
    private int pollIntervalMillis;
}
