package com.example.wikipediaclient.data.repository;

import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import java.util.List;

/** Blocking operations. Use the shared disk executor; deliver UI updates on the main thread. */
public interface HistoryRepository {
    /** Newest first; equal timestamps use pageId descending. Returns a detached list. */
    List<PersonalArticleEntry> getHistory();
    /** TV3 calls only AFTER successful rendering, with the final resolved article identity. */
    void recordRead(PersonalArticle article, long lastReadAt);
    /** Removes only the specified history entry; a missing entry is a no-op. */
    void removeHistory(String language, long pageId);
    /** Must not change bookmarks. */
    void clearHistory();
}
