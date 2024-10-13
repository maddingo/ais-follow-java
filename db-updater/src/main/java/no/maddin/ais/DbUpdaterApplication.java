package no.maddin.ais;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.maddin.ais.reader.AisReaderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class DbUpdaterApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(DbUpdaterApplication.class)
            .web(WebApplicationType.NONE)
            .run(args);
    }

    private final AisReaderService aisReaderService;
    private final ApplicationContext applicationContext;

    @Bean
    @Profile("!test")
    CommandLineRunner runner() {
        return args -> {
            var data = aisReaderService.readAis()
                .log("main")
                .blockLast(Duration.ofHours(1L));

            log.info("Last Record: {}", data);
            SpringApplication.exit(applicationContext, () -> data != null ? 0 : 2);
        };
    }
}
