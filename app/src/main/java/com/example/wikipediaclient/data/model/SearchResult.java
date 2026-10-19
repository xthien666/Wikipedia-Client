package com.example.wikipediaclient.data.model;
import androidx.annotation.Nullable;
/** One search hit. The snippet is plain text (HTML highlight tags already removed). */
public final class SearchResult {
    private final long pageId;
    private final String title;
    private final String snippet;
    @Nullable private final String thumbnailUrl;
    public SearchResult(long pageId, String title, String snippet, @Nullable String thumbnailUrl) {
        if (pageId <= 0) throw new IllegalArgumentException("pageId must be positive.");
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Title is required.");
        this.pageId = pageId;
        this.title = title.trim();
        this.snippet = snippet == null ? "" : snippet;
        this.thumbnailUrl = thumbnailUrl;
    }
    public long getPageId() { return pageId; }
    public String getTitle() { return title; }
    public String getSnippet() { return snippet; }
    @Nullable public String getThumbnailUrl() { return thumbnailUrl; }
}
