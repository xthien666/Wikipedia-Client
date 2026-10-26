package com.example.wikipediaclient.data.repository;

import static org.junit.Assert.*;
import android.content.Context;
import com.example.wikipediaclient.data.local.WikiDatabaseHelper;
import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 28})
public class LocalRepositoriesTest {
    private Context context;
    private WikiDatabaseHelper helper;
    private BookmarkRepository bookmarks;
    private SqliteHistoryStore history;
    private ExecutorService executor;
    private final PersonalArticle a = article(1, "Hà Nội");
    private final PersonalArticle b = article(2, "Kinh Bắc");

    /** Creates canonical test metadata, including Unicode titles. */
    private static PersonalArticle article(long id, String title) {
        return new PersonalArticle("vi", id, title, "https://vi.wikipedia.org/wiki/" + title);
    }
    /** Creates an isolated database and the same serial executor used by the app. */
    @Before public void setup() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("personal_articles.db");
        executor = Executors.newSingleThreadExecutor();
        reopen();
    }
    /** Recreates services without deleting the database file. */
    private void reopen() {
        helper = new WikiDatabaseHelper(context);
        bookmarks = new LocalBookmarkRepository(helper);
        history = new SqliteHistoryStore(helper);
    }
    /** Waits for a disk operation and propagates assertion failures to JUnit. */
    private void onDisk(Runnable check) throws Exception { executor.submit(check).get(20, TimeUnit.SECONDS); }
    /** Releases resources even after a failing assertion. */
    @After public void cleanup() throws Exception {
        try { onDisk(() -> helper.close()); }
        finally { executor.shutdownNow(); context.deleteDatabase("personal_articles.db"); }
    }
    /** Repeated canonical IDs update metadata without duplicating or changing savedAt. */
    @Test public void bookmarksDeduplicateAndRefreshMetadata() throws Exception {
        onDisk(() -> {
            bookmarks.saveBookmark(a, 100);
            bookmarks.saveBookmark(article(1, "Hà Nội ' renamed"), 300);
            bookmarks.saveBookmark(b, 200);
            List<PersonalArticleEntry> rows = bookmarks.getBookmarks();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).getArticle().getPageId());
            assertEquals("Hà Nội ' renamed", rows.get(1).getArticle().getTitle());
            assertEquals(100, rows.get(1).getTimestamp());
            rows.clear();
            assertEquals(2, bookmarks.getBookmarks().size());
        });
    }
    /** A-B-A reads put A first; an older completion cannot replace newer metadata. */
    @Test public void repeatedReadsMoveToTopWithoutDuplicates() throws Exception {
        onDisk(() -> {
            history.recordRead(a, 100); history.recordRead(b, 200);
            history.recordRead(article(1, "Updated title"), 300); history.recordRead(a, 50);
            List<PersonalArticleEntry> rows = history.getHistory();
            assertEquals(2, rows.size());
            assertEquals(1, rows.get(0).getArticle().getPageId());
            assertEquals(300, rows.get(0).getTimestamp());
            assertEquals("Updated title", rows.get(0).getArticle().getTitle());
        });
    }
    /** Both collections survive closing and reopening all repository instances. */
    @Test public void dataSurvivesReopen() throws Exception {
        onDisk(() -> {
            bookmarks.saveBookmark(a, 100); history.recordRead(b, 200);
            helper.close(); reopen();
            assertTrue(bookmarks.isBookmarked("vi", 1));
            assertEquals(a.getCanonicalUrl(), bookmarks.getBookmarks().get(0).getArticle().getCanonicalUrl());
            assertEquals(200, history.getHistory().get(0).getTimestamp());
        });
    }
    /** Deleting one collection never deletes the other; missing IDs are harmless. */
    @Test public void deletionsAreIndependent() throws Exception {
        onDisk(() -> {
            bookmarks.saveBookmark(a, 100); history.recordRead(a, 100); history.recordRead(b, 200);
            history.removeHistory("vi", 1); history.removeHistory("vi", 1);
            assertEquals(1, history.getHistory().size());
            history.clearHistory();
            assertTrue(history.getHistory().isEmpty()); assertTrue(bookmarks.isBookmarked("vi", 1));
            history.recordRead(a, 300);
            bookmarks.removeBookmark("vi", 1); bookmarks.removeBookmark("vi", 1);
            assertFalse(bookmarks.isBookmarked("vi", 1)); assertEquals(1, history.getHistory().size());
        });
    }
    /** Equal timestamps have a deterministic order across both repositories. */
    @Test public void equalTimestampsHaveStableOrder() throws Exception {
        onDisk(() -> {
            bookmarks.saveBookmark(a, 100); bookmarks.saveBookmark(b, 100);
            history.recordRead(a, 100); history.recordRead(b, 100);
            assertEquals(2, bookmarks.getBookmarks().get(0).getArticle().getPageId());
            assertEquals(2, history.getHistory().get(0).getArticle().getPageId());
        });
    }
    /** Invalid input is rejected without partially modifying stored rows. */
    @Test public void invalidWriteLeavesDataUntouched() throws Exception {
        onDisk(() -> {
            bookmarks.saveBookmark(a, 100);
            assertThrows(IllegalArgumentException.class, () -> bookmarks.saveBookmark(b, -1));
            assertThrows(IllegalArgumentException.class, () -> bookmarks.removeBookmark("vi", 0));
            assertThrows(IllegalArgumentException.class, () -> history.recordRead(a, -1));
            assertEquals(1, bookmarks.getBookmarks().size()); assertTrue(history.getHistory().isEmpty());
        });
    }
    /** Production repositories refuse to block the UI thread. */
    @Test public void mainThreadAccessIsRejected() {
        assertThrows(IllegalStateException.class, () -> bookmarks.getBookmarks());
        assertThrows(IllegalStateException.class, () -> history.recordRead(a, 100));
    }
    /** A rapid sequence of explicit save/remove actions preserves the final intent. */
    @Test public void rapidRepeatedActionsKeepFinalState() throws Exception {
        for (int i = 0; i < 30; i++) {
            executor.submit(() -> bookmarks.saveBookmark(a, 100));
            executor.submit(() -> bookmarks.removeBookmark("vi", 1));
        }
        onDisk(() -> {
            bookmarks.saveBookmark(a, 200);
            helper.close();
            reopen();
            assertEquals(1, bookmarks.getBookmarks().size());
            assertEquals(200, bookmarks.getBookmarks().get(0).getTimestamp());
        });
    }
}
