package no.maddin.ais.reader;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import lombok.extern.slf4j.Slf4j;
import no.maddin.ais.data.AisData;
import no.maddin.ais.repository.AisDataReactiveRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
//    "ais.reader.start-date=2018-01-01",
    "spring.data.mongodb.database=ais-reader-test",
    "spring.data.mongodb.uri=${mongodb.uri}"
})
@Slf4j
@Testcontainers
class AisReaderServiceTest {

    @Container
    static final MongoDBContainer mongodb = new MongoDBContainer("mongo:8.0.0-noble").withExposedPorts(27017);

    @RegisterExtension
    static final WireMockExtension wm = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
        .build();

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("wiremock.server.port", wm::getPort);
        registry.add("ais.reader.start-date", () -> LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        registry.add("mongodb.uri", () -> mongodb.getConnectionString());
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
                .status("001")
                .build(),
            AisData.builder()
                .id("000000000000000000000002")
                .mmsi("123456789")
                .lon("0.0")
                .lat("0.0")
                .timestamp(OffsetDateTime.of(2021, 1, 1, 0, 1, 0, 0, ZoneOffset.UTC))
                .speed("0")
                .status("002")
                .build(),
            AisData.builder()
                .id("000000000000000000000003")
                .mmsi("123456789")
                .lon("0.0")
                .lat("0.0")
                .timestamp(OffsetDateTime.of(2021, 1, 1, 0, 2, 0, 0, ZoneOffset.UTC))
                .speed("0")
                .status("003")
                .build()
        )).blockLast();
    }

    @AfterEach
    void tearDown() {
        aisDataReactiveRepository.deleteAll().block();
    }

    @Test
    void readSimpleAis() {

        var timestamp1 = LocalDateTime.now().minusDays(1).minusSeconds(60).withNano(0);
        var timestamp2 = timestamp1.plusSeconds(60).withNano(0);
        var responseBody = """
            [
              {
                "MMSI": "1234567890",
                "IMO": "0",
                "STATUS": "5",
                "SPEED": "0",
                "LON": "23.726880",
                "LAT": "37.878850",
                "COURSE": "0",
                "HEADING": "320",
                "TIMESTAMP": "%s",
                "SHIP_ID": "1234567"
              },
              {
                "MMSI": "1234567890",
                "IMO": "0",
                "STATUS": "15",
                "SPEED": "2",
                "LON": "23.548990",
                "LAT": "37.903030",
                "COURSE": "160",
                "HEADING": "160",
                "TIMESTAMP": "%s",
                "SHIP_ID": "1234567"
              }
            ]
            """.formatted(timestamp1.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")), timestamp2.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")));

        wm.stubFor(get("/exportvesseltrack/1234567890")
            .willReturn(aResponse()
                .withBody(responseBody)
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
            )
        );

        var result = aisReaderService.readAis();

        List<AisData> dataList = Collections.synchronizedList(new ArrayList<>());

        StepVerifier.create(result)
            .assertNext(ad -> assertDataAndRecord(ad, dataList))
            .assertNext(ad -> assertDataAndRecord(ad, dataList))
            .expectComplete()
            .verify();

        // Events do not necessarily come in expected order
        List<OffsetDateTime> timestamps = dataList.stream()
            .map(AisData::getTimestamp)
            .collect(Collectors.toList());

        assertThat(timestamps, Matchers.hasItems(
            Matchers.equalTo(timestamp1.atOffset(ZoneOffset.UTC)),
            Matchers.equalTo(timestamp2.atOffset(ZoneOffset.UTC))
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

    @ParameterizedTest
    @ValueSource(strings = {"insufficient-credits.json"})
    void insufficientCredits(String resultFile) {
        wm.stubFor(get("/exportvesseltrack/1234567890")
            .willReturn(aResponse()
                .withBodyFile(resultFile)
                .withHeader("Content-Type", "text/html; charset=UTF-8")
                .withStatus(401)
            )
        );

        var result = aisReaderService.readAis();
        StepVerifier.create(result)
            .expectErrorSatisfies(e -> {
                assertThat(e, Matchers.instanceOf(RuntimeException.class));
                assertThat(e.getMessage(), Matchers.startsWith("INSUFFICIENT CREDITS."));
            })
            .verify();
    }

    private void assertDataAndRecord(AisData aisData, List<AisData> mmsiList) {
        assertThat(aisData, hasProperty("id", Matchers.matchesRegex("\\p{XDigit}{24}")));
        mmsiList.add(aisData);
    }
}