package vn.hoangtung.jobfind.service;

import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import vn.hoangtung.jobfind.domain.Company;
import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Resume;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.repository.UserRepository;
import vn.hoangtung.jobfind.util.SecurityUtil;

@Service
public class DataScopeService {

    private final UserRepository userRepository;

    public DataScopeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getCurrentUser() {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        if (email.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.userRepository.findByEmail(email));
    }

    public User getCurrentUserOrThrow() {
        return getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Vui long dang nhap"));
    }

    public boolean isAdmin(User user) {
        return roleName(user).contains("ADMIN");
    }

    public boolean isHr(User user) {
        String roleName = roleName(user);
        return roleName.contains("HR") || roleName.contains("RECRUITER");
    }

    public boolean isHrOnly(User user) {
        return isHr(user) && !isAdmin(user);
    }

    public String roleName(User user) {
        return user != null && user.getRole() != null && user.getRole().getName() != null
                ? user.getRole().getName().toUpperCase()
                : "";
    }

    public Company getCurrentCompanyOrThrow(User user) {
        if (user == null || user.getCompany() == null) {
            throw new AccessDeniedException("Tai khoan HR chua duoc gan cong ty");
        }
        return user.getCompany();
    }

    public boolean isSameCompany(Company first, Company second) {
        return first != null && second != null && first.getId() == second.getId();
    }

    public boolean isJobInCurrentCompany(Job job, User user) {
        return job != null && isSameCompany(job.getCompany(), user != null ? user.getCompany() : null);
    }

    public boolean isResumeInCurrentCompany(Resume resume, User user) {
        return resume != null && isJobInCurrentCompany(resume.getJob(), user);
    }

    public Specification<Company> companyScopeFor(User user) {
        if (isAdmin(user)) {
            return allowAll();
        }
        if (isHrOnly(user)) {
            long companyId = getCurrentCompanyOrThrow(user).getId();
            return (root, query, cb) -> cb.equal(root.get("id"), companyId);
        }
        return denyAll();
    }

    public Specification<Job> jobScopeFor(User user) {
        if (isAdmin(user)) {
            return allowAll();
        }
        if (isHrOnly(user)) {
            long companyId = getCurrentCompanyOrThrow(user).getId();
            return (root, query, cb) -> cb.equal(root.get("company").get("id"), companyId);
        }
        return denyAll();
    }

    private <T> Specification<T> denyAll() {
        return (root, query, cb) -> cb.disjunction();
    }

    private <T> Specification<T> allowAll() {
        return (root, query, cb) -> cb.conjunction();
    }
}
