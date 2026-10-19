package com.example.wikipediaclient.ui.search;
import android.os.Handler;
import android.os.Looper;

/** Debounce seam: the real one posts to the main looper, tests use a fake. */
public interface DelayedExecutor {
    void postDelayed(Runnable task, long delayMs);
    void cancel(Runnable task);

    static DelayedExecutor mainThread() {
        Handler handler = new Handler(Looper.getMainLooper());
        return new DelayedExecutor() {
            @Override public void postDelayed(Runnable task, long delayMs) { handler.postDelayed(task, delayMs); }
            @Override public void cancel(Runnable task) { handler.removeCallbacks(task); }
        };
    }
}
