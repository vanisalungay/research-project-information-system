package com.rpis.backend.util;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * The institution's display zone, pinned once at start-up.
 *
 * <p>Every notification timestamp is stored as a zone-less {@code TIMESTAMP} written from
 * the JVM's local clock. Pinning the JVM default here makes that wall-clock value
 * deterministic (the deployment host's own zone can no longer change it), and the
 * notification serializer uses the same zone to emit an explicit offset such as
 * {@code +08:00} so browsers anywhere parse the correct instant.
 */
public final class RpisTimeZone {

    private static volatile ZoneId zone = ZoneId.systemDefault();

    private RpisTimeZone() {
    }

    public static ZoneId zone() {
        return zone;
    }

    /** Pins the JVM default zone and the serializer zone. */
    public static synchronized void use(String zoneId) {
        ZoneId resolved = ZoneId.of(zoneId);
        TimeZone.setDefault(TimeZone.getTimeZone(resolved));
        zone = resolved;
    }
}
