package com.example.wikipediaclient.data.repository;

import com.example.wikipediaclient.data.local.WikiDatabaseHelper;
import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import java.util.List;

/** SQLite implementation. Call every operation on AppContainer.getDiskExecutor(). */
public final class SqliteHistoryStore {
    private final LocalArticleStore store;
    /** Shares the application database with the other personal-data repository. */
    public SqliteHistoryStore(WikiDatabaseHelper helper) { store = new LocalArticleStore(helper, true); }
    /** Returns the most recently read articles first. */
    public List<PersonalArticleEntry> getHistory() { return store.list(); }
    /** Records a successful display; the reader must supply its final resolved identity. */
    public void recordRead(PersonalArticle article, long lastReadAt) { store.save(article, lastReadAt); }
    /** Removes one history entry without changing bookmarks. */
    public void removeHistory(String language, long pageId) { store.remove(language, pageId); }
    /** Clears reading history only. */
    public void clearHistory() { store.clear(); }
}
