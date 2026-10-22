package com.example.wikipediaclient.data.repository;

import androidx.lifecycle.LiveData;
import com.example.wikipediaclient.data.model.HistoryEntry;
import com.example.wikipediaclient.data.model.PersonalArticle;

/** Reader calls record only after the resolved article is displayed successfully. */
public interface HistoryRepository {
    LiveData<HistoryState> observe();
    void refresh();
    void record(HistoryEntry entry);
    /** Asynchronous: records resolved metadata after the reader renders successfully. */
    default void recordRead(PersonalArticle article, long displayedAt) {
        record(new HistoryEntry(article.getLanguage(), article.getPageId(), article.getTitle(),
                article.getCanonicalUrl(), displayedAt));
    }
    void remove(String language, long pageId);
    void clear();
}
