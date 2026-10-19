package com.example.wikipediaclient.data.remote;
import androidx.annotation.Nullable;
import com.example.wikipediaclient.data.model.ArticleSummary;
import org.json.JSONException;
import org.json.JSONObject;

/** Pure JSON -> model mapping for REST {@code /page/summary/<title>}. */
public final class ArticleSummaryMapper {
    private ArticleSummaryMapper() {}

    /** @throws JSONException when the canonical identity (pageid, title, url) is missing. */
    public static ArticleSummary parse(JSONObject root) throws JSONException {
        long pageId = root.optLong("pageid", 0);
        String title = root.optString("title", "").trim();
        JSONObject desktop = root.optJSONObject("content_urls") == null
                ? null : root.optJSONObject("content_urls").optJSONObject("desktop");
        String pageUrl = desktop == null ? "" : desktop.optString("page", "");
        if (pageId <= 0 || title.isEmpty() || pageUrl.isEmpty()) {
            throw new JSONException("Summary is missing pageid, title or page url");
        }
        JSONObject thumbnail = root.optJSONObject("thumbnail");
        return new ArticleSummary(pageId, title, nullIfEmpty(root.optString("description", "")),
                root.optString("extract", ""),
                thumbnail == null ? null : nullIfEmpty(thumbnail.optString("source", "")), pageUrl);
    }

    @Nullable private static String nullIfEmpty(String value) { return value.isEmpty() ? null : value; }
}
