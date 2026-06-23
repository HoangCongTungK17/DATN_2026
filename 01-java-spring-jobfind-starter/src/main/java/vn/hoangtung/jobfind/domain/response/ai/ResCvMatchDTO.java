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
    private int matchScore;
    private int skillMatchScore;
    private int experienceMatchScore;
    private int domainMatchScore;
    private int softSkillMatchScore;
    private int semanticMatchScore;
    private boolean semanticAvailable;
    private Integer semanticRank;
    private String summary;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> recommendations;
    private List<String> evidence;
    private String detectedCandidateLevel;
    private boolean cached;
}
