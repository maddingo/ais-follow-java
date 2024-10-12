package no.maddin.ais.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.maddin.ais.data.AisData;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
@Component
@ConditionalOnProperty(prefix = "ais.reader", name = "type", havingValue = "vesselfinder", matchIfMissing = true)
@Slf4j
public class VesselfinderAisReader implements AisReader {

    /*
   HttpRequest request = HttpRequest.newBuilder()
		.uri(URI.create("https://vesselfinder1.p.rapidapi.com/search"))
		.header("x-rapidapi-key", "Sign Up for Key")
		.header("x-rapidapi-host", "vesselfinder1.p.rapidapi.com")
		.method("GET", HttpRequest.BodyPublishers.noBody())
		.build();
HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
     */

    @Override
    public Publisher<AisData> readAis(String mmsi, LocalDateTime startDate, LocalDateTime endDate) {
        return Flux
            .just(
                new AisData(null, "1", "1", "1", "speed", "lon", "lat", "course", "head", OffsetDateTime.now(), "1")
            )
            .log("vesselfinder")
            ;
    }
}