package vn.hoangtung.jobfind.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoangtung.jobfind.domain.CvAnalysis;
import vn.hoangtung.jobfind.domain.CvChunk;
import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Resume;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.repository.CvChunkRepository;
import vn.hoangtung.jobfind.util.ai.AiFeatureUtils;
import vn.hoangtung.jobfind.util.ai.ParsedCv;
import vn.hoangtung.jobfind.util.ai.ParsedCv.EducationEntry;
import vn.hoangtung.jobfind.util.ai.ParsedCv.ProjectEntry;
import vn.hoangtung.jobfind.util.ai.ParsedCv.SkillEvidence;
import vn.hoangtung.jobfind.util.ai.ParsedCv.WorkEntry;

@Service
public class CvVectorService {

    public static final String DOC_TYPE_JOB = "JOB";
    public static final String DOC_TYPE_CV = "CV";
    public static final String SOURCE_CV_ANALYSIS = "CV_ANALYSIS";
    public static final String SOURCE_RESUME = "RESUME";

    private static final int MAX_CHUNK_CHARS = 1400;
    private static final int MAX_PROMPT_CHUNKS = 7;
    private static final int MAX_VECTOR_TEXT_CHARS = 1800;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d .()/-]{7,}\\d)(?!\\d)");

    private final VectorStore vectorStore;
    private final CvChunkRepository cvChunkRepository;
    private final AiGatewayService aiGatewayService;

    public CvVectorService(
            VectorStore vectorStore,
            CvChunkRepository cvChunkRepository,
            AiGatewayService aiGatewayService) {
        this.vectorStore = vectorStore;
        this.cvChunkRepository = cvChunkRepository;
        this.aiGatewayService = aiGatewayService;
    }

    public List<CvChunkDraft> chunkCv(ParsedCv parsedCv, String cvText) {
        List<CvChunkDraft> drafts = new ArrayList<>();
        if (parsedCv != null) {
            addProfileChunk(drafts, parsedCv);
            addSkillsChunk(drafts, parsedCv);
            addExperienceChunks(drafts, parsedCv);
            addProjectChunks(drafts, parsedCv);
            addEducationChunk(drafts, parsedCv);
        }

        if (drafts.isEmpty()) {
            addFallbackChunks(drafts, cvText);
        }

        List<CvChunkDraft> indexed = new ArrayList<>();
        for (CvChunkDraft draft : drafts) {
            indexed.add(new CvChunkDraft(
                    draft.chunkType(),
                    indexed.size(),
                    draft.text(),
                    draft.wordCount()));
        }
        return indexed;
    }

    public String buildPromptChunkContext(ParsedCv parsedCv, String cvText) {
        return chunkCv(parsedCv, cvText).stream()
                .limit(MAX_PROMPT_CHUNKS)
                .map(chunk -> "[%s] %s".formatted(chunk.chunkType(), chunk.text()))
                .collect(Collectors.joining("\n---\n"));
    }

    @Transactional
    public boolean indexResumeSafely(
            Resume resume,
            ParsedCv parsedCv,
            String cvText,
            User user,
            String contentHash) {
        try {
            indexResume(resume, parsedCv, cvText, user, contentHash);
            return true;
        } catch (Exception e) {
            markResumeVectorError(resume, e.getMessage());
            System.out.println(">>> [CV Vector] Cannot index resume vectors: " + e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean indexCvAnalysisSafely(
            CvAnalysis analysis,
            ParsedCv parsedCv,
            String cvText,
            User user,
            String contentHash) {
        try {
            indexCvAnalysis(analysis, parsedCv, cvText, user, contentHash);
            return true;
        } catch (Exception e) {
            System.out.println(">>> [CV Vector] Cannot index analysis vectors: " + e.getMessage());
            return false;
        }
    }

    @Transactional
    public List<CvChunk> indexResume(
            Resume resume,
            ParsedCv parsedCv,
            String cvText,
            User user,
            String contentHash) {
        if (resume == null || resume.getId() <= 0 || cvText == null || cvText.isBlank()) {
            return List.of();
        }

        String resolvedHash = resolveContentHash(contentHash, cvText);
        if (resume.isVectorized()
                && resolvedHash.equals(resume.getCvVectorContentHash())
                && !cvChunkRepository.findByResumeIdAndContentHash(resume.getId(), resolvedHash).isEmpty()) {
            return cvChunkRepository.findByResumeIdAndContentHash(resume.getId(), resolvedHash);
        }

        deleteResumeVectors(resume.getId());
        List<CvChunkDraft> drafts = chunkCv(parsedCv, cvText);
        if (drafts.isEmpty()) {
            markResumeVectorError(resume, "No readable CV chunks");
            return List.of();
        }

        List<Document> documents = new ArrayList<>();
        for (CvChunkDraft draft : drafts) {
            String vectorId = resumeVectorId(resume.getId(), resolvedHash, draft.chunkIndex());
            documents.add(new Document(vectorId, draft.text(), metadataForResumeChunk(
                    resume,
                    user,
                    parsedCv,
                    resolvedHash,
                    draft)));
        }

        vectorStore.add(documents);

        List<CvChunk> chunks = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            chunks.add(toEntity(documents.get(i), drafts.get(i), resume, null, user, resolvedHash));
        }
        chunks = cvChunkRepository.saveAll(chunks);

        resume.setVectorized(true);
        resume.setVectorizedAt(Instant.now());
        resume.setCvVectorContentHash(resolvedHash);
        resume.setCvChunkCount(chunks.size());
        resume.setCvVectorError(null);
        return chunks;
    }

    @Transactional
    public List<CvChunk> indexCvAnalysis(
            CvAnalysis analysis,
            ParsedCv parsedCv,
            String cvText,
            User user,
            String contentHash) {
        if (analysis == null || analysis.getId() == null || cvText == null || cvText.isBlank()) {
            return List.of();
        }

        String resolvedHash = resolveContentHash(contentHash, cvText);
        deleteAnalysisVectors(analysis.getId());
        List<CvChunkDraft> drafts = chunkCv(parsedCv, cvText);
        if (drafts.isEmpty()) {
            return List.of();
        }

        List<Document> documents = new ArrayList<>();
        for (CvChunkDraft draft : drafts) {
            String vectorId = analysisVectorId(analysis.getId(), resolvedHash, draft.chunkIndex());
            documents.add(new Document(vectorId, draft.text(), metadataForAnalysisChunk(
                    analysis,
                    user,
                    parsedCv,
                    resolvedHash,
                    draft)));
        }

        vectorStore.add(documents);

        List<CvChunk> chunks = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            chunks.add(toEntity(documents.get(i), drafts.get(i), null, analysis, user, resolvedHash));
        }
        return cvChunkRepository.saveAll(chunks);
    }

    @Transactional
    public void deleteResumeVectorsSafely(long resumeId) {
        try {
            deleteResumeVectors(resumeId);
        } catch (Exception e) {
            System.out.println(">>> [CV Vector] Cannot delete resume vectors: " + e.getMessage());
        }
    }

    public SemanticChunkMatch findRelevantResumeChunks(Resume resume, Job job, int topK) {
        if (resume == null || resume.getId() <= 0 || job == null) {
            return SemanticChunkMatch.unavailable();
        }
        return searchResumeChunks(resume.getId(), buildJobSemanticQuery(job), topK);
    }

    public List<String> findRelevantResumeChunkTexts(Resume resume, String query, int topK) {
        if (resume == null || resume.getId() <= 0 || query == null || query.isBlank()) {
            return List.of();
        }
        return searchResumeChunks(resume.getId(), query, topK).documents().stream()
                .map(Document::getContent)
                .filter(Objects::nonNull)
                .map(this::shortenEvidence)
                .toList();
    }

    private SemanticChunkMatch searchResumeChunks(long resumeId, String query, int topK) {
        if (query == null || query.isBlank()) {
            return SemanticChunkMatch.unavailable();
        }

        try {
            List<Document> docs = vectorStore.similaritySearch(SearchRequest.query(query)
                    .withTopK(Math.max(1, topK))
                    .withFilterExpression(resumeChunkFilterExpression(resumeId)));
            if (docs == null || docs.isEmpty()) {
                return SemanticChunkMatch.unavailable();
            }

            int score = scoreFromDocuments(docs);
            List<String> evidence = docs.stream()
                    .limit(4)
                    .map(this::evidenceFromDocument)
                    .filter(value -> !value.isBlank())
                    .toList();
            return new SemanticChunkMatch(score, true, evidence, docs);
        } catch (Exception e) {
            System.out.println(">>> [CV Vector] Resume chunk search failed: " + e.getMessage());
            return SemanticChunkMatch.unavailable();
        }
    }

    static Filter.Expression docTypeFilterExpression(String docType) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        return builder.eq("doc_type", docType).build();
    }

    static Filter.Expression resumeChunkFilterExpression(long resumeId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        return builder.and(
                builder.eq("doc_type", DOC_TYPE_CV),
                builder.eq("resume_id", resumeId))
                .build();
    }

    private void deleteResumeVectors(long resumeId) {
        List<CvChunk> chunks = cvChunkRepository.findByResumeId(resumeId);
        deleteVectors(chunks);
        cvChunkRepository.deleteByResumeId(resumeId);
    }

    private void deleteAnalysisVectors(long analysisId) {
        List<CvChunk> chunks = cvChunkRepository.findByCvAnalysisId(analysisId);
        deleteVectors(chunks);
        cvChunkRepository.deleteByCvAnalysisId(analysisId);
    }

    private void deleteVectors(List<CvChunk> chunks) {
        List<String> ids = chunks.stream()
                .map(CvChunk::getVectorId)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (!ids.isEmpty()) {
            Optional<Boolean> ignored = vectorStore.delete(ids);
        }
    }

    private void addProfileChunk(List<CvChunkDraft> drafts, ParsedCv parsedCv) {
        String content = joinLines(
                "CV profile",
                label("Headline", parsedCv.headline()),
                label("Summary", parsedCv.summary()),
                parsedCv.contact().location().isBlank() ? "" : label("Location", parsedCv.contact().location()));
        addChunk(drafts, "CV_PROFILE", content);
    }

    private void addSkillsChunk(List<CvChunkDraft> drafts, ParsedCv parsedCv) {
        if (parsedCv.skills().isEmpty()) {
            return;
        }
        String skills = parsedCv.skills().stream()
                .map(this::skillLine)
                .collect(Collectors.joining("\n"));
        addChunk(drafts, "CV_SKILLS", "CV skills\n" + skills);
    }

    private void addExperienceChunks(List<CvChunkDraft> drafts, ParsedCv parsedCv) {
        int index = 1;
        for (WorkEntry entry : parsedCv.experience()) {
            String content = joinLines(
                    "CV work experience " + index,
                    label("Role", entry.role()),
                    label("Company", entry.company()),
                    label("Period", joinInline(entry.startDate(), entry.endDate())),
                    entry.durationMonths() > 0 ? label("Duration months", String.valueOf(entry.durationMonths())) : "",
                    label("Technologies", String.join(", ", entry.technologies())),
                    label("Bullets", String.join(" ", entry.bullets())),
                    entry.hasMetrics() ? "Has quantified impact" : "");
            addChunk(drafts, "CV_EXPERIENCE", content);
            index++;
        }
    }

    private void addProjectChunks(List<CvChunkDraft> drafts, ParsedCv parsedCv) {
        int index = 1;
        for (ProjectEntry entry : parsedCv.projects()) {
            String content = joinLines(
                    "CV project " + index,
                    label("Name", entry.name()),
                    label("Role", entry.role()),
                    label("Description", entry.description()),
                    label("Technologies", String.join(", ", entry.technologies())),
                    label("Bullets", String.join(" ", entry.bullets())),
                    entry.hasMetrics() ? "Has quantified impact" : "");
            addChunk(drafts, "CV_PROJECT", content);
            index++;
        }
    }

    private void addEducationChunk(List<CvChunkDraft> drafts, ParsedCv parsedCv) {
        List<String> entries = new ArrayList<>();
        for (EducationEntry entry : parsedCv.education()) {
            entries.add(joinInline(entry.school(), entry.degree(), entry.major(), entry.gpa()));
        }
        entries.addAll(parsedCv.certifications());
        entries.addAll(parsedCv.languages());
        addChunk(drafts, "CV_EDUCATION", "CV education and credentials\n" + String.join("\n", entries));
    }

    private void addFallbackChunks(List<CvChunkDraft> drafts, String cvText) {
        String sanitized = sanitizeChunkText(cvText);
        if (sanitized.isBlank()) {
            return;
        }

        String[] paragraphs = sanitized.split("\\R\\s*\\R");
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String normalized = AiFeatureUtils.normalizeWhitespace(paragraph);
            if (normalized.isBlank()) {
                continue;
            }
            if (current.length() + normalized.length() > MAX_CHUNK_CHARS && !current.isEmpty()) {
                addChunk(drafts, "CV_RAW", current.toString());
                current.setLength(0);
            }
            current.append(normalized).append("\n");
        }
        if (!current.isEmpty()) {
            addChunk(drafts, "CV_RAW", current.toString());
        }
    }

    private void addChunk(List<CvChunkDraft> drafts, String chunkType, String text) {
        String cleaned = trimToMax(sanitizeChunkText(text), MAX_CHUNK_CHARS);
        if (cleaned.isBlank() || AiFeatureUtils.wordCount(cleaned) < 3) {
            return;
        }
        drafts.add(new CvChunkDraft(chunkType, drafts.size(), cleaned, AiFeatureUtils.wordCount(cleaned)));
    }

    private Map<String, Object> metadataForResumeChunk(
            Resume resume,
            User user,
            ParsedCv parsedCv,
            String contentHash,
            CvChunkDraft draft) {
        Map<String, Object> metadata = baseCvMetadata(user, parsedCv, contentHash, draft);
        metadata.put("source_type", SOURCE_RESUME);
        metadata.put("resume_id", resume.getId());
        if (resume.getJob() != null) {
            metadata.put("job_id", resume.getJob().getId());
        }
        return metadata;
    }

    private Map<String, Object> metadataForAnalysisChunk(
            CvAnalysis analysis,
            User user,
            ParsedCv parsedCv,
            String contentHash,
            CvChunkDraft draft) {
        Map<String, Object> metadata = baseCvMetadata(user, parsedCv, contentHash, draft);
        metadata.put("source_type", SOURCE_CV_ANALYSIS);
        metadata.put("analysis_id", analysis.getId());
        return metadata;
    }

    private Map<String, Object> baseCvMetadata(
            User user,
            ParsedCv parsedCv,
            String contentHash,
            CvChunkDraft draft) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("doc_type", DOC_TYPE_CV);
        metadata.put("content_hash", contentHash);
        metadata.put("chunk_type", draft.chunkType());
        metadata.put("chunk_index", draft.chunkIndex());
        metadata.put("word_count", draft.wordCount());
        if (user != null && user.getId() > 0) {
            metadata.put("user_id", user.getId());
        }
        if (parsedCv != null) {
            metadata.put("candidate_level", AiFeatureUtils.analyzeCv(parsedCv, "", parsedCv.skillNames()).inferredLevel());
            metadata.put("skill_names", String.join(", ", parsedCv.skillNames()));
        }
        return metadata;
    }

    private CvChunk toEntity(
            Document document,
            CvChunkDraft draft,
            Resume resume,
            CvAnalysis analysis,
            User user,
            String contentHash) {
        CvChunk entity = new CvChunk();
        entity.setResume(resume);
        entity.setCvAnalysis(analysis);
        entity.setUser(user);
        entity.setVectorId(document.getId());
        entity.setContentHash(contentHash);
        entity.setChunkType(draft.chunkType());
        entity.setChunkIndex(draft.chunkIndex());
        entity.setWordCount(draft.wordCount());
        entity.setText(draft.text());
        return entity;
    }

    private String buildJobSemanticQuery(Job job) {
        String skills = String.join(", ", AiFeatureUtils.jobSkillNames(job));
        String companyName = job.getCompany() == null ? "" : job.getCompany().getName();
        return trimToMax(joinLines(
                "Target job profile",
                label("Job", job.getName()),
                label("Company", companyName),
                label("Level", job.getLevel() == null ? "" : job.getLevel().name()),
                label("Skills", skills),
                label("Description", job.getDescription())),
                MAX_VECTOR_TEXT_CHARS);
    }

    private int scoreFromDocuments(List<Document> docs) {
        double best = docs.stream().mapToDouble(this::similarityScore).max().orElse(0.55d);
        double topAverage = docs.stream().limit(3).mapToDouble(this::similarityScore).average().orElse(best);
        int score = (int) Math.round((best * 0.70d + topAverage * 0.30d) * 100d);
        return AiFeatureUtils.clampScore(score);
    }

    private double similarityScore(Document doc) {
        Object distance = doc.getMetadata().get("distance");
        if (distance instanceof Number number) {
            return Math.max(0d, Math.min(1d, 1d - number.doubleValue()));
        }
        return 0.65d;
    }

    private String evidenceFromDocument(Document doc) {
        Object chunkType = doc.getMetadata().get("chunk_type");
        String type = chunkType == null ? "CV_CHUNK" : String.valueOf(chunkType);
        return "Semantic CV evidence [" + type + "]: " + shortenEvidence(doc.getContent());
    }

    private String shortenEvidence(String value) {
        return trimToMax(AiFeatureUtils.normalizeWhitespace(value), 420);
    }

    private void markResumeVectorError(Resume resume, String message) {
        if (resume == null) {
            return;
        }
        resume.setVectorized(false);
        resume.setVectorizedAt(null);
        resume.setCvChunkCount(0);
        resume.setCvVectorError(trimToMax(message == null ? "Unknown vector error" : message, 400));
    }

    private String resolveContentHash(String contentHash, String cvText) {
        return contentHash == null || contentHash.isBlank()
                ? aiGatewayService.fingerprint(cvText)
                : contentHash;
    }

    private String resumeVectorId(long resumeId, String contentHash, int chunkIndex) {
        return "cv:resume:" + resumeId + ":" + shortHash(contentHash) + ":" + chunkIndex;
    }

    private String analysisVectorId(long analysisId, String contentHash, int chunkIndex) {
        return "cv:analysis:" + analysisId + ":" + shortHash(contentHash) + ":" + chunkIndex;
    }

    private String shortHash(String value) {
        String safeValue = value == null ? "" : value.replaceAll("[^a-zA-Z0-9]", "");
        return safeValue.length() <= 16 ? safeValue : safeValue.substring(0, 16);
    }

    private String sanitizeChunkText(String text) {
        String normalized = AiFeatureUtils.normalizeWhitespace(text);
        normalized = EMAIL_PATTERN.matcher(normalized).replaceAll("[email]");
        normalized = PHONE_PATTERN.matcher(normalized).replaceAll("[phone]");
        return normalized;
    }

    private String skillLine(SkillEvidence skill) {
        return joinInline(
                skill.name(),
                skill.category().isBlank() ? "" : "(" + skill.category() + ")",
                skill.evidence().isBlank() ? "" : "evidence: " + skill.evidence());
    }

    private String label(String label, String value) {
        return value == null || value.isBlank() ? "" : label + ": " + value;
    }

    private String joinLines(String... lines) {
        return List.of(lines).stream()
                .filter(Objects::nonNull)
                .map(AiFeatureUtils::normalizeWhitespace)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String joinInline(String... values) {
        return List.of(values).stream()
                .filter(Objects::nonNull)
                .map(AiFeatureUtils::normalizeWhitespace)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String trimToMax(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).trim();
    }

    public record CvChunkDraft(String chunkType, int chunkIndex, String text, int wordCount) {
    }

    public record SemanticChunkMatch(int score, boolean available, List<String> evidence, List<Document> documents) {
        public SemanticChunkMatch {
            evidence = evidence == null ? List.of() : evidence;
            documents = documents == null ? List.of() : documents;
        }

        public static SemanticChunkMatch unavailable() {
            return new SemanticChunkMatch(0, false, List.of(), List.of());
        }
    }
}
