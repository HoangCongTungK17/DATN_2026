package vn.hoangtung.jobfind.domain.response.ai;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResCvMatchDTO {
    private long resumeId;
    private long jobId;
    private String jobName;
    private int matchScore; // 0-100
    private String summary; // Nhận xét tổng quan
    private List<String> matchedSkills; // Skills CV có mà JD yêu cầu
    private List<String> missingSkills; // Skills JD yêu cầu mà CV thiếu
    private List<String> recommendations; // Gợi ý cho ứng viên
}
