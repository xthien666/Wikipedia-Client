package com.example.wikipediaclient.data.repository;

import com.example.wikipediaclient.data.local.WikiDatabaseHelper;
import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import java.util.List;

/** SQLite implementation. Call every operation on AppContainer.getDiskExecutor(). */
public final class LocalBookmarkRepository implements BookmarkRepository {
    private final LocalArticleStore store;
    /** Shares the application database with the other personal-data repository. */
    public LocalBookmarkRepository(WikiDatabaseHelper helper) { store = new LocalArticleStore(helper, false); }
    /** Returns bookmarks in descending saved-time order. */
    @Override public List<PersonalArticleEntry> getBookmarks() { return store.list(); }
    /** Returns whether the resolved article is already saved. */
    @Override public boolean isBookmarked(String language, long pageId) { return store.contains(language, pageId); }
    /** Saves metadata, keeping the original saved time if the key already exists. */
    @Override public void saveBookmark(PersonalArticle article, long savedAt) { store.save(article, savedAt); }
    /** Removes the bookmark without changing reading history. */
    @Override public void removeBookmark(String language, long pageId) { store.remove(language, pageId); }
}
