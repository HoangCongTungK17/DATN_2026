package vn.hoangtung.jobfind.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Skill;
import vn.hoangtung.jobfind.domain.Subscriber;
import vn.hoangtung.jobfind.domain.response.email.ResEmailJob;
import vn.hoangtung.jobfind.repository.JobRepository;
import vn.hoangtung.jobfind.repository.SkillRepository;
import vn.hoangtung.jobfind.repository.SubscriberRepository;

@Service
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final EmailService emailService;

    public SubscriberService(
            vn.hoangtung.jobfind.repository.SubscriberRepository subscriberRepository,
            SkillRepository skillRepository,
            EmailService emailService,
            JobRepository jobRepository) {
        this.subscriberRepository = subscriberRepository;
        this.skillRepository = skillRepository;
        this.emailService = emailService;
        this.jobRepository = jobRepository;
    }

    public boolean isExistsByEmail(String email) {
        return this.subscriberRepository.existsByEmail(email);
    }

    public Subscriber create(Subscriber subs) {
        // check skills
        if (subs.getSkills() != null) {
            List<Long> reqSkills = subs.getSkills()
                    .stream().map(x -> x.getId())
                    .collect(Collectors.toList());

            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            subs.setSkills(dbSkills);
        }

        return this.subscriberRepository.save(subs);
    }

    public Subscriber update(Subscriber subsDB, Subscriber subsRequest) {
        // check skills
        if (subsRequest.getSkills() != null) {
            List<Long> reqSkills = subsRequest.getSkills()
                    .stream().map(x -> x.getId())
                    .collect(Collectors.toList());

            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            subsDB.setSkills(dbSkills);
        }
        return this.subscriberRepository.save(subsDB);
    }

    public Subscriber findById(long id) {
        Optional<Subscriber> subsOptional = this.subscriberRepository.findById(id);
        if (subsOptional.isPresent())
            return subsOptional.get();
        return null;
    }

    public ResEmailJob convertJobToSendEmail(Job job) {
        ResEmailJob res = new ResEmailJob();
        res.setName(job.getName());
        res.setSalary(job.getSalary());
        res.setCompany(new ResEmailJob.CompanyEmail(job.getCompany().getName()));
        List<Skill> skills = job.getSkills();
        List<ResEmailJob.SkillEmail> s = skills.stream().map(skill -> new ResEmailJob.SkillEmail(skill.getName()))
                .collect(Collectors.toList());
        res.setSkills(s);
        return res;
    }

    public void sendSubscribersEmailJobs() {
        List<Subscriber> listSubs = this.subscriberRepository.findAll();
        if (listSubs != null && listSubs.size() > 0) {
            for (Subscriber sub : listSubs) {
                List<Skill> listSkills = sub.getSkills();
                if (listSkills != null && listSkills.size() > 0) {
                    List<Job> listJobs = this.jobRepository.findBySkillsIn(listSkills);
                    if (listJobs != null && listJobs.size() > 0) {

                        List<ResEmailJob> arr = listJobs.stream().map(
                                job -> this.convertJobToSendEmail(job)).collect(Collectors.toList());

                        this.emailService.sendEmailFromTemplateSync(
                                sub.getEmail(),
                                "Cơ hội việc làm hot đang chờ đón bạn, khám phá ngay",
                                "job",
                                sub.getName(),
                                arr);
                    }
                }
            }
        }
    }

    public void sendSubscriberEmail(Subscriber subscriber) {
        if (subscriber == null) {
            System.out.println(">>> SUBSCRIBER EMAIL DEBUG: subscriber is null, skip sending email");
            return;
        }

        System.out.println(">>> SUBSCRIBER EMAIL DEBUG: email=" + subscriber.getEmail()
                + ", name=" + subscriber.getName());

        List<Skill> listSkills = subscriber.getSkills();
        System.out.println(">>> SUBSCRIBER EMAIL DEBUG: skills="
                + (listSkills == null ? "null" : listSkills.stream()
                        .map(Skill::getName)
                        .collect(Collectors.joining(", "))));

        if (listSkills != null && listSkills.size() > 0) {
            List<Job> listJobs = this.jobRepository.findBySkillsIn(listSkills);
            System.out.println(">>> SUBSCRIBER EMAIL DEBUG: matchingJobs="
                    + (listJobs == null ? 0 : listJobs.size()));

            if (listJobs != null && listJobs.size() > 0) {
                List<ResEmailJob> arr = listJobs.stream()
                        .map(job -> this.convertJobToSendEmail(job))
                        .collect(Collectors.toList());

                System.out.println(">>> SUBSCRIBER EMAIL DEBUG: start sending email to "
                        + subscriber.getEmail());
                this.emailService.sendEmailFromTemplateSync(
                        subscriber.getEmail(),
                        "Cơ hội việc làm hot đang chờ đón bạn, khám phá ngay",
                        "job",
                        subscriber.getName(),
                        arr);
            } else {
                System.out.println(">>> SUBSCRIBER EMAIL DEBUG: no matching jobs, email not sent");
            }
        } else {
            System.out.println(">>> SUBSCRIBER EMAIL DEBUG: no skills, email not sent");
        }
    }

    public Subscriber findByEmail(String email) {
        return this.subscriberRepository.findByEmail(email);
    }

}
