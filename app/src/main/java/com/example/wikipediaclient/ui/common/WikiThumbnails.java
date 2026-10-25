package com.example.wikipediaclient.ui.common;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.data.remote.WikipediaApi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Loads Wikipedia lead images for article titles into ImageViews.
 *
 * <p>Titles requested within a short window are batched into a single {@code prop=pageimages}
 * query (up to 50 titles), so a list of results costs one metadata request instead of one per row.
 * Resolved URLs and decoded bitmaps are cached in memory; Volley's disk cache handles the rest.
 * The view's background acts as the placeholder: rows that have no lead image simply keep it.
 * A disabled instance (used by injected/test containers) never opens a network connection.
 */
public final class WikiThumbnails {
    private static final int THUMB_PX = 480;
    private static final int MAX_BATCH = 50;
    private static final long BATCH_DELAY_MS = 40;
    private static final String NONE = "";

    @Nullable private final RequestQueue queue;
    @Nullable private final ImageLoader images;
    private final Handler main = new Handler(Looper.getMainLooper());
    /** Requested title key -> image URL, or {@link #NONE} when the article has no lead image. */
    private final Map<String, String> urls = new HashMap<>();
    private final Map<String, List<ImageView>> waiting = new HashMap<>();
    private final Set<String> pending = new LinkedHashSet<>();
    private final Runnable flush = this::flush;

    /** Returns a loader that only resets views to their placeholder. */
    public static WikiThumbnails disabled() { return new WikiThumbnails(null); }

    public WikiThumbnails(@Nullable RequestQueue queue) {
        this.queue = queue;
        this.images = queue == null ? null : new UserAgentImageLoader(queue, new BitmapCache());
    }

    /** Binds the lead image of {@code title} to {@code target}; safe to call from RecyclerView binds. */
    public void load(@Nullable String title, @NonNull ImageView target) {
        cancel(target);
        target.setImageDrawable(null);
        target.setAlpha(1f);
        if (queue == null || title == null || title.trim().isEmpty()) {
            target.setTag(R.id.thumbnail_title, null);
            return;
        }
        String key = key(title);
        target.setTag(R.id.thumbnail_title, key);
        String url = urls.get(key);
        if (url != null) {
            if (!url.isEmpty()) fetch(url, key, target);
            return;
        }
        List<ImageView> views = waiting.get(key);
        if (views == null) {
            views = new ArrayList<>();
            waiting.put(key, views);
        }
        views.add(target);
        if (pending.add(key)) {
            main.removeCallbacks(flush);
            if (pending.size() >= MAX_BATCH) flush();
            else main.postDelayed(flush, BATCH_DELAY_MS);
        }
    }

    /** Cancels any in-flight image download bound to {@code target}. */
    public void cancel(@NonNull ImageView target) {
        Object title = target.getTag(R.id.thumbnail_title);
        List<ImageView> views = waiting.get(title);
        if (views != null) {
            views.remove(target);
            if (views.isEmpty()) {
                waiting.remove(title);
                pending.remove(title);
            }
        }
        target.setTag(R.id.thumbnail_title, null);
        target.animate().cancel();
        target.setAlpha(1f);
        Object request = target.getTag(R.id.thumbnail_request);
        if (request instanceof ImageLoader.ImageContainer) {
            ((ImageLoader.ImageContainer) request).cancelRequest();
        }
        target.setTag(R.id.thumbnail_request, null);
    }

