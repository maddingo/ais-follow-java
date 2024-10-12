package no.maddin.ais.data;

import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonRepresentation;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;

@Document(collection = "aisdata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"mmsi", "imo", "status", "speed", "lon", "lat", "course", "heading", "timestamp", "shipId"})
public class AisData {
    @Id
    private String id;

    @JsonProperty("MMSI")
    private String mmsi;

    @JsonProperty("IMO")
    private String imo;

    @JsonProperty("STATUS")
    private String status;

    @JsonProperty("SPEED")
    private String speed;

    @JsonProperty("LON")
    private String lon;

    @JsonProperty("LAT")
    private String lat;

    @JsonProperty("COURSE")
    private String course;

    @JsonProperty("HEADING")
    private String heading;

    @JsonProperty("TIMESTAMP")
    private OffsetDateTime timestamp;

    @JsonProperty("SHIP_ID")
    private String shipId;
}
