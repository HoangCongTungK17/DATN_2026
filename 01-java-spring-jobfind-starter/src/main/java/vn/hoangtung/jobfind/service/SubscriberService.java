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

    // @Scheduled(cron = "*/10 * * * * *")
    // public void testCron() {
    // System.out.println(">>> TEST CRON");
    // }

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

    // Send email for a single subscriber
    public void sendSubscriberEmail(Subscriber subscriber) {
        System.out.println("▶▶▶ [EMAIL] Starting email send for subscriber: " +
                (subscriber != null ? subscriber.getEmail() : "null"));

        if (subscriber == null) {
            System.out.println("❌ [EMAIL] Subscriber is null, aborting");
            return;
        }

        List<Skill> listSkills = subscriber.getSkills();
        System.out.println("▶▶▶ [EMAIL] Subscriber skills count: " +
                (listSkills != null ? listSkills.size() : 0));

        if (listSkills != null && listSkills.size() > 0) {
            System.out.println("▶▶▶ [EMAIL] Skills: " +
                    listSkills.stream()
                            .map(Skill::getName)
                            .collect(Collectors.joining(", ")));

            List<Job> listJobs = this.jobRepository.findBySkillsIn(listSkills);
            System.out.println("▶▶▶ [EMAIL] Found " +
                    (listJobs != null ? listJobs.size() : 0) + " matching jobs");

            if (listJobs != null && listJobs.size() > 0) {
                System.out.println("▶▶▶ [EMAIL] Jobs: " +
                        listJobs.stream()
                                .map(Job::getName)
                                .collect(Collectors.joining(", ")));

                List<ResEmailJob> arr = listJobs.stream()
                        .map(job -> this.convertJobToSendEmail(job))
                        .collect(Collectors.toList());

                System.out.println("✉️ [EMAIL] Sending email to: " + subscriber.getEmail());
                this.emailService.sendEmailFromTemplateSync(
                        subscriber.getEmail(),
                        "Cơ hội việc làm hot đang chờ đón bạn, khám phá ngay",
                        "job",
                        subscriber.getName(),
                        arr);
                System.out.println("✅ [EMAIL] Email sent successfully");
            } else {
                System.out.println("⚠️ [EMAIL] No matching jobs found for subscriber's skills");
            }
        } else {
            System.out.println("⚠️ [EMAIL] Subscriber has no skills selected");
        }
    }

    public Subscriber findByEmail(String email) {
        return this.subscriberRepository.findByEmail(email);
    }

}