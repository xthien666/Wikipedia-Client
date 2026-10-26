package com.example.wikipediaclient.data.remote;
import androidx.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Parses the HTTP Retry-After header: either delta-seconds or an HTTP-date (RFC 9110). */
public final class RetryAfter {
    private RetryAfter() {}

    /** @return the wait in milliseconds, or -1 when the header is missing or invalid. */
    public static long parseMs(@Nullable String value, long nowMs) {
        if (value == null) return -1;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return -1;
        if (trimmed.matches("\\d+")) {
            try {
                return Math.multiplyExact(Long.parseLong(trimmed), 1000L);
            } catch (ArithmeticException | NumberFormatException e) {
                return Long.MAX_VALUE;
            }
        }
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        format.setLenient(false);
        try {
            Date date = format.parse(trimmed);
            return date == null ? -1 : Math.max(0, date.getTime() - nowMs);
        } catch (ParseException e) {
            return -1;
        }
    }

    /** Case-insensitive lookup; Volley's header maps are not guaranteed to be. */
    public static long fromHeaders(@Nullable Map<String, String> headers, long nowMs) {
        if (headers == null) return -1;
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if ("Retry-After".equalsIgnoreCase(header.getKey())) return parseMs(header.getValue(), nowMs);
        }
        return -1;
    }
}
