package vn.hoangtung.jobfind.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.turkraft.springfilter.builder.FilterBuilder;
import com.turkraft.springfilter.converter.FilterSpecification;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import com.turkraft.springfilter.parser.node.FilterNode;

import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Resume;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.response.ResultPaginationDTO;
import vn.hoangtung.jobfind.domain.response.resume.ResCreateResumeDTO;
import vn.hoangtung.jobfind.domain.response.resume.ResFetchResumeDTO;
import vn.hoangtung.jobfind.domain.response.resume.ResUpdateResumeDTO;
import vn.hoangtung.jobfind.repository.JobRepository;
import vn.hoangtung.jobfind.repository.ResumeRepository;
import vn.hoangtung.jobfind.repository.UserRepository;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.constant.ResumeStateEnum;

@Service
public class ResumeService {

    @Autowired
    FilterBuilder filterBuilder;

    @Autowired
    private FilterParser filterParser;

    @Autowired
    private FilterSpecificationConverter filterSpecificationConverter;

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final CvVectorService cvVectorService;

    public ResumeService(
            ResumeRepository resumeRepository,
            UserRepository userRepository,
            JobRepository jobRepository,
            CvVectorService cvVectorService) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.cvVectorService = cvVectorService;
    }

    public Optional<Resume> fetchById(long id) {
        return this.resumeRepository.findById(id);
    }

    public boolean checkResumeExistByUserAndJob(Resume resume) {
        // check user by id
        if (resume.getUser() == null)
            return false;
        Optional<User> userOptional = this.userRepository.findById(resume.getUser().getId());
        if (userOptional.isEmpty())
            return false;

        // check job by id
        if (resume.getJob() == null)
            return false;
        Optional<Job> jobOptional = this.jobRepository.findById(resume.getJob().getId());
        if (jobOptional.isEmpty())
            return false;

        return true;
    }

    public ResCreateResumeDTO create(Resume resume) {
        User currentUser = getCurrentUserOrThrow();
        Job job = resolveJobOrThrow(resume);
        if (isHr(currentUser) && !isAdmin(currentUser) && !isJobOwnedByCurrentCompany(job, currentUser)) {
            throw new IllegalArgumentException("Ban khong co quyen tao CV cho job ngoai cong ty cua minh");
        }

        // chặn ứng tuyển vào job đã đóng hoặc hết hạn (admin được bỏ qua để thao tác dữ liệu)
        ensureJobOpenForApplication(job, currentUser);

        if (!isAdmin(currentUser) && !isHr(currentUser)) {
            resume.setUser(currentUser);
            resume.setEmail(currentUser.getEmail());
        } else if (resume.getUser() != null) {
            User resumeUser = userRepository.findById(resume.getUser().getId())
                    .orElseThrow(() -> new IllegalArgumentException("User cua CV khong ton tai"));
            resume.setUser(resumeUser);
            if (resume.getEmail() == null || resume.getEmail().isBlank()) {
                resume.setEmail(resumeUser.getEmail());
            }
        }
        resume.setJob(job);

        // chặn ứng tuyển trùng: mỗi user chỉ nộp 1 lần cho 1 job
        if (resume.getUser() != null
                && this.resumeRepository.existsByUserIdAndJobId(resume.getUser().getId(), job.getId())) {
            throw new IllegalArgumentException("Bạn đã ứng tuyển vị trí này rồi");
        }

        resume = this.resumeRepository.save(resume);
        ResCreateResumeDTO res = new ResCreateResumeDTO();
        res.setId(resume.getId());
        res.setCreatedBy(resume.getCreatedBy());
        res.setCreatedAt(resume.getCreatedAt());
        return res;
    }

    public ResUpdateResumeDTO updateStatusForCurrentUser(long resumeId, ResumeStateEnum status) {
        Resume resume = this.resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));
        User currentUser = getCurrentUserOrThrow();
        // Đổi trạng thái hồ sơ ứng tuyển là đặc quyền của nhà tuyển dụng (ADMIN/HR
        // phụ trách job), ứng viên không được tự duyệt hồ sơ của mình.
        ensureCanReviewResume(resume, currentUser);
        resume.setStatus(status);
        return update(resume);
    }

    public void deleteForCurrentUser(long id) {
        Resume resume = this.resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));
        User currentUser = getCurrentUserOrThrow();
        ensureCanManageResume(resume, currentUser);
        cvVectorService.deleteResumeVectorsSafely(id);
        this.resumeRepository.deleteById(id);
    }

    public ResFetchResumeDTO getResumeForCurrentUser(long id) {
        Resume resume = this.resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));
        User currentUser = getCurrentUserOrThrow();
        ensureCanReadResume(resume, currentUser);
        return getResume(resume);
    }

    public ResUpdateResumeDTO update(Resume resume) {
        resume = this.resumeRepository.save(resume);
        ResUpdateResumeDTO res = new ResUpdateResumeDTO();
        res.setUpdatedAt(resume.getUpdatedAt());
        res.setUpdatedBy(resume.getUpdatedBy());
        return res;
    }

    public void delete(long id) {
        cvVectorService.deleteResumeVectorsSafely(id);
        this.resumeRepository.deleteById(id);
    }

    public ResFetchResumeDTO getResume(Resume resume) {
        ResFetchResumeDTO res = new ResFetchResumeDTO();
        res.setId(resume.getId());
        res.setEmail(resume.getEmail());
        res.setUrl(resume.getUrl());
        res.setStatus(resume.getStatus());
        res.setCreatedAt(resume.getCreatedAt());
        res.setCreatedBy(resume.getCreatedBy());
        res.setUpdatedAt(resume.getUpdatedAt());
        res.setUpdatedBy(resume.getUpdatedBy());
        res.setAiMatchScore(resume.getAiMatchScore());

        if (resume.getJob() != null) {
            if (resume.getJob().getCompany() != null) {
                res.setCompanyName(resume.getJob().getCompany().getName());
            }
            res.setJob(new ResFetchResumeDTO.JobResume(resume.getJob().getId(), resume.getJob().getName()));
        }

        if (resume.getUser() != null) {
            res.setUser(new ResFetchResumeDTO.UserResume(resume.getUser().getId(), resume.getUser().getName()));
        }

        return res;
    }

    public ResultPaginationDTO fetchAllResume(Specification<Resume> spec, Pageable pageable) {
        Page<Resume> pageResume = this.resumeRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageResume.getTotalPages());
        mt.setTotal(pageResume.getTotalElements());

        rs.setMeta(mt);

        // remove sensitive data
        List<ResFetchResumeDTO> listResume = pageResume.getContent()
                .stream().map(item -> this.getResume(item))
                .collect(Collectors.toList());

        rs.setResult(listResume);

        return rs;
    }

    public ResultPaginationDTO fetchVisibleResumes(Specification<Resume> spec, Pageable pageable) {
        User currentUser = getCurrentUserOrThrow();
        Specification<Resume> visibilitySpec = visibleToUserSpec(currentUser);
        Specification<Resume> finalSpec = spec == null ? visibilitySpec : visibilitySpec.and(spec);
        return fetchAllResume(finalSpec, pageable);
    }

    public ResultPaginationDTO fetchResumeByUser(Pageable pageable) {
        return fetchAllResume(ownedByCurrentUserSpec(getCurrentUserOrThrow()), pageable);
    }

    private User getCurrentUserOrThrow() {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        User currentUser = this.userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new IllegalArgumentException("Vui long dang nhap");
        }
        return currentUser;
    }

    private Job resolveJobOrThrow(Resume resume) {
        if (resume.getJob() == null || resume.getJob().getId() <= 0) {
            throw new IllegalArgumentException("Job id khong hop le");
        }
        return this.jobRepository.findById(resume.getJob().getId())
                .orElseThrow(() -> new IllegalArgumentException("Job khong ton tai"));
    }

    private void ensureJobOpenForApplication(Job job, User currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (!job.isActive()) {
            throw new IllegalArgumentException("Job nay da dong, khong the ung tuyen");
        }
        if (job.getEndDate() != null && job.getEndDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Job nay da het han ung tuyen");
        }
    }

    private void ensureCanReadResume(Resume resume, User currentUser) {
        if (!canReadResume(resume, currentUser)) {
            throw new IllegalArgumentException("Ban khong co quyen truy cap CV nay");
        }
    }

    private void ensureCanManageResume(Resume resume, User currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (isHr(currentUser) && isResumeInCurrentCompany(resume, currentUser)) {
            return;
        }
        if (isResumeOwnedByUser(resume, currentUser)) {
            return;
        }
        throw new IllegalArgumentException("Ban khong co quyen thay doi CV nay");
    }

    /**
     * Chỉ ADMIN hoặc HR phụ trách job mới được đổi trạng thái hồ sơ ứng tuyển.
     * Ứng viên (chủ hồ sơ) không được tự duyệt/loại hồ sơ của chính mình; nếu muốn
     * rút hồ sơ thì dùng chức năng xóa (deleteForCurrentUser).
     */
    private void ensureCanReviewResume(Resume resume, User currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (isHr(currentUser) && isResumeInCurrentCompany(resume, currentUser)) {
            return;
        }
        throw new IllegalArgumentException(
                "Chi nha tuyen dung phu trach job moi duoc doi trang thai ho so ung tuyen");
    }

    private boolean canReadResume(Resume resume, User currentUser) {
        return isAdmin(currentUser)
                || isResumeOwnedByUser(resume, currentUser)
                || (isHr(currentUser) && isResumeInCurrentCompany(resume, currentUser));
    }

    private boolean isResumeOwnedByUser(Resume resume, User currentUser) {
        boolean sameUser = resume.getUser() != null && resume.getUser().getId() == currentUser.getId();
        boolean sameEmail = resume.getEmail() != null && resume.getEmail().equalsIgnoreCase(currentUser.getEmail());
        return sameUser || sameEmail;
    }

    private boolean isResumeInCurrentCompany(Resume resume, User currentUser) {
        return resume.getJob() != null && isJobOwnedByCurrentCompany(resume.getJob(), currentUser);
    }

    private boolean isJobOwnedByCurrentCompany(Job job, User currentUser) {
        return currentUser.getCompany() != null
                && job.getCompany() != null
                && job.getCompany().getId() == currentUser.getCompany().getId();
    }

    private boolean isAdmin(User user) {
        return roleName(user).contains("ADMIN");
    }

    private boolean isHr(User user) {
        String roleName = roleName(user);
        return roleName.contains("HR") || roleName.contains("RECRUITER");
    }

    private String roleName(User user) {
        return user != null && user.getRole() != null && user.getRole().getName() != null
                ? user.getRole().getName().toUpperCase()
                : "";
    }

    private Specification<Resume> visibleToUserSpec(User currentUser) {
        if (isAdmin(currentUser)) {
            return Specification.where(null);
        }
        if (isHr(currentUser) && currentUser.getCompany() != null) {
            long companyId = currentUser.getCompany().getId();
            return (root, query, cb) -> cb.equal(root.get("job").get("company").get("id"), companyId);
        }
        return ownedByCurrentUserSpec(currentUser);
    }

    private Specification<Resume> ownedByCurrentUserSpec(User currentUser) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("user").get("id"), currentUser.getId()),
                cb.equal(cb.lower(root.get("email")), currentUser.getEmail().toLowerCase()));
    }

}
