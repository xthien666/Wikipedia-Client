package com.example.wikipediaclient.data.remote;
import androidx.core.text.HtmlCompat;
import com.example.wikipediaclient.data.model.SearchPage;
import com.example.wikipediaclient.data.model.SearchResult;
import java.util.ArrayList;
import java.util.List;
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
        JSONObject cont = root.optJSONObject("continue");
        Integer nextOffset = cont != null && cont.has("sroffset") ? cont.optInt("sroffset") : null;
        return new SearchPage(results, nextOffset, totalHits);
    }

    /** Removes highlight tags and decodes entities (&quot;, &amp;, ...). */
    static String stripHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
                .toString().replaceAll("\\s+", " ").trim();
    }
}
