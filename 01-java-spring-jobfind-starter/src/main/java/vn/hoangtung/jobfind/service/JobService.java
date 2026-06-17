package vn.hoangtung.jobfind.service;

import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import vn.hoangtung.jobfind.domain.Company;
import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Skill;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.response.ResCreateJobDTO;
import vn.hoangtung.jobfind.domain.response.ResJobCardDTO;
import vn.hoangtung.jobfind.domain.response.ResUpdateJobDTO;
import vn.hoangtung.jobfind.domain.response.ResultPaginationDTO;
import vn.hoangtung.jobfind.repository.CompanyRepository;
import vn.hoangtung.jobfind.repository.JobRepository;
import vn.hoangtung.jobfind.repository.SkillRepository;
import java.util.Optional;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final CompanyRepository companyRepository;
    private final DataScopeService dataScopeService;

    public JobService(
            JobRepository jobRepository,
            SkillRepository skillRepository,
            CompanyRepository companyRepository,
            DataScopeService dataScopeService) {
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
        this.companyRepository = companyRepository;
        this.dataScopeService = dataScopeService;
    }

    public Optional<Job> fetchJobById(long id) {
        return this.jobRepository.findById(id);
    }

    public Optional<Job> fetchJobByIdForAdminScope(long id) {
        User currentUser = this.dataScopeService.getCurrentUserOrThrow();
        Optional<Job> job = this.jobRepository.findById(id);
        job.ifPresent(item -> ensureCanReadJobInAdminScope(item, currentUser));
        return job;
    }

    public ResCreateJobDTO create(Job j) {
        User currentUser = this.dataScopeService.getCurrentUser().orElse(null);
        // check skills
        if (j.getSkills() != null) {
            List<Long> reqSkills = j.getSkills()
                    .stream().map(x -> x.getId())
                    .collect(Collectors.toList());

            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            j.setSkills(dbSkills);
        }

        // check company
        Company requestedCompany = null;
        if (j.getCompany() != null) {
            Optional<Company> cOptional = this.companyRepository.findById(j.getCompany().getId());
            if (cOptional.isPresent()) {
                requestedCompany = cOptional.get();
            }
        }
        if (this.dataScopeService.isHrOnly(currentUser)) {
            Company hrCompany = this.dataScopeService.getCurrentCompanyOrThrow(currentUser);
            if (requestedCompany != null && !this.dataScopeService.isSameCompany(requestedCompany, hrCompany)) {
                throw new AccessDeniedException("HR chi duoc tao job cho cong ty cua minh");
            }
            j.setCompany(hrCompany);
        } else if (requestedCompany != null) {
            j.setCompany(requestedCompany);
        }

        // create job
        Job currentJob = this.jobRepository.save(j);

        ResCreateJobDTO dto = new ResCreateJobDTO();

        dto.setId(currentJob.getId());
        dto.setName(currentJob.getName());
        dto.setSalary(currentJob.getSalary());
        dto.setQuantity(currentJob.getQuantity());
        dto.setLocation(currentJob.getLocation());
        dto.setLevel(currentJob.getLevel());
        dto.setStartDate(currentJob.getStartDate());
        dto.setEndDate(currentJob.getEndDate());
        dto.setActive(currentJob.isActive());
        dto.setCreatedAt(currentJob.getCreatedAt());
        dto.setCreatedBy(currentJob.getCreatedBy());

        if (currentJob.getSkills() != null) {
            List<String> skills = currentJob.getSkills()
                    .stream().map(x -> x.getName())
                    .collect(Collectors.toList());

            dto.setSkills(skills);
        }

        return dto;
    }

    public ResUpdateJobDTO update(Job j, Job jobInDB) {
        User currentUser = this.dataScopeService.getCurrentUser().orElse(null);
        ensureCanManageJob(jobInDB, currentUser);

        // check skills
        if (j.getSkills() != null) {
            List<Long> reqSkills = j.getSkills()
                    .stream().map(x -> x.getId())
                    .collect(Collectors.toList());

            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            jobInDB.setSkills(dbSkills);
        }

        // check company
        Company requestedCompany = null;
        if (j.getCompany() != null) {
            Optional<Company> cOptional = this.companyRepository.findById(j.getCompany().getId());
            if (cOptional.isPresent()) {
                requestedCompany = cOptional.get();
            }
        }
        if (this.dataScopeService.isHrOnly(currentUser)) {
            Company hrCompany = this.dataScopeService.getCurrentCompanyOrThrow(currentUser);
            if (requestedCompany != null && !this.dataScopeService.isSameCompany(requestedCompany, hrCompany)) {
                throw new AccessDeniedException("HR khong duoc chuyen job sang cong ty khac");
            }
            jobInDB.setCompany(hrCompany);
        } else if (requestedCompany != null) {
            jobInDB.setCompany(requestedCompany);
        }

        // update correct info
        jobInDB.setName(j.getName());
        jobInDB.setSalary(j.getSalary());
        jobInDB.setQuantity(j.getQuantity());
        jobInDB.setLocation(j.getLocation());
        jobInDB.setLevel(j.getLevel());
        jobInDB.setStartDate(j.getStartDate());
        jobInDB.setEndDate(j.getEndDate());
        jobInDB.setActive(j.isActive());

        // update job
        Job currentJob = this.jobRepository.save(jobInDB);
        // convert response
        ResUpdateJobDTO dto = new ResUpdateJobDTO();

        dto.setId(currentJob.getId());
        dto.setName(currentJob.getName());
        dto.setSalary(currentJob.getSalary());
        dto.setQuantity(currentJob.getQuantity());
        dto.setLocation(currentJob.getLocation());
        dto.setLevel(currentJob.getLevel());
        dto.setStartDate(currentJob.getStartDate());
        dto.setEndDate(currentJob.getEndDate());
        dto.setActive(currentJob.isActive());
        dto.setUpdatedAt(currentJob.getUpdatedAt());
        dto.setUpdatedBy(currentJob.getUpdatedBy());

        if (currentJob.getSkills() != null) {
            List<String> skills = currentJob.getSkills()
                    .stream().map(x -> x.getName())
                    .collect(Collectors.toList());

            dto.setSkills(skills);
        }

        return dto;
    }

    public void delete(long id) {
        Job job = this.jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
        ensureCanManageJob(job, this.dataScopeService.getCurrentUser().orElse(null));
        this.jobRepository.deleteById(id);
    }

    public ResultPaginationDTO fetchAll(Specification<Job> spec, Pageable pageable) {

        Page<Job> pageJob = this.jobRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageJob.getTotalPages());
        mt.setTotal(pageJob.getTotalElements());

        rs.setMeta(mt);

        rs.setResult(pageJob.getContent());

        return rs;

    }

    public ResultPaginationDTO fetchAllForAdminScope(Specification<Job> spec, Pageable pageable) {
        User currentUser = this.dataScopeService.getCurrentUserOrThrow();
        Specification<Job> scopeSpec = this.dataScopeService.jobScopeFor(currentUser);
        Specification<Job> finalSpec = spec == null ? scopeSpec : scopeSpec.and(spec);
        return fetchAll(finalSpec, pageable);
    }

    /**
     * Danh sách job cho khách vãng lai: chỉ trả job đang tuyển (active=true) và map
     * sang DTO an toàn (không lộ createdBy/updatedBy, không serialize cả entity).
     * Lazy skills/company được nạp theo batch (@BatchSize trên entity) nên hạn chế N+1.
     */
    @Transactional(readOnly = true)
    public ResultPaginationDTO fetchAllPublic(Specification<Job> spec, Pageable pageable) {
        Specification<Job> activeSpec = (root, query, cb) -> cb.isTrue(root.get("active"));
        Specification<Job> finalSpec = spec == null ? activeSpec : activeSpec.and(spec);
        Page<Job> pageJob = this.jobRepository.findAll(finalSpec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageJob.getTotalPages());
        mt.setTotal(pageJob.getTotalElements());
        rs.setMeta(mt);

        List<ResJobCardDTO> cards = pageJob.getContent().stream()
                .map(this::toJobCard)
                .collect(Collectors.toList());
        rs.setResult(cards);
        return rs;
    }

    private ResJobCardDTO toJobCard(Job job) {
        ResJobCardDTO dto = new ResJobCardDTO();
        dto.setId(job.getId());
        dto.setName(job.getName());
        dto.setLocation(job.getLocation());
        dto.setSalary(job.getSalary());
        dto.setQuantity(job.getQuantity());
        dto.setLevel(job.getLevel());
        dto.setStartDate(job.getStartDate());
        dto.setEndDate(job.getEndDate());
        dto.setActive(job.isActive());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());

        Company company = job.getCompany();
        if (company != null) {
            dto.setCompany(new ResJobCardDTO.CompanyCard(
                    company.getId(),
                    company.getName(),
                    company.getLogo(),
                    company.getAddress()));
        }
        if (job.getSkills() != null) {
            dto.setSkills(job.getSkills().stream()
                    .map(skill -> new ResJobCardDTO.SkillCard(skill.getId(), skill.getName()))
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private void ensureCanReadJobInAdminScope(Job job, User currentUser) {
        if (this.dataScopeService.isAdmin(currentUser)) {
            return;
        }
        if (this.dataScopeService.isHrOnly(currentUser)
                && this.dataScopeService.isJobInCurrentCompany(job, currentUser)) {
            return;
        }
        throw new AccessDeniedException("Ban khong co quyen truy cap job nay");
    }

    private void ensureCanManageJob(Job job, User currentUser) {
        if (currentUser == null || this.dataScopeService.isAdmin(currentUser)) {
            return;
        }
        if (this.dataScopeService.isHrOnly(currentUser)
                && this.dataScopeService.isJobInCurrentCompany(job, currentUser)) {
            return;
        }
        if (this.dataScopeService.isHrOnly(currentUser)) {
            throw new AccessDeniedException("HR chi duoc quan ly job cua cong ty minh");
        }
    }
}
