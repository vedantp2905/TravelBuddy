package TravelBuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "TravelBuddy")
@EnableScheduling
public class TravelBuddyApplication {
    public static void main(String[] args) {
        SpringApplication.run(TravelBuddyApplication.class, args);
    }
}
