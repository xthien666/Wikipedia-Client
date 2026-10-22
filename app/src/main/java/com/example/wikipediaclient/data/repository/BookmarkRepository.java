package com.example.wikipediaclient.data.repository;

import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import java.util.List;

/**
 * Blocking data operations: call on AppContainer.getDiskExecutor(), never from a View.
 * Implementations return detached lists, newest first; ties use pageId descending.
 * Failures propagate to the caller, which must publish an error state on the main thread.
 */
public interface BookmarkRepository {
    /** Returns saved articles, newest first, without exposing mutable internal state. */
    List<PersonalArticleEntry> getBookmarks();
    /** Checks membership by resolved identity. */
    boolean isBookmarked(String language, long pageId);
    /** Idempotent: update metadata for an existing key, preserving its original savedAt. */
    void saveBookmark(PersonalArticle article, long savedAt);
    /** Removing a missing bookmark is a no-op. */
    void removeBookmark(String language, long pageId);
}
