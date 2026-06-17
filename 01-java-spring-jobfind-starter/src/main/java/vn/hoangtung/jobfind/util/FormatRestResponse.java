package vn.hoangtung.jobfind.util;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import vn.hoangtung.jobfind.domain.response.RestResponse;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

@ControllerAdvice
public class FormatRestResponse implements ResponseBodyAdvice {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return !returnType.getParameterType().equals(RestResponse.class)
                && !SseEmitter.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        int status = servletResponse.getStatus();

        if (selectedContentType != null && MediaType.TEXT_EVENT_STREAM.isCompatibleWith(selectedContentType)) {
            return body;
        }
        if (body instanceof Resource
                || (selectedContentType != null && MediaType.APPLICATION_OCTET_STREAM.isCompatibleWith(selectedContentType))) {
            return body;
        }

        RestResponse<Object> res = new RestResponse<Object>();
        res.setStatusCode(status);

        if (status >= 400) {
            // case error
            return body;
        } else {
            res.setData(body);
            ApiMessage message = returnType.getMethodAnnotation(ApiMessage.class);
            res.setMessage(message != null ? message.value() : "CALL API SUCCESS");
        }

        String path = request.getURI().getPath();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return body;
        }

        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(res);
            } catch (Exception e) {
                return body; // Nếu lỗi thì trả về string gốc
            }
        }
        return res;
    }

}
