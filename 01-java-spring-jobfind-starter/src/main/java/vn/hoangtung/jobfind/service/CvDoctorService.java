package vn.hoangtung.jobfind.service;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import vn.hoangtung.jobfind.domain.CvAnalysis;
import vn.hoangtung.jobfind.domain.Resume;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.response.ResultPaginationDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResCvAnalysisDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResCvHistoryDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResCvMatchDTO;
import vn.hoangtung.jobfind.domain.CvAnalysis;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.util.SecurityUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoangtung.jobfind.domain.response.ResultPaginationDTO;
import vn.hoangtung.jobfind.domain.response.ai.ResCvHistoryDTO;
import java.util.stream.Collectors;

import vn.hoangtung.jobfind.domain.Resume;
import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.response.ai.ResCvMatchDTO;
import vn.hoangtung.jobfind.repository.ResumeRepository;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.ArrayList;

import vn.hoangtung.jobfind.repository.CvAnalysisRepository;
import vn.hoangtung.jobfind.repository.UserRepository;
import vn.hoangtung.jobfind.util.SecurityUtil;

@Service
public class CvDoctorService {

    private final CvAnalysisRepository cvAnalysisRepository;
    private final UserRepository userRepository;
    private final ChatModel chatModel;
    private final ResumeRepository resumeRepository;

    @Value("${hoangtung.upload-file.base-uri}")
    private String uploadFileBaseUri;

    // Giới hạn file: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB in bytes

