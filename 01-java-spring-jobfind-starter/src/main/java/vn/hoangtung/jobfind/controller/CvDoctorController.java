package vn.hoangtung.jobfind.controller;

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

import vn.hoangtung.jobfind.domain.response.ResultPaginationDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResCvAnalysisDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResCvMatchDTO;
import vn.hoangtung.jobfind.service.CvDoctorService;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/ai/cv")
public class CvDoctorController {

    private final CvDoctorService cvDoctorService;

    public CvDoctorController(CvDoctorService cvDoctorService) {
        this.cvDoctorService = cvDoctorService;
    }

    /**
     * API 1: Upload và phân tích CV
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiMessage("Phân tích CV bằng AI")
    public ResponseEntity<ResCvAnalysisDTO> analyzeCV(
            @RequestParam("file") MultipartFile file) {

        ResCvAnalysisDTO result = cvDoctorService.analyzeCV(file);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /**
     * API 2: Lấy lịch sử phân tích CV của user
     */
    @GetMapping("/history")
    @ApiMessage("Lấy lịch sử phân tích CV")
    public ResponseEntity<ResultPaginationDTO> getCvHistory(Pageable pageable) {

        ResultPaginationDTO result = cvDoctorService.getCvAnalysisHistory(pageable);
        return ResponseEntity.ok().body(result);
    }

    /**
     * API 3: Lấy chi tiết 1 kết quả phân tích CV theo ID
     * GET /api/v1/ai/cv/detail/8
     */
    @GetMapping("/detail/{id}")
    @ApiMessage("Lấy chi tiết kết quả phân tích CV")
    public ResponseEntity<ResCvAnalysisDTO> getCvAnalysisDetail(@PathVariable Long id) {

        ResCvAnalysisDTO result = cvDoctorService.getCvAnalysisById(id);
        return ResponseEntity.ok().body(result);
    }

    /**
     * API 4: AI matching CV với Job Description
     * POST /api/v1/ai/cv/match?resumeId=8
     */
    @PostMapping("/match")
    @ApiMessage("AI đánh giá mức độ phù hợp CV với Job")
    public ResponseEntity<ResCvMatchDTO> matchCvWithJob(@RequestParam long resumeId) {

        ResCvMatchDTO result = cvDoctorService.matchCvWithJob(resumeId);
        return ResponseEntity.ok().body(result);
    }

}
