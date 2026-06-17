package vn.hoangtung.jobfind.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiGatewayService {

    private static final int MAX_RETRIES = 2;
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(30);

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, CacheEntry> promptCache = new ConcurrentHashMap<>();

    public AiGatewayService(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public String callText(String promptText, String context) {
        return callText(promptText, context, null, Duration.ZERO);
    }

    public String callText(String promptText, String context, String cacheKey) {
        return callText(promptText, context, cacheKey, DEFAULT_CACHE_TTL);
    }

    public String callText(String promptText, String context, String cacheKey, Duration ttl) {
        return callText(promptText, context, cacheKey, ttl, AiCallOptions.defaults());
    }

    public String callText(String promptText, String context, String cacheKey, Duration ttl, AiCallOptions options) {
        if (promptText == null || promptText.isBlank()) {
            throw new IllegalArgumentException("Prompt không được để trống");
        }

        String effectiveKey = cacheKey == null || cacheKey.isBlank() ? null : cacheKey;
        if (effectiveKey != null) {
            CacheEntry cached = promptCache.get(effectiveKey);
            if (cached != null && !cached.isExpired(ttl)) {
                return cached.response();
            }
        }

        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String response = chatModel.call(buildPrompt(promptText, options))
                        .getResult()
                        .getOutput()
                        .getContent();

                if (effectiveKey != null) {
                    promptCache.put(effectiveKey, new CacheEntry(response, Instant.now()));
                }
                return response;
            } catch (Exception e) {
                lastError = e;
                System.out.println(">>> [" + context + "] ⚠️ Lần " + attempt + " thất bại: " + e.getMessage());
            }
        }

        throw new RuntimeException("AI không phản hồi cho " + context + ": "
                + (lastError != null ? lastError.getMessage() : "unknown error"));
    }

    private Prompt buildPrompt(String promptText, AiCallOptions options) {
        AiCallOptions safeOptions = options == null ? AiCallOptions.defaults() : options;
        if (safeOptions.isDefault()) {
            return new Prompt(promptText);
        }

        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        if (safeOptions.temperature() != null) {
            builder.withTemperature(safeOptions.temperature());
        }
        if (safeOptions.maxTokens() != null) {
            builder.withMaxTokens(safeOptions.maxTokens());
        }
        return new Prompt(promptText, builder.build());
    }

    public JsonNode readJsonTreeFromResponse(String rawResponse) {
        try {
            return objectMapper.readTree(extractJsonFromResponse(rawResponse));
        } catch (Exception e) {
            throw new IllegalArgumentException("AI trả về JSON không hợp lệ: " + e.getMessage(), e);
        }
    }

    public String extractJsonFromResponse(String response) {
        if (response == null) {
            return "{}";
        }

        String trimmed = response.trim();

        if (trimmed.contains("```json")) {
            int start = trimmed.indexOf("```json") + 7;
            int end = trimmed.indexOf("```", start);
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }

        if (trimmed.contains("```")) {
            int start = trimmed.indexOf("```") + 3;
            int end = trimmed.indexOf("```", start);
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }

        int braceStart = trimmed.indexOf('{');
        int braceEnd = trimmed.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            return trimmed.substring(braceStart, braceEnd + 1).trim();
        }

        return trimmed;
    }

    public String boundedBlock(String label, String rawContent, int maxChars) {
        String content = sanitizeForPrompt(rawContent, maxChars);
        return "<" + label + ">\n" + content + "\n</" + label + ">";
    }

    public String sanitizeForPrompt(String rawContent, int maxChars) {
        if (rawContent == null || rawContent.isBlank()) {
            return "";
        }

        String sanitized = rawContent
                .replace("\u0000", "")
                .replace("```", "'''")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[\\p{Cntrl}&&[^\n\t]]", " ")
                .trim();

        if (maxChars > 0 && sanitized.length() > maxChars) {
            return sanitized.substring(0, maxChars) + "\n\n[... nội dung đã được cắt bớt ...]";
        }

        return sanitized;
    }

    public String fingerprint(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                digest.update((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Thiếu SHA-256 trong runtime", e);
        }
    }

    public void clearCache() {
        promptCache.clear();
    }

    public Map<String, CacheEntry> snapshotCache() {
        return Map.copyOf(promptCache);
    }

    public record CacheEntry(String response, Instant createdAt) {
        public boolean isExpired(Duration ttl) {
            return ttl == null || ttl.isZero() || ttl.isNegative()
                    ? false
                    : createdAt.plus(ttl).isBefore(Instant.now());
        }
    }

    public record AiCallOptions(Float temperature, Integer maxTokens) {
        public AiCallOptions {
            if (temperature != null) {
                temperature = Math.max(0f, Math.min(2f, temperature));
            }
            if (maxTokens != null && maxTokens <= 0) {
                maxTokens = null;
            }
        }

        public static AiCallOptions defaults() {
            return new AiCallOptions(null, null);
        }

        private boolean isDefault() {
            return temperature == null && maxTokens == null;
        }
    }
}
