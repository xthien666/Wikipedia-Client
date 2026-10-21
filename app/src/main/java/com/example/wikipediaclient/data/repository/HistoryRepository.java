package com.example.wikipediaclient.data.repository;

import androidx.lifecycle.LiveData;
import com.example.wikipediaclient.data.model.HistoryEntry;

/** Reader calls record only after the resolved article is displayed successfully. */
public interface HistoryRepository {
    LiveData<HistoryState> observe();
    void refresh();
    void record(HistoryEntry entry);
    void remove(String language, long pageId);
    void clear();
}
