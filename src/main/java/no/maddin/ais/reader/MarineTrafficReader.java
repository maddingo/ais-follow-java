package no.maddin.ais.reader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.maddin.ais.config.MarineTrafficProperties;
import no.maddin.ais.data.AisData;
import no.maddin.ais.data.MarineTrafficError;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarineTrafficReader {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final MarineTrafficProperties marineTrafficProperties;

    private final ObjectMapper objectMapper;

    public Flux<AisData> readAis(String mmsi, LocalDateTime startDate, LocalDateTime endDate) {

        WebClient webClient = WebClient.builder()
            .baseUrl(marineTrafficProperties.getUrl())
            .defaultUriVariables(Map.of("apikey", marineTrafficProperties.getApiKey(), "mmsi", mmsi))
            .build();


        return webClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .build(Map.of(
                    "fromdate", startDate.format(DATE_TIME_FORMATTER),
                    "todate", endDate.format(DATE_TIME_FORMATTER))
                )
            )
            .exchangeToFlux(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToFlux(AisData.class);
                    } else if (response.statusCode().is4xxClientError()) {
                        log.info("Got response: {}", response);
                        return errorFromResponse(response);
                    }
                    return Flux.error(new RuntimeException("Unhandled Status: " + response.statusCode()));
                }
            ).log();
    }

    /**
     * Marine Traffic responds with the wrong content type, i.e. we can't use bodyToMono(MarineTrafficError.class)
     */
    private Flux<AisData> errorFromResponse(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(eTxt -> {
                try {
                    return Mono.just(objectMapper.readValue(eTxt, MarineTrafficError.class));
                } catch (JsonProcessingException e) {
                    return Mono.error(new RuntimeException(e));
                }
            })
            .map(MarineTrafficError::getErrors)
            .map(el -> el.stream().map(MarineTrafficError.Error::getDetail).collect(Collectors.joining(", ")))
            .flatMap(e -> Mono.error(new RuntimeException(e)))
            .flux()
            .map(e -> AisData.builder().build()); // satisfy the return type
    }

}
