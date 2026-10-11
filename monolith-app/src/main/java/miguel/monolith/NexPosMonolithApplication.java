package miguel.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "miguel")
@EntityScan(basePackages = "miguel")
@EnableJpaRepositories(basePackages = "miguel")
public class NexPosMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexPosMonolithApplication.class, args);
    }
}
