package com.example.wikipediaclient.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.wikipediaclient.data.model.HistoryEntry;
import com.example.wikipediaclient.data.repository.HistoryRepository;
import com.example.wikipediaclient.data.repository.HistoryState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import org.json.JSONArray;
import org.json.JSONObject;

/** Small durable bridge for history until the personal-data database is integrated. */
public final class LocalHistoryRepository implements HistoryRepository {
    private static final String KEY = "entries";
    private final SharedPreferences preferences;
    private final Executor diskExecutor;
    private final MutableLiveData<HistoryState> state = new MutableLiveData<>(HistoryState.loading());
    // Accessed only on the serial disk executor supplied by AppContainer.
    private List<HistoryEntry> lastKnownEntries = Collections.emptyList();

    public LocalHistoryRepository(Context context, Executor diskExecutor) {
        preferences = context.getApplicationContext().getSharedPreferences("reading_history", Context.MODE_PRIVATE);
        this.diskExecutor = diskExecutor;
        refresh();
    }

    @Override public LiveData<HistoryState> observe() { return state; }

    @Override public void refresh() {
        diskExecutor.execute(() -> {
            try { publish(read()); }
            catch (Exception error) {
                state.postValue(HistoryState.error(lastKnownEntries, HistoryState.Failure.LOAD));
            }
        });
    }

    @Override public void record(HistoryEntry entry) {
        diskExecutor.execute(() -> change(entries -> {
            removeMatching(entries, entry.getLanguage(), entry.getPageId());
            entries.add(entry);
        }));
    }

    @Override public void remove(String language, long pageId) {
        diskExecutor.execute(() -> change(entries -> removeMatching(entries, language, pageId)));
    }

    @Override public void clear() {
        diskExecutor.execute(() -> {
            // Clearing must also recover an unreadable store after explicit confirmation.
            if (preferences.edit().putString(KEY, "[]").commit()) {
                publish(Collections.emptyList());
            } else {
                state.postValue(HistoryState.error(lastKnownEntries, HistoryState.Failure.WRITE));
            }
        });
    }

    private interface Change { void apply(List<HistoryEntry> entries); }

    private void change(Change change) {
        try {
            List<HistoryEntry> entries = read();
            change.apply(entries);
            sortNewestFirst(entries);
            JSONArray json = new JSONArray();
            for (HistoryEntry entry : entries) {
                JSONObject item = new JSONObject();
                item.put("language", entry.getLanguage());
                item.put("pageId", entry.getPageId());
                item.put("title", entry.getTitle());
                item.put("canonicalUrl", entry.getCanonicalUrl());
                item.put("lastReadAt", entry.getLastReadAt());
                json.put(item);
            }
            if (!preferences.edit().putString(KEY, json.toString()).commit()) {
                throw new IllegalStateException("Could not save history");
            }
            publish(entries);
        } catch (Exception error) {
            state.postValue(HistoryState.error(lastKnownEntries, HistoryState.Failure.WRITE));
        }
    }

    private void publish(List<HistoryEntry> entries) {
        lastKnownEntries = new ArrayList<>(entries);
        state.postValue(HistoryState.ready(entries));
    }

    private List<HistoryEntry> read() throws Exception {
        JSONArray json = new JSONArray(preferences.getString(KEY, "[]"));
        List<HistoryEntry> entries = new ArrayList<>();
        for (int index = 0; index < json.length(); index++) {
            JSONObject item = json.getJSONObject(index);
            entries.add(new HistoryEntry(item.getString("language"), item.getLong("pageId"),
                    item.getString("title"), item.getString("canonicalUrl"),
                    item.getLong("lastReadAt")));
        }
        sortNewestFirst(entries);
        return entries;
    }

    private static void removeMatching(List<HistoryEntry> entries, String language, long pageId) {
        Iterator<HistoryEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            HistoryEntry item = iterator.next();
            if (item.getLanguage().equals(language) && item.getPageId() == pageId) iterator.remove();
        }
    }

    private static void sortNewestFirst(List<HistoryEntry> entries) {
        Collections.sort(entries, (left, right) -> Long.compare(
                right.getLastReadAt(), left.getLastReadAt()));
    }
}
