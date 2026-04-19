package vn.hoangtung.jobfind.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Skill;
import vn.hoangtung.jobfind.repository.JobRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AIService {

    private final JobRepository jobRepository;
    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    public AIService(JobRepository jobRepository, VectorStore vectorStore, ChatModel chatModel) {
        this.jobRepository = jobRepository;
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    // 1. Hàm đồng bộ - ENHANCED với Company info và data đầy đủ
    public String syncJobData() {
        List<Job> allJobs = jobRepository.findAll();
        List<Document> documents = new ArrayList<>();

        for (Job job : allJobs) {
            // Lấy danh sách skills
            String skillNames = job.getSkills().stream()
                    .map(Skill::getName)
                    .collect(Collectors.joining(", "));

            // Lấy thông tin company (nếu có)
            String companyName = job.getCompany() != null ? job.getCompany().getName() : "Chưa xác định";
            String companyDesc = job.getCompany() != null && job.getCompany().getDescription() != null
                    ? job.getCompany().getDescription()
                    : "";
            String companyAddress = job.getCompany() != null && job.getCompany().getAddress() != null
                    ? job.getCompany().getAddress()
                    : "";

            // Format level
            String level = job.getLevel() != null ? job.getLevel().toString() : "Không yêu cầu";

            // Content với cấu trúc rõ ràng hơn để LLM dễ parse
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("=== THÔNG TIN CÔNG VIỆC ===\n");
            contentBuilder.append("Vị trí: ").append(job.getName()).append("\n");
            contentBuilder.append("Công ty: ").append(companyName).append("\n");
            contentBuilder.append("Cấp độ: ").append(level).append("\n");
            contentBuilder.append("Mức lương: ").append(String.format("%.0f", job.getSalary())).append(" VNĐ\n");
            contentBuilder.append("Địa điểm: ").append(job.getLocation()).append("\n");
            contentBuilder.append("Kỹ năng yêu cầu: ").append(skillNames).append("\n");
            contentBuilder.append("Số lượng tuyển: ").append(job.getQuantity()).append(" người\n");
            contentBuilder.append("Trạng thái: ").append(job.isActive() ? "Đang tuyển" : "Đã đóng").append("\n");

            if (!companyDesc.isEmpty()) {
                contentBuilder.append("\n=== VỀ CÔNG TY ===\n");
                contentBuilder.append(companyDesc).append("\n");
                if (!companyAddress.isEmpty()) {
                    contentBuilder.append("Địa chỉ công ty: ").append(companyAddress).append("\n");
                }
            }

            contentBuilder.append("\n=== MÔ TẢ CÔNG VIỆC ===\n");
            contentBuilder.append(job.getDescription());

            // Metadata đầy đủ hơn để filter
            Map<String, Object> metadata = Map.of(
                    "job_id", job.getId(),
                    "company_name", companyName,
                    "level", level,
                    "salary", job.getSalary(),
                    "location", job.getLocation(),
                    "active", job.isActive());

            // Dùng job ID làm document ID để tránh duplicate khi sync nhiều lần
            Document doc = new Document(String.valueOf(job.getId()), contentBuilder.toString(), metadata);
            documents.add(doc);
        }

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
            return "Đã đồng bộ thành công " + documents.size() + " công việc (bao gồm thông tin công ty) lên Pinecone!";
        }
        return "Không có công việc nào để đồng bộ.";
    }

    // 2. Hàm Chat - ADVANCED với Few-Shot Learning và Query Preprocessing
    public String chat(String userMessage) {
        System.out.println(">>> [1] Tìm kiếm vector DB: " + userMessage);

        // ENHANCED: Query preprocessing để cải thiện search
        String processedQuery = preprocessQuery(userMessage);
        System.out.println(">>> [1.1] Processed query: " + processedQuery);

        // TopK = 12 để có nhiều candidates cho reranking
        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.query(processedQuery).withTopK(12));

        System.out.println(">>> [2] Tìm thấy " + similarDocs.size() + " kết quả");

        if (similarDocs.isEmpty()) {
            return "Xin lỗi, tôi không tìm thấy công việc phù hợp với yêu cầu của bạn. " +
                    "Hãy thử:\n" +
                    "- Mô tả về vị trí công việc (ví dụ: 'Java developer', 'React Native')\n" +
                    "- Công nghệ bạn quan tâm (ví dụ: 'tìm việc Node.js')\n" +
                    "- Địa điểm bạn muốn làm việc (ví dụ: 'công việc ở Hà Nội')";
        }

        // Filter active jobs và limit
        List<Document> activeDocs = similarDocs.stream()
                .filter(doc -> {
                    Object active = doc.getMetadata().get("active");
                    return active == null || (active instanceof Boolean && (Boolean) active);
                })
                .limit(8)
                .toList();

        if (activeDocs.isEmpty()) {
            activeDocs = similarDocs.stream().limit(8).toList();
        }

        // ENHANCED: Format context với numbering để LLM dễ reference
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < activeDocs.size(); i++) {
            contextBuilder.append(String.format("[CÔNG VIỆC #%d]\n", i + 1));
            contextBuilder.append(activeDocs.get(i).getContent());
            if (i < activeDocs.size() - 1) {
                contextBuilder.append("\n\n" + "=".repeat(50) + "\n\n");
            }
        }
        String context = contextBuilder.toString();

        // ADVANCED PROMPT với Few-Shot Examples
        String template = """
                Bạn là JobFind AI - chuyên gia tư vấn việc làm IT hàng đầu tại Việt Nam.

                DỮ LIỆU CÔNG VIỆC:
                {context}

                CÂU HỎI CỦA NGƯỜI DÙNG: {question}

                HƯỚNG DẪN TRẢ LỜI (QUAN TRỌNG):

                1. PHÂN TÍCH câu hỏi:
                   - Xác định CHÍNH XÁC điều người dùng muốn (vị trí? skills? địa điểm? lương?)
                   - Chú ý các từ khóa: "yêu cầu", "cần", "phải có", "tìm", "có"

                2. MATCHING LOGIC:
                   a) Nếu hỏi về VỊ TRÍ/CÔNG NGHỆ:
                      → Tìm jobs có tên vị trí CHỨA từ khóa
                      → Hoặc có công nghệ đó trong skills/description

                   b) Nếu hỏi về KỸ NĂNG CỤ THỂ:
                      → Kiểm tra "Kỹ năng yêu cầu: ..."
                      → CHỈ liệt kê jobs CÓ skill đó

                   c) Nếu hỏi về LƯƠNG:
                      → Đọc "Mức lương: XXX VNĐ"
                      → So sánh số liệu (1 triệu = 1.000.000)
                      → CHỈ liệt kê jobs THỎA MÃN điều kiện

                   d) Nếu hỏi về ĐỊA ĐIỂM:
                      → Kiểm tra "Địa điểm: ..."
                      → Match tên thành phố/tỉnh

                3. FORMAT TRẢ LỜI:
                   - Nếu TÌM THẤY: Liệt kê 3-5 jobs PHÙ HỢP NHẤT
                     Format mỗi job:
                     ```
                     🔹 [Tên vị trí] - [Công ty]
                         Lương: [X triệu VNĐ]
                         Địa điểm: [Thành phố]
                         Kỹ năng: [Skill 1, Skill 2, ...]
                     ```

                   - Nếu KHÔNG TÌM THẤY: Giải thích rõ tại sao + gợi ý thay thế

                4. LƯU Ý ĐẶC BIỆT:
                   - KHÔNG bịa đặt thông tin không có trong dữ liệu
                   - Nếu không chắc chắn, NÓI THẲNG "Tôi không chắc chắn..."
                   - Ưu tiên jobs có NHIỀU tiêu chí match nhất

                VÍ DỤ CỤ THỂ (Few-Shot Learning):

                Ví dụ 1:
                Q: "Tìm việc React Native"
                A: "Dưới đây là các công việc React Native phù hợp:

                🔹 Senior React Native - Zalo (VNG)
                      Lương: 50.000.000 VNĐ
                      Địa điểm: TP Hồ Chí Minh
                      Kỹ năng: Node.js, TypeScript, AWS, CI/CD

                🔹 React Native Developer - FPT Software
                    Lương: 25.000.000 VNĐ
                    Địa điểm: Hà Nội
                    Kỹ năng: React Native, Redux, Firebase"

                Ví dụ 2:
                Q: "Công việc yêu cầu kỹ năng Java"
                A: "Các công việc yêu cầu Java:

                🔹 Java Solution Architect - VNPT Technology
                    Lương: 70.000.000 VNĐ
                    Địa điểm: Hà Nội
                    Kỹ năng: Java, Spring Boot, Microservices, Docker"

                Ví dụ 3:
                Q: "Tìm việc lương 60 triệu"
                A: "Các công việc có mức lương từ 60 triệu trở lên:

                🔹 Java Solution Architect - VNPT Technology
                   Lương: 70.000.000 VNĐ
                   Địa điểm: Hà Nội
                   Kỹ năng: Java, Spring Boot, Microservices"

                Ví dụ 4:
                Q: "Tìm việc Python"
                A: "Xin lỗi, trong dữ liệu hiện tại tôi không tìm thấy công việc yêu cầu Python.
                Bạn có thể thử:
                - Tìm việc Backend developer (có thể sử dụng Python)
                - Tìm việc Data Engineer (thường dùng Python)
                - Hoặc kiểm tra lại sau khi có thêm dữ liệu mới"

                QUY ĐỔI TIỀN TỆ:
                - 1 triệu = 1.000.000 VNĐ
                - 15 triệu = 15.000.000 VNĐ
                - 25 triệu = 25.000.000 VNĐ
                - 50 triệu = 50.000.000 VNĐ
                - 60 triệu = 60.000.000 VNĐ
                - 70 triệu = 70.000.000 VNĐ

                BẮT ĐẦU TRẢ LỜI:
                (Phân tích câu hỏi → Match với dữ liệu → Format response theo ví dụ)
                """;

        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(Map.of(
                "context", context,
                "question", userMessage));

        System.out.println(">>> [3] Gọi LLM API...");

        // Retry logic cho Groq rate limit
        String response = null;
        int maxRetries = 2;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                response = chatModel.call(prompt).getResult().getOutput().getContent();
                break;
            } catch (Exception e) {
                System.out.println(">>> [Chat] ⚠️ Lần " + attempt + " thất bại: " + e.getMessage());
                if (attempt < maxRetries) {
                    System.out.println(">>> [Chat] ⏳ Đợi 5 giây rồi thử lại...");
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    return "Xin lỗi, hệ thống AI đang quá tải. Vui lòng thử lại sau ít phút.";
                }
            }
        }

        System.out.println(">>> [4] Hoàn thành!");

        return response;
    }

    // Helper: Query preprocessing để cải thiện search
    private String preprocessQuery(String query) {
        String processed = query.toLowerCase().trim();

        // Expand common abbreviations
        processed = processed.replaceAll("\\bfe\\b", "frontend");
        processed = processed.replaceAll("\\bbe\\b", "backend");
        processed = processed.replaceAll("\\bfs\\b", "fullstack");
        processed = processed.replaceAll("\\bjs\\b", "javascript");
        processed = processed.replaceAll("\\bts\\b", "typescript");

        // Add related terms for better matching
        if (processed.contains("java") && !processed.contains("javascript")) {
            processed += " spring boot";
        }
        if (processed.contains("react") && !processed.contains("native")) {
            processed += " frontend";
        }
        if (processed.contains("node")) {
            processed += " backend javascript";
        }

        return processed;
    }
}