    // ===== PROMPT TEMPLATE CHO PHÂN TÍCH CV =====
    private static final String CV_ANALYSIS_PROMPT = """
                        Bạn là chuyên gia tuyển dụng IT với 15 năm kinh nghiệm review CV.
                        Nhiệm vụ: Phân tích CV của ứng viên IT và đánh giá chi tiết.

                        === NỘI DUNG CV ===
                        {cvText}

                        === YÊU CẦU ===
                        Phân tích CV và trả về KẾT QUẢ DUY NHẤT là một JSON object (TUYỆT ĐỐI không thêm bất kỳ text nào trước hoặc sau JSON):

                        {
                          "overallScore": <số nguyên 0-100>,
                          "formatScore": <số nguyên 0-100>,
                          "contentScore": <số nguyên 0-100>,
                          "keywordScore": <số nguyên 0-100>,
                          "impactScore": <số nguyên 0-100>,
                          "summary": "<nhận xét tổng quan 3-4 câu bằng tiếng Việt>",
                          "strengths": ["điểm mạnh 1 (cụ thể)", "điểm mạnh 2", "điểm mạnh 3"],
                          "suggestions": [
                            {
                              "category": "FORMAT",
                              "priority": "HIGH",
                              "issue": "<vấn đề cụ thể>",
                              "suggestion": "<gợi ý cải thiện kèm ví dụ>"
                            }
                          ]
                        }

                        === TIÊU CHÍ CHẤM ĐIỂM CHI TIẾT ===

                        **1. FORMAT (0-100):** Bố cục và trình bày cho CV trong ngành IT
                        - 80-100: Có header (tên, email, GitHub/LinkedIn), sections rõ ràng (Summary, Skills, Experience, Education), bullet points, 1-2 trang
                        - 60-79: Có cấu trúc cơ bản nhưng thiếu 1-2 sections quan trọng (vd: thiếu Skills section riêng biệt)
                        - 40-59: Cấu trúc lộn xộn, khó đọc, thiếu sections chuẩn
                        - 20-39: Không có format rõ ràng, text liền nhau
                        - 0-19: Hầu như không có cấu trúc, rất ngắn

                        **2. CONTENT (0-100):** Nội dung chuyên môn IT
                        - 80-100: Kinh nghiệm IT chi tiết, mô tả rõ vai trò + công nghệ + thành tựu, có dự án cụ thể
                        - 60-79: Có kinh nghiệm IT nhưng mô tả chung chung, thiếu chi tiết kỹ thuật
                        - 40-59: Ít kinh nghiệm IT hoặc mô tả quá ngắn, không rõ công nghệ dùng
                        - 20-39: Gần như không có nội dung chuyên môn IT
                        - 0-19: Không có kinh nghiệm IT hoặc CV không thuộc lĩnh vực IT

                        **3. KEYWORD (0-100):** Từ khóa kỹ thuật IT cụ thể
                        - 80-100: Liệt kê ≥8 skills/tools IT cụ thể (Java, React, Docker, AWS, PostgreSQL...)
                        - 60-79: 4-7 skills IT cụ thể
                        - 40-59: 2-3 skills IT cụ thể
                        - 20-39: Chỉ có 1 skill IT hoặc chỉ nói chung "biết lập trình"
                        - 0-19: Không liệt kê skill IT nào cụ thể (Word, Excel KHÔNG PHẢI IT skill)

                        **4. IMPACT (0-100):** Tác động & thành tựu đo lường được trong IT
                        - 80-100: Có ≥3 thành tựu IT với con số cụ thể ("tăng 30% performance", "phục vụ 1M users", "giảm 40% response time")
                        - 60-79: Có 1-2 thành tựu IT với con số
                        - 40-59: Có thành tựu nhưng không có con số
                        - 20-39: Chỉ mô tả công việc, không có thành tựu
                        - 0-19: Không có thành tựu nào

                        === CĂN CHỈNH ĐIỂM (CALIBRATION) ===
                        Tham khảo các ví dụ sau để đảm bảo nhất quán:

                        🟢 CV Senior Java Developer (5 năm, FPT Software, microservices, 10+ skills, "giảm 40% response time"):
                        → Format: 90, Content: 95, Keyword: 95, Impact: 90 → Overall: ~93

                        🟡 CV Junior Python Developer (1 năm intern, 6 skills, có projects nhưng ít metrics):
                        → Format: 85, Content: 70, Keyword: 75, Impact: 55 → Overall: ~71

                        🔴 CV Developer chỉ nói "làm dự án" không liệt kê skills cụ thể, mô tả chung chung:
                        → Format: 40, Content: 30, Keyword: 15, Impact: 10 → Overall: ~24

                        ⛔ CV Kế toán (không liên quan IT dù format đẹp):
                        → Format: 15, Content: 5, Keyword: 0, Impact: 5 → Overall: ~6
                        (Ghi rõ trong summary: "CV không thuộc lĩnh vực IT")

                        === QUY TẮC QUAN TRỌNG ===
                        - overallScore = trung bình có trọng số: format(20%) + content(35%) + keyword(25%) + impact(20%)
                        - overallScore PHẢI BẰNG CHÍNH XÁC công thức trên (làm tròn)
                        - Mỗi suggestion PHẢI có ví dụ cụ thể trong trường "suggestion"
                        - strengths: liệt kê 2-5 điểm mạnh LIÊN QUAN ĐẾN NGÀNH IT/CÔNG NGHỆ
                        - suggestions: 3-6 gợi ý, ưu tiên HIGH trước
                        - category chỉ gồm: FORMAT, CONTENT, KEYWORD, IMPACT
                        - priority chỉ gồm: HIGH, MEDIUM, LOW
                        - suggestions PHẢI có ít nhất 1 gợi ý cho mỗi category
                        - Trả lời hoàn toàn bằng tiếng Việt
                        - CHỈ trả về JSON, KHÔNG thêm text giải thích bên ngoài

                        === PENALTY RULES (BẮT BUỘC) ===
                        - Nếu CV KHÔNG THUỘC LĨNH VỰC IT (kế toán, marketing, y tế, luật...): TẤT CẢ điểm ≤ 20, overall ≤ 15
                        - Nếu CV không liệt kê BẤT KỲ skill IT cụ thể nào: keywordScore ≤ 20, formatScore ≤ 50
                        - Nếu mô tả kinh nghiệm chỉ là "làm dự án", "tham gia phát triển" mà KHÔNG nêu cụ thể công nghệ: contentScore ≤ 40
                        - Word, Excel, PowerPoint KHÔNG PHẢI IT skill. Chỉ tính: ngôn ngữ lập trình, frameworks, databases, cloud, DevOps tools
                        - Bỏ qua nội dung template/hướng dẫn trong file, chỉ đánh giá thông tin cá nhân thực tế
                        """;

