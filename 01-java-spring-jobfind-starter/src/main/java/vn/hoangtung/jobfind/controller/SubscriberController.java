package vn.hoangtung.jobfind.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.hoangtung.jobfind.domain.Skill;
import vn.hoangtung.jobfind.domain.Subscriber;
import vn.hoangtung.jobfind.service.SubscriberService;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;
import vn.hoangtung.jobfind.domain.request.ReqSubscriberDTO;
import vn.hoangtung.jobfind.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class SubscriberController {
    private final SubscriberService subscriberService;

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @PostMapping("/subscribers")
    @ApiMessage("Create a subscriber")
    public ResponseEntity<Subscriber> create(@Valid @RequestBody ReqSubscriberDTO reqDTO) throws IdInvalidException {
        // check email
        boolean isExist = this.subscriberService.isExistsByEmail(reqDTO.getEmail());
        if (isExist == true) {
            throw new IdInvalidException("Email " + reqDTO.getEmail() + " đã tồn tại");
        }

        // Convert DTO to Entity
        Subscriber sub = new Subscriber();
        sub.setEmail(reqDTO.getEmail());
        sub.setName(reqDTO.getName());

        // Convert skill IDs to Skill entities
        if (reqDTO.getSkills() != null) {
            List<Skill> skills = reqDTO.getSkills().stream()
                    .map(skillId -> {
                        Skill skill = new Skill();
                        skill.setId(skillId.getId());
                        return skill;
                    })
                    .collect(Collectors.toList());
            sub.setSkills(skills);
        }

        Subscriber createdSubscriber = this.subscriberService.create(sub);

        // Send welcome email with matching jobs
        this.subscriberService.sendSubscriberEmail(createdSubscriber);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdSubscriber);
    }

    @PutMapping("/subscribers")
    @ApiMessage("Update a subscriber")
    public ResponseEntity<Subscriber> update(@RequestBody ReqSubscriberDTO reqDTO) throws IdInvalidException {
        // check id
        if (reqDTO.getId() == null) {
            throw new IdInvalidException("Id không được để trống");
        }

        Subscriber subsDB = this.subscriberService.findById(reqDTO.getId());
        if (subsDB == null) {
            throw new IdInvalidException("Id " + reqDTO.getId() + " không tồn tại");
        }

        // Convert skill IDs to Skill entities
        if (reqDTO.getSkills() != null) {
            List<Skill> skills = reqDTO.getSkills().stream()
                    .map(skillId -> {
                        Skill skill = new Skill();
                        skill.setId(skillId.getId());
                        return skill;
                    })
                    .collect(Collectors.toList());

            Subscriber subsRequest = new Subscriber();
            subsRequest.setSkills(skills);
            Subscriber updatedSubscriber = this.subscriberService.update(subsDB, subsRequest);

            // Send email with matching jobs
            this.subscriberService.sendSubscriberEmail(updatedSubscriber);

            return ResponseEntity.ok().body(updatedSubscriber);
        }

        return ResponseEntity.ok().body(subsDB);
    }

    @PostMapping("/subscribers/skills")
    @ApiMessage("Get subscriber's skill")
    public ResponseEntity<Subscriber> getSubscribersSkill() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() == true
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        return ResponseEntity.ok().body(this.subscriberService.findByEmail(email));
    }

}