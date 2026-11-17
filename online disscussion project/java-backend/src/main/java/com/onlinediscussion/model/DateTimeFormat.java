package com.onlinediscussion.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

/**
 * Formats timestamps the same way the existing Node.js server does, so that both backends
 * render dates identically.
 *
 * <p>The wire format is {@code MMM-dd-yyyy:HH:mm:ss}, for example {@code Jan-24-2024:11:34:28}.
 */
public final class DateTimeFormat {

    private static final DateTimeFormatter KEY_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM-dd-yyyy:HH:mm:ss")
            .toFormatter(Locale.ENGLISH);

    private DateTimeFormat() {
    }

    /** Renders a timestamp in the shared {@code MMM-dd-yyyy:HH:mm:ss} format. */
    public static String format(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return KEY_FORMAT.format(value);
    }

    /**
     * Parses a timestamp produced by {@link #format(LocalDateTime)} back into a
     * {@link LocalDateTime}. Returns {@code null} when the text cannot be parsed.
     */
    public static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, KEY_FORMAT);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
