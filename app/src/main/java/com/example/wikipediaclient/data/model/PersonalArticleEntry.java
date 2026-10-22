package com.example.wikipediaclient.data.model;

import java.util.Objects;

/** timestamp is savedAt for a bookmark, or lastReadAt for a history entry (epoch milliseconds). */
public final class PersonalArticleEntry {
    private final PersonalArticle article;
    private final long timestamp;

    /** Pairs immutable metadata with a nonnegative epoch-millisecond timestamp. */
    public PersonalArticleEntry(PersonalArticle article, long timestamp) {
        if (timestamp < 0) throw new IllegalArgumentException("Timestamp must not be negative.");
        this.article = Objects.requireNonNull(article, "article");
        this.timestamp = timestamp;
    }

    /** Returns the stored article metadata. */
    public PersonalArticle getArticle() { return article; }
    /** Returns savedAt for bookmarks or lastReadAt for history. */
    public long getTimestamp() { return timestamp; }
}
