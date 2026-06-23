package vn.hoangtung.jobfind.util.ai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Skill;
import vn.hoangtung.jobfind.util.constant.LevelEnum;

public final class AiFeatureUtils {

    private static final Pattern MILLION_PATTERN = Pattern.compile("(?i)(\\d+(?:[\\.,]\\d+)?)\\s*(trieu|tr|m)\\b");
    private static final Pattern VND_PATTERN = Pattern.compile("(?i)(\\d{7,11})\\s*(vnd)?\\b");
    private static final Pattern YEAR_PATTERN = Pattern.compile("(?i)(\\d{1,2})\\+?\\s*(nam|years?)");
    private static final Pattern METRIC_PATTERN = Pattern.compile(
            "(?i)(\\d+%|\\d+\\+?\\s*(users?|nguoi dung|request|requests|project|projects|ms|s|trieu|million|ty|m))");

    private static final Set<String> STOP_WORDS = Set.of(
            "tim", "tìm", "viec", "việc", "cho", "toi", "tôi", "va", "và", "o", "ở", "tai", "tại",
            "can", "cần", "yeu", "yêu", "cau", "cầu", "muon", "muốn", "lam", "làm", "muc", "mức",
            "luong", "lương", "job", "jobs", "it", "developer", "engineer", "position", "phu", "phù",
            "hop", "hợp", "co", "có", "khong", "không", "tren", "trên", "tu", "từ", "den", "đến",
            "the", "thể", "nao", "nào", "dang", "đang");

    private static final Set<String> DEFAULT_SKILLS = Set.of(
            "java", "spring", "spring boot", "hibernate", "jpa", "javascript", "typescript", "node.js",
            "node", "react", "react native", "angular", "vue", "next.js", "nestjs", "python", "django",
            "flask", "fastapi", "c#", ".net", "asp.net", "php", "laravel", "golang", "go", "kotlin",
            "swift", "android", "ios", "flutter", "docker", "kubernetes", "aws", "azure", "gcp",
            "postgresql", "mysql", "sql server", "mongodb", "redis", "kafka", "rabbitmq", "elasticsearch",
            "graphql", "rest", "microservices", "ci/cd", "jenkins", "gitlab ci", "terraform", "linux",
            "devops", "qa", "selenium", "cypress", "data engineer", "spark", "hadoop", "airflow",
            "machine learning", "ai", "nlp", "pytorch", "tensorflow", "figma", "ui/ux", "business analyst");

    private static final Map<String, Set<String>> DOMAIN_KEYWORDS = Map.of(
            "backend", Set.of("backend", "java", "spring", "node", "node.js", "python", ".net", "api", "microservices"),
            "frontend", Set.of("frontend", "react", "angular", "vue", "css", "html", "typescript", "javascript"),
            "mobile", Set.of("mobile", "android", "ios", "flutter", "react native", "swift", "kotlin"),
            "data", Set.of("data", "spark", "hadoop", "airflow", "etl", "machine learning", "ai", "nlp", "analytics"),
            "devops", Set.of("devops", "aws", "azure", "gcp", "docker", "kubernetes", "terraform", "jenkins"),
            "qa", Set.of("qa", "tester", "testing", "selenium", "cypress", "automation test"),
            "security", Set.of("security", "cyber", "pentest", "soc", "owasp"),
            "product", Set.of("business analyst", "product", "project manager", "scrum", "agile"),
            "design", Set.of("ui/ux", "figma", "design"));

    // Các domain cùng nhóm "lập trình phần mềm ứng dụng" — dùng để cho điểm domain liền kề.
    private static final Set<String> ENGINEERING_DOMAINS = Set.of("backend", "frontend", "mobile", "devops");

    // Ngưỡng liên quan tối thiểu: dưới mức này coi như CV không liên quan tới JD.
    private static final double RELEVANCE_FLOOR = 0.15;

    // Trần điểm cho CV gần như không liên quan tới JD (vd: tài liệu sai chủ đề).
    private static final int IRRELEVANT_SCORE_CAP = 15;

    private AiFeatureUtils() {
    }

    public static String normalizeWhitespace(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        normalized = normalized.replaceAll("(\\n){3,}", "\n\n");
        normalized = normalized.replaceAll("[ \\t]+", " ");
        return normalized.trim();
    }

    public static String normalizeForSearch(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        normalized = normalized.replace('đ', 'd');
        normalized = normalized.replaceAll("[^a-z0-9+.#/% ]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    public static int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public static int wordCount(String text) {
        String normalized = normalizeWhitespace(text);
        return normalized.isBlank() ? 0 : normalized.split("\\s+").length;
    }

    public static Set<String> buildKnownSkills(Collection<String> dbSkills) {
        LinkedHashSet<String> skills = new LinkedHashSet<>(DEFAULT_SKILLS);
        if (dbSkills != null) {
            dbSkills.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(skills::add);
        }
        return skills;
    }

    public static Set<String> extractKnownSkills(String text, Collection<String> knownSkills) {
        return SkillTaxonomy.from(knownSkills).extractSkillNames(text);
    }

    public static List<SkillTaxonomy.SkillMatch> extractSkillMatches(String text, Collection<String> knownSkills) {
        return SkillTaxonomy.from(knownSkills).findMatches(text);
    }

    public static ChatIntent parseChatIntent(String userMessage, Collection<String> knownLocations,
            Collection<String> knownSkills) {
        String normalized = normalizeForSearch(userMessage);
        Set<String> requestedSkills = extractKnownSkills(userMessage, knownSkills);

        String matchedLocation = null;
        for (String location : knownLocations) {
            if (location == null || location.isBlank()) {
                continue;
            }
            String normalizedLocation = normalizeForSearch(location);
            if (!normalizedLocation.isBlank() && normalized.contains(normalizedLocation)) {
                matchedLocation = location;
                break;
            }
        }

        Integer minSalary = null;
        Matcher millionMatcher = MILLION_PATTERN.matcher(normalized);
        while (millionMatcher.find()) {
            double value = Double.parseDouble(millionMatcher.group(1).replace(",", "."));
            int parsed = (int) Math.round(value * 1_000_000d);
            minSalary = minSalary == null ? parsed : Math.max(minSalary, parsed);
        }

        if (minSalary == null) {
            Matcher vndMatcher = VND_PATTERN.matcher(normalized);
            while (vndMatcher.find()) {
                int parsed = Integer.parseInt(vndMatcher.group(1));
                minSalary = minSalary == null ? parsed : Math.max(minSalary, parsed);
            }
        }

        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        for (String token : normalized.split(" ")) {
            if (token.isBlank() || token.length() < 2 || STOP_WORDS.contains(token)) {
                continue;
            }
            keywords.add(token);
        }

        return new ChatIntent(userMessage, normalized, matchedLocation, minSalary, requestedSkills, keywords);
    }

    public static boolean matchesHardConstraints(ChatIntent intent, Job job) {
        if (intent == null || job == null) {
            return false;
        }

        if (intent.minSalary() != null && job.getSalary() < intent.minSalary()) {
            return false;
        }

        if (intent.location() != null) {
            String normalizedLocation = normalizeForSearch(job.getLocation());
            String expectedLocation = normalizeForSearch(intent.location());
            if (!normalizedLocation.contains(expectedLocation) && !expectedLocation.contains(normalizedLocation)) {
                return false;
            }
        }

        if (!intent.requestedSkills().isEmpty()) {
            Set<String> jobSkills = jobSkillNames(job).stream()
                    .map(AiFeatureUtils::normalizeForSearch)
                    .collect(Collectors.toSet());
            boolean hasOverlap = intent.requestedSkills().stream()
                    .map(AiFeatureUtils::normalizeForSearch)
                    .anyMatch(jobSkills::contains);
            if (!hasOverlap) {
                return false;
            }
        }
        return true;
    }

    public static List<RankedJob> rankJobs(ChatIntent intent, Collection<Job> jobs, Set<String> userSkills) {
        if (jobs == null || jobs.isEmpty()) {
            return List.of();
        }

        List<RankedJob> ranked = new ArrayList<>();
        for (Job job : jobs) {
            if (job == null) {
                continue;
            }

            int score = 0;
            List<String> reasons = new ArrayList<>();

            String jobText = normalizeForSearch(job.getName() + " "
                    + safe(job.getDescription()) + " "
                    + safe(job.getLocation()) + " "
                    + safe(job.getCompany() != null ? job.getCompany().getName() : ""));

            if (job.isActive()) {
                score += 8;
                reasons.add("Tin đang tuyển");
            }

            if (intent.location() != null) {
                String normalizedLocation = normalizeForSearch(job.getLocation());
                String expectedLocation = normalizeForSearch(intent.location());
                if (normalizedLocation.contains(expectedLocation) || expectedLocation.contains(normalizedLocation)) {
                    score += 28;
                    reasons.add("Đúng địa điểm " + job.getLocation());
                } else {
                    score -= 18;
                }
            }

            if (intent.minSalary() != null) {
                if (job.getSalary() >= intent.minSalary()) {
                    score += 18;
                    reasons.add("Đáp ứng mức lương tối thiểu");
                } else {
                    score -= 25;
                }
            }

            Set<String> normalizedJobSkills = jobSkillNames(job).stream()
                    .map(AiFeatureUtils::normalizeForSearch)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            int skillMatches = 0;
            for (String requestedSkill : intent.requestedSkills()) {
                String normalizedRequestedSkill = normalizeForSearch(requestedSkill);
                if (normalizedJobSkills.contains(normalizedRequestedSkill)) {
                    skillMatches++;
                }
            }
            if (!intent.requestedSkills().isEmpty()) {
                if (skillMatches > 0) {
                    score += 20 + skillMatches * 10;
                    reasons.add("Khớp " + skillMatches + " kỹ năng chính");
                } else {
                    score -= 20;
                }
            }

            int keywordMatches = 0;
            for (String keyword : intent.keywords()) {
                if (jobText.contains(keyword)) {
                    keywordMatches++;
                }
            }
            if (keywordMatches > 0) {
                score += Math.min(18, keywordMatches * 4);
                reasons.add("Khớp từ khóa tìm kiếm");
            }

            Set<String> normalizedUserSkills = userSkills.stream()
                    .map(AiFeatureUtils::normalizeForSearch)
                    .collect(Collectors.toSet());
            long profileOverlap = normalizedJobSkills.stream()
                    .filter(normalizedUserSkills::contains)
                    .count();
            if (profileOverlap > 0) {
                score += (int) Math.min(12, profileOverlap * 3);
                reasons.add("Phù hợp hồ sơ kỹ năng của người dùng");
            }

            if (job.getCompany() != null && intent.normalizedQuery().contains(normalizeForSearch(job.getCompany().getName()))) {
                score += 12;
                reasons.add("Đúng công ty được nhắc tới");
            }

            ranked.add(new RankedJob(job, score, reasons));
        }

        return ranked.stream()
                .sorted(Comparator.comparingInt(RankedJob::score).reversed()
                        .thenComparing(rankedJob -> rankedJob.job().getSalary(), Comparator.reverseOrder()))
                .toList();
    }

    public static CvSignalProfile analyzeCv(String cvText, Collection<String> knownSkills) {
        String normalizedText = normalizeForSearch(cvText);
        Set<String> detectedSkills = extractKnownSkills(cvText, knownSkills);
        boolean hasSummary = containsAny(normalizedText, Set.of("summary", "gioi thieu", "muc tieu", "objective", "profile"));
        boolean hasSkillsSection = containsAny(normalizedText, Set.of("skills", "ky nang", "technical skills", "cong nghe"));
        boolean hasExperienceSection = containsAny(normalizedText, Set.of("experience", "kinh nghiem", "work history", "du an"));
        boolean hasEducationSection = containsAny(normalizedText, Set.of("education", "hoc van", "university", "truong"));
        boolean hasGithub = normalizedText.contains("github");
        boolean hasLinkedin = normalizedText.contains("linkedin");
        boolean hasMetrics = METRIC_PATTERN.matcher(normalizedText).find();
        int estimatedYears = estimateYears(normalizedText);
        String inferredLevel = inferCandidateLevel(normalizedText, estimatedYears);
        Set<String> domains = inferDomains(normalizedText, detectedSkills);
        int softSkillSignals = countSoftSkillSignals(normalizedText);
        boolean hasDegreeSignal = containsAny(normalizedText,
                Set.of("dai hoc", "đại học", "college", "bachelor", "engineer", "university", "cao dang"));

        return new CvSignalProfile(
                detectedSkills,
                hasSummary,
                hasSkillsSection,
                hasExperienceSection,
                hasEducationSection,
                hasGithub,
                hasLinkedin,
                hasMetrics,
                estimatedYears,
                inferredLevel,
                domains,
                softSkillSignals,
                hasDegreeSignal);
    }

    public static CvSignalProfile analyzeCv(ParsedCv parsedCv, String cvText, Collection<String> knownSkills) {
        CvSignalProfile rawProfile = analyzeCv(cvText, knownSkills);
        if (parsedCv == null) {
            return rawProfile;
        }

        SkillTaxonomy taxonomy = SkillTaxonomy.from(knownSkills);
        LinkedHashSet<String> detectedSkills = new LinkedHashSet<>(rawProfile.detectedSkills());
        parsedCv.skillNames().stream()
                .map(taxonomy::canonicalName)
                .filter(value -> !value.isBlank())
                .forEach(detectedSkills::add);

        String structuredText = cvText + " " + structuredCvText(parsedCv);
        String normalizedStructuredText = normalizeForSearch(structuredText);
        int parsedYears = parsedCv.totalExperienceMonths() > 0
                ? (int) Math.ceil(parsedCv.totalExperienceMonths() / 12d)
                : 0;
        int estimatedYears = Math.max(rawProfile.estimatedYears(), parsedYears);

        LinkedHashSet<String> domains = new LinkedHashSet<>(rawProfile.domains());
        domains.addAll(inferDomains(normalizedStructuredText, detectedSkills));

        boolean hasSummary = rawProfile.hasSummarySection() || !parsedCv.summary().isBlank();
        boolean hasSkillsSection = rawProfile.hasSkillsSection() || !parsedCv.skills().isEmpty();
        boolean hasExperienceSection = rawProfile.hasExperienceSection()
                || !parsedCv.experience().isEmpty()
                || !parsedCv.projects().isEmpty();
        boolean hasEducationSection = rawProfile.hasEducationSection() || !parsedCv.education().isEmpty();
        boolean hasGithub = rawProfile.hasGithub() || parsedCv.hasGithub();
        boolean hasLinkedin = rawProfile.hasLinkedin() || parsedCv.hasLinkedin();
        boolean hasMetrics = rawProfile.hasMetrics() || parsedCv.hasMetrics();
        int softSkillSignals = Math.max(rawProfile.softSkillSignals(), countSoftSkillSignals(normalizedStructuredText));
        boolean hasDegreeSignal = rawProfile.hasDegreeSignal() || parsedCv.education().stream()
                .anyMatch(entry -> containsAny(
                        normalizeForSearch(entry.school() + " " + entry.degree() + " " + entry.major()),
                        Set.of("dai hoc", "college", "bachelor", "engineer", "university", "cao dang")));
        String inferredLevel = inferCandidateLevel(normalizedStructuredText, estimatedYears);

        return new CvSignalProfile(
                detectedSkills,
                hasSummary,
                hasSkillsSection,
                hasExperienceSection,
                hasEducationSection,
                hasGithub,
                hasLinkedin,
                hasMetrics,
                estimatedYears,
                inferredLevel,
                domains,
                softSkillSignals,
                hasDegreeSignal);
    }

    public static int recomputeOverallScore(int formatScore, int contentScore, int keywordScore, int impactScore) {
        double weighted = formatScore * 0.20 + contentScore * 0.35 + keywordScore * 0.25 + impactScore * 0.20;
        return clampScore((int) Math.round(weighted));
    }

    public static MatchBreakdown computeMatch(Job job, String cvText, Collection<String> knownSkills) {
        CvSignalProfile profile = analyzeCv(cvText, knownSkills);
        return computeMatch(job, profile);
    }

    public static MatchBreakdown computeMatch(
            Job job,
            ParsedCv parsedCv,
            String cvText,
            Collection<String> knownSkills) {
        CvSignalProfile profile = analyzeCv(parsedCv, cvText, knownSkills);
        return computeMatch(job, profile);
    }

    public static MatchBreakdown computeMatch(Job job, CvSignalProfile profile) {
        return computeMatch(job, profile, SemanticMatchSignal.unavailable());
    }

    public static MatchBreakdown computeMatch(
            Job job,
            ParsedCv parsedCv,
            String cvText,
            Collection<String> knownSkills,
            SemanticMatchSignal semanticSignal) {
        CvSignalProfile profile = analyzeCv(parsedCv, cvText, knownSkills);
        return computeMatch(job, profile, semanticSignal);
    }

    public static MatchBreakdown computeMatch(Job job, CvSignalProfile profile, SemanticMatchSignal semanticSignal) {
        if (profile == null) {
            profile = analyzeCv("", Set.of());
        }
        SemanticMatchSignal safeSemanticSignal = semanticSignal == null
                ? SemanticMatchSignal.unavailable()
                : semanticSignal;

        List<String> originalJobSkills = jobSkillNames(job);
        LinkedHashSet<String> skillCatalog = new LinkedHashSet<>(originalJobSkills);
        skillCatalog.addAll(profile.detectedSkills());
        SkillTaxonomy taxonomy = SkillTaxonomy.from(skillCatalog);

        Map<String, String> normalizedToOriginal = new LinkedHashMap<>();
        for (String jobSkill : originalJobSkills) {
            String canonicalSkill = taxonomy.canonicalName(jobSkill);
            String normalizedSkill = normalizeForSearch(canonicalSkill);
            if (!normalizedSkill.isBlank()) {
                normalizedToOriginal.putIfAbsent(normalizedSkill, jobSkill);
            }
        }

        Set<String> normalizedCvSkills = profile.detectedSkills().stream()
                .map(taxonomy::canonicalName)
                .map(AiFeatureUtils::normalizeForSearch)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        for (Map.Entry<String, String> entry : normalizedToOriginal.entrySet()) {
            if (normalizedCvSkills.contains(entry.getKey())) {
                matchedSkills.add(entry.getValue());
            } else {
                missingSkills.add(entry.getValue());
            }
        }

        int skillRequirementCount = normalizedToOriginal.size();
        boolean jobDeclaresSkills = skillRequirementCount > 0;
        double skillMatch = jobDeclaresSkills ? (double) matchedSkills.size() / skillRequirementCount : 0.5;

        String candidateLevel = profile.inferredLevel();
        LevelEnum jobLevel = job.getLevel();
        double experienceMatch = computeExperienceMatch(candidateLevel, jobLevel);

        Set<String> jobDomains = inferDomains(
                normalizeForSearch(job.getName() + " " + safe(job.getDescription()) + " " + String.join(" ", originalJobSkills)),
                originalJobSkills.stream()
                        .map(taxonomy::canonicalName)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        double domainMatch = computeDomainMatch(profile.domains(), jobDomains);

        double softSkillMatch = computeSoftSkillMatch(profile.hasDegreeSignal(), profile.softSkillSignals());

        // Mức độ liên quan chủ đề giữa CV và đúng JD này: chỉ tính khi có khớp kỹ năng
        // thực sự, trùng domain, hoặc trùng ngữ nghĩa. Đây là "cổng" chặn các tài liệu
        // không liên quan (vd: nội quy trường học) đạt điểm cao nhờ tín hiệu chung chung.
        double skillRelevance = jobDeclaresSkills
                ? (double) matchedSkills.size() / skillRequirementCount
                : 0d;
        double domainOverlap = domainOverlapRatio(profile.domains(), jobDomains);
        double semanticRelevance = safeSemanticSignal.available()
                ? safeSemanticSignal.score() / 100d
                : 0d;
        double topicalRelevance = Math.max(skillRelevance, Math.max(domainOverlap, semanticRelevance));

        // Level kinh nghiệm và soft-skill là tín hiệu chung chung, không phản ánh mức phù hợp
        // với JD cụ thể, nên chỉ được cộng tương ứng với độ liên quan chủ đề. Nhờ vậy một tài
        // liệu sai chủ đề không thể tích lũy điểm chỉ nhờ có bằng cấp hay level mặc định.
        double genericFit = (experienceMatch * 25d + softSkillMatch * 15d) * topicalRelevance;
        int deterministicScore = clampScore((int) Math.round(
                skillMatch * 40d + domainMatch * 20d + genericFit));
        int finalScore = deterministicScore;
        if (safeSemanticSignal.available()) {
            finalScore = clampScore((int) Math.round(deterministicScore * 0.80d
                    + safeSemanticSignal.score() * 0.20d));
        }

        // Cổng liên quan cứng: CV gần như không liên quan tới JD thì không thể đạt điểm cao,
        // bất kể các tín hiệu chung chung (bằng cấp, level mặc định, base ngữ nghĩa...).
        if (topicalRelevance < RELEVANCE_FLOOR) {
            finalScore = Math.min(finalScore, IRRELEVANT_SCORE_CAP);
        }

        List<String> evidence = new ArrayList<>();
        if (!matchedSkills.isEmpty()) {
            evidence.add("CV đã thể hiện các kỹ năng phù hợp: " + String.join(", ", matchedSkills));
        }
        if (!missingSkills.isEmpty()) {
            evidence.add("JD còn thiếu các kỹ năng: " + String.join(", ", missingSkills));
        }
        evidence.add("Level ứng viên ước lượng: " + candidateLevel);
        evidence.add("Nhóm năng lực chính: " + String.join(", ", profile.domains()));

        evidence.addAll(safeSemanticSignal.evidence());

        return new MatchBreakdown(
                finalScore,
                (int) Math.round(skillMatch * 100),
                (int) Math.round(experienceMatch * 100),
                (int) Math.round(domainMatch * 100),
                (int) Math.round(softSkillMatch * 100),
                safeSemanticSignal.score(),
                safeSemanticSignal.available(),
                safeSemanticSignal.rank(),
                matchedSkills,
                missingSkills,
                evidence,
                candidateLevel,
                profile);
    }

    public static String buildCandidateProfileSummary(CvSignalProfile profile, Set<String> subscriberSkills) {
        List<String> parts = new ArrayList<>();
        if (!profile.detectedSkills().isEmpty()) {
            parts.add("Kỹ năng CV nổi bật: " + String.join(", ", profile.detectedSkills()));
        }
        if (!subscriberSkills.isEmpty()) {
            parts.add("Kỹ năng ưu tiên từ hồ sơ người dùng: " + String.join(", ", subscriberSkills));
        }
        parts.add("Level ước lượng: " + profile.inferredLevel());
        parts.add("Số năm kinh nghiệm ước lượng: " + profile.estimatedYears());
        parts.add("Có section kỹ năng: " + yesNo(profile.hasSkillsSection()));
        parts.add("Có section kinh nghiệm: " + yesNo(profile.hasExperienceSection()));
        parts.add("Có thành tựu định lượng: " + yesNo(profile.hasMetrics()));
        return String.join(". ", parts);
    }

    public static String expectedInterviewCategory(int questionNumber, int totalQuestions, String level) {
        if (questionNumber <= 2) {
            return "TECHNICAL";
        }
        if (questionNumber == totalQuestions) {
            return isSeniorLike(level) ? "SYSTEM_DESIGN" : "BEHAVIORAL";
        }
        if (questionNumber == totalQuestions - 1 && totalQuestions >= 4) {
            return "BEHAVIORAL";
        }
        return isSeniorLike(level) && questionNumber >= 4 ? "SYSTEM_DESIGN" : "TECHNICAL";
    }

    public static String expectedInterviewDifficulty(String level, int questionNumber) {
        String normalizedLevel = normalizeForSearch(level);
        if (normalizedLevel.contains("intern") || normalizedLevel.contains("fresher") || normalizedLevel.contains("junior")) {
            return questionNumber <= 2 ? "EASY" : "MEDIUM";
        }
        if (normalizedLevel.contains("middle") || normalizedLevel.contains("mid")) {
            return questionNumber <= 2 ? "MEDIUM" : "HARD";
        }
        return questionNumber == 1 ? "MEDIUM" : "HARD";
    }

    public static String buildInterviewFocusSummary(List<Integer> scores, List<String> categories, List<String> feedbacks) {
        if (scores == null || scores.isEmpty()) {
            return "Chưa có dữ liệu trước đó, hãy bắt đầu bằng câu hỏi nền tảng.";
        }

        List<String> focus = new ArrayList<>();
        double avgScore = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        if (avgScore < 55) {
            focus.add("Mức trả lời hiện tại còn yếu, cần ưu tiên câu hỏi kiểm tra kiến thức nền và khả năng giải thích rõ ràng.");
        } else if (avgScore < 75) {
            focus.add("Cần câu hỏi đào sâu vào các điểm ứng viên trả lời còn mơ hồ.");
        } else {
            focus.add("Ứng viên đang làm khá tốt, có thể tăng độ khó và hỏi trade-off thực tế.");
        }

        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i) < 60) {
                String category = i < categories.size() ? categories.get(i) : "TECHNICAL";
                focus.add("Có điểm yếu ở nhóm " + category + ".");
            }
        }

