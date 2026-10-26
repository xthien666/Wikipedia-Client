package com.example.wikipediaclient.data.repository.fake;

import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import com.example.wikipediaclient.data.repository.BookmarkRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Development fake only: empty initially, all data disappears when this instance is discarded. */
public final class FakeBookmarkRepository implements BookmarkRepository {
    private final Map<String, PersonalArticleEntry> entries = new HashMap<>();

    /** Returns a sorted snapshot for isolated tests. */
    @Override public synchronized List<PersonalArticleEntry> getBookmarks() {
        List<PersonalArticleEntry> result = new ArrayList<>(entries.values());
        Collections.sort(result, (left, right) -> {
            int byTime = Long.compare(right.getTimestamp(), left.getTimestamp());
            return byTime != 0 ? byTime
                    : Long.compare(right.getArticle().getPageId(), left.getArticle().getPageId());
        });
        return result;
    }

    /** Checks whether the test collection contains this identity. */
    @Override public synchronized boolean isBookmarked(String language, long pageId) {
        return entries.containsKey(key(language, pageId));
    }

    /** Updates metadata without duplicating a saved key. */
    @Override public synchronized void saveBookmark(PersonalArticle article, long savedAt) {
        PersonalArticleEntry incoming = new PersonalArticleEntry(article, savedAt);
        String key = key(article.getLanguage(), article.getPageId());
        PersonalArticleEntry previous = entries.get(key);
        entries.put(key, previous == null ? incoming
                : new PersonalArticleEntry(article, previous.getTimestamp()));
    }

    /** Removes only the requested bookmark. */
    @Override public synchronized void removeBookmark(String language, long pageId) {
        entries.remove(key(language, pageId));
    }

    /** Validates and combines language and page ID. */
    private static String key(String language, long pageId) {
        if (!"vi".equals(language) || pageId <= 0) throw new IllegalArgumentException("Invalid article key.");
        return language + ":" + pageId;
    }
}
