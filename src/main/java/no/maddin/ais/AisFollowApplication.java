package no.maddin.ais;

import lombok.extern.slf4j.Slf4j;
import no.maddin.ais.reader.AisReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class AisFollowApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(AisFollowApplication.class, args);
    }

    @Autowired
    AisReaderService aisReaderService;

    @Override
    public void run(String... args) throws Exception {
        var data = aisReaderService.readAis()
            .log()
            .blockLast();

        log.info("Last Record: {}", data);
    }
}
