package vn.hoangtung.jobfind.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Skill;
import vn.hoangtung.jobfind.domain.Subscriber;
import vn.hoangtung.jobfind.repository.JobRepository;
import vn.hoangtung.jobfind.repository.SkillRepository;
import vn.hoangtung.jobfind.repository.SubscriberRepository;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.ai.AiFeatureUtils;
import vn.hoangtung.jobfind.util.ai.AiFeatureUtils.ChatIntent;
import vn.hoangtung.jobfind.util.ai.AiFeatureUtils.RankedJob;

@Service
public class AIService {

    private static final String CHATBOT_PROMPT_VERSION = "job-chat-v2";
    private static final int VECTOR_TOP_K = 12;
    private static final int MAX_RESULT_JOBS = 5;

    private final JobRepository jobRepository;
    private final VectorStore vectorStore;
    private final SkillRepository skillRepository;
    private final SubscriberRepository subscriberRepository;
    private final AiGatewayService aiGatewayService;

    public AIService(JobRepository jobRepository,
            VectorStore vectorStore,
            SkillRepository skillRepository,
            SubscriberRepository subscriberRepository,
            AiGatewayService aiGatewayService) {
        this.jobRepository = jobRepository;
        this.vectorStore = vectorStore;
        this.skillRepository = skillRepository;
        this.subscriberRepository = subscriberRepository;
        this.aiGatewayService = aiGatewayService;
    }

    public String syncJobData() {
        List<Job> allJobs = jobRepository.findAll();
        List<Document> documents = new ArrayList<>();

        for (Job job : allJobs) {
            String skillNames = AiFeatureUtils.jobSkillNames(job).stream()
                    .collect(Collectors.joining(", "));

            String companyName = job.getCompany() != null ? job.getCompany().getName() : "Chưa xác định";
            String companyDesc = job.getCompany() != null && job.getCompany().getDescription() != null
                    ? job.getCompany().getDescription()
                    : "";
            String companyAddress = job.getCompany() != null && job.getCompany().getAddress() != null
                    ? job.getCompany().getAddress()
                    : "";
            String level = job.getLevel() != null ? job.getLevel().toString() : "Không yêu cầu";

            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("=== THÔNG TIN CÔNG VIỆC ===\n");
            contentBuilder.append("ID: ").append(job.getId()).append("\n");
            contentBuilder.append("Vị trí: ").append(job.getName()).append("\n");
            contentBuilder.append("Công ty: ").append(companyName).append("\n");
            contentBuilder.append("Cấp độ: ").append(level).append("\n");
            contentBuilder.append("Mức lương: ").append(String.format("%.0f", job.getSalary())).append(" VNĐ\n");
            contentBuilder.append("Địa điểm: ").append(job.getLocation()).append("\n");
            contentBuilder.append("Kỹ năng yêu cầu: ").append(skillNames.isBlank() ? "Chưa cập nhật" : skillNames).append("\n");
            contentBuilder.append("Số lượng tuyển: ").append(job.getQuantity()).append(" người\n");
            contentBuilder.append("Trạng thái: ").append(job.isActive() ? "Đang tuyển" : "Đã đóng").append("\n");

            if (!companyDesc.isEmpty()) {
                contentBuilder.append("\n=== VỀ CÔNG TY ===\n");
                contentBuilder.append(companyDesc).append("\n");
            }
            if (!companyAddress.isEmpty()) {
                contentBuilder.append("Địa chỉ công ty: ").append(companyAddress).append("\n");
            }

            if (job.getDescription() != null && !job.getDescription().isBlank()) {
                contentBuilder.append("\n=== MÔ TẢ CÔNG VIỆC ===\n");
                contentBuilder.append(job.getDescription());
            }

            Map<String, Object> metadata = Map.of(
                    "doc_type", CvVectorService.DOC_TYPE_JOB,
                    "source_type", "JOB",
                    "job_id", job.getId(),
                    "company_name", companyName,
                    "level", level,
                    "salary", job.getSalary(),
                    "location", job.getLocation(),
                    "active", job.isActive());

            documents.add(new Document(String.valueOf(job.getId()), contentBuilder.toString(), metadata));
        }

        if (documents.isEmpty()) {
            return "Không có công việc nào để đồng bộ.";
        }

        vectorStore.add(documents);
        aiGatewayService.clearCache();
        return "Đã đồng bộ thành công " + documents.size() + " công việc lên Pinecone và làm mới cache ChatBot.";
    }

