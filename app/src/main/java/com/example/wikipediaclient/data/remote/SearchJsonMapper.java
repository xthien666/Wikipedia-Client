package com.example.wikipediaclient.data.remote;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import com.example.wikipediaclient.data.model.SearchContinuation;
import com.example.wikipediaclient.data.model.SearchPage;
import com.example.wikipediaclient.data.model.SearchResult;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Pure JSON -> model mapping for the Action API {@code list=search} (formatversion=2). */
public final class SearchJsonMapper {
    private SearchJsonMapper() {}

    /** @throws JSONException when the response is not a valid search response (e.g. API error). */
    public static SearchPage parse(JSONObject root) throws JSONException {
        JSONObject query = root.optJSONObject("query");
        JSONArray search = query == null ? null : query.optJSONArray("search");
        if (search == null) {
            JSONObject error = root.optJSONObject("error");
            throw new JSONException(error != null
                    ? "API error: " + error.optString("code") : "Missing query.search");
        }
        List<SearchResult> results = new ArrayList<>(search.length());
        for (int i = 0; i < search.length(); i++) {
            JSONObject item = search.optJSONObject(i);
            if (item == null) continue;
            long pageId = item.optLong("pageid", 0);
            String title = item.optString("title", "").trim();
            if (pageId <= 0 || title.isEmpty()) continue; // unusable hit
            results.add(new SearchResult(pageId, title, stripHtml(item.optString("snippet", "")), null));
        }
        JSONObject searchInfo = query.optJSONObject("searchinfo");
        int totalHits = searchInfo == null ? results.size() : searchInfo.optInt("totalhits", results.size());
        return new SearchPage(results, parseContinuation(root.optJSONObject("continue")), totalHits);
    }

    /** Keeps every key of {@code continue}; MediaWiki needs all of them on the next request. */
    @Nullable
    static SearchContinuation parseContinuation(@Nullable JSONObject cont) {
        if (cont == null) return null;
        Map<String, String> params = new LinkedHashMap<>();
        Iterator<String> keys = cont.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = cont.opt(key);
            if (value == null || value == JSONObject.NULL
                    || value instanceof JSONObject || value instanceof JSONArray) continue;
            params.put(key, String.valueOf(value));
        }
        return params.isEmpty() ? null : new SearchContinuation(params);
    }

    /** Removes highlight tags and decodes entities (&quot;, &amp;, ...). */
    static String stripHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
                .toString().replaceAll("\\s+", " ").trim();
    }
}
