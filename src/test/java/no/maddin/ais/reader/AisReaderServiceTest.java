package no.maddin.ais.reader;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import lombok.extern.slf4j.Slf4j;
import no.maddin.ais.data.AisData;
import no.maddin.ais.repository.AisDataReactiveRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "marinetraffic.api-key=1234567890",
    "marinetraffic.url=http://localhost:${wiremock.server.port}/exportvesseltrack/{apikey}",
    "ais.reader.mmsi=123456789",
    "ais.reader.start-date=2023-04-22",
    "spring.data.mongodb.host=localhost",
    "spring.data.mongodb.port=27017",
    "server.data.mongodb.database=ais-reader-test",
})
@Slf4j
class AisReaderServiceTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
        .build();

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("wiremock.server.port", () -> wm.getPort());
    }

    @Autowired
    AisReaderService aisReaderService;

    @Autowired
    AisDataReactiveRepository aisDataReactiveRepository;

    @Autowired
    private Environment environment;

    @BeforeEach
    void setUp() {
        aisDataReactiveRepository.saveAll(List.of(
            AisData.builder()
                .id("000000000000000000000001")
                .mmsi("123456789")
                .lon("0.0")
                .lat("0.0")
                .timestamp(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .speed("0")
                .build(),
            AisData.builder()
                .id("000000000000000000000002")
                .mmsi("123456789")
                .lon("0.0")
                .lat("0.0")
                .timestamp(OffsetDateTime.of(2021, 1, 1, 0, 1, 0, 0, ZoneOffset.UTC))
                .speed("0")
                .build(),
            AisData.builder()
                .id("000000000000000000000003")
                .mmsi("123456789")
                .lon("0.0")
                .lat("0.0")
                .timestamp(OffsetDateTime.of(2021, 1, 1, 0, 2, 0, 0, ZoneOffset.UTC))
                .speed("0")
                .build()
        )).blockLast();
    }

    @AfterEach
    void tearDown() {
        aisDataReactiveRepository.deleteAll().block();
    }

    @ParameterizedTest
    @ValueSource(strings = {/*"ais-data-simple-json.json", */"ais-data-simple-jsono.json"})
    void readSimpleAis(String resultFile) {

        log.info("environment: {}", environment);

        wm.stubFor(get("/exportvesseltrack/1234567890")
            .willReturn(aResponse()
                .withBodyFile(resultFile)
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
            )
        );

        var result = aisReaderService.readAis().log();
        List<AisData> dataList = Collections.synchronizedList(new ArrayList<>());

        StepVerifier.create(result)
            .consumeNextWith(ad -> assertDataAndRecord(ad, dataList))
            .consumeNextWith(ad -> assertDataAndRecord(ad, dataList))
            .expectComplete()
            .verify();

        // Events do not necessarily come in expected order
        List<OffsetDateTime> timestamps = dataList.stream()
            .map(AisData::getTimestamp)
            .collect(Collectors.toList());

        assertThat(timestamps, Matchers.hasItems(
            Matchers.equalTo(OffsetDateTime.of(2021, 1, 1, 12, 58, 1, 0, ZoneOffset.UTC)),
            Matchers.equalTo(OffsetDateTime.of(2021, 1, 1, 12, 57, 1, 0, ZoneOffset.UTC))
        ));

        var storedData = aisDataReactiveRepository.findAll().log();
        StepVerifier.create(storedData)
            .expectNextCount(5)
            .expectComplete()
            .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"max-polling-period.json"})
    void readExceededPollingPeriod(String resultFile) {

            log.info("environment: {}", environment);

            wm.stubFor(get("/exportvesseltrack/1234567890")
                .willReturn(aResponse()
                    .withBodyFile(resultFile)
                    .withHeader("Content-Type", "text/html; charset=UTF-8")
                    .withStatus(400)
                )
            );

            var result = aisReaderService.readAis();

            StepVerifier.create(result)
                .expectErrorSatisfies(e -> {
                    assertThat(e, Matchers.instanceOf(RuntimeException.class));
                    assertThat(e.getMessage(), Matchers.equalTo("POLLING RANGE MAXIMUM IS 190 DAYS"));
                })
                .verify();
    }

    private void assertDataAndRecord(AisData aisData, List<AisData> mmsiList) {
        assertThat(aisData, hasProperty("id", Matchers.matchesRegex("\\p{XDigit}{24}")));
        mmsiList.add(aisData);
    }
}