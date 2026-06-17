package vn.hoangtung.jobfind.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.Optional;
import vn.hoangtung.jobfind.domain.Skill;
import vn.hoangtung.jobfind.domain.response.ResultPaginationDTO;
import vn.hoangtung.jobfind.repository.SkillRepository;

@Service
public class SkillService {
    private final SkillRepository skillRepository;
    private final DataScopeService dataScopeService;

    public SkillService(SkillRepository skillRepository, DataScopeService dataScopeService) {
        this.skillRepository = skillRepository;
        this.dataScopeService = dataScopeService;
    }

    public boolean isNameExist(String name) {
        return this.skillRepository.existsByName(name);
    }

    public Skill fetchSkillById(long id) {
        Optional<Skill> skillOptional = this.skillRepository.findById(id);
        if (skillOptional.isPresent()) {
            return skillOptional.get();
        }
        return null;
    }

    public Skill createSkill(Skill s) {
        ensureCanWriteSkill();
        return this.skillRepository.save(s);
    }

    public Skill updateSkill(Skill s) {
        ensureCanWriteSkill();
        return this.skillRepository.save(s);
    }

    public void deleteSkill(long id) {
        ensureCanWriteSkill();
        // delete job (inside job_skill table)
        Optional<Skill> skillOptional = this.skillRepository.findById(id);
        Skill currentSkill = skillOptional.get();
        currentSkill.getJobs().forEach(job -> job.getSkills().remove(currentSkill));

        // delete subscriber (inside subscriber_skill table)
        currentSkill.getSubscribers().forEach(subs -> subs.getSkills().remove(currentSkill));

        this.skillRepository.delete(currentSkill);
    }

    public ResultPaginationDTO fetchAll(Specification<Skill> spec, Pageable pageable) {
        Page<Skill> pageSkill = this.skillRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageSkill.getTotalPages());
        mt.setTotal(pageSkill.getTotalElements());

        rs.setMeta(mt);

        rs.setResult(pageSkill.getContent());

        return rs;
    }

    private void ensureCanWriteSkill() {
        this.dataScopeService.getCurrentUser().ifPresent(user -> {
            if (this.dataScopeService.isHrOnly(user)) {
                throw new AccessDeniedException("HR chi duoc xem ky nang, khong duoc them/sua/xoa ky nang");
            }
        });
    }
}
