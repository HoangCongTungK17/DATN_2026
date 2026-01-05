package vn.hoangtung.jobfind.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Cho phép frontend (React) chạy ở localhost:3000 được gọi API
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:4173",
                "http://localhost:5173"));

        // Cho phép các phương thức HTTP cụ thể
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));

        // Cho phép các header được gửi kèm trong request
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "x-no-retry"));

        // Cho phép gửi cookie (ví dụ: JSESSIONID) từ frontend sang backend
        configuration.setAllowCredentials(true);

        // Thời gian cache kết quả “preflight request” (OPTIONS) tính bằng giây
        configuration.setMaxAge(3600L);

        // Đăng ký cấu hình CORS cho toàn bộ các endpoint (/**)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
