package no.maddin.ais.reader;

import no.maddin.ais.data.DataInterval;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class IntervalService {

    private static final int MAX_DAYS = 190;

    public Flux<DataInterval> intervals(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(23, 59, 59);
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
