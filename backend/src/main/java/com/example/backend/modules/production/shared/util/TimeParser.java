package com.example.backend.modules.production.shared.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Utility class for parsing time strings used across production modules.
 */
public final class TimeParser {

    private TimeParser() {
        // Prevent instantiation
    }

    /**
     * Parses a time string into a LocalTime object, handling various common ISO formats.
     * @param timeStr the time string to parse
     * @return the parsed LocalTime, or null if the string is null/empty
     * @throws IllegalArgumentException if the time string cannot be parsed
     */
    public static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(timeStr).toLocalTime();
        } catch (DateTimeParseException e) {
            try {
                return LocalTime.parse(timeStr);
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDateTime.parse(timeStr).toLocalTime();
                } catch (DateTimeParseException e3) {
                    throw new IllegalArgumentException("Invalid time format: " + timeStr);
                }
            }
        }
    }
}