    public String chat(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "Vui lòng nhập câu hỏi cụ thể về việc làm.";
        }

        List<Job> activeJobs = jobRepository.findActiveJobsWithSkillsAndCompany();
        if (activeJobs.isEmpty()) {
            return "Hiện chưa có việc làm đang tuyển để tư vấn.";
        }

        Set<String> knownSkills = loadKnownSkills();
        Set<String> knownLocations = activeJobs.stream()
                .map(Job::getLocation)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> userSkills = resolveUserSkillPreferences();

        String processedQuery = preprocessQuery(userMessage);
        ChatIntent intent = AiFeatureUtils.parseChatIntent(processedQuery, knownLocations, knownSkills);

        List<Job> vectorCandidates = findVectorCandidates(processedQuery, activeJobs);
        List<RankedJob> rankedCandidates = AiFeatureUtils.rankJobs(intent, vectorCandidates, userSkills);

        List<RankedJob> exactMatches = rankedCandidates.stream()
                .filter(rankedJob -> AiFeatureUtils.matchesHardConstraints(intent, rankedJob.job()))
                .limit(MAX_RESULT_JOBS)
                .toList();

        List<RankedJob> finalJobs = !exactMatches.isEmpty()
                ? exactMatches
                : rankedCandidates.stream().limit(MAX_RESULT_JOBS).toList();

        if (finalJobs.isEmpty()) {
            return buildNoResultMessage(intent, activeJobs);
        }

        String promptText = buildChatPrompt(userMessage, intent, finalJobs, userSkills, !exactMatches.isEmpty());
        String cacheKey = aiGatewayService.fingerprint(
                CHATBOT_PROMPT_VERSION,
                SecurityUtil.getCurrentUserLogin().orElse("guest"),
                userMessage,
                finalJobs.stream().map(item -> String.valueOf(item.job().getId())).collect(Collectors.joining(",")));

