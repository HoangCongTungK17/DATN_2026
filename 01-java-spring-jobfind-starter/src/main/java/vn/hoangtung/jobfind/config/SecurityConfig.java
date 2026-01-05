package vn.hoangtung.jobfind.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;

import vn.hoangtung.jobfind.util.SecurityUtil;

@Configuration // Đánh dấu đây là một file cấu hình của Spring
@EnableMethodSecurity(securedEnabled = true) // Bật tính năng bảo mật ở cấp độ phương thức.
// `securedEnabled = true` cho phép bạn dùng annotation @Secured("ROLE_ADMIN")
// trên các hàm
public class SecurityConfig {

    // Tiêm (inject) giá trị từ file application.properties
    @Value("${hoangtung.jwt.base64-secret}")
    private String jwtKey; // Biến này sẽ chứa chuỗi secret key (đã mã hóa base64)

    /**
     * @Bean: Đăng ký một Bean tên là "passwordEncoder" vào Spring Context.
     *        Bất cứ nơi nào cần @Autowired PasswordEncoder, Spring sẽ cung cấp Bean
     *        này.
     *        * @return Một trình mã hóa mật khẩu sử dụng thuật toán BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Sử dụng BCrypt để mã hóa mật khẩu
    }

    /**
     * @Bean: Đây là Bean quan trọng nhất, định nghĩa toàn bộ "chuỗi lọc" (filter
     *        chain) bảo mật.
     *        Mọi request gửi đến ứng dụng đều phải đi qua chuỗi lọc này.
     *        * @param http Đối tượng HttpSecurity để xây dựng cấu
     *        hình.
     * @param customAuthenticationEntryPoint Điểm xử lý lỗi 401 tùy chỉnh (được
     *                                       inject từ Bean khác, thường là
     *                                       CustomAuthenticationEntryPoint.java).
     * @return Chuỗi lọc bảo mật đã được cấu hình.
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint) throws Exception {

        // Danh sách các đường dẫn (endpoint) được phép truy cập công khai mà không cần
        // xác thực.
        String[] whiteList = {
                "/", // Trang chủ
                "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/register", // Các API xác thực
                "/storage/**", // Đường dẫn đến file tĩnh (ảnh,...)
                "/api/v1/email/**", // API gửi email (nếu có)
                "/v3/api-docs/**", // API docs của Swagger
                "/swagger-ui/**", // Giao diện Swagger UI
                "/swagger-ui.html" // Trang Swagger UI
        };

        http
                // 1. Tắt CSRF (Cross-Site Request Forgery)
                // Bắt buộc phải tắt khi dùng API stateless (không dùng session, chỉ dùng JWT)
                .csrf(c -> c.disable())

                // 2. Bật CORS (Cross-Origin Resource Sharing)
                // Cho phép các tên miền khác (ví dụ: React app ở localhost:3000) gọi API
                .cors(Customizer.withDefaults())

                // 3. Phân quyền cho các request (Đây là phần chính)
                .authorizeHttpRequests(
                        authz -> authz
                                // Cho phép tất cả các request trong whiteList
                                .requestMatchers(whiteList).permitAll()

                                // Cho phép request GET đến các tài nguyên công khai (xem công ty, job,
                                // skill)
                                .requestMatchers(HttpMethod.GET, "/api/v1/companies/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/skills/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                                .requestMatchers("/api/v1/ai/**").permitAll()
                                // Bất kỳ request nào khác (không khớp các quy tắc trên) đều phải được xác
                                // thực
                                .anyRequest().authenticated())

                // 4. Cấu hình xác thực bằng OAuth2 Resource Server (sử dụng JWT)
                .oauth2ResourceServer((oauth2) -> oauth2
                        // Bảo Spring Security sử dụng cấu hình JWT.
                        // .jwt(Customizer.withDefaults()) sẽ tự động tìm đến Bean `JwtDecoder` bạn
                        // định nghĩa bên dưới.
                        .jwt(Customizer.withDefaults())
                        // Khi xác thực thất bại (token sai, hết hạn, không có token...)
                        // Spring sẽ gọi đến `customAuthenticationEntryPoint` để trả về lỗi 401 JSON
                        // thay vì trang login.
                        .authenticationEntryPoint(customAuthenticationEntryPoint))

                // 5. Tắt giao diện đăng nhập bằng Form HTML (vì ta dùng API)
                .formLogin(f -> f.disable())

                // 6. Cấu hình quản lý session: STATELESS (Không trạng thái)
                // CỰC KỲ QUAN TRỌNG: Bảo Spring không tạo hoặc sử dụng session
                // Mọi request đều phải tự chứa thông tin xác thực (chính là JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Xây dựng và trả về cấu hình
        return http.build();
    }

    /**
     * @Bean: Tùy chỉnh cách Spring Security "đọc" JWT.
     *        Cụ thể là đọc danh sách quyền (authorities) từ bên trong token.
     *        * @return Một bộ chuyển đổi (converter) để "dịch" JWT.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // Rất quan trọng: Bỏ tiền tố "SCOPE_" mặc định.
        // Nếu không có dòng này, quyền "ROLE_ADMIN" trong token sẽ bị Spring hiểu là
        // "SCOPE_ROLE_ADMIN" -> gây lỗi 403.
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        // Bảo Spring đọc danh sách quyền từ claim (trường) có tên là "permission"
        // trong JWT.
        grantedAuthoritiesConverter.setAuthoritiesClaimName("permission");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    /**
     * @Bean: Định nghĩa cách **GIẢI MÃ** và **XÁC THỰC** Access Token.
     *        Bean này sẽ được `oauth2ResourceServer` (ở trên) tự động sử dụng.
     *        * @return Một trình giải mã JWT (JwtDecoder).
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Tạo trình giải mã NimbusJwtDecoder
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
                getSecretKey()) // Sử dụng khóa bí mật (lấy từ hàm getSecretKey())
                .macAlgorithm(SecurityUtil.JWT_ALGORITHM) // Chỉ định thuật toán (phải khớp lúc tạo)
                .build();

        // Trả về một lambda function (implementation của JwtDecoder)
        // bọc ngoài trình giải mã Nimbus
        return token -> {
            try {
                // Thử giải mã token
                return jwtDecoder.decode(token);
            } catch (Exception e) {
                // Nếu có lỗi (token hết hạn, chữ ký sai...)
                // In lỗi ra console để debug và ném exception
                System.out.println(">>> JWT error: " + e.getMessage());
                throw e;
            }
        };
    }

    /**
     * @Bean: Định nghĩa cách **TẠO (MÃ HÓA)** Access Token.
     *        Bean này sẽ được @Autowired trong `SecurityUtil` để tạo token.
     *        * @return Một trình mã hóa JWT (JwtEncoder).
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        // Cung cấp khóa bí mật cho trình mã hóa
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    /**
     * Hàm private tiện ích để chuyển đổi chuỗi Base64 `jwtKey`
     * (từ application.properties) thành đối tượng `SecretKey`.
     * * @return Đối tượng SecretKey.
     */
    private SecretKey getSecretKey() {
        // 1. Giải mã chuỗi Base64 `jwtKey` thành một mảng byte[]
        byte[] keyBytes = Base64.from(jwtKey).decode();

        // 2. Tạo một đối tượng SecretKey từ mảng byte và tên thuật toán
        // Thuật toán này (ví dụ: HmacSHA512) phải được lưu nhất quán (ví dụ: trong
        // SecurityUtil.JWT_ALGORITHM)
        return new SecretKeySpec(keyBytes, 0, keyBytes.length,
                SecurityUtil.JWT_ALGORITHM.getName());
    }
}
