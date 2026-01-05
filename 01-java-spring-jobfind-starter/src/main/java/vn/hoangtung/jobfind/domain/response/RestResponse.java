package vn.hoangtung.jobfind.domain.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestResponse<T> {

    private int statusCode; // Bỏ final
    private String error; // Bỏ final
    private Object message; // Bỏ final
    private T data; // Bỏ final

    // Constructor rỗng (giờ đã hợp lệ)
    public RestResponse() {
    }

    // Constructor đầy đủ tham số (vẫn có thể giữ lại)
    public RestResponse(int statusCode, String error, Object message, T data) {
        this.statusCode = statusCode;
        this.error = error;
        this.message = message;
        this.data = data;
    }

    // Getters (giữ nguyên)
    public int getStatusCode() {
        return statusCode;
    }

    public String getError() {
        return error;
    }

    public Object getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    // Setters (hoàn thiện logic)
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setMessage(Object body) {
        this.message = body;
    }

    public void setData(T data) {
        this.data = data;
    }
}