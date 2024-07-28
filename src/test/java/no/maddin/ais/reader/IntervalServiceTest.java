package no.maddin.ais.reader;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

import java.time.LocalDate;

class IntervalServiceTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
    void intervals(int nboIntervals) {
        int expectedIntervals = nboIntervals + 1;
        IntervalService intervalService = new IntervalService();
        LocalDate startDate = LocalDate.now().minusDays(nboIntervals*190);

        var intervals = intervalService.intervals(startDate, LocalDate.now()).log();

        StepVerifier.create(intervals)
            .expectNextCount(expectedIntervals)
            .expectComplete()
            .verify();
    }

}