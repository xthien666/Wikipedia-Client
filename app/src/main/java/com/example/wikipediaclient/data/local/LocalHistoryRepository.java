package com.example.wikipediaclient.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
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
    // Accessed only on the serial disk executor. A new explicit write supersedes a failed one.
    private Write pendingWrite;

    private interface Write { void run() throws Exception; }

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
        diskExecutor.execute(this::load);
    }

    @Override public void retry() {
        diskExecutor.execute(() -> {
            if (pendingWrite == null) load();
            else write(pendingWrite);
        });
    }

    private void load() {
        try {
            migrateLegacy();
            publish();
        } catch (Exception error) {
            Log.e("HistoryRepository", "History reload failed", error);
            state.postValue(HistoryState.error(lastKnownEntries, pendingWrite == null
                    ? HistoryState.Failure.LOAD : HistoryState.Failure.WRITE));
        }
    }

    @Override public void record(HistoryEntry entry) {
        change(() -> store.recordRead(new PersonalArticle(entry.getLanguage(), entry.getPageId(),
                entry.getTitle(), entry.getCanonicalUrl()), entry.getLastReadAt()));
    }

    @Override public void remove(String language, long pageId) {
        change(() -> store.removeHistory(language, pageId));
    }

    @Override public void clear() {
        diskExecutor.execute(() -> write(() -> {
            // A confirmed reset may discard malformed legacy JSON without attempting to import it.
            if (!legacy.edit().remove("entries").commit()) {
                throw new IllegalStateException("Could not reset legacy history");
            }
            store.clearHistory();
        }));
    }

    private void change(Runnable action) {
        diskExecutor.execute(() -> write(() -> {
            migrateLegacy();
            action.run();
        }));
    }

    private void write(Write action) {
        pendingWrite = action;
        try {
            action.run();
            pendingWrite = null;
            publish();
        } catch (Exception error) {
            pendingWrite = action;
            Log.e("HistoryRepository", "History write failed", error);
            state.postValue(HistoryState.error(lastKnownEntries, HistoryState.Failure.WRITE));
        }
    }

    private void publish() {
        List<HistoryEntry> entries = new ArrayList<>();
        for (PersonalArticleEntry row : store.getHistory()) {
            PersonalArticle article = row.getArticle();
            entries.add(new HistoryEntry(article.getLanguage(), article.getPageId(), article.getTitle(),
                    article.getCanonicalUrl(), row.getTimestamp()));
        }
        lastKnownEntries = entries;
        state.postValue(pendingWrite == null ? HistoryState.ready(entries)
                : HistoryState.error(entries, HistoryState.Failure.WRITE));
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
