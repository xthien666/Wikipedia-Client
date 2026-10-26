package com.example.wikipediaclient.data.remote;

import static org.junit.Assert.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class RetryAfterTest {
    // Sun, 06 Nov 1994 08:49:37 GMT (the RFC 9110 example date).
    private static final long NOW = 784111777000L;

    @Test public void deltaSeconds() {
        assertEquals(120_000, RetryAfter.parseMs("120", NOW));
        assertEquals(0, RetryAfter.parseMs("0", NOW));
        assertEquals(5_000, RetryAfter.parseMs("  5 ", NOW));
    }

    @Test public void httpDateIsRelativeToNow() {
        assertEquals(30_000, RetryAfter.parseMs("Sun, 06 Nov 1994 08:50:07 GMT", NOW));
        assertEquals("A date in the past means retry now", 0,
                RetryAfter.parseMs("Sun, 06 Nov 1994 08:00:00 GMT", NOW));
    }

    @Test public void missingOrInvalidIsUnknown() {
        assertEquals(-1, RetryAfter.parseMs(null, NOW));
        assertEquals(-1, RetryAfter.parseMs("", NOW));
        assertEquals(-1, RetryAfter.parseMs("soon", NOW));
        assertEquals(-1, RetryAfter.parseMs("-5", NOW));
        assertEquals(-1, RetryAfter.parseMs("1.5", NOW));
    }

    @Test public void hugeValueDoesNotOverflow() {
        assertEquals(Long.MAX_VALUE, RetryAfter.parseMs("99999999999999999999", NOW));
    }

    @Test public void headerLookupIgnoresCase() {
        Map<String, String> headers = new HashMap<>();
        headers.put("retry-after", "7");
        assertEquals(7_000, RetryAfter.fromHeaders(headers, NOW));
        assertEquals(-1, RetryAfter.fromHeaders(Collections.emptyMap(), NOW));
        assertEquals(-1, RetryAfter.fromHeaders(null, NOW));
    }
}
