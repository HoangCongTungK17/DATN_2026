package vn.hoangtung.jobfind.util.ai;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class SkillTaxonomy {

    private static final List<SkillSeed> CURATED_SKILLS = List.of(
            seed("Java", "backend", "core java", "jdk"),
            seed("Spring", "backend", "spring framework"),
            seed("Spring Boot", "backend", "springboot", "spring boot framework"),
            seed("Hibernate", "backend", "hibernate orm"),
            seed("JPA", "backend", "java persistence api"),
            seed("JavaScript", "frontend", "js", "ecmascript"),
            seed("TypeScript", "frontend", "ts"),
            seed("Node.js", "backend", "node", "nodejs", "node js"),
            seed("React", "frontend", "reactjs", "react.js", "react js"),
            seed("React Native", "mobile", "reactnative"),
            seed("Angular", "frontend", "angularjs", "angular js"),
            seed("Vue", "frontend", "vue.js", "vuejs", "vue js"),
            seed("Next.js", "frontend", "nextjs", "next js"),
            seed("NestJS", "backend", "nest.js", "nest js"),
            seed("Python", "backend"),
            seed("Django", "backend"),
            seed("Flask", "backend"),
            seed("FastAPI", "backend", "fast api"),
            seed("C#", "backend", "c sharp", "csharp"),
            seed(".NET", "backend", "dotnet", "asp.net", "asp net"),
            seed("PHP", "backend"),
            seed("Laravel", "backend"),
            seed("Go", "backend", "golang"),
            seed("Kotlin", "mobile"),
            seed("Swift", "mobile"),
            seed("Android", "mobile"),
            seed("iOS", "mobile"),
            seed("Flutter", "mobile"),
            seed("Docker", "devops"),
            seed("Kubernetes", "devops", "k8s", "kube"),
            seed("AWS", "cloud", "amazon web services", "amazon aws"),
            seed("Azure", "cloud", "microsoft azure"),
            seed("GCP", "cloud", "google cloud", "google cloud platform"),
            seed("PostgreSQL", "database", "postgres", "postgresql", "psql"),
            seed("MySQL", "database"),
            seed("SQL Server", "database", "mssql", "ms sql", "microsoft sql server"),
            seed("MongoDB", "database", "mongo"),
            seed("Redis", "database"),
            seed("Kafka", "backend", "apache kafka"),
            seed("RabbitMQ", "backend", "rabbit mq"),
            seed("Elasticsearch", "backend", "elastic search", "elastic"),
            seed("GraphQL", "backend", "graph ql"),
            seed("REST", "backend", "rest api", "restful api", "restful"),
            seed("Microservices", "backend", "microservice", "micro service", "micro services"),
            seed("CI/CD", "devops", "cicd", "ci cd", "continuous integration", "continuous deployment"),
            seed("Jenkins", "devops"),
            seed("GitLab CI", "devops", "gitlab-ci", "gitlabci"),
            seed("Terraform", "devops"),
            seed("Linux", "devops"),
            seed("DevOps", "devops"),
            seed("QA", "testing", "quality assurance", "tester", "testing"),
            seed("Selenium", "testing"),
            seed("Cypress", "testing"),
            seed("Data Engineer", "data", "data engineering"),
            seed("Spark", "data", "apache spark"),
            seed("Hadoop", "data"),
            seed("Airflow", "data", "apache airflow"),
            seed("Machine Learning", "data", "ml", "ml/ai", "ai ml"),
            seed("AI", "data", "artificial intelligence"),
            seed("NLP", "data", "natural language processing"),
            seed("PyTorch", "data", "pytorch"),
            seed("TensorFlow", "data", "tensorflow"),
            seed("Figma", "design"),
            seed("UI/UX", "design", "ui ux", "ux/ui", "ux ui"),
            seed("Business Analyst", "product", "ba", "business analysis"));

    private final Map<String, SkillEntry> entriesByCanonical;
    private final Map<String, SkillEntry> entriesByAlias;

    private SkillTaxonomy(Map<String, SkillEntry> entriesByCanonical) {
        this.entriesByCanonical = Collections.unmodifiableMap(new LinkedHashMap<>(entriesByCanonical));
        Map<String, SkillEntry> aliasIndex = new LinkedHashMap<>();
        for (SkillEntry entry : entriesByCanonical.values()) {
            for (String alias : entry.normalizedAliases()) {
                aliasIndex.putIfAbsent(alias, entry);
            }
        }
        this.entriesByAlias = Collections.unmodifiableMap(aliasIndex);
    }

    public static SkillTaxonomy from(Collection<String> knownSkills) {
        Map<String, SkillEntry> entries = new LinkedHashMap<>();
        for (SkillSeed seed : CURATED_SKILLS) {
            addOrMerge(entries, seed.name(), seed.category(), seed.aliases());
        }
        if (knownSkills != null) {
            for (String rawSkill : knownSkills) {
                if (rawSkill == null || rawSkill.isBlank()) {
                    continue;
                }
                SkillSeed curated = findCuratedSeed(rawSkill).orElse(null);
                if (curated != null) {
                    addOrMerge(entries, curated.name(), curated.category(), curated.aliases());
                } else {
                    addOrMerge(entries, rawSkill.trim(), inferCategory(rawSkill), generatedAliases(rawSkill));
                }
            }
        }
        return new SkillTaxonomy(entries);
    }

    public List<SkillMatch> findMatches(String text) {
        String normalizedText = AiFeatureUtils.normalizeForSearch(text);
        if (normalizedText.isBlank()) {
            return List.of();
        }

        List<AliasCandidate> aliases = entriesByCanonical.values().stream()
                .flatMap(entry -> entry.normalizedAliases().stream()
                        .map(alias -> new AliasCandidate(entry, alias)))
                .sorted(Comparator.comparingInt((AliasCandidate item) -> item.alias().length()).reversed())
                .toList();

        Map<String, SkillMatch> matches = new LinkedHashMap<>();
        for (AliasCandidate candidate : aliases) {
            if (matches.containsKey(candidate.entry().normalizedName())) {
                continue;
            }
            if (containsAlias(normalizedText, candidate.alias())) {
                SkillEntry entry = candidate.entry();
                matches.put(entry.normalizedName(), new SkillMatch(
                        entry.name(),
                        entry.category(),
                        candidate.alias(),
                        entry.displayAliases()));
            }
        }
        return List.copyOf(matches.values());
    }

    public Set<String> extractSkillNames(String text) {
        return findMatches(text).stream()
                .map(SkillMatch::name)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }

    public String canonicalName(String rawSkill) {
        if (rawSkill == null || rawSkill.isBlank()) {
            return "";
        }
        String normalized = AiFeatureUtils.normalizeForSearch(rawSkill);
        SkillEntry entry = entriesByAlias.get(normalized);
        return entry == null ? rawSkill.trim() : entry.name();
    }

    public String categoryOf(String rawSkill) {
        if (rawSkill == null || rawSkill.isBlank()) {
            return "other";
        }
        String normalized = AiFeatureUtils.normalizeForSearch(rawSkill);
        SkillEntry entry = entriesByAlias.get(normalized);
        return entry == null ? inferCategory(rawSkill) : entry.category();
    }

    public boolean matches(String leftSkill, String rightSkill) {
        String left = AiFeatureUtils.normalizeForSearch(canonicalName(leftSkill));
        String right = AiFeatureUtils.normalizeForSearch(canonicalName(rightSkill));
        return !left.isBlank() && left.equals(right);
    }

    private static void addOrMerge(
            Map<String, SkillEntry> entries,
            String rawName,
            String category,
            Collection<String> aliases) {
        String name = displayName(rawName);
        String normalizedName = AiFeatureUtils.normalizeForSearch(name);
        if (normalizedName.isBlank()) {
            return;
        }

        LinkedHashSet<String> normalizedAliases = new LinkedHashSet<>();
        normalizedAliases.add(normalizedName);
        generatedAliases(name).stream()
                .map(AiFeatureUtils::normalizeForSearch)
                .filter(value -> !value.isBlank())
                .forEach(normalizedAliases::add);
        if (aliases != null) {
            aliases.stream()
                    .filter(Objects::nonNull)
                    .map(AiFeatureUtils::normalizeForSearch)
                    .filter(value -> !value.isBlank())
                    .forEach(normalizedAliases::add);
        }

        SkillEntry existing = entries.get(normalizedName);
        if (existing == null) {
            entries.put(normalizedName, new SkillEntry(name, category, normalizedAliases));
        } else {
            LinkedHashSet<String> merged = new LinkedHashSet<>(existing.normalizedAliases());
            merged.addAll(normalizedAliases);
            entries.put(normalizedName, new SkillEntry(existing.name(), existing.category(), merged));
        }
    }

    private static Optional<SkillSeed> findCuratedSeed(String rawSkill) {
        String normalized = AiFeatureUtils.normalizeForSearch(rawSkill);
        return CURATED_SKILLS.stream()
                .filter(seed -> seed.allAliases().contains(normalized))
                .findFirst();
    }

    private static List<String> generatedAliases(String rawSkill) {
        String normalized = AiFeatureUtils.normalizeForSearch(rawSkill);
        if (normalized.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        aliases.add(normalized);
        aliases.add(normalized.replace(".", ""));
        aliases.add(normalized.replace(".", " "));
        aliases.add(normalized.replace("/", ""));
        aliases.add(normalized.replace("/", " "));
        aliases.add(normalized.replace("-", ""));
        aliases.add(normalized.replace("-", " "));
        return aliases.stream().filter(value -> !value.isBlank()).toList();
    }

    private static boolean containsAlias(String normalizedText, String normalizedAlias) {
        if (normalizedAlias.length() < 2) {
            return false;
        }
        String boundary = "(?<![a-z0-9+#])" + Pattern.quote(normalizedAlias) + "(?![a-z0-9+#])";
        return Pattern.compile(boundary).matcher(normalizedText).find();
    }

    private static String displayName(String rawSkill) {
        String trimmed = rawSkill == null ? "" : rawSkill.trim();
        if (trimmed.isBlank()) {
            return "";
        }
        return findCuratedSeed(trimmed).map(SkillSeed::name).orElse(trimmed);
    }

    private static String inferCategory(String rawSkill) {
        String normalized = AiFeatureUtils.normalizeForSearch(rawSkill);
        if (containsAny(normalized, "react", "angular", "vue", "html", "css", "javascript", "typescript")) {
            return "frontend";
        }
        if (containsAny(normalized, "java", "spring", "node", "python", ".net", "api", "microservice")) {
            return "backend";
        }
        if (containsAny(normalized, "mysql", "postgres", "mongodb", "redis", "sql", "oracle")) {
            return "database";
        }
        if (containsAny(normalized, "aws", "azure", "gcp", "docker", "kubernetes", "jenkins", "terraform")) {
            return "devops";
        }
        if (containsAny(normalized, "android", "ios", "flutter", "react native", "swift", "kotlin")) {
            return "mobile";
        }
        if (containsAny(normalized, "machine learning", "ai", "nlp", "spark", "hadoop", "airflow")) {
            return "data";
        }
        if (containsAny(normalized, "selenium", "cypress", "testing", "qa")) {
            return "testing";
        }
        if (containsAny(normalized, "figma", "ui/ux", "design")) {
            return "design";
        }
        return "other";
    }

    private static boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(AiFeatureUtils.normalizeForSearch(fragment))) {
                return true;
            }
        }
        return false;
    }

    public record SkillMatch(
            String name,
            String category,
            String matchedAlias,
            List<String> aliases) {
    }

    private record AliasCandidate(SkillEntry entry, String alias) {
    }

    private record SkillEntry(String name, String category, Set<String> normalizedAliases) {
        private String normalizedName() {
            return AiFeatureUtils.normalizeForSearch(name);
        }

        private List<String> displayAliases() {
            return normalizedAliases.stream()
                    .filter(alias -> !alias.equals(normalizedName()))
                    .limit(8)
                    .toList();
        }
    }

    private record SkillSeed(String name, String category, Set<String> aliases) {
        private Set<String> allAliases() {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            values.add(AiFeatureUtils.normalizeForSearch(name));
            values.addAll(generatedAliases(name));
            values.addAll(aliases);
            return values;
        }
    }

    private static SkillSeed seed(String name, String category, String... aliases) {
        LinkedHashSet<String> normalizedAliases = new LinkedHashSet<>();
        for (String alias : aliases) {
            String normalized = AiFeatureUtils.normalizeForSearch(alias);
            if (!normalized.isBlank()) {
                normalizedAliases.add(normalized);
            }
        }
        return new SkillSeed(name, category, normalizedAliases);
    }
}
