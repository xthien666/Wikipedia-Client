package com.example.wikipediaclient.data.model;
import androidx.annotation.Nullable;
/**
 * Canonical article identity returned by the summary endpoint (after redirects).
 * Immutable so TV3 (reader) and TV4 (favorites/history) can store it safely.
 */
public final class ArticleSummary {
    private final long pageId;
    private final String title;
    @Nullable private final String description;
    private final String extract;
    @Nullable private final String thumbnailUrl;
    private final String pageUrl;
    public ArticleSummary(long pageId, String title, @Nullable String description, String extract,
                          @Nullable String thumbnailUrl, String pageUrl) {
        if (pageId <= 0) throw new IllegalArgumentException("pageId must be positive.");
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Title is required.");
        if (pageUrl == null || pageUrl.trim().isEmpty()) throw new IllegalArgumentException("pageUrl is required.");
        this.pageId = pageId;
        this.title = title.trim();
        this.description = description;
        this.extract = extract == null ? "" : extract;
        this.thumbnailUrl = thumbnailUrl;
        this.pageUrl = pageUrl;
    }
    public long getPageId() { return pageId; }
    public String getTitle() { return title; }
    @Nullable public String getDescription() { return description; }
    public String getExtract() { return extract; }
    @Nullable public String getThumbnailUrl() { return thumbnailUrl; }
    public String getPageUrl() { return pageUrl; }
}
