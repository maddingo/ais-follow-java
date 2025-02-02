package no.maddin.ais.reader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.maddin.ais.config.MarineTrafficProperties;
import no.maddin.ais.data.AisData;
import no.maddin.ais.data.DataInterval;
import no.maddin.ais.data.MarineTrafficError;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "ais.reader", name = "type", havingValue = "marinetraffic")
public class MarineTrafficAisReader implements AisReader {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long MAX_DAYS = 190;
    private final MarineTrafficProperties marineTrafficProperties;

    private final ObjectMapper objectMapper;

    @Override
    public Publisher<AisData> readAis(String mmsi, LocalDateTime startDate, LocalDateTime endDate) {

        WebClient webClient = WebClient.builder()
                .baseUrl(marineTrafficProperties.getUrl())
                .defaultUriVariables(Map.of("apikey", marineTrafficProperties.getApiKey(), "mmsi", mmsi))
                .build();

        return intervals(startDate, endDate)
            .parallel()
            .runOn(Schedulers.parallel())
            .flatMap(dataInterval -> webClient
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
                ).log());


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
                    return Mono.error(new RuntimeException(eTxt, e));
                }
            })
            .map(MarineTrafficError::getErrors)
            .map(el -> el.stream().map(MarineTrafficError.Error::getDetail).collect(Collectors.joining(", ")))
            .flatMap(e -> Mono.error(new RuntimeException(e)))
            .flux()
            .map(e -> AisData.builder().build()); // satisfy the return type
    }

    /**
     * MarineTraffic has a limit of {@link #MAX_DAYS} per call.
     * This splits up the whole timespan in smaller intervals not larger than MAX_DAYS.
     * Static access for testing.
     */
    static Flux<DataInterval> intervals(LocalDateTime startTime, LocalDateTime endTime) {
        return Flux.generate(() -> startTime, (state, sink) -> {
            LocalDateTime next = state.plusDays(MAX_DAYS);
            if (next.isAfter(endTime)) {
                sink.next(new DataInterval(state, endTime));
                sink.complete();
                return state;
            }
            sink.next(new DataInterval(state, next.minusSeconds(1)));
            return next;
        });
    }
}