    // ======================================
    // PROMPT TEMPLATE CHO CV-JD MATCHING
    // ======================================
    private static final String CV_JD_MATCHING_PROMPT = """
            Bạn là chuyên gia tuyển dụng IT với 15 năm kinh nghiệm đánh giá ứng viên.
            Nhiệm vụ: So sánh CV của ứng viên với Job Description (JD) và đánh giá mức độ phù hợp.

            === JOB DESCRIPTION ===
            Vị trí: {jobName}
            Level: {jobLevel}
            Skills yêu cầu: {jobSkills}
            Mô tả công việc:
            {jobDescription}

            === CV ỨNG VIÊN ===
            {cvText}

            === YÊU CẦU ===
            So sánh CV với JD và trả về KẾT QUẢ DUY NHẤT là JSON (TUYỆT ĐỐI không thêm text nào khác):

            {
              "matchScore": <số nguyên 0-100>,
              "summary": "<nhận xét 3-4 câu bằng tiếng Việt>",
              "matchedSkills": ["skill1", "skill2"],
              "missingSkills": ["skill3", "skill4"],
              "recommendations": ["gợi ý cụ thể 1", "gợi ý cụ thể 2", "gợi ý cụ thể 3"]
            }

            === CÁCH TÍNH matchScore ===
            matchScore = skillMatch×40 + experienceMatch×25 + domainMatch×20 + softSkillMatch×15

            Trong đó (mỗi thành phần tính trên thang 0.0 - 1.0):
            - skillMatch = (số skills CV có ÷ tổng skills JD yêu cầu)
              Ví dụ: JD yêu cầu 5 skills, CV có 3 → skillMatch = 3/5 = 0.6
            - experienceMatch:
              + CV level >= JD level: 1.0
              + CV level thấp hơn 1 bậc: 0.6
              + CV level thấp hơn 2+ bậc: 0.3
              + Không xác định: 0.5
            - domainMatch:
              + CV cùng lĩnh vực với JD: 1.0
              + CV lĩnh vực gần (ví dụ: Frontend ↔ Fullstack): 0.7
              + CV khác lĩnh vực hoàn toàn: 0.2
            - softSkillMatch:
              + Có bằng cấp liên quan + kỹ năng mềm: 1.0
              + Chỉ có 1 trong 2: 0.6
              + Không có: 0.3

            === CĂN CHỈNH ĐIỂM (CALIBRATION) ===
            Tham khảo các ví dụ để đảm bảo nhất quán:

            🟢 CV Senior Java (Spring Boot, AWS, K8s) → JD Java Architect (Spring, AWS, K8s, Kafka):
            → matchScore: 85, matchedSkills: [Java, Spring Boot, AWS, K8s], missingSkills: [Kafka]

            🟡 CV Mid React Developer → JD Frontend Vue.js Junior:
            → matchScore: 55, matchedSkills: [TypeScript, CSS], missingSkills: [Vue.js, Svelte]
            (Khác framework chính nhưng cùng lĩnh vực frontend)

            🔴 CV Junior Python → JD Senior Java Architect:
            → matchScore: 20, matchedSkills: [PostgreSQL], missingSkills: [Java, Spring, AWS, K8s, Kafka...]
            (Khác stack, khác level)

            ⛔ CV Kế toán → Bất kỳ JD IT nào:
            → matchScore: 5, matchedSkills: [], missingSkills: [tất cả], summary ghi "CV không thuộc lĩnh vực IT"

            === QUY TẮC QUAN TRỌNG ===
            - matchedSkills: CHỈ liệt kê skills mà CẢ CV VÀ JD đều có (so sánh chính xác tên công nghệ)
            - missingSkills: CHỈ liệt kê skills mà JD yêu cầu nhưng CV KHÔNG CÓ
            - recommendations: 3-5 gợi ý CỤ THỂ để ứng viên cải thiện, bao gồm:
              + Skill cần học thêm và cách học (khoá học, tài liệu cụ thể)
              + Kinh nghiệm cần bổ sung
              + Chứng chỉ nên lấy
            - Nếu CV không liên quan đến IT → matchScore ≤ 10, ghi rõ trong summary
            - Nếu CV và JD cùng stack chính xác → matchScore ≥ 75
            - Nếu CV cùng lĩnh vực nhưng khác framework → matchScore 40-65
            - Nếu CV hoàn toàn khác stack → matchScore ≤ 30
            - Trả lời hoàn toàn bằng tiếng Việt
            - CHỈ trả về JSON, KHÔNG thêm text bên ngoài
            """;

