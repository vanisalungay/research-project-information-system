package com.rpis.backend.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Serializes a notification's stored wall-clock timestamp with the institution's offset.
 *
 * <p>The column is zone-less, so Jackson's default {@code LocalDateTime} serializer emits
 * {@code 2026-11-18T14:07:12.345} with no zone. Browsers interpret such a string in their
 * own local zone, which shifts every notification for a user outside the server zone. This
 * serializer appends the configured offset (for example {@code +08:00}) without changing
 * any other entity's JSON.
 */
public class NotificationTimestampSerializer extends JsonSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            generator.writeNull();
            return;
        }
        generator.writeString(value.atZone(RpisTimeZone.zone()).toOffsetDateTime().toString());
    }
}
