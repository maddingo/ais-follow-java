package no.maddin.ais.repository;

import no.maddin.ais.data.AisData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface AisDataReactiveRepository extends ReactiveMongoRepository<AisData, String> {
    Mono<AisData> findFirstByOrderByTimestampDesc(String mmsi);
}
