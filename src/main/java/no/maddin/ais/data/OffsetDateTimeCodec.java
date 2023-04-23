package no.maddin.ais.data;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.OffsetDateTime;
import java.util.List;

public class OffsetDateTimeCodec implements Codec<OffsetDateTime> {
    @Override
    public OffsetDateTime decode(BsonReader reader, DecoderContext decoderContext) {
        long dateTime = reader.readDateTime();
        return OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(dateTime), java.time.ZoneOffset.UTC);
    }

    @Override
    public void encode(BsonWriter writer, OffsetDateTime value, EncoderContext encoderContext) {
        writer.writeDateTime(value.toInstant().toEpochMilli());
    }

    @Override
    public Class<OffsetDateTime> getEncoderClass() {
        return OffsetDateTime.class;
    }
}
