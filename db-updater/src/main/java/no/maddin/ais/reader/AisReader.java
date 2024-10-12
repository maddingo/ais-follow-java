package no.maddin.ais.reader;

import no.maddin.ais.data.AisData;
import org.reactivestreams.Publisher;

import java.time.LocalDateTime;

public interface AisReader {
    Publisher<AisData> readAis(String mmsi, LocalDateTime startDate, LocalDateTime endDate);
}
