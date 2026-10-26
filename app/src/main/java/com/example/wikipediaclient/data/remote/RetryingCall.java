package com.example.wikipediaclient.data.remote;

/**
 * Re-sends a request when the server answers 429 or 503, waiting as long as Retry-After asks
 * (or an exponential backoff when it does not say). Gives up and reports the error when the
 * attempts run out or the requested wait is too long to hold the user on a spinner.
 * Cancelling stops both the in-flight attempt and any pending retry; no callback follows.
 */
public final class RetryingCall<T> implements Cancelable {
    public interface Attempt<T> { Cancelable start(ApiCallback<T> callback); }

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_MAX_WAIT_MS = 10_000;
    static final long BASE_BACKOFF_MS = 1_000;

    private final Attempt<T> attempt;
    private final RetryScheduler scheduler;
    private final ApiCallback<T> callback;
    private final int maxAttempts;
    private final long maxWaitMs;
    private final Runnable retryTask = this::run;
    private Cancelable current;
    private int attempts;
    private boolean cancelled;

    public RetryingCall(Attempt<T> attempt, RetryScheduler scheduler, ApiCallback<T> callback,
                        int maxAttempts, long maxWaitMs) {
        this.attempt = attempt;
        this.scheduler = scheduler;
        this.callback = callback;
        this.maxAttempts = maxAttempts;
        this.maxWaitMs = maxWaitMs;
    }

    public static <T> Cancelable start(Attempt<T> attempt, RetryScheduler scheduler, ApiCallback<T> callback) {
        RetryingCall<T> call = new RetryingCall<>(attempt, scheduler, callback,
                DEFAULT_MAX_ATTEMPTS, DEFAULT_MAX_WAIT_MS);
        call.run();
        return call;
    }

    public void run() {
        if (cancelled) return;
        attempts++;
        final int attemptNumber = attempts;
        current = attempt.start(new ApiCallback<T>() {
            @Override public void onSuccess(T result) {
                if (!cancelled) callback.onSuccess(result);
            }
            @Override public void onError(WikiApiException error) {
                if (cancelled) return;
                long delay = retryDelayMs(error, attemptNumber, maxAttempts, maxWaitMs);
                if (delay < 0) {
                    callback.onError(error);
                } else {
                    scheduler.postDelayed(retryTask, delay);
                }
            }
        });
    }

    @Override public void cancel() {
        cancelled = true;
        scheduler.cancel(retryTask);
        if (current != null) current.cancel();
    }

    int getAttempts() { return attempts; }

    /** @return how long to wait before the next attempt, or -1 to give up and report the error. */
    static long retryDelayMs(WikiApiException error, int attemptsSoFar, int maxAttempts, long maxWaitMs) {
        if (!error.isRateLimited() || attemptsSoFar >= maxAttempts) return -1;
        long wait = error.getRetryAfterMs() >= 0
                ? error.getRetryAfterMs()
                : BASE_BACKOFF_MS << Math.min(attemptsSoFar - 1, 10);
        return wait > maxWaitMs ? -1 : wait;
    }
}
