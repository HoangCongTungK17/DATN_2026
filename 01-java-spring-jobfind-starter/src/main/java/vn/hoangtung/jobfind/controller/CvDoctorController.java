package vn.hoangtung.jobfind.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.response.ResultPaginationDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResAiTaskSubmittedDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResCvAnalysisDTO;
import vn.hoangtung.jobfind.service.AiRateLimitService;
import vn.hoangtung.jobfind.service.AiTaskExecutorService;
import vn.hoangtung.jobfind.service.CvDoctorService;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;
import vn.hoangtung.jobfind.util.constant.AiTaskTypeEnum;

@RestController
@RequestMapping("/api/v1/ai/cv")
public class CvDoctorController {

    private final CvDoctorService cvDoctorService;
    private final AiTaskExecutorService aiTaskExecutorService;
    private final AiRateLimitService aiRateLimitService;
    private final ObjectMapper objectMapper;

    public CvDoctorController(
            CvDoctorService cvDoctorService,
            AiTaskExecutorService aiTaskExecutorService,
            AiRateLimitService aiRateLimitService,
            ObjectMapper objectMapper) {
        this.cvDoctorService = cvDoctorService;
        this.aiTaskExecutorService = aiTaskExecutorService;
        this.aiRateLimitService = aiRateLimitService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiMessage("Analyze CV with AI")
    public ResponseEntity<ResAiTaskSubmittedDTO> analyzeCV(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        aiRateLimitService.checkTaskLimit(rateLimitKey(request), AiTaskTypeEnum.CV_ANALYZE.name());

        cvDoctorService.extractTextFromPdf(file);
        String tempFilePath = saveTempFile(file);

        User currentUser = aiTaskExecutorService.getCurrentUserOrThrow();
        String inputJson = buildJsonSafe(
                "tempFilePath", tempFilePath,
                "originalFileName", file.getOriginalFilename());

        ResAiTaskSubmittedDTO result = aiTaskExecutorService.submitTask(
                AiTaskTypeEnum.CV_ANALYZE, inputJson, currentUser);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @GetMapping("/history")
    @ApiMessage("Get CV analysis history")
    public ResponseEntity<ResultPaginationDTO> getCvHistory(Pageable pageable) {
        ResultPaginationDTO result = cvDoctorService.getCvAnalysisHistory(pageable);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/detail/{id}")
    @ApiMessage("Get CV analysis detail")
    public ResponseEntity<ResCvAnalysisDTO> getCvAnalysisDetail(@PathVariable Long id) {
        ResCvAnalysisDTO result = cvDoctorService.getCvAnalysisById(id);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/match")
    @ApiMessage("Match CV with job")
    public ResponseEntity<ResAiTaskSubmittedDTO> matchCvWithJob(@RequestParam long resumeId,
            HttpServletRequest request) {
        aiRateLimitService.checkTaskLimit(rateLimitKey(request), AiTaskTypeEnum.CV_MATCH.name());
        User currentUser = aiTaskExecutorService.getCurrentUserOrThrow();
        String inputJson = buildJsonSafe("resumeId", resumeId);

        ResAiTaskSubmittedDTO result = aiTaskExecutorService.submitTask(
                AiTaskTypeEnum.CV_MATCH, inputJson, currentUser);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    private String saveTempFile(MultipartFile file) {
        try {
            Path tempDir = Files.createTempDirectory("cv-upload-");
            String safeFileName = Path.of(file.getOriginalFilename() == null ? "cv.pdf" : file.getOriginalFilename())
                    .getFileName()
                    .toString();
            Path tempFile = tempDir.resolve(safeFileName);
            file.transferTo(tempFile.toFile());
            return tempFile.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Cannot save temporary CV file: " + e.getMessage(), e);
        }
    }

    private String buildJsonSafe(Object... keyValuePairs) {
        try {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            for (int i = 0; i < keyValuePairs.length; i += 2) {
                map.put(String.valueOf(keyValuePairs[i]), keyValuePairs[i + 1]);
            }
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Cannot serialize AI task input: " + e.getMessage(), e);
        }
    }

    private String rateLimitKey(HttpServletRequest request) {
        return SecurityUtil.getCurrentUserLogin()
                .map(email -> "user:" + email)
                .orElse("ip:" + request.getRemoteAddr());
    }
}
