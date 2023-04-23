package no.maddin.ais.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "marinetraffic")
@Configuration
@Data
public class MarineTrafficProperties {

    private String apiKey;
    private String url;
}
