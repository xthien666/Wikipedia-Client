package com.example.wikipediaclient.data.model;

/** Canonical metadata only. Never construct this from an unresolved search title. */
public final class PersonalArticle {
    private final String language;
    private final long pageId;
    private final String title;
    private final String canonicalUrl;

    /** Validates the resolved identity before it reaches persistent storage. */
    public PersonalArticle(String language, long pageId, String title, String canonicalUrl) {
        if (!"vi".equals(language)) throw new IllegalArgumentException("Only Vietnamese is supported.");
        if (pageId <= 0) throw new IllegalArgumentException("pageId must be positive.");
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Title is required.");
        if (canonicalUrl == null || canonicalUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Canonical URL is required.");
        }
        this.language = language;
        this.pageId = pageId;
        this.title = title.trim();
        this.canonicalUrl = canonicalUrl.trim();
    }

    /** Returns the Wikipedia language code used in the composite key. */
    public String getLanguage() { return language; }
    /** Returns the resolved Wikipedia page ID. */
    public long getPageId() { return pageId; }
    /** Returns the latest known display title. */
    public String getTitle() { return title; }
    /** Returns the canonical URL supplied by the article API. */
    public String getCanonicalUrl() { return canonicalUrl; }
}
