package TravelBuddy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import TravelBuddy.service.NewsletterService;
import TravelBuddy.service.UserService;
import jakarta.annotation.PostConstruct;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Autowired
    private NewsletterService newsletterService;

    @Autowired
    private UserService userService;

    @Value("${newsletter.cron:0 0 10 1,15 * ?}")
    private String newsletterCronExpression;

    @Value("${newsletter.timezone:UTC}")
    private String timeZone;

    @Scheduled(cron = "${newsletter.cron:0 0 10 1,15 * ?}", zone = "${newsletter.timezone:UTC}")
    public void sendBimonthlyNewsletter() {
        String topic = "Travel Tips and Tricks";
        newsletterService.sendNewsletterToAllUsers(topic);
    }

    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    public void checkAndDowngradePremiumUsers() {
        userService.checkAndDowngradePremiumUsers();
    }

    @PostConstruct
    public void init() {
        System.out.println("Newsletter scheduled with cron: " + newsletterCronExpression + " in time zone: " + timeZone);
        System.out.println("Premium user downgrade check scheduled to run daily at midnight UTC");
    }
}
