package com.example.wikipediaclient.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WikiDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "personal_articles.db";
    private static final int DATABASE_VERSION = 1;

    /** Uses the application context so the helper never retains an Activity. */
    public WikiDatabaseHelper(Context context) {
        super(context.getApplicationContext(),
                DATABASE_NAME, null, DATABASE_VERSION);
    }

    /** Creates the two independent tables on the first database open. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createBookmarks = "CREATE TABLE bookmarks ("
            + "language TEXT NOT NULL, "
            + "page_id INTEGER NOT NULL, "
            + "title TEXT NOT NULL, "
            + "canonical_url TEXT NOT NULL, "
            + "saved_at INTEGER NOT NULL, "
            + "PRIMARY KEY (language, page_id)"
            + ")";
        db.execSQL(createBookmarks);

        String createHistory = "CREATE TABLE history ("
            + "language TEXT NOT NULL, "
            + "page_id INTEGER NOT NULL, "
            + "title TEXT NOT NULL, "
            + "canonical_url TEXT NOT NULL, "
            + "last_read_at INTEGER NOT NULL, "
            + "PRIMARY KEY (language, page_id)"
            + ")";
        db.execSQL(createHistory);
    }

    /** Requires an explicit migration rather than dropping saved articles. */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException(
                "Missing database migration from "
                        + oldVersion + " to " + newVersion
        );
    }
}