package no.maddin.ais;

import lombok.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class AisFollowApplication {

    public static void main(String[] args) {
        SpringApplication.run(AisFollowApplication.class, args);
    }

}
