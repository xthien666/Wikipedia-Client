package com.example.wikipediaclient.data.remote;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/** Pure JVM: a fake transport and a manual clock stand in for Volley and the main looper. */
public class RetryingCallTest {
    private static final class FakeScheduler implements RetryScheduler {
        Runnable pending;
        final List<Long> delays = new ArrayList<>();
        @Override public void postDelayed(Runnable task, long delayMs) { pending = task; delays.add(delayMs); }
        @Override public void cancel(Runnable task) { if (pending == task) pending = null; }
        void elapse() { Runnable task = pending; pending = null; if (task != null) task.run(); }
    }

    /** Each attempt waits for the test to answer it, like a real request in flight. */
    private static final class FakeTransport implements RetryingCall.Attempt<String> {
        final List<ApiCallback<String>> attempts = new ArrayList<>();
        int cancelled;
        @Override public Cancelable start(ApiCallback<String> callback) {
            attempts.add(callback);
            return () -> cancelled++;
        }
        ApiCallback<String> last() { return attempts.get(attempts.size() - 1); }
    }

    private static final class Recorder implements ApiCallback<String> {
        final List<String> successes = new ArrayList<>();
        final List<WikiApiException> errors = new ArrayList<>();
        @Override public void onSuccess(String result) { successes.add(result); }
        @Override public void onError(WikiApiException error) { errors.add(error); }
    }

    private FakeScheduler scheduler;
    private FakeTransport transport;
    private Recorder recorder;

    @Before public void setUp() {
        scheduler = new FakeScheduler();
        transport = new FakeTransport();
        recorder = new Recorder();
    }

    private Cancelable start() { return RetryingCall.start(transport, scheduler, recorder); }

    private static WikiApiException http(int status, long retryAfterMs) {
        return new WikiApiException(WikiApiException.Kind.SERVER, "HTTP " + status, null, status, retryAfterMs);
    }

    @Test public void tooManyRequestsWaitsForRetryAfterThenRecovers() {
        start();
        transport.last().onError(http(429, 2_000));
        assertEquals("Nothing reported while waiting", 0, recorder.errors.size());
        assertEquals(Long.valueOf(2_000), scheduler.delays.get(0));
        assertEquals("Not resent before the wait", 1, transport.attempts.size());

        scheduler.elapse();
        assertEquals(2, transport.attempts.size());
        transport.last().onSuccess("ok");
        assertEquals(Arrays.asList("ok"), recorder.successes);
        assertTrue(recorder.errors.isEmpty());
    }

    @Test public void serviceUnavailableWithoutHeaderBacksOffExponentially() {
        start();
        transport.last().onError(http(503, -1));
        scheduler.elapse();
        transport.last().onError(http(503, -1));
        assertEquals(Arrays.asList(1_000L, 2_000L), scheduler.delays);
        scheduler.elapse();
        transport.last().onSuccess("ok");
        assertEquals(3, transport.attempts.size());
        assertEquals(Arrays.asList("ok"), recorder.successes);
    }

    @Test public void givesUpAfterMaxAttemptsAndReportsTheLastError() {
        start();
        for (int i = 0; i < RetryingCall.DEFAULT_MAX_ATTEMPTS - 1; i++) {
            transport.last().onError(http(429, 0));
            scheduler.elapse();
        }
        transport.last().onError(http(429, 0));
        assertEquals(RetryingCall.DEFAULT_MAX_ATTEMPTS, transport.attempts.size());
        assertNull("No further retry scheduled", scheduler.pending);
        assertEquals(1, recorder.errors.size());
        assertEquals(429, recorder.errors.get(0).getStatusCode());
    }

    @Test public void retryAfterLongerThanTheCapIsReportedImmediately() {
        start();
        transport.last().onError(http(429, 120_000));
        assertNull(scheduler.pending);
        assertEquals(1, transport.attempts.size());
        assertEquals(1, recorder.errors.size());
        assertEquals(120_000, recorder.errors.get(0).getRetryAfterMs());
    }

    @Test public void networkAndOtherHttpErrorsAreNotRetried() {
        start();
        transport.last().onError(new WikiApiException(WikiApiException.Kind.NO_CONNECTION, "offline", null));
        start();
        transport.last().onError(new WikiApiException(WikiApiException.Kind.TIMEOUT, "slow", null));
        start();
        transport.last().onError(http(404, -1));
        start();
        transport.last().onError(http(500, -1));
        assertEquals(4, transport.attempts.size());
        assertTrue(scheduler.delays.isEmpty());
        assertEquals(4, recorder.errors.size());
    }

    @Test public void cancelWhileWaitingStopsTheRetryAndSilencesCallbacks() {
        Cancelable call = start();
        ApiCallback<String> first = transport.last();
        first.onError(http(429, 1_000));
        call.cancel();
        assertNull("Pending retry removed", scheduler.pending);
        scheduler.elapse();
        assertEquals(1, transport.attempts.size());
        first.onSuccess("late");
        assertTrue(recorder.successes.isEmpty());
        assertTrue(recorder.errors.isEmpty());
    }

    @Test public void cancelDuringAttemptCancelsTheTransport() {
        Cancelable call = start();
        call.cancel();
        assertEquals(1, transport.cancelled);
        transport.last().onError(http(503, 0));
        assertNull(scheduler.pending);
        assertTrue(recorder.errors.isEmpty());
    }

    @Test public void retryDelayRules() {
        assertEquals(-1, RetryingCall.retryDelayMs(http(500, 1_000), 1, 3, 10_000));
        assertEquals(1_000, RetryingCall.retryDelayMs(http(429, 1_000), 1, 3, 10_000));
        assertEquals(0, RetryingCall.retryDelayMs(http(503, 0), 1, 3, 10_000));
        assertEquals(4_000, RetryingCall.retryDelayMs(http(503, -1), 3, 5, 10_000));
        assertEquals(-1, RetryingCall.retryDelayMs(http(429, 1_000), 3, 3, 10_000));
        assertEquals(-1, RetryingCall.retryDelayMs(http(429, 10_001), 1, 3, 10_000));
    }
}
