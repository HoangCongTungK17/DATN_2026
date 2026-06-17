package vn.hoangtung.jobfind.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.hoangtung.jobfind.domain.Permission;
import vn.hoangtung.jobfind.domain.Role;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.service.UserService;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.error.PermissionException;

public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    UserService userService;

    @Override
    @Transactional
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler)
            throws Exception {

        String path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String httpMethod = request.getMethod();

        if (isHandledOutsidePermissionTable(request, path, httpMethod)) {
            return true;
        }

        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        if (email.isEmpty()) {
            throw new PermissionException("You do not have permission to access this endpoint.");
        }

        User user = this.userService.handleGetUserByUsername(email);
        if (user == null || user.getRole() == null) {
            throw new PermissionException("You do not have permission to access this endpoint.");
        }

        Role role = user.getRole();
        List<Permission> permissions = role.getPermissions();
        boolean isAllow = permissions != null
                && permissions.stream().anyMatch(item -> item.getApiPath().equals(path)
                        && item.getMethod().equals(httpMethod));

        if (isAllow == false) {
            throw new PermissionException("You do not have permission to access this endpoint.");
        }

        return true;
    }

    private boolean isHandledOutsidePermissionTable(HttpServletRequest request, String path, String httpMethod) {
        if (path == null || httpMethod == null) {
            return true;
        }
        if (path.startsWith("/api/v1/auth/") || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui") || path.startsWith("/storage/") || "/".equals(path)) {
            return true;
        }
        if ("GET".equals(httpMethod)
                && (path.startsWith("/api/v1/companies")
                        || path.startsWith("/api/v1/jobs")
                        || path.startsWith("/api/v1/skills"))) {
            if (isAdminScopeRequest(request)
                    && (path.startsWith("/api/v1/companies") || path.startsWith("/api/v1/jobs"))) {
                return false;
            }
            return true;
        }
        if ("POST".equals(httpMethod) && "/api/v1/users".equals(path)) {
            return true;
        }
        if ("PUT".equals(httpMethod)
                && ("/api/v1/users/profile".equals(path)
                        || "/api/v1/users/change-password".equals(path))) {
            return true;
        }
        if (path.startsWith("/api/v1/ai/") || path.startsWith("/api/v1/resumes")) {
            return true;
        }
        if ("POST".equals(httpMethod) && "/api/v1/files".equals(path)) {
            return true;
        }
        if (path.startsWith("/api/v1/subscribers")) {
            return true;
        }
        return false;
    }

    private boolean isAdminScopeRequest(HttpServletRequest request) {
        return "admin".equalsIgnoreCase(request.getParameter("scope"));
    }
}
