package vn.hoangtung.jobfind.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.hoangtung.jobfind.domain.response.RestResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
// Ghi chú: Hãy đảm bảo đường dẫn này đúng với project của bạn

import java.io.IOException;
import java.util.Optional;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint { // SỬA LỖI: Phải là
                                                                                  // AuthenticationEntryPoint

    private final AuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();

    private final ObjectMapper mapper;

    public CustomAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {

        // Dòng này sẽ thiết lập header WWW-Authenticate theo chuẩn, rất hữu ích
        this.delegate.commence(request, response, authException);

        response.setContentType("application/json;charset=UTF-8");

        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.UNAUTHORIZED.value());

        // An toàn hơn khi kiểm tra null trước khi gọi getCause()
        // String errorMessage = authException.getMessage();
        // if (authException.getCause() != null) {
        // errorMessage = authException.getCause().getMessage();
        // }
        String errorMessage = Optional.ofNullable(authException.getCause())
                .map(Throwable::getMessage)
                .orElse(authException.getMessage());
        res.setError(errorMessage);

        res.setMessage("Token không hợp lệ (hết hạn, không đúng định dạng, hoặc không có)");

        mapper.writeValue(response.getWriter(), res);
    }
}