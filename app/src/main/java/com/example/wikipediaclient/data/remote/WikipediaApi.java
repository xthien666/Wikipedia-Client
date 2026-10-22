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
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/** Thin Volley wrapper for vi.wikipedia.org. Returns raw JSON; mapping lives in the mappers. */
public final class WikipediaApi {
    public static final String USER_AGENT = "WikiRead/0.1 (https://github.com/xthien666/Wikipedia-Client; Android)";
    static final String TAG = "wikipedia-api";
    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_RETRIES = 1;

    private final RequestQueue queue;

    public WikipediaApi(RequestQueue queue) { this.queue = queue; }

    public static String buildSearchUrl(String query, int offset, int limit) {
        return new Uri.Builder()
                .scheme("https").authority("vi.wikipedia.org")
                .appendPath("w").appendPath("api.php")
                .appendQueryParameter("action", "query")
                .appendQueryParameter("list", "search")
                .appendQueryParameter("srnamespace", "0")
                .appendQueryParameter("srsearch", query)
                .appendQueryParameter("srlimit", String.valueOf(limit))
                .appendQueryParameter("sroffset", String.valueOf(offset))
                .appendQueryParameter("format", "json")
                .appendQueryParameter("formatversion", "2")
                .build().toString();
    }

    public Cancelable search(String query, int offset, int limit, ApiCallback<JSONObject> callback) {
        return get(buildSearchUrl(query, offset, limit), callback);
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

    public void cancelAll() { queue.cancelAll(TAG); }

    private Cancelable get(String url, ApiCallback<JSONObject> callback) {
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
            return new WikiApiException(WikiApiException.Kind.SERVER, "HTTP " + status, error, status);
        }
        if (error instanceof NoConnectionError || error instanceof NetworkError) {
            return new WikiApiException(WikiApiException.Kind.NO_CONNECTION, "No connection", error);
        }
        return new WikiApiException(WikiApiException.Kind.UNKNOWN, String.valueOf(error.getMessage()), error);
    }
}
