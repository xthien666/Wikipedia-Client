package com.example.wikipediaclient.data.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import com.example.wikipediaclient.data.local.WikiDatabaseHelper;
import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared SQL for bookmarks and history. Table names are fixed, never supplied by the UI. */
final class LocalArticleStore {
    private static final String KEY = "language = ? AND page_id = ?";
    private final WikiDatabaseHelper helper;
    private final String table;
    private final String timeColumn;
    private final boolean history;

    /** Selects one of the two tables while sharing the same database helper. */
    LocalArticleStore(WikiDatabaseHelper helper, boolean history) {
        this.helper = Objects.requireNonNull(helper, "helper");
        this.history = history;
        table = history ? "history" : "bookmarks";
        timeColumn = history ? "last_read_at" : "saved_at";
    }

    /** Reads detached records, newest first, with a stable order for equal timestamps. */
    List<PersonalArticleEntry> list() {
        requireWorker();
        List<PersonalArticleEntry> result = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query(table,
                new String[]{"language", "page_id", "title", "canonical_url", timeColumn},
                null, null, null, null, timeColumn + " DESC, page_id DESC")) {
            while (cursor.moveToNext()) {
                PersonalArticle article = new PersonalArticle(cursor.getString(0), cursor.getLong(1),
                        cursor.getString(2), cursor.getString(3));
                result.add(new PersonalArticleEntry(article, cursor.getLong(4)));
            }
        }
        return result;
    }

    /** Checks the canonical key without loading the full list. */
    boolean contains(String language, long pageId) {
        requireWorker();
        try (Cursor cursor = helper.getReadableDatabase().query(table, new String[]{"page_id"},
                KEY, keyArgs(language, pageId), null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    /** Atomically inserts or updates one record without replacing its original bookmark time. */
    void save(PersonalArticle article, long timestamp) {
        requireWorker();
        new PersonalArticleEntry(article, timestamp);
        String[] args = keyArgs(article.getLanguage(), article.getPageId());
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            Long previous = null;
            try (Cursor cursor = db.query(table, new String[]{timeColumn}, KEY, args, null, null, null)) {
                if (cursor.moveToFirst()) previous = cursor.getLong(0);
            }
            // An older reader completion must not move history backwards or replace newer metadata.
            if (previous == null || !history || timestamp >= previous) {
                ContentValues values = new ContentValues();
                values.put("title", article.getTitle());
                values.put("canonical_url", article.getCanonicalUrl());
                values.put(timeColumn, previous != null && !history ? previous : timestamp);
                if (previous == null) {
                    values.put("language", article.getLanguage());
                    values.put("page_id", article.getPageId());
                    db.insertOrThrow(table, null, values);
                } else {
                    db.update(table, values, KEY, args);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Deletes only the requested key; deleting an absent record is harmless. */
    void remove(String language, long pageId) {
        requireWorker();
        helper.getWritableDatabase().delete(table, KEY, keyArgs(language, pageId));
    }

    /** Clears this table without touching the other collection. */
    void clear() {
        requireWorker();
        helper.getWritableDatabase().delete(table, null, null);
    }

    /** Validates the identity and binds values separately from SQL. */
    private static String[] keyArgs(String language, long pageId) {
        if (!"vi".equals(language) || pageId <= 0) throw new IllegalArgumentException("Invalid article key.");
        return new String[]{language, Long.toString(pageId)};
    }

    /** Prevents disk access from blocking Android's UI thread. */
    private static void requireWorker() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Database operations must use the disk executor.");
        }
    }
}
