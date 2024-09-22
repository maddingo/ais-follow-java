package no.maddin.ais.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import lombok.extern.slf4j.Slf4j;
import no.maddin.ais.data.OffsetDateTimeCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.convert.ConverterBuilder;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

@Configuration
@Slf4j
public class MongoDbConfig {

    @Bean
    public MongoClientSettings mongoClientSettings(Environment env) {

        CodecRegistry myRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(new OffsetDateTimeCodec())
        );
        return MongoClientSettings.builder()
            .codecRegistry(myRegistry)
                .applyConnectionString(new ConnectionString(env.getProperty("spring.data.mongodb.uri")))
            .build();
    }

    @Bean
    public MongoCustomConversions customConversions() {
        ConverterBuilder.WritingConverterBuilder<OffsetDateTime, Date> writer = ConverterBuilder.writing(OffsetDateTime.class, Date.class, offsetDateTime -> Date.from(offsetDateTime.toInstant()));
        ConverterBuilder.ReadingConverterBuilder<Date, OffsetDateTime> reader = ConverterBuilder.reading(Date.class, OffsetDateTime.class, date -> OffsetDateTime.ofInstant(date.toInstant(), java.time.ZoneOffset.UTC));

        return new MongoCustomConversions(List.of(reader, writer));
    }

}