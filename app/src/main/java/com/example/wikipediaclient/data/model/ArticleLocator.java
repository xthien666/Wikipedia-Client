package com.example.wikipediaclient.data.model;
import androidx.annotation.Nullable;
/** An open request, not the canonical identity returned by the article API. */
public final class ArticleLocator {
    private final String language;
    private final String title;
    @Nullable private final Long pageId;
    public ArticleLocator(String language, String title, @Nullable Long pageId) {
        if (!"vi".equals(language)) throw new IllegalArgumentException("Only Vietnamese is supported.");
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Title is required.");
        if (pageId != null && pageId <= 0) throw new IllegalArgumentException("pageId must be positive.");
        this.language = language;
        this.title = title.trim();
        this.pageId = pageId;
    }
    public String getLanguage() { return language; }
    public String getTitle() { return title; }
    @Nullable public Long getPageId() { return pageId; }
}
