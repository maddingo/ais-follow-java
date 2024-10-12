package no.maddin.ais.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@ConfigurationProperties(prefix = "ais.reader")
@Configuration
@Data
public class AisReaderConfig {
    String type;
    LocalDate startDate;
    String mmsi;
}