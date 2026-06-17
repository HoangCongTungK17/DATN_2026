package vn.hoangtung.jobfind.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * ThreadPool riêng cho AI tasks.
     * - corePoolSize=4: luôn giữ 4 thread sẵn sàng
     * - maxPoolSize=8: scale lên tối đa 8 thread khi queue đầy
     * - queueCapacity=50: hàng đợi chứa tối đa 50 task chờ
     * - AbortPolicy: khi queue đầy thì fail-fast, không block HTTP request thread
     * - SecurityContextTaskDecorator: truyền SecurityContext từ HTTP thread sang async thread
     */
    @Bean("aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new SecurityContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * TaskDecorator để propagate SecurityContext sang async thread.
     * Rất quan trọng vì CvDoctorService, InterviewCoachService đều
     * dùng SecurityUtil.getCurrentUserLogin() bên trong.
     */
    private static class SecurityContextTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            SecurityContext context = SecurityContextHolder.getContext();
            return () -> {
                try {
                    SecurityContextHolder.setContext(context);
                    runnable.run();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            };
        }
    }
}
