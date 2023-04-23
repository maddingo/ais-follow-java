package no.maddin.ais.repository;

import no.maddin.ais.data.AisData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface AisDataReactiveRepository extends ReactiveMongoRepository<AisData, String> {
}
