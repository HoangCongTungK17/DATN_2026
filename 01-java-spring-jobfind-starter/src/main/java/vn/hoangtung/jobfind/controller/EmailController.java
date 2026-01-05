package vn.hoangtung.jobfind.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.hoangtung.jobfind.domain.response.RestResponse;
import vn.hoangtung.jobfind.domain.response.email.ResEmailJob;
import vn.hoangtung.jobfind.service.EmailService;
import vn.hoangtung.jobfind.service.SubscriberService;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1")
public class EmailController {

    private final EmailService emailService;
    private final SubscriberService subscriberService;

    public EmailController(EmailService emailService, SubscriberService subscriberService) {
        this.emailService = emailService;
        this.subscriberService = subscriberService;
    }

//
@GetMapping("/email")
@ApiMessage("Send simple email")
public String sendSimpleEmail() {
    //this.emailService.sendSimpleEmail();

    String username = "HoangTung";
    Object value = List.of("Job1", "Job2", "Job3"); // Example data for jobs

    this.emailService.sendEmailFromTemplateSync("tungthcstt@gmail.com", "testsend email", "job", username, value);

    this.subscriberService.sendSubscribersEmailJobs();
    return "ok";
}
}