    public CvDoctorService(
            CvAnalysisRepository cvAnalysisRepository,
            UserRepository userRepository, ChatModel chatModel, ResumeRepository resumeRepository) {
        this.cvAnalysisRepository = cvAnalysisRepository;
        this.userRepository = userRepository;
        this.chatModel = chatModel;
        this.resumeRepository = resumeRepository;
    }

    /**
     * Đọc nội dung text từ file PDF.
     * Xử lý các edge cases: file rỗng, file quá lớn, file không phải PDF, file scan
     * (ảnh).
     *
     * @param file - file PDF từ người dùng upload
     * @return String - nội dung text đã trích xuất
     * @throws IllegalArgumentException - nếu file không hợp lệ
     */
    public String extractTextFromPdf(MultipartFile file) {

        // === VALIDATE 1: Kiểm tra file có rỗng không ===
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng upload file CV");
        }

        // === VALIDATE 2: Kiểm tra file có phải PDF không ===
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file PDF. Vui lòng upload file có đuôi .pdf");
        }

        // Kiểm tra content type (bảo mật hơn, tránh đổi đuôi file)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("File không phải định dạng PDF hợp lệ");
        }

        // === VALIDATE 3: Kiểm tra kích thước file ===
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File quá lớn. Kích thước tối đa là 5MB, file của bạn là "
                            + String.format("%.1f", file.getSize() / (1024.0 * 1024.0)) + "MB");
        }

        // === ĐỌC PDF ===
        try {
            // PDFBox 3.x dùng Loader.loadPDF() thay vì PDDocument.load()
            PDDocument document = Loader.loadPDF(file.getInputStream().readAllBytes());

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Đóng document sau khi đọc xong
            document.close();

            // === VALIDATE 4: Kiểm tra text có rỗng không (CV dạng ảnh scan) ===
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Không thể đọc nội dung CV. Có vẻ CV của bạn là dạng ảnh scan. "
                                + "Vui lòng upload CV dạng text (được tạo từ Word, Google Docs...)");
            }

            // Chuẩn hóa text: bỏ khoảng trắng thừa, dòng trống liên tiếp
            text = normalizeText(text);

            // === VALIDATE 5: Text quá ngắn (có thể không phải CV) ===
            if (text.length() < 100) {
                throw new IllegalArgumentException(
                        "Nội dung CV quá ngắn (chỉ có " + text.length()
                                + " ký tự). Vui lòng upload CV đầy đủ thông tin");
            }

            System.out.println(">>> [CV Doctor] Đọc PDF thành công: " + originalFilename
                    + " (" + text.length() + " ký tự, " + document.getNumberOfPages() + " trang)");

            return text;

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Không thể đọc file PDF. File có thể bị hỏng hoặc được bảo vệ bằng mật khẩu. "
                            + "Chi tiết: " + e.getMessage());
        }
    }

    /**
     * Chuẩn hóa text đọc từ PDF:
     * - Thay nhiều dòng trống liên tiếp bằng 1 dòng trống
     * - Bỏ khoảng trắng đầu/cuối mỗi dòng
     * - Giới hạn độ dài (tránh prompt quá dài → tốn token)
     */
    private String normalizeText(String text) {
        // Thay nhiều dòng trống liên tiếp bằng 1 dòng trống
        text = text.replaceAll("(\\r?\\n){3,}", "\n\n");

        // Bỏ khoảng trắng thừa
        text = text.replaceAll("[ \\t]+", " ");

        // Trim mỗi dòng
        String[] lines = text.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line.trim()).append("\n");
        }

        text = sb.toString().trim();

        // Giới hạn tối đa 8000 ký tự (khoảng 2000 token)
        // để tránh prompt quá dài khi gửi cho LLM
        if (text.length() > 8000) {
            text = text.substring(0, 8000) + "\n\n[... CV còn dài, đã cắt bớt ...]";
        }

        return text;
    }

    /**
     * Xây dựng prompt để gửi cho LLM phân tích CV.
     * Dùng string replace thay vì PromptTemplate vì prompt chứa nhiều {} trong JSON
     * mẫu.
     *
     * @param cvText - nội dung text đã trích xuất từ PDF
     * @return Prompt - prompt đã được build, sẵn sàng gửi cho ChatModel
     */
    public Prompt buildAnalysisPrompt(String cvText) {
        String promptText = CV_ANALYSIS_PROMPT.replace("{cvText}", cvText);
        return new Prompt(promptText);
    }

    /**
     * Gọi LLM để phân tích CV và parse kết quả JSON.
     *
     * @param cvText - nội dung CV đã trích xuất
     * @return ResCvAnalysisDTO - kết quả phân tích đã parse
     */
    public ResCvAnalysisDTO callAiAndParseResponse(String cvText) {
        // Xây dựng prompt
        Prompt prompt = buildAnalysisPrompt(cvText);

        System.out.println(">>> [CV Doctor] Đang gọi AI phân tích CV...");
        System.out.println(">>> [CV Doctor] Prompt length: " + cvText.length() + " chars");

        int maxRetries = 2;
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Gọi LLM (dùng ChatModel)
                String rawResponse = chatModel.call(prompt).getResult().getOutput().getContent();

                System.out.println(">>> [CV Doctor] AI đã trả về response (" + rawResponse.length() + " ký tự)");

                // Parse JSON từ response
                return parseAiResponse(rawResponse);

            } catch (Exception e) {
                lastError = e;
                System.out.println(">>> [CV Doctor] ⚠️ Lần " + attempt + " thất bại: " + e.getMessage());

                if (attempt < maxRetries) {
                    System.out.println(">>> [CV Doctor] ⏳ Đợi 5 giây rồi thử lại...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        // Tất cả retry đều thất bại
        System.out.println(">>> [CV Doctor] ❌ Tất cả " + maxRetries + " lần đều thất bại: " + lastError.getMessage());
        lastError.printStackTrace();

        // Trả về kết quả fallback khi AI bị lỗi/timeout
        ResCvAnalysisDTO fallback = new ResCvAnalysisDTO();
        fallback.setOverallScore(0);
        fallback.setSummary(
                "Không thể phân tích CV lúc này. AI đang quá tải hoặc timeout. Vui lòng thử lại sau 1 phút.");
        fallback.setStrengths(List.of());
        fallback.setSuggestions(List.of());
        return fallback;
    }

    /**
     * Parse JSON response từ AI thành DTO.
     * Xử lý trường hợp AI trả về text kèm JSON (extract JSON block).
     */
    private ResCvAnalysisDTO parseAiResponse(String rawResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // Trích xuất JSON nếu AI trả về text kèm JSON
            String jsonStr = extractJsonFromResponse(rawResponse);

            JsonNode root = objectMapper.readTree(jsonStr);

            ResCvAnalysisDTO dto = new ResCvAnalysisDTO();
            dto.setOverallScore(root.path("overallScore").asInt(0));
            dto.setFormatScore(root.path("formatScore").asInt(0));
            dto.setContentScore(root.path("contentScore").asInt(0));
            dto.setKeywordScore(root.path("keywordScore").asInt(0));
            dto.setImpactScore(root.path("impactScore").asInt(0));
            dto.setSummary(root.path("summary").asText(""));

            // Parse strengths (JSON array → List<String>)
            List<String> strengths = new ArrayList<>();
            if (root.has("strengths") && root.get("strengths").isArray()) {
                for (JsonNode item : root.get("strengths")) {
                    strengths.add(item.asText());
                }
            }
            dto.setStrengths(strengths);

            // Parse suggestions (JSON array → List<Suggestion>)
            List<ResCvAnalysisDTO.Suggestion> suggestions = new ArrayList<>();
            if (root.has("suggestions") && root.get("suggestions").isArray()) {
                for (JsonNode item : root.get("suggestions")) {
                    ResCvAnalysisDTO.Suggestion s = new ResCvAnalysisDTO.Suggestion();
                    s.setCategory(item.path("category").asText(""));
                    s.setPriority(item.path("priority").asText("MEDIUM"));
                    s.setIssue(item.path("issue").asText(""));
                    s.setSuggestion(item.path("suggestion").asText(""));
                    suggestions.add(s);
                }
            }
            dto.setSuggestions(suggestions);

            return dto;

        } catch (Exception e) {
            System.out.println(">>> [CV Doctor] ⚠️ Lỗi parse JSON: " + e.getMessage());
            System.out.println(">>> [CV Doctor] Raw response: " + rawResponse);

            // Trả về kết quả mặc định khi parse lỗi
            ResCvAnalysisDTO fallback = new ResCvAnalysisDTO();
            fallback.setOverallScore(0);
            fallback.setSummary("Không thể phân tích CV lúc này. Vui lòng thử lại.");
            fallback.setStrengths(List.of());
            fallback.setSuggestions(List.of());
            return fallback;
        }
    }

    /**
     * Trích xuất JSON từ response của AI.
     * AI đôi khi trả về text kèm JSON block (```json ... ```),
     * hàm này sẽ extract phần JSON ra.
     */
    private String extractJsonFromResponse(String response) {
        // Trường hợp 1: Response bọc trong ```json ... ```
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }

        // Trường hợp 2: Response bọc trong ``` ... ```
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }

        // Trường hợp 3: Response bắt đầu bằng { (JSON thuần)
        int braceStart = response.indexOf("{");
        int braceEnd = response.lastIndexOf("}");
        if (braceStart >= 0 && braceEnd > braceStart) {
            return response.substring(braceStart, braceEnd + 1);
        }

        // Trường hợp 4: Trả nguyên response, hy vọng nó là JSON hợp lệ
        return response.trim();
    }

    /**
     * ===== HÀM CHÍNH: Pipeline phân tích CV end-to-end =====
     * 
     * Flow: Upload PDF → Extract text → Build prompt → Call AI → Parse response →
     * Save DB → Return DTO
     *
     * @param file - file PDF từ người dùng upload
     * @return ResCvAnalysisDTO - kết quả phân tích hoàn chỉnh
     */
    public ResCvAnalysisDTO analyzeCV(MultipartFile file) {

        // ======= BƯỚC 1: Lấy thông tin user đang đăng nhập =======
        String email = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để sử dụng tính năng này");
        }

        // ======= BƯỚC 2: Đọc nội dung PDF (đã viết Ngày 8) =======
        System.out.println(">>> [CV Doctor] Bước 1/4: Đọc PDF...");
        String cvText = extractTextFromPdf(file);

        // ======= BƯỚC 3: Gọi AI phân tích + parse kết quả (đã viết Ngày 9) =======
        System.out.println(">>> [CV Doctor] Bước 2/4: Gọi AI phân tích...");
        ResCvAnalysisDTO result = callAiAndParseResponse(cvText);

        // ======= BƯỚC 4: Lưu kết quả vào Database =======
        System.out.println(">>> [CV Doctor] Bước 3/4: Lưu vào database...");
        CvAnalysis entity = saveCvAnalysis(file, result, currentUser);

        // Set id và createdAt từ entity đã lưu
        result.setId(entity.getId());
        result.setCreatedAt(entity.getCreatedAt());
        result.setFileName(file.getOriginalFilename());

        System.out.println(">>> [CV Doctor] Bước 4/4: Hoàn thành! Score = " + result.getOverallScore());

        return result;
    }

    /**
     * Lưu kết quả phân tích CV vào database.
     */
    private CvAnalysis saveCvAnalysis(MultipartFile file, ResCvAnalysisDTO result, User user) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            CvAnalysis entity = new CvAnalysis();
            entity.setFileName(file.getOriginalFilename());
            entity.setOverallScore(result.getOverallScore());
            entity.setFormatScore(result.getFormatScore());
            entity.setContentScore(result.getContentScore());
            entity.setKeywordScore(result.getKeywordScore());
            entity.setImpactScore(result.getImpactScore());
            entity.setSummary(result.getSummary());
            entity.setUser(user);

            // Convert List → JSON String để lưu vào cột JSON
            entity.setStrengths(objectMapper.writeValueAsString(result.getStrengths()));
            entity.setSuggestions(objectMapper.writeValueAsString(result.getSuggestions()));

            return cvAnalysisRepository.save(entity);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi khi lưu kết quả phân tích: " + e.getMessage());
        }
    }

    /**
     * So sánh CV với Job Description và trả về % matching.
     *
     * @param resumeId - ID của resume cần matching
     * @return ResCvMatchDTO - kết quả matching
     */
    public ResCvMatchDTO matchCvWithJob(long resumeId) {
        // 1. Lấy Resume từ DB
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CV với ID: " + resumeId));

        Job job = resume.getJob();
        if (job == null) {
            throw new IllegalArgumentException("CV này chưa được ứng tuyển vào job nào");
        }

        // 2. Đọc PDF text từ CV (trực tiếp bằng PDFBox, không qua MultipartFile)
        String cvText;
        try {
            String baseUri = uploadFileBaseUri.replace("file:", "").replace("/", java.io.File.separator);
            java.io.File pdfFile = new java.io.File(baseUri + "resume" + java.io.File.separator + resume.getUrl());

            if (!pdfFile.exists()) {
                throw new IllegalArgumentException("Không tìm thấy file CV: " + resume.getUrl());
            }

            // Đọc PDF trực tiếp bằng PDFBox
            try (PDDocument document = Loader.loadPDF(pdfFile)) {
                PDFTextStripper stripper = new PDFTextStripper();
                cvText = stripper.getText(document);
                cvText = normalizeText(cvText);

                if (cvText.length() < 50) {
                    throw new IllegalArgumentException("CV không có đủ nội dung text để phân tích");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi đọc file CV: " + e.getMessage());
        }

        // 3. Xây dựng thông tin JD
        String jobSkills = job.getSkills() != null
                ? job.getSkills().stream().map(s -> s.getName()).collect(Collectors.joining(", "))
                : "Không xác định";

        String jobLevel = job.getLevel() != null ? job.getLevel().toString() : "Không xác định";

        // 4. Build prompt
        String promptText = CV_JD_MATCHING_PROMPT
                .replace("{jobName}", job.getName())
                .replace("{jobLevel}", jobLevel)
                .replace("{jobSkills}", jobSkills)
                .replace("{jobDescription}", job.getDescription() != null ? job.getDescription() : "")
                .replace("{cvText}", cvText);

        Prompt prompt = new Prompt(promptText);

        System.out.println(">>> [CV Matching] Đang so sánh CV với JD: " + job.getName());

        // Retry logic cho Groq rate limit
        String rawResponse = null;
        int maxRetries = 2;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                rawResponse = chatModel.call(prompt).getResult().getOutput().getContent();
                System.out.println(">>> [CV Matching] AI response: " + rawResponse.length() + " chars");
                break;
            } catch (Exception e) {
                System.out.println(">>> [CV Matching] ⚠️ Lần " + attempt + " thất bại: " + e.getMessage());
                if (attempt < maxRetries) {
                    System.out.println(">>> [CV Matching] ⏳ Đợi 5 giây rồi thử lại...");
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    throw new RuntimeException("AI không phản hồi sau " + maxRetries + " lần: " + e.getMessage());
                }
            }
        }

        // 6. Parse JSON response
        try {
            String jsonStr = extractJsonFromResponse(rawResponse);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonStr);

            ResCvMatchDTO result = new ResCvMatchDTO();
            result.setResumeId(resumeId);
            result.setJobId(job.getId());
            result.setJobName(job.getName());
            result.setMatchScore(root.path("matchScore").asInt(0));
            result.setSummary(root.path("summary").asText(""));
            result.setMatchedSkills(objectMapper.readValue(
                    root.path("matchedSkills").toString(), new TypeReference<List<String>>() {
                    }));
            result.setMissingSkills(objectMapper.readValue(
                    root.path("missingSkills").toString(), new TypeReference<List<String>>() {
                    }));
            result.setRecommendations(objectMapper.readValue(
                    root.path("recommendations").toString(), new TypeReference<List<String>>() {
                    }));

            // 7. Lưu kết quả vào Resume
            resume.setAiMatchScore(result.getMatchScore());
            resume.setAiMatchSummary(result.getSummary());
            resume.setAiMatchDetails(jsonStr);
            resumeRepository.save(resume);

            return result;

        } catch (Exception e) {
            System.out.println(">>> [CV Matching] ❌ Lỗi parse: " + e.getMessage());
            ResCvMatchDTO fallback = new ResCvMatchDTO();
            fallback.setResumeId(resumeId);
            fallback.setJobId(job.getId());
            fallback.setJobName(job.getName());
            fallback.setMatchScore(0);
            fallback.setSummary("Không thể đánh giá lúc này. Vui lòng thử lại sau.");
            fallback.setMatchedSkills(List.of());
            fallback.setMissingSkills(List.of());
            fallback.setRecommendations(List.of());
            return fallback;
        }
    }

    /**
     * Lấy lịch sử phân tích CV của user đang đăng nhập (phân trang).
     */
    public ResultPaginationDTO getCvAnalysisHistory(Pageable pageable) {
        String email = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập");
        }

        Page<CvAnalysis> page = cvAnalysisRepository
                .findByUserOrderByCreatedAtDesc(currentUser, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(page.getTotalPages());
        mt.setTotal(page.getTotalElements());
        rs.setMeta(mt);

        // Convert entity → DTO rút gọn (chỉ hiển thị tên file + điểm + ngày)
        List<ResCvHistoryDTO> list = page.getContent().stream()
                .map(item -> new ResCvHistoryDTO(
                        item.getId(),
                        item.getFileName(),
                        item.getOverallScore(),
                        item.getCreatedAt()))
                .collect(Collectors.toList());

        rs.setResult(list);
        return rs;
    }

    /**
     * Lấy chi tiết 1 kết quả phân tích CV theo ID.
     */
    public ResCvAnalysisDTO getCvAnalysisById(Long id) {
        CvAnalysis entity = cvAnalysisRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kết quả phân tích CV"));

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            ResCvAnalysisDTO dto = new ResCvAnalysisDTO();
            dto.setId(entity.getId());
            dto.setFileName(entity.getFileName());
            dto.setOverallScore(entity.getOverallScore());
            dto.setFormatScore(entity.getFormatScore());
            dto.setContentScore(entity.getContentScore());
            dto.setKeywordScore(entity.getKeywordScore());
            dto.setImpactScore(entity.getImpactScore());
            dto.setSummary(entity.getSummary());
            dto.setCreatedAt(entity.getCreatedAt());

            // Parse JSON string → List
            if (entity.getStrengths() != null) {
                dto.setStrengths(objectMapper.readValue(entity.getStrengths(),
                        new TypeReference<List<String>>() {
                        }));
            } else {
                dto.setStrengths(List.of());
            }

            if (entity.getSuggestions() != null) {
                dto.setSuggestions(objectMapper.readValue(entity.getSuggestions(),
                        new TypeReference<List<ResCvAnalysisDTO.Suggestion>>() {
                        }));
            } else {
                dto.setSuggestions(List.of());
            }

            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi đọc dữ liệu phân tích: " + e.getMessage());
        }
    }

}
