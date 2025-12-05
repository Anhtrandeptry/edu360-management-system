package fpt.capstone.edu360managementsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Edu360ManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(Edu360ManagementSystemApplication.class, args);
    }

}
