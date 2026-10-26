package com.example.wikipediaclient.data.remote;
import android.os.Handler;
import android.os.Looper;

/** Delays retries on the main thread; tests substitute a manual clock. */
public interface RetryScheduler {
    void postDelayed(Runnable task, long delayMs);
    void cancel(Runnable task);

    static RetryScheduler mainThread() {
        Handler handler = new Handler(Looper.getMainLooper());
        return new RetryScheduler() {
            @Override public void postDelayed(Runnable task, long delayMs) { handler.postDelayed(task, delayMs); }
            @Override public void cancel(Runnable task) { handler.removeCallbacks(task); }
        };
    }
}
