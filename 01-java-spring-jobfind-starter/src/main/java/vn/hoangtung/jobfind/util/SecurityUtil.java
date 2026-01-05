package vn.hoangtung.jobfind.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.util.Base64;

import vn.hoangtung.jobfind.domain.response.ResLoginDTO;

@Service // Đánh dấu đây là một Bean (Service), để có thể inject JwtEncoder vào
public class SecurityUtil {

    // Spring sẽ inject (tiêm) Bean `JwtEncoder` đã được định nghĩa trong
    // SecurityConfig.java
    private final JwtEncoder jwtEncoder;

    // Constructor-based injection
    public SecurityUtil(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    // Hằng số định nghĩa thuật toán Mã hóa.
    // Dùng chung cho cả tạo (Encoder) và giải mã (Decoder)
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    // Tiêm các giá trị cấu hình từ file application.properties
    @Value("${hoangtung.jwt.base64-secret}")
    private String jwtKey; // Khóa bí mật

    @Value("${hoangtung.jwt.access-token-validity-in-seconds}")
    private long accessTokenExpiration; // Thời gian hết hạn Access Token (giây)

    @Value("${hoangtung.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration; // Thời gian hết hạn Refresh Token (giây)

    /**
     * Tạo ra Access Token (token truy cập).
     *
     * @param email Email của người dùng (sẽ được dùng làm 'subject' của token).
     * @param dto   Đối tượng chứa thông tin user để đưa vào token.
     * @return Chuỗi Access Token đã được ký.
     */
    public String createAccessToken(String email, ResLoginDTO dto) {
        // Chuẩn bị thông tin user sẽ được nhúng vào token
        ResLoginDTO.UserInsideToken userToken = new ResLoginDTO.UserInsideToken();
        userToken.setId(dto.getUser().getId());
        userToken.setEmail(dto.getUser().getEmail());
        userToken.setName(dto.getUser().getName());

        Instant now = Instant.now(); // Thời điểm hiện tại
        // Tính thời điểm hết hạn của Access Token
        Instant validity = now.plus(this.accessTokenExpiration, ChronoUnit.SECONDS);

        // Lấy danh sách quyền (permission) của user
        // LƯU Ý QUAN TRỌNG: Ở ĐÂY BẠN ĐANG HARDCODE (GÁN CỨNG) QUYỀN!
        // Trong thực tế, bạn PHẢI lấy danh sách quyền này từ `UserService`
        // dựa trên `email` hoặc `dto` của người dùng.
        List<String> listAuthority = new ArrayList<String>();
        listAuthority.add("ROLE_USER_CREATE");
        listAuthority.add("ROLE_USER_UPDATE");

        // Bắt đầu xây dựng các "claims" (thông tin chứa trong) của JWT
        // @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now) // iat: Thời điểm phát hành
                .expiresAt(validity) // exp: Thời điểm hết hạn
                .subject(email) // sub: Chủ thể của token (thường là username/email)
                .claim("user", userToken) // claim tùy chỉnh: nhúng đối tượng thông tin user
                .claim("permission", listAuthority) // claim tùy chỉnh: nhúng danh sách quyền
                .build();
        // @formatter:on

        // Tạo JWS Header (JSON Web Signature Header), chỉ định thuật toán sẽ dùng để ký
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();

        // Mã hóa (ký) token
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    /**
     * Tạo ra Refresh Token (token làm mới).
     *
     * @param email Email của người dùng.
     * @param dto   Đối tượng chứa thông tin user.
     * @return Chuỗi Refresh Token đã được ký.
     */
    public String createRefreshToken(String email, ResLoginDTO dto) {
        Instant now = Instant.now();
        // Tính thời điểm hết hạn của Refresh Token (dài hơn Access Token)
        Instant validity = now.plus(this.refreshTokenExpiration, ChronoUnit.SECONDS);

        // Chuẩn bị thông tin user (tương tự Access Token)
        ResLoginDTO.UserInsideToken userToken = new ResLoginDTO.UserInsideToken();
        userToken.setId(dto.getUser().getId());
        userToken.setEmail(dto.getUser().getEmail());
        userToken.setName(dto.getUser().getName());

        // @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("user", userToken)
                // QUAN TRỌNG: Refresh Token KHÔNG cần và KHÔNG NÊN chứa quyền (permission)
                // vì nó chỉ có một mục đích duy nhất là "lấy Access Token mới".
                .build();
        // @formatter:on

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    /**
     * Hàm private tiện ích: Chuyển đổi chuỗi Secret Key (Base64) thành đối tượng
     * SecretKey.
     * Hàm này dùng nội bộ cho `checkValidRefreshToken`.
     */
    private SecretKey getSecretKey() {
        // Giải mã chuỗi Base64
        byte[] keyBytes = Base64.from(jwtKey).decode();
        // Tạo SecretKey dùng cho thuật toán đã định nghĩa
        return new SecretKeySpec(keyBytes, 0, keyBytes.length,
                JWT_ALGORITHM.getName());
    }

    /**
     * Hàm này dùng để **kiểm tra thủ công** một Refresh Token.
     * Nó không phải là Bean `JwtDecoder` mà Spring Security tự động dùng.
     * `AuthController` sẽ gọi hàm này khi xử lý API `/refresh`.
     *
     * @param token Chuỗi Refresh Token lấy từ cookie.
     * @return Đối tượng Jwt đã được giải mã và xác thực.
     * @throws Exception Ném ra lỗi nếu token không hợp lệ (hết hạn, sai chữ ký...).
     */
    public Jwt checkValidRefreshToken(String token) {
        // Tạo một trình giải mã (decoder) CỤC BỘ,
        // sử dụng cùng SecretKey và thuật toán
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
                getSecretKey()).macAlgorithm(SecurityUtil.JWT_ALGORITHM).build();

        try {
            // Thử giải mã và xác thực token
            return jwtDecoder.decode(token);
        } catch (Exception e) {
            // Nếu thất bại (hết hạn, sai chữ ký,...)
            System.out.println(">>> JWT error: " + e.getMessage());
            throw e; // Ném lỗi ra để `AuthController` hoặc `GlobalException` xử lý
        }
    }

    /**
     * Hàm `static` tiện ích: Lấy email (principal) của người dùng
     * đang đăng nhập từ `SecurityContextHolder`.
     *
     * @return Optional<String> chứa email nếu đã xác thực, ngược lại là Optional
     *         rỗng.
     */
    public static Optional<String> getCurrentUserLogin() {
        // SecurityContextHolder là nơi Spring Security lưu trữ thông tin của user đã
        // được xác thực
        SecurityContext securityContext = SecurityContextHolder.getContext();
        // Lấy đối tượng Authentication (chứa thông tin user và quyền)
        Authentication authentication = securityContext.getAuthentication();
        // Gọi hàm extractPrincipal để lấy tên user (email)
        return Optional.ofNullable(extractPrincipal(authentication));
    }

    /**
     * Hàm `static` private: Trích xuất 'principal' (thường là username/email)
     * từ đối tượng Authentication.
     */
    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        // Khi Spring Security xác thực bằng `UserDetailsService`, principal là
        // `UserDetails`
        else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        }
        // Khi Spring Security xác thực bằng JWT, principal là đối tượng `Jwt`
        else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject(); // Lấy 'subject' (email) từ token
        }
        // Các trường hợp khác (ví dụ: principal là String)
        else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null; // Không thể trích xuất
    }

    /**
     * Hàm `static` tiện ích: Lấy chuỗi JWT (Access Token) thô
     * từ `SecurityContextHolder`.
     * (Hàm này có vẻ không được sử dụng trong luồng chính của bạn).
     */
    public static Optional<String> getCurrentUserJWT() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
                // Token thường được lưu trong 'credentials' khi dùng JWT
                .filter(authentication -> authentication.getCredentials() instanceof String)
                .map(authentication -> (String) authentication.getCredentials());
    }

}