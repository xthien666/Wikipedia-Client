package com.example.wikipediaclient.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.wikipediaclient.data.model.HistoryEntry;
import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import com.example.wikipediaclient.data.repository.HistoryRepository;
import com.example.wikipediaclient.data.repository.HistoryState;
import com.example.wikipediaclient.data.repository.SqliteHistoryStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import org.json.JSONArray;
import org.json.JSONObject;

/** Observable history backed by TV4's SQLite store; all disk work uses one serial executor. */
public final class LocalHistoryRepository implements HistoryRepository {
    private final SharedPreferences legacy;
    private final Executor diskExecutor;
    private final WikiDatabaseHelper database;
    private final SqliteHistoryStore store;
    private final MutableLiveData<HistoryState> state = new MutableLiveData<>(HistoryState.loading());
    private List<HistoryEntry> lastKnownEntries = Collections.emptyList();

    public LocalHistoryRepository(Context context, Executor diskExecutor) {
        this(context, diskExecutor, new WikiDatabaseHelper(context));
    }

    public LocalHistoryRepository(Context context, Executor diskExecutor, WikiDatabaseHelper database) {
        legacy = context.getApplicationContext().getSharedPreferences("reading_history", Context.MODE_PRIVATE);
        this.diskExecutor = diskExecutor;
        this.database = database;
        store = new SqliteHistoryStore(database);
        refresh();
    }

    @Override public LiveData<HistoryState> observe() { return state; }

    @Override public void refresh() {
        diskExecutor.execute(() -> {
            try {
                migrateLegacy();
                publish();
            } catch (Exception error) {
                state.postValue(HistoryState.error(lastKnownEntries, HistoryState.Failure.LOAD));
            }
        });
    }

    @Override public void record(HistoryEntry entry) {
        change(() -> store.recordRead(new PersonalArticle(entry.getLanguage(), entry.getPageId(),
                entry.getTitle(), entry.getCanonicalUrl()), entry.getLastReadAt()));
    }

    @Override public void remove(String language, long pageId) {
        change(() -> store.removeHistory(language, pageId));
    }

    @Override public void clear() {
        diskExecutor.execute(() -> {
            try {
                // A confirmed reset may discard malformed legacy JSON without attempting to import it.
                if (!legacy.edit().remove("entries").commit()) {
                    throw new IllegalStateException("Could not reset legacy history");
                }
                store.clearHistory();
                publish();
            } catch (Exception error) {
                state.postValue(HistoryState.error(lastKnownEntries, HistoryState.Failure.WRITE));
            }
        });
    }

    private void change(Runnable action) {
        diskExecutor.execute(() -> {
            try {
                migrateLegacy();
                action.run();
                publish();
            } catch (Exception error) {
                state.postValue(HistoryState.error(lastKnownEntries, HistoryState.Failure.WRITE));
            }
        });
    }

    private void publish() {
        List<HistoryEntry> entries = new ArrayList<>();
        for (PersonalArticleEntry row : store.getHistory()) {
            PersonalArticle article = row.getArticle();
            entries.add(new HistoryEntry(article.getLanguage(), article.getPageId(), article.getTitle(),
                    article.getCanonicalUrl(), row.getTimestamp()));
        }
        lastKnownEntries = entries;
        state.postValue(HistoryState.ready(entries));
    }

    /** Validate all old records before importing. A retry is idempotent if the process stops midway. */
    private void migrateLegacy() throws Exception {
        if (!legacy.contains("entries")) return;
        JSONArray json = new JSONArray(legacy.getString("entries", "[]"));
        List<HistoryEntry> entries = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            JSONObject item = json.getJSONObject(i);
            entries.add(new HistoryEntry(item.getString("language"), item.getLong("pageId"),
                    item.getString("title"), item.getString("canonicalUrl"), item.getLong("lastReadAt")));
        }
        SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            for (HistoryEntry entry : entries) {
                store.recordRead(new PersonalArticle(entry.getLanguage(), entry.getPageId(), entry.getTitle(),
                        entry.getCanonicalUrl()), entry.getLastReadAt());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (!legacy.edit().remove("entries").commit()) {
            throw new IllegalStateException("Could not finish legacy history migration");
        }
    }
}
