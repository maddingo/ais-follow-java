package no.maddin.ais.reader;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;

class IntervalServiceTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
    void intervals(int nboIntervals) {
        int expectedIntervals = nboIntervals + 1;
        LocalDateTime startTime = LocalDateTime.now().minusDays(nboIntervals*190L);

        var intervals = MarineTrafficAisReader.intervals(startTime, LocalDateTime.now()).log();

        StepVerifier.create(intervals)
            .expectNextCount(expectedIntervals)
            .expectComplete()
            .verify();
    }

}