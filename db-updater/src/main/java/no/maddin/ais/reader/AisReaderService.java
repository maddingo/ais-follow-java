package no.maddin.ais.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.maddin.ais.config.AisReaderConfig;
import no.maddin.ais.data.AisData;
import no.maddin.ais.repository.AisDataReactiveRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Read AIS data from MarineTraffic.com.
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AisReaderService {

    private final AisReader aisReader;

    private final AisDataReactiveRepository aisDataReactiveRepository;

    private final AisReaderConfig aisReaderConfig;

    public Flux<AisData> readAis() {
        var marineTrafficData = findLastRecordedDate()
            .flux()
            .log("readAis")
            .flatMap(newStartDate -> aisReader.readAis(aisReaderConfig.getMmsi(), newStartDate.atStartOfDay(), LocalDateTime.now()))
            ;

        return aisDataReactiveRepository.saveAll(marineTrafficData);
    }

    private Mono<LocalDate> findLastRecordedDate() {
        return aisDataReactiveRepository.findFirstByOrderByTimestampDesc(aisReaderConfig.getMmsi())
            .map(AisData::getTimestamp)
            .log("lastRecordedDate")
            .map(OffsetDateTime::toLocalDate)
            .map(ld -> ld.isAfter(aisReaderConfig.getStartDate()) ? ld : aisReaderConfig.getStartDate())
            .switchIfEmpty(Mono.just(aisReaderConfig.getStartDate()))
            .log("lastRecordedDate 1")

            ;
    }
}
