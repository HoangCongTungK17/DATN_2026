package vn.hoangtung.jobfind.util.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record ParsedCv(
        ContactInfo contact,
        String headline,
        String summary,
        List<SkillEvidence> skills,
        List<WorkEntry> experience,
        List<ProjectEntry> projects,
        List<EducationEntry> education,
        List<String> certifications,
        List<String> languages,
        List<String> links,
        DocumentStats documentStats,
        List<String> parserWarnings) {

    private static final Pattern METRIC_PATTERN = Pattern.compile(
            "(?i)(\\d+%|\\d+\\+?\\s*(users?|requests?|projects?|ms|seconds?|million|trieu|ty|m))");

    public ParsedCv {
        contact = contact == null ? ContactInfo.empty() : contact;
        headline = clean(headline);
        summary = clean(summary);
        skills = copyList(skills);
        experience = copyList(experience);
        projects = copyList(projects);
        education = copyList(education);
        certifications = copyStrings(certifications);
        languages = copyStrings(languages);
        links = copyStrings(links);
        documentStats = documentStats == null ? DocumentStats.empty() : documentStats;
        parserWarnings = copyStrings(parserWarnings);
    }

    public static ParsedCv empty(int pageCount, int wordCount) {
        return new ParsedCv(
                ContactInfo.empty(),
                "",
                "",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DocumentStats(pageCount, wordCount, 0, 0, 0, List.of()),
                List.of("Structured parser returned fallback data."));
    }

    public List<String> skillNames() {
        return skills.stream()
                .map(SkillEvidence::name)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    public Set<String> skillNameSet() {
        return new LinkedHashSet<>(skillNames());
    }

    public List<String> allBullets() {
        List<String> bullets = new ArrayList<>();
        for (WorkEntry entry : experience) {
            bullets.addAll(entry.bullets());
        }
        for (ProjectEntry entry : projects) {
            bullets.addAll(entry.bullets());
        }
        return Collections.unmodifiableList(bullets);
    }

    public int totalExperienceMonths() {
        return experience.stream()
                .mapToInt(WorkEntry::durationMonths)
                .filter(value -> value > 0)
                .sum();
    }

    public boolean hasMetrics() {
        if (experience.stream().anyMatch(WorkEntry::hasMetrics)
                || projects.stream().anyMatch(ProjectEntry::hasMetrics)) {
            return true;
        }
        return allBullets().stream().anyMatch(value -> METRIC_PATTERN.matcher(value).find());
    }

    public boolean hasGithub() {
        return containsLink("github");
    }

    public boolean hasLinkedin() {
        return containsLink("linkedin");
    }

    private boolean containsLink(String fragment) {
        String needle = fragment == null ? "" : fragment.toLowerCase();
        return links.stream().anyMatch(link -> link.toLowerCase().contains(needle))
                || contact.links().stream().anyMatch(link -> link.toLowerCase().contains(needle));
    }

    public record ContactInfo(
            String name,
            String email,
            String phone,
            String location,
            List<String> links) {

        public ContactInfo {
            name = clean(name);
            email = clean(email);
            phone = clean(phone);
            location = clean(location);
            links = copyStrings(links);
        }

        public static ContactInfo empty() {
            return new ContactInfo("", "", "", "", List.of());
        }
    }

    public record SkillEvidence(
            String name,
            String category,
            List<String> aliases,
            String evidence) {

        public SkillEvidence {
            name = clean(name);
            category = clean(category).isBlank() ? "other" : clean(category);
            aliases = copyStrings(aliases);
            evidence = clean(evidence);
        }
    }

    public record WorkEntry(
            String company,
            String role,
            String startDate,
            String endDate,
            int durationMonths,
            List<String> bullets,
            List<String> technologies,
            boolean hasMetrics) {

        public WorkEntry {
            company = clean(company);
            role = clean(role);
            startDate = clean(startDate);
            endDate = clean(endDate);
            durationMonths = Math.max(0, durationMonths);
            bullets = copyStrings(bullets);
            technologies = copyStrings(technologies);
        }
    }

    public record ProjectEntry(
            String name,
            String role,
            String description,
            List<String> bullets,
            List<String> technologies,
            boolean hasMetrics) {

        public ProjectEntry {
            name = clean(name);
            role = clean(role);
            description = clean(description);
            bullets = copyStrings(bullets);
            technologies = copyStrings(technologies);
        }
    }

    public record EducationEntry(
            String school,
            String degree,
            String major,
            String startDate,
            String endDate,
            String gpa) {

        public EducationEntry {
            school = clean(school);
            degree = clean(degree);
            major = clean(major);
            startDate = clean(startDate);
            endDate = clean(endDate);
            gpa = clean(gpa);
        }
    }

    public record DocumentStats(
            int pageCount,
            int wordCount,
            int sectionCount,
            int totalBulletCount,
            int avgBulletWordCount,
            List<String> sectionOrder) {

        public DocumentStats {
            pageCount = Math.max(0, pageCount);
            wordCount = Math.max(0, wordCount);
            sectionCount = Math.max(0, sectionCount);
            totalBulletCount = Math.max(0, totalBulletCount);
            avgBulletWordCount = Math.max(0, avgBulletWordCount);
            sectionOrder = copyStrings(sectionOrder);
        }

        public static DocumentStats empty() {
            return new DocumentStats(0, 0, 0, 0, 0, List.of());
        }
    }

    private static String clean(String value) {
        return value == null ? "" : AiFeatureUtils.normalizeWhitespace(value);
    }

    private static List<String> copyStrings(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(ParsedCv::clean)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static <T> List<T> copyList(Collection<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
