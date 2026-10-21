package com.example.wikipediaclient.data.model;

/** Metadata for a successfully displayed, resolved Wikipedia article. */
public final class HistoryEntry {
    private final String language;
    private final long pageId;
    private final String title;
    private final String canonicalUrl;
    private final long lastReadAt;

    public HistoryEntry(String language, long pageId, String title, String canonicalUrl, long lastReadAt) {
        if (!"vi".equals(language) || pageId <= 0 || title == null || title.trim().isEmpty()
                || canonicalUrl == null || canonicalUrl.trim().isEmpty() || lastReadAt <= 0) {
            throw new IllegalArgumentException("Resolved article metadata and read time are required.");
        }
        this.language = language;
        this.pageId = pageId;
        this.title = title;
        this.canonicalUrl = canonicalUrl;
        this.lastReadAt = lastReadAt;
    }

    public String getLanguage() { return language; }
    public long getPageId() { return pageId; }
    public String getTitle() { return title; }
    public String getCanonicalUrl() { return canonicalUrl; }
    public long getLastReadAt() { return lastReadAt; }
    public ArticleLocator toLocator() { return new ArticleLocator(language, title, pageId); }
}