    private void flush() {
        if (queue == null || pending.isEmpty()) return;
        List<String> batch = new ArrayList<>();
        Iterator<String> it = pending.iterator();
        while (it.hasNext() && batch.size() < MAX_BATCH) {
            batch.add(it.next());
            it.remove();
        }
        if (!pending.isEmpty()) main.post(flush);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, buildUrl(batch), null,
                response -> resolve(batch, response), error -> resolve(batch, null)) {
            @Override public Map<String, String> getHeaders() { return headers(); }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(10_000, 0, 1f));
        queue.add(request);
    }

    /** Maps each requested title through MediaWiki normalization and redirects to its page image. */
    private void resolve(List<String> batch, @Nullable JSONObject response) {
        Map<String, String> byPage = new HashMap<>();
        Map<String, String> renamed = new HashMap<>();
        JSONObject query = response == null ? null : response.optJSONObject("query");
        if (query != null) {
            collect(query.optJSONArray("normalized"), renamed);
            collect(query.optJSONArray("redirects"), renamed);
            JSONArray pages = query.optJSONArray("pages");
            for (int i = 0; pages != null && i < pages.length(); i++) {
                JSONObject page = pages.optJSONObject(i);
                JSONObject thumb = page == null ? null : page.optJSONObject("thumbnail");
                if (thumb != null) byPage.put(page.optString("title"), thumb.optString("source", NONE));
            }
        }
        for (String key : batch) {
            String title = key;
            for (int hop = 0; hop < 3 && renamed.containsKey(title); hop++) title = renamed.get(title);
            String url = byPage.get(title);
            // Network failures are not cached, so the next bind can retry.
            if (response != null) urls.put(key, url == null ? NONE : url);
            List<ImageView> views = waiting.remove(key);
            if (views == null || url == null || url.isEmpty()) continue;
            for (ImageView view : views) {
                if (key.equals(view.getTag(R.id.thumbnail_title))) fetch(url, key, view);
            }
        }
    }

    private void fetch(String url, String key, ImageView target) {
        if (images == null) return;
        ImageLoader.ImageContainer container = images.get(url, new ImageLoader.ImageListener() {
            @Override public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                Bitmap bitmap = response.getBitmap();
                if (bitmap == null || !key.equals(target.getTag(R.id.thumbnail_title))) return;
                target.setImageBitmap(bitmap);
                if (!isImmediate) {
                    target.setAlpha(0f);
                    target.animate().alpha(1f).setDuration(220).start();
                }
            }
            @Override public void onErrorResponse(VolleyError error) { /* keep the placeholder */ }
        });
        target.setTag(R.id.thumbnail_request, container);
    }

    private static void collect(@Nullable JSONArray pairs, Map<String, String> into) {
        for (int i = 0; pairs != null && i < pairs.length(); i++) {
            JSONObject pair = pairs.optJSONObject(i);
            if (pair != null) into.put(pair.optString("from"), pair.optString("to"));
        }
    }

    private static String key(String title) { return title.trim().replace('_', ' '); }

    static String buildUrl(List<String> titles) {
        StringBuilder joined = new StringBuilder();
        for (String title : titles) {
            if (joined.length() > 0) joined.append('|');
            joined.append(title);
        }
        return new Uri.Builder().scheme("https").authority("vi.wikipedia.org")
                .appendPath("w").appendPath("api.php")
                .appendQueryParameter("action", "query")
                .appendQueryParameter("prop", "pageimages")
                .appendQueryParameter("piprop", "thumbnail")
                .appendQueryParameter("pithumbsize", String.valueOf(THUMB_PX))
                .appendQueryParameter("pilimit", String.valueOf(MAX_BATCH))
                .appendQueryParameter("redirects", "1")
                .appendQueryParameter("titles", joined.toString())
                .appendQueryParameter("format", "json")
                .appendQueryParameter("formatversion", "2")
                .build().toString();
    }

    private static Map<String, String> headers() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", WikipediaApi.USER_AGENT);
        return headers;
    }

    /** Wikimedia's media servers require an identifying User-Agent on image requests too. */
    private static final class UserAgentImageLoader extends ImageLoader {
        UserAgentImageLoader(RequestQueue queue, ImageCache cache) { super(queue, cache); }

        @Override protected Request<Bitmap> makeImageRequest(String requestUrl, int maxWidth, int maxHeight,
                ImageView.ScaleType scaleType, final String cacheKey) {
            return new ImageRequest(requestUrl, response -> onGetImageSuccess(cacheKey, response),
                    maxWidth, maxHeight, scaleType, Bitmap.Config.RGB_565,
                    error -> onGetImageError(cacheKey, error)) {
                @Override public Map<String, String> getHeaders() { return headers(); }
            };
        }
    }

    /** Memory cache bounded to 1/8 of the heap. */
    private static final class BitmapCache extends LruCache<String, Bitmap> implements ImageLoader.ImageCache {
        BitmapCache() { super((int) Math.min(Integer.MAX_VALUE, Runtime.getRuntime().maxMemory() / 8)); }
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
        @Override public Bitmap getBitmap(String url) { return get(url); }
        @Override public void putBitmap(String url, Bitmap bitmap) { put(url, bitmap); }
    }
}
