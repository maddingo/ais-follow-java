package no.maddin.ais.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.maddin.ais.data.AisData;
import no.maddin.ais.repository.AisDataReactiveRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Read AIS data from MarineTraffic.com.
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AisReaderService {

    private final MarineTrafficReader marineTrafficReader;

    private final AisDataReactiveRepository aisDataReactiveRepository;

    @Value("${ais.reader.start-date}")
    private LocalDate startDate;

    @Value("${ais.reader.mmsi}")
    private String mmsi;

    private final IntervalService intervalService;

    public Flux<AisData> readAis() {
        var marineTrafficData = findLastRecordedDate()
            .flux()
            .log("readAis")
            .flatMap(newStartDate -> intervalService.intervals(newStartDate, LocalDate.now()))
            .flatMap(di -> marineTrafficReader.readAis(mmsi, di.fromDate(), di.toDate()))
            ;

        return aisDataReactiveRepository.saveAll(marineTrafficData);
    }

    private Mono<LocalDate> findLastRecordedDate() {
        return aisDataReactiveRepository.findFirstByOrderByTimestampDesc(mmsi)
            .map(AisData::getTimestamp)
            .log()
            .map(OffsetDateTime::toLocalDate)
            .map(ld -> ld.isAfter(startDate) ? ld : startDate);
    }
}
