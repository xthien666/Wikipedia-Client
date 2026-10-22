package com.example.wikipediaclient;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.Looper;
import com.example.wikipediaclient.data.local.LocalHistoryRepository;
import com.example.wikipediaclient.data.local.WikiDatabaseHelper;
import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import com.example.wikipediaclient.data.repository.LocalBookmarkRepository;
import com.example.wikipediaclient.data.repository.SqliteHistoryStore;
import com.example.wikipediaclient.data.repository.HistoryState;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class PersonalStorageTest {
    private Context context;
    private WikiDatabaseHelper database;
    private ExecutorService disk;

    @Before public void setup() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("personal_articles.db");
        context.getSharedPreferences("reading_history", Context.MODE_PRIVATE).edit().clear().commit();
        database = new WikiDatabaseHelper(context);
        disk = Executors.newSingleThreadExecutor();
    }

    private <T> T worker(Callable<T> action) throws Exception {
        return disk.submit(action).get(10, TimeUnit.SECONDS);
    }

    @After public void cleanup() throws Exception {
        worker(() -> { database.close(); return null; });
        disk.shutdown();
        assertTrue(disk.awaitTermination(10, TimeUnit.SECONDS));
    }

    private PersonalArticle article(long id, String title) {
        return new PersonalArticle("vi", id, title, "https://vi.wikipedia.org/wiki/" + title);
    }

    @Test public void bookmarksKeepOriginalTimeAndHistoryClearDoesNotDeleteThem() throws Exception {
        worker(() -> {
            LocalBookmarkRepository bookmarks = new LocalBookmarkRepository(database);
            SqliteHistoryStore history = new SqliteHistoryStore(database);
            bookmarks.saveBookmark(article(1, "A"), 100);
            bookmarks.saveBookmark(article(2, "B"), 200);
            bookmarks.saveBookmark(article(1, "A_updated"), 300);
            List<PersonalArticleEntry> rows = bookmarks.getBookmarks();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).getArticle().getPageId());
            assertEquals(100, rows.get(1).getTimestamp());
            assertEquals("A_updated", rows.get(1).getArticle().getTitle());
            history.recordRead(article(1, "A"), 400);
            history.recordRead(article(1, "Stale_A"), 300);
            assertEquals("A", history.getHistory().get(0).getArticle().getTitle());
            history.clearHistory();
            assertTrue(history.getHistory().isEmpty());
            assertEquals(2, bookmarks.getBookmarks().size());
            database.close();
            WikiDatabaseHelper reopened = new WikiDatabaseHelper(context);
            try {
                LocalBookmarkRepository persisted = new LocalBookmarkRepository(reopened);
                assertTrue(persisted.isBookmarked("vi", 1));
                persisted.removeBookmark("vi", 1);
                persisted.removeBookmark("vi", 1);
                assertEquals(1, persisted.getBookmarks().size());
            } finally { reopened.close(); }
            return null;
        });
    }

    @Test public void legacyHistoryMigratesOnceWithoutReplacingNewerSqliteMetadata() throws Exception {
        worker(() -> {
            new SqliteHistoryStore(database).recordRead(article(1, "New_A"), 300);
            return null;
        });
        String old = "[{\"language\":\"vi\",\"pageId\":1,\"title\":\"Old_A\","
                + "\"canonicalUrl\":\"https://vi.wikipedia.org/wiki/Old_A\",\"lastReadAt\":100},"
                + "{\"language\":\"vi\",\"pageId\":2,\"title\":\"B\","
                + "\"canonicalUrl\":\"https://vi.wikipedia.org/wiki/B\",\"lastReadAt\":200}]";
        context.getSharedPreferences("reading_history", Context.MODE_PRIVATE).edit().putString("entries", old).commit();
        LocalHistoryRepository history = new LocalHistoryRepository(context, disk, database);
        worker(() -> null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(HistoryState.Kind.READY, history.observe().getValue().getKind());
        assertEquals(2, history.observe().getValue().getEntries().size());
        assertEquals("New_A", history.observe().getValue().getEntries().get(0).getTitle());
        assertFalse(context.getSharedPreferences("reading_history", Context.MODE_PRIVATE).contains("entries"));
        history.remove("vi", 2);
        history.refresh();
        worker(() -> null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, history.observe().getValue().getEntries().size());
    }

    @Test public void invalidLegacyBatchDoesNotPartiallyImport() throws Exception {
        String old = "[{\"language\":\"vi\",\"pageId\":1,\"title\":\"A\","
                + "\"canonicalUrl\":\"https://vi.wikipedia.org/wiki/A\",\"lastReadAt\":100},{}]";
        context.getSharedPreferences("reading_history", Context.MODE_PRIVATE).edit().putString("entries", old).commit();
        LocalHistoryRepository history = new LocalHistoryRepository(context, disk, database);
        worker(() -> null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(HistoryState.Failure.LOAD, history.observe().getValue().getFailure());
        assertTrue(worker(() -> new SqliteHistoryStore(database).getHistory().isEmpty()));
        assertEquals(old, context.getSharedPreferences("reading_history", Context.MODE_PRIVATE).getString("entries", null));
    }

    @Test(expected = IllegalStateException.class)
    public void databaseRejectsMainThreadCalls() {
        new LocalBookmarkRepository(database).getBookmarks();
    }
}
