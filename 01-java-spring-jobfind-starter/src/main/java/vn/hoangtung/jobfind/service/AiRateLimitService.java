package vn.hoangtung.jobfind.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AiRateLimitService {

    private static final Duration CHAT_WINDOW = Duration.ofMinutes(1);
    private static final int CHAT_LIMIT_PER_WINDOW = 20;
    private static final Duration TASK_WINDOW = Duration.ofMinutes(1);
    private static final int TASK_LIMIT_PER_WINDOW = 10;
    private static final Duration STALE_ENTRY_TTL = Duration.ofMinutes(10);

    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    public void checkChatLimit(String key) {
        checkLimit("chat:" + normalizeKey(key), CHAT_WINDOW, CHAT_LIMIT_PER_WINDOW);
    }

    public void checkTaskLimit(String key, String taskType) {
        String effectiveTaskType = taskType == null || taskType.isBlank() ? "unknown" : taskType;
        checkLimit("task:" + effectiveTaskType + ":" + normalizeKey(key), TASK_WINDOW, TASK_LIMIT_PER_WINDOW);
    }

    private void checkLimit(String counterKey, Duration window, int limit) {
        Instant now = Instant.now();
        Counter counter = counters.computeIfAbsent(counterKey, ignored -> new Counter(now));

        synchronized (counter) {
            if (counter.windowStartedAt.plus(window).isBefore(now)) {
                counter.windowStartedAt = now;
                counter.count = 0;
            }
            counter.lastSeenAt = now;
            counter.count++;
            if (counter.count > limit) {
                throw new IllegalArgumentException("Too many AI requests. Please try again later.");
            }
        }
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupStaleCounters() {
        Instant cutoff = Instant.now().minus(STALE_ENTRY_TTL);
        counters.entrySet().removeIf(entry -> entry.getValue().lastSeenAt.isBefore(cutoff));
    }

    private String normalizeKey(String key) {
        return key == null || key.isBlank() ? "anonymous" : key;
    }

    private static final class Counter {
        private Instant windowStartedAt;
        private Instant lastSeenAt;
        private int count;

        private Counter(Instant now) {
            this.windowStartedAt = now;
            this.lastSeenAt = now;
        }
    }
}
