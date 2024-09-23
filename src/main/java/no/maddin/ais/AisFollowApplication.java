package no.maddin.ais;

import lombok.extern.slf4j.Slf4j;
import no.maddin.ais.reader.AisReaderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class AisFollowApplication implements CommandLineRunner {

    public AisFollowApplication(AisReaderService aisReaderService) {
        this.aisReaderService = aisReaderService;
    }

    public static void main(String[] args) {
        SpringApplication.run(AisFollowApplication.class, args);
    }

    private final AisReaderService aisReaderService;

    @Override
    public void run(String... args) {
        var data = aisReaderService.readAis()
            .log()
            .blockLast();

        log.info("Last Record: {}", data);
    }
}
