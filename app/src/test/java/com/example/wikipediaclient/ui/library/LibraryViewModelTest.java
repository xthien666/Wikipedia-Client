package com.example.wikipediaclient.ui.library;

import static org.junit.Assert.*;
import android.os.Looper;
import androidx.lifecycle.ViewModelStore;
import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import com.example.wikipediaclient.data.repository.BookmarkRepository;
import com.example.wikipediaclient.data.repository.fake.FakeBookmarkRepository;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LibraryViewModelTest {
    private final PersonalArticle article = new PersonalArticle("vi", 1, "Article", "https://vi.wikipedia.org/wiki/Article");

    /** Delivers posted LiveData updates on Robolectric's main thread. */
    private void idle() { Shadows.shadowOf(Looper.getMainLooper()).idle(); }

    /** A failed deletion stays retryable and is applied only after the retry succeeds. */
    @Test public void failedDeleteCanBeRetried() {
        FailingRepository repository = new FailingRepository();
        repository.saveBookmark(article, 100);
        LibraryViewModel model = new LibraryViewModel(repository, Runnable::run);
        repository.failDelete = true;
        model.remove(article); idle();
        assertTrue(model.getState().getValue().error);
        assertTrue(repository.isBookmarked("vi", 1));
        repository.failDelete = false;
        model.retry(); idle();
        assertFalse(model.getState().getValue().error);
        assertTrue(model.getState().getValue().entries.isEmpty());
    }

    /** Failed reads expose an error state rather than pretending the collection is empty. */
    @Test public void readFailureThenRetryLoadsRows() {
        FailingRepository repository = new FailingRepository();
        repository.saveBookmark(article, 100);
        repository.failRead = true;
        LibraryViewModel model = new LibraryViewModel(repository, Runnable::run);
        model.refresh(); idle();
        assertTrue(model.getState().getValue().error);
        repository.failRead = false;
        model.retry(); idle();
        assertEquals(1, model.getState().getValue().entries.size());
        assertFalse(model.getState().getValue().loading);
    }

    /** A late old result cannot restore an article after a newer delete request. */
    @Test public void supersededRefreshDoesNotPublishOldRows() {
        FakeBookmarkRepository repository = new FakeBookmarkRepository();
        repository.saveBookmark(article, 100);
        Queue<Runnable> work = new ArrayDeque<>();
        LibraryViewModel model = new LibraryViewModel(repository, work::add);
        model.refresh();
        work.remove().run();
        model.remove(article);
        idle();
        assertTrue(model.getState().getValue().loading);
        work.remove().run(); idle();
        assertTrue(model.getState().getValue().entries.isEmpty());
        assertFalse(model.getState().getValue().loading);
    }

    /** Destroyed models ignore results without shutting down shared executors. */
    @Test public void clearedModelIgnoresLateResults() {
        FakeBookmarkRepository repository = new FakeBookmarkRepository();
        Queue<Runnable> work = new ArrayDeque<>();
        LibraryViewModel model = new LibraryViewModel(repository, work::add);
        ViewModelStore store = new ViewModelStore(); store.put("library", model);
        model.refresh(); store.clear(); work.remove().run(); idle();
        assertTrue(model.getState().getValue().loading);
    }

    /** Background/foreground refresh preserves the failed write and known rows. */
    @Test public void failedDeleteSurvivesRefreshUntilExplicitRetry() {
        FailingRepository repository = new FailingRepository();
        repository.saveBookmark(article, 100);
        LibraryViewModel model = new LibraryViewModel(repository, Runnable::run);
        model.refresh(); idle();
        repository.failDelete = true;
        model.remove(article); idle();
        assertEquals(1, model.getState().getValue().entries.size());
        repository.failDelete = false;
        model.refresh(); idle();
        assertTrue(model.getState().getValue().error);
        assertTrue(repository.isBookmarked("vi", 1));
        model.retry(); idle();
        assertFalse(repository.isBookmarked("vi", 1));
        assertTrue(model.getState().getValue().entries.isEmpty());
    }

    /** Refresh cannot invalidate a write failure whose main-thread callback is still queued. */
    @Test public void refreshBeforeFailureCallbackKeepsDeletionRetryable() {
        FailingRepository repository = new FailingRepository();
        repository.saveBookmark(article, 100);
        repository.failDelete = true;
        Queue<Runnable> work = new ArrayDeque<>();
        LibraryViewModel model = new LibraryViewModel(repository, work::add);
        model.remove(article);
        work.remove().run();
        model.refresh();
        idle();
        assertTrue(work.isEmpty());
        assertTrue(model.getState().getValue().error);
        repository.failDelete = false;
        model.retry(); work.remove().run(); idle();
        assertFalse(repository.isBookmarked("vi", 1));
    }

    /** A new deletion replaces the previous retry target without reviving its late error. */
    @Test public void newDeletionSupersedesOldFailure() {
        FailingRepository repository = new FailingRepository();
        PersonalArticle other = new PersonalArticle("vi", 2, "Other", "https://vi.wikipedia.org/wiki/Other");
        repository.saveBookmark(article, 100);
        repository.saveBookmark(other, 200);
        Queue<Runnable> work = new ArrayDeque<>();
        LibraryViewModel model = new LibraryViewModel(repository, work::add);
        repository.failDelete = true;
        model.remove(article); work.remove().run();
        model.remove(other);
        idle();
        assertTrue(model.getState().getValue().loading);
        repository.failDelete = false;
        work.remove().run(); idle();
        model.retry(); idle();
        assertTrue(work.isEmpty());
        assertTrue(repository.isBookmarked("vi", 1));
        assertFalse(repository.isBookmarked("vi", 2));
    }

    /** A read error after a successful write must not delete a subsequently re-saved article. */
    @Test public void retryAfterSuccessfulDeleteOnlyReloads() {
        FailingRepository repository = new FailingRepository();
        repository.saveBookmark(article, 100);
        LibraryViewModel model = new LibraryViewModel(repository, Runnable::run);
        repository.failRead = true;
        model.remove(article); idle();
        assertTrue(model.getState().getValue().error);
        repository.saveBookmark(article, 200);
        repository.failRead = false;
        model.retry(); idle();
        assertTrue(repository.isBookmarked("vi", 1));
        assertEquals(1, model.getState().getValue().entries.size());
    }

    private static final class FailingRepository implements BookmarkRepository {
        private final FakeBookmarkRepository delegate = new FakeBookmarkRepository();
        boolean failRead;
        boolean failDelete;
        /** Simulates a disk read failure on demand. */
        @Override public List<PersonalArticleEntry> getBookmarks() {
            if (failRead) throw new IllegalStateException("Test read failure");
            return delegate.getBookmarks();
        }
        /** Reads the underlying fake state independently of failure injection. */
        @Override public boolean isBookmarked(String language, long pageId) { return delegate.isBookmarked(language, pageId); }
        /** Seeds test data through the normal contract. */
        @Override public void saveBookmark(PersonalArticle article, long time) { delegate.saveBookmark(article, time); }
        /** Simulates a failed write without removing any data. */
        @Override public void removeBookmark(String language, long pageId) {
            if (failDelete) throw new IllegalStateException("Test delete failure");
            delegate.removeBookmark(language, pageId);
        }
    }
}