        for (String feedback : feedbacks) {
            String normalizedFeedback = normalizeForSearch(feedback);
            if (normalizedFeedback.contains("thieu vi du") || normalizedFeedback.contains("thiếu ví dụ")) {
                focus.add("Nên buộc ứng viên đưa ví dụ thực tế.");
                break;
            }
            if (normalizedFeedback.contains("trade off") || normalizedFeedback.contains("kien truc")
                    || normalizedFeedback.contains("kiến trúc")) {
                focus.add("Nên khai thác thêm reasoning và trade-off.");
                break;
            }
        }

        return focus.stream().distinct().collect(Collectors.joining(" "));
    }

    public static List<String> buildFallbackRecommendations(MatchBreakdown breakdown, Job job) {
        List<String> recommendations = new ArrayList<>();

        if (!breakdown.missingSkills().isEmpty()) {
            recommendations.add("Ưu tiên bổ sung các kỹ năng còn thiếu: " + String.join(", ", breakdown.missingSkills()) + ".");
        }
        if (breakdown.experienceMatchScore() < 70) {
            recommendations.add("Bổ sung thêm kinh nghiệm gần với level " + job.getLevel() + " bằng project có độ phức tạp tương đương.");
        }
        if (breakdown.domainMatchScore() < 70) {
            recommendations.add("Tăng mức độ liên quan domain bằng project hoặc case study sát với vị trí " + job.getName() + ".");
        }
        if (breakdown.semanticAvailable() && breakdown.semanticMatchScore() < 60) {
            recommendations.add("Điều chỉnh mô tả kinh nghiệm/project theo ngôn ngữ gần JD hơn: trách nhiệm chính, domain, scale hệ thống và stack liên quan.");
        }
        if (breakdown.softSkillMatchScore() < 70) {
            recommendations.add("Thể hiện rõ kỹ năng teamwork, giao tiếp kỹ thuật và kết quả học tập/chứng chỉ liên quan trong CV.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("CV hiện đã khá sát JD, hãy bổ sung thêm số liệu thành tựu để tăng sức thuyết phục.");
        }

        return recommendations.stream().distinct().limit(4).toList();
    }

    public static List<String> jobSkillNames(Job job) {
        if (job == null || job.getSkills() == null) {
            return List.of();
        }
        return job.getSkills().stream()
                .map(Skill::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static boolean containsAny(String normalizedText, Collection<String> fragments) {
        for (String fragment : fragments) {
            if (normalizedText.contains(normalizeForSearch(fragment))) {
                return true;
            }
        }
        return false;
    }

    private static String inferCandidateLevel(String normalizedText, int estimatedYears) {
        if (normalizedText.contains("senior")) {
            return "SENIOR";
        }
        if (normalizedText.contains("middle") || normalizedText.contains("mid-level") || normalizedText.contains("mid ")) {
            return "MIDDLE";
        }
        if (normalizedText.contains("junior")) {
            return "JUNIOR";
        }
        if (normalizedText.contains("fresher")) {
            return "FRESHER";
        }
        if (normalizedText.contains("intern")) {
            return "INTERN";
        }

        if (estimatedYears >= 5) {
            return "SENIOR";
        }
        if (estimatedYears >= 3) {
            return "MIDDLE";
        }
        if (estimatedYears >= 1) {
            return "JUNIOR";
        }
        return "FRESHER";
    }

    private static String structuredCvText(ParsedCv parsedCv) {
        if (parsedCv == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        parts.add(parsedCv.headline());
        parts.add(parsedCv.summary());
        parts.add(String.join(" ", parsedCv.skillNames()));
        parsedCv.experience().forEach(entry -> {
            parts.add(entry.company());
            parts.add(entry.role());
            parts.add(String.join(" ", entry.technologies()));
            parts.add(String.join(" ", entry.bullets()));
        });
        parsedCv.projects().forEach(entry -> {
            parts.add(entry.name());
            parts.add(entry.role());
            parts.add(entry.description());
            parts.add(String.join(" ", entry.technologies()));
            parts.add(String.join(" ", entry.bullets()));
        });
        parsedCv.education().forEach(entry -> {
            parts.add(entry.school());
            parts.add(entry.degree());
            parts.add(entry.major());
        });
        parts.add(String.join(" ", parsedCv.certifications()));
        return parts.stream()
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    private static int estimateYears(String normalizedText) {
        int maxYears = 0;
        Matcher matcher = YEAR_PATTERN.matcher(normalizedText);
        while (matcher.find()) {
            int years = Integer.parseInt(matcher.group(1));
            maxYears = Math.max(maxYears, years);
        }
        return maxYears;
    }

    private static Set<String> inferDomains(String normalizedText, Collection<String> relatedSkills) {
        if (normalizedText == null) {
            return Set.of();
        }

        Set<String> domains = new LinkedHashSet<>();
        String extraSkills = relatedSkills == null ? "" : " " + relatedSkills.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        String searchable = normalizedText + " " + normalizeForSearch(extraSkills);

        for (Map.Entry<String, Set<String>> entry : DOMAIN_KEYWORDS.entrySet()) {
            boolean matched = entry.getValue().stream()
                    .map(AiFeatureUtils::normalizeForSearch)
                    .anyMatch(searchable::contains);
            if (matched) {
                domains.add(entry.getKey());
            }
        }

        return domains;
    }

    private static int countSoftSkillSignals(String normalizedText) {
        int count = 0;
        List<String> keywords = List.of("teamwork", "team work", "giao tiep", "communication", "leadership",
                "problem solving", "critical thinking", "collaboration", "mentor", "chu dong", "ownership");
        for (String keyword : keywords) {
            if (normalizedText.contains(normalizeForSearch(keyword))) {
                count++;
            }
        }
        return count;
    }

    private static double computeExperienceMatch(String candidateLevel, LevelEnum jobLevel) {
        if (jobLevel == null || candidateLevel == null) {
            return 0.5;
        }

        int candidateRank = levelRank(candidateLevel);
        int jobRank = levelRank(jobLevel.name());
        int diff = candidateRank - jobRank;

        if (diff >= 0) {
            return 1.0;
        }
        if (diff == -1) {
            return 0.6;
        }
        return 0.3;
    }

    private static double computeDomainMatch(Set<String> candidateDomains, Set<String> jobDomains) {
        if (jobDomains.isEmpty()) {
            return 0.5; // JD không nêu rõ domain → trung tính.
        }
        if (candidateDomains.isEmpty()) {
            return 0.0; // CV không thể hiện domain kỹ thuật nào → không liên quan.
        }
        Set<String> intersection = new HashSet<>(candidateDomains);
        intersection.retainAll(jobDomains);
        if (!intersection.isEmpty()) {
            return 1.0;
        }

        boolean bothEngineering = intersects(candidateDomains, ENGINEERING_DOMAINS)
                && intersects(jobDomains, ENGINEERING_DOMAINS);
        if (bothEngineering) {
            return 0.4; // Cùng nhóm lập trình ứng dụng nhưng khác mảng.
        }
        return 0.1; // Domain hoàn toàn khác nhau.
    }

    private static double domainOverlapRatio(Set<String> candidateDomains, Set<String> jobDomains) {
        if (candidateDomains == null || jobDomains == null
                || candidateDomains.isEmpty() || jobDomains.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new HashSet<>(candidateDomains);
        intersection.retainAll(jobDomains);
        return (double) intersection.size() / jobDomains.size();
    }

    private static double computeSoftSkillMatch(boolean hasDegreeSignal, int softSkillSignals) {
        boolean hasSoftSkills = softSkillSignals >= 2;
        if (hasDegreeSignal && hasSoftSkills) {
            return 1.0;
        }
        if (hasDegreeSignal || hasSoftSkills) {
            return 0.6;
        }
        return 0.3;
    }

    private static boolean intersects(Set<String> left, Set<String> right) {
        for (String value : left) {
            if (right.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static int levelRank(String level) {
        String normalized = normalizeForSearch(level);
        if (normalized.contains("intern")) {
            return 1;
        }
        if (normalized.contains("fresher")) {
            return 2;
        }
        if (normalized.contains("junior")) {
            return 3;
        }
        if (normalized.contains("middle") || normalized.contains("mid")) {
            return 4;
        }
        return 5;
    }

    private static boolean isSeniorLike(String level) {
        String normalized = normalizeForSearch(level);
        return normalized.contains("senior") || normalized.contains("middle") || normalized.contains("mid");
    }

    private static String yesNo(boolean value) {
        return value ? "có" : "không";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public record ChatIntent(
            String originalQuery,
            String normalizedQuery,
            String location,
            Integer minSalary,
            Set<String> requestedSkills,
            Set<String> keywords) {
    }

    public record RankedJob(Job job, int score, List<String> reasons) {
    }

    public record CvSignalProfile(
            Set<String> detectedSkills,
            boolean hasSummarySection,
            boolean hasSkillsSection,
            boolean hasExperienceSection,
            boolean hasEducationSection,
            boolean hasGithub,
            boolean hasLinkedin,
            boolean hasMetrics,
            int estimatedYears,
            String inferredLevel,
            Set<String> domains,
            int softSkillSignals,
            boolean hasDegreeSignal) {
        public List<String> detectedSkillsSorted() {
            List<String> skills = new ArrayList<>(detectedSkills);
            Collections.sort(skills);
            return skills;
        }
    }

    public record MatchBreakdown(
            int finalScore,
            int skillMatchScore,
            int experienceMatchScore,
            int domainMatchScore,
            int softSkillMatchScore,
            int semanticMatchScore,
            boolean semanticAvailable,
            Integer semanticRank,
            List<String> matchedSkills,
            List<String> missingSkills,
            List<String> evidence,
            String candidateLevel,
            CvSignalProfile profile) {
    }

    public record SemanticMatchSignal(
            int score,
            boolean available,
            Integer rank,
            List<String> evidence) {

        public SemanticMatchSignal {
            score = available ? clampScore(score) : 0;
            evidence = evidence == null ? List.of() : evidence.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        }

        public static SemanticMatchSignal unavailable() {
            return new SemanticMatchSignal(0, false, null, List.of());
        }
    }

}