        try {
            return aiGatewayService.callText(promptText, "ChatBot", cacheKey, Duration.ofMinutes(10));
        } catch (Exception e) {
            System.out.println(">>> [ChatBot] ⚠️ Dùng fallback do LLM lỗi: " + e.getMessage());
            return buildDeterministicResponse(finalJobs, !exactMatches.isEmpty());
        }
    }

    private Set<String> loadKnownSkills() {
        return AiFeatureUtils.buildKnownSkills(
                skillRepository.findAll().stream().map(Skill::getName).toList());
    }

    private Set<String> resolveUserSkillPreferences() {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        if (email.isBlank()) {
            return Set.of();
        }

        Subscriber subscriber = subscriberRepository.findByEmailWithSkills(email);
        if (subscriber == null || subscriber.getSkills() == null) {
            return Set.of();
        }

        return subscriber.getSkills().stream()
                .map(Skill::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Job> findVectorCandidates(String processedQuery, List<Job> activeJobs) {
        try {
            List<Document> similarDocs = findJobVectorDocuments(processedQuery);
            if (similarDocs == null || similarDocs.isEmpty()) {
                return activeJobs;
            }

            LinkedHashSet<Long> orderedIds = new LinkedHashSet<>();
            for (Document doc : similarDocs) {
                Long jobId = extractJobId(doc);
                if (jobId != null) {
                    orderedIds.add(jobId);
                }
            }

            if (orderedIds.isEmpty()) {
                return activeJobs;
            }

            Map<Long, Job> jobsById = activeJobs.stream()
                    .collect(Collectors.toMap(Job::getId, job -> job, (left, right) -> left, LinkedHashMap::new));

            List<Job> candidates = orderedIds.stream()
                    .map(jobsById::get)
                    .filter(Objects::nonNull)
                    .toList();

            if (!candidates.isEmpty()) {
                return candidates;
            }
        } catch (Exception e) {
            System.out.println(">>> [ChatBot] ⚠️ Vector search lỗi, fallback DB-only: " + e.getMessage());
        }
        return activeJobs;
    }

    private List<Document> findJobVectorDocuments(String processedQuery) {
        List<Document> filteredDocs = vectorStore.similaritySearch(
                SearchRequest.query(processedQuery)
                        .withTopK(VECTOR_TOP_K)
                        .withFilterExpression(CvVectorService.docTypeFilterExpression(CvVectorService.DOC_TYPE_JOB)));
        if (filteredDocs != null && !filteredDocs.isEmpty()) {
            return filteredDocs;
        }
        return vectorStore.similaritySearch(SearchRequest.query(processedQuery).withTopK(VECTOR_TOP_K));
    }

    private Long extractJobId(Document doc) {
        if (doc == null) {
            return null;
        }

        Object docType = doc.getMetadata().get("doc_type");
        if (docType != null && CvVectorService.DOC_TYPE_CV.equalsIgnoreCase(String.valueOf(docType))) {
            return null;
        }

        Object metadataJobId = doc.getMetadata().get("job_id");
        if (metadataJobId instanceof Number number) {
            return number.longValue();
        }
        if (metadataJobId instanceof String metadataString && !metadataString.isBlank()) {
            try {
                return Long.parseLong(metadataString.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            return Long.parseLong(doc.getId());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildChatPrompt(String userMessage,
            ChatIntent intent,
            List<RankedJob> rankedJobs,
            Set<String> userSkills,
            boolean exactMatchMode) {
        StringBuilder jobsBlock = new StringBuilder();
        for (RankedJob rankedJob : rankedJobs) {
            Job job = rankedJob.job();
            String companyName = job.getCompany() != null ? job.getCompany().getName() : "Chưa cập nhật";
            String skillNames = String.join(", ", AiFeatureUtils.jobSkillNames(job));
            jobsBlock.append("JOB_ID: ").append(job.getId()).append("\n");
            jobsBlock.append("Vị trí: ").append(job.getName()).append("\n");
            jobsBlock.append("Công ty: ").append(companyName).append("\n");
            jobsBlock.append("Level: ").append(job.getLevel() != null ? job.getLevel() : "Không rõ").append("\n");
            jobsBlock.append("Lương: ").append(String.format("%.0f", job.getSalary())).append(" VNĐ\n");
            jobsBlock.append("Địa điểm: ").append(job.getLocation()).append("\n");
            jobsBlock.append("Skills: ").append(skillNames.isBlank() ? "Chưa cập nhật" : skillNames).append("\n");
            jobsBlock.append("Lý do match: ").append(String.join("; ", rankedJob.reasons())).append("\n");
            jobsBlock.append("Mô tả rút gọn: ").append(aiGatewayService.sanitizeForPrompt(job.getDescription(), 420)).append("\n");
            jobsBlock.append("-----\n");
        }

        String profileBlock = userSkills.isEmpty()
                ? "Người dùng chưa có hồ sơ kỹ năng cá nhân hóa."
                : "Ưu tiên kỹ năng của người dùng: " + String.join(", ", userSkills);

        return """
                Bạn là JobFind AI, trợ lý tư vấn việc làm IT.
                Chỉ được dùng đúng dữ liệu job bên dưới. Không được bịa công ty, kỹ năng, lương hay địa điểm.
                Nếu dữ liệu không có thì nói rõ là chưa có dữ liệu.

                %s

                %s

                %s

                %s

                YÊU CẦU TRẢ LỜI:
                1. Nếu có kết quả phù hợp cao, liệt kê 3-5 job tốt nhất.
                2. Mỗi job phải có: tên vị trí, công ty, lương, địa điểm, kỹ năng, lý do phù hợp.
                3. Nếu đây chỉ là match gần đúng, nói rõ "kết quả gần đúng" ở câu mở đầu.
                4. Không được lặp lại job trùng nhau.
                5. Trả lời bằng tiếng Việt, ngắn gọn, giàu thông tin, không dùng markdown code block.
                """.formatted(
                aiGatewayService.boundedBlock("USER_QUERY", userMessage, 500),
                aiGatewayService.boundedBlock("MATCH_CRITERIA",
                        """
                                exactMatchMode=%s
                                requestedLocation=%s
                                requestedMinSalary=%s
                                requestedSkills=%s
                                keywords=%s
                                """.formatted(
                                exactMatchMode,
                                intent.location() == null ? "none" : intent.location(),
                                intent.minSalary() == null ? "none" : intent.minSalary(),
                                intent.requestedSkills().isEmpty() ? "none" : String.join(", ", intent.requestedSkills()),
                                intent.keywords().isEmpty() ? "none" : String.join(", ", intent.keywords())),
                        600),
                aiGatewayService.boundedBlock("USER_PROFILE", profileBlock, 400),
                aiGatewayService.boundedBlock("JOB_CANDIDATES", jobsBlock.toString(), 3500));
    }

    private String buildDeterministicResponse(List<RankedJob> rankedJobs, boolean exactMatchMode) {
        StringBuilder response = new StringBuilder();
        response.append(exactMatchMode
                ? "Dưới đây là các công việc phù hợp nhất với yêu cầu của bạn:\n\n"
                : "Dưới đây là các kết quả gần đúng tốt nhất với yêu cầu của bạn:\n\n");

        for (RankedJob rankedJob : rankedJobs) {
            Job job = rankedJob.job();
            String companyName = job.getCompany() != null ? job.getCompany().getName() : "Chưa cập nhật";
            String skills = String.join(", ", AiFeatureUtils.jobSkillNames(job));
            response.append("• ").append(job.getName()).append(" - ").append(companyName).append("\n");
            response.append("  Lương: ").append(String.format("%.0f", job.getSalary())).append(" VNĐ").append("\n");
            response.append("  Địa điểm: ").append(job.getLocation()).append("\n");
            response.append("  Kỹ năng: ").append(skills.isBlank() ? "Chưa cập nhật" : skills).append("\n");
            response.append("  Lý do phù hợp: ").append(String.join("; ", rankedJob.reasons())).append("\n\n");
        }

        return response.toString().trim();
    }

    private String buildNoResultMessage(ChatIntent intent, List<Job> activeJobs) {
        List<String> suggestions = new ArrayList<>();
        if (intent.location() != null) {
            suggestions.add("bỏ bớt ràng buộc địa điểm `" + intent.location() + "`");
        }
        if (intent.minSalary() != null) {
            suggestions.add("giảm mức lương tối thiểu đang đặt");
        }
        if (!intent.requestedSkills().isEmpty()) {
            suggestions.add("thử tìm theo 1-2 kỹ năng chính thay vì tất cả kỹ năng");
        }

        String popularSkills = activeJobs.stream()
                .flatMap(job -> AiFeatureUtils.jobSkillNames(job).stream())
                .filter(Objects::nonNull)
                .limit(6)
                .collect(Collectors.joining(", "));

        StringBuilder response = new StringBuilder("Hiện tôi chưa tìm thấy job khớp đủ điều kiện bạn đang hỏi.");
        if (!suggestions.isEmpty()) {
            response.append("\nBạn có thể thử: ");
            response.append(String.join("; ", suggestions)).append(".");
        }
        if (!popularSkills.isBlank()) {
            response.append("\nMột số kỹ năng đang có nhiều job hơn trong hệ thống: ").append(popularSkills).append(".");
        }
        return response.toString();
    }

    private String preprocessQuery(String query) {
        String processed = query.toLowerCase().trim();
        processed = processed.replaceAll("\\bfe\\b", "frontend");
        processed = processed.replaceAll("\\bbe\\b", "backend");
        processed = processed.replaceAll("\\bfs\\b", "fullstack");
        processed = processed.replaceAll("\\bjs\\b", "javascript");
        processed = processed.replaceAll("\\bts\\b", "typescript");

        if (processed.contains("java") && !processed.contains("javascript")) {
            processed += " spring boot backend";
        }
        if (processed.contains("react") && !processed.contains("native")) {
            processed += " frontend typescript";
        }
        if (processed.contains("node")) {
            processed += " backend javascript api";
        }
        return processed;
    }
}
