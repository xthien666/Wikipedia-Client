package com.example.wikipediaclient.data.remote;
import android.net.Uri;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import androidx.annotation.Nullable;
import com.example.wikipediaclient.data.model.SearchContinuation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/** Thin Volley wrapper for vi.wikipedia.org. Returns raw JSON; mapping lives in the mappers. */
public final class WikipediaApi {
    public static final String USER_AGENT = "WikiRead/0.1 (https://github.com/xthien666/Wikipedia-Client; Android)";
    static final String TAG = "wikipedia-api";
    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_RETRIES = 1;

    /** Request parameters a server-sent continuation must never override. */
    private static final Set<String> FIXED_SEARCH_PARAMS = new HashSet<>(Arrays.asList(
            "action", "list", "srnamespace", "srsearch", "srlimit", "format", "formatversion"));

    private final RequestQueue queue;
    private final RetryScheduler scheduler;
    interface Clock { long now(); }
    private final Clock clock;
    private long retryNotBefore;
    private final Set<ManagedCall> activeCalls = new HashSet<>();

    public WikipediaApi(RequestQueue queue) { this(queue, RetryScheduler.mainThread()); }

    public WikipediaApi(RequestQueue queue, RetryScheduler scheduler) {
        this(queue, scheduler, android.os.SystemClock::elapsedRealtime);
    }

    WikipediaApi(RequestQueue queue, RetryScheduler scheduler, Clock clock) {
        this.queue = queue;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    /**
     * First page when {@code continuation} is null; otherwise every key of the previous
     * response's {@code continue} object is sent back, as MediaWiki requires.
     */
    public static String buildSearchUrl(String query, @Nullable SearchContinuation continuation, int limit) {
        Uri.Builder builder = new Uri.Builder()
                .scheme("https").authority("vi.wikipedia.org")
                .appendPath("w").appendPath("api.php")
                .appendQueryParameter("action", "query")
                .appendQueryParameter("list", "search")
                .appendQueryParameter("srnamespace", "0")
                .appendQueryParameter("srsearch", query)
                .appendQueryParameter("srlimit", String.valueOf(limit))
                .appendQueryParameter("format", "json")
                .appendQueryParameter("formatversion", "2");
        if (continuation != null) {
            for (Map.Entry<String, String> param : continuation.getParams().entrySet()) {
                if (!FIXED_SEARCH_PARAMS.contains(param.getKey())) {
                    builder.appendQueryParameter(param.getKey(), param.getValue());
                }
            }
        }
        return builder.build().toString();
    }

    public static String buildSearchUrl(String query, int offset, int limit) {
        return buildSearchUrl(query, offset > 0 ? SearchContinuation.ofOffset(offset) : null, limit);
    }

    public Cancelable search(String query, @Nullable SearchContinuation continuation, int limit,
                             ApiCallback<JSONObject> callback) {
        return get(buildSearchUrl(query, continuation, limit), callback);
    }

    public static String buildSummaryUrl(String title) {
        return new Uri.Builder()
                .scheme("https").authority("vi.wikipedia.org")
                .appendPath("api").appendPath("rest_v1").appendPath("page").appendPath("summary")
                .appendPath(title.trim().replace(' ', '_'))
                .build().toString();
    }

    public Cancelable summary(String title, ApiCallback<JSONObject> callback) {
        return get(buildSummaryUrl(title), callback);
    }

    public static String buildContentUrl(String title) {
        return new Uri.Builder().scheme("https").authority("vi.wikipedia.org")
                .appendPath("w").appendPath("rest.php").appendPath("v1").appendPath("page")
                .appendPath(title.trim().replace(' ', '_')).appendPath("with_html").build().toString();
    }

    public Cancelable content(String title, ApiCallback<JSONObject> callback) {
        return get(buildContentUrl(title), callback);
    }

    public void cancelAll() {
        for (ManagedCall call : new HashSet<>(activeCalls)) call.cancel();
        queue.cancelAll(TAG);
    }

    /** The shared server deadline also applies to manual retries and newly opened articles. */
    private Cancelable get(String url, ApiCallback<JSONObject> callback) {
        ManagedCall call = new ManagedCall(url, callback);
        activeCalls.add(call);
        call.start();
        return call;
    }

    private final class ManagedCall implements Cancelable {
        final String url; final ApiCallback<JSONObject> callback;
        Cancelable delegate; boolean finished;
        ManagedCall(String url, ApiCallback<JSONObject> callback) { this.url = url; this.callback = callback; }
        void start() {
            delegate = RetryingCall.start(attempt -> {
                DeferredSend send = new DeferredSend(url, attempt); send.run(); return send;
            }, scheduler, new ApiCallback<JSONObject>() {
                public void onSuccess(JSONObject value) {
                    if (!finished) { finish(); callback.onSuccess(value); }
                }
                public void onError(WikiApiException error) {
                    if (!finished) { finish(); callback.onError(error); }
                }
            });
        }
        void finish() { finished = true; activeCalls.remove(this); }
        public void cancel() { finish(); if (delegate != null) delegate.cancel(); }
    }

    private final class DeferredSend implements Cancelable, Runnable {
        final String url; final ApiCallback<JSONObject> callback;
        Cancelable current; boolean cancelled;
        DeferredSend(String url, ApiCallback<JSONObject> callback) { this.url = url; this.callback = callback; }
        public void run() {
            if (cancelled) return;
            long remaining = retryNotBefore > clock.now() ? retryNotBefore - clock.now() : 0;
            if (remaining > 0) {
                scheduler.postDelayed(this, Math.min(remaining, 60_000L));
                return;
            }
            current = send(url, new ApiCallback<JSONObject>() {
                public void onSuccess(JSONObject value) { if (!cancelled) callback.onSuccess(value); }
                public void onError(WikiApiException error) {
                    if (cancelled) return;
                    if (error.isRateLimited()) {
                        long wait = error.getRetryAfterMs() >= 0 ? error.getRetryAfterMs() : 1_000;
                        long now = clock.now();
                        retryNotBefore = Math.max(retryNotBefore, wait > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + wait);
                    }
                    callback.onError(error);
                }
            });
        }
        public void cancel() {
            cancelled = true; scheduler.cancel(this);
            if (current != null) current.cancel();
        }
    }

    private Cancelable send(String url, ApiCallback<JSONObject> callback) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                callback::onSuccess, error -> callback.onError(toException(error))) {
            @Override public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        request.setTag(TAG);
        request.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS, MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
        return request::cancel;
    }

    static WikiApiException toException(VolleyError error) {
        if (error instanceof TimeoutError) {
            return new WikiApiException(WikiApiException.Kind.TIMEOUT, "Request timed out", error);
        }
        if (error instanceof ParseError) {
            return new WikiApiException(WikiApiException.Kind.DATA, "Malformed response", error);
        }
        if (error.networkResponse != null) {
            int status = error.networkResponse.statusCode;
            long retryAfterMs = RetryAfter.fromHeaders(error.networkResponse.headers, System.currentTimeMillis());
            return new WikiApiException(WikiApiException.Kind.SERVER, "HTTP " + status, error, status, retryAfterMs);
        }
        if (error instanceof NoConnectionError || error instanceof NetworkError) {
            return new WikiApiException(WikiApiException.Kind.NO_CONNECTION, "No connection", error);
        }
        return new WikiApiException(WikiApiException.Kind.UNKNOWN, String.valueOf(error.getMessage()), error);
    }
}
