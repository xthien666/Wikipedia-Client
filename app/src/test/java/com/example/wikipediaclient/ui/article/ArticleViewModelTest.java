package com.example.wikipediaclient.ui.article;
import static org.junit.Assert.*;
import android.os.Looper;
import androidx.lifecycle.*;
import com.example.wikipediaclient.data.model.*;
import com.example.wikipediaclient.data.remote.*;
import com.example.wikipediaclient.data.repository.*;
import java.util.*;
import java.util.concurrent.Executor;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ArticleViewModelTest {
    static class Articles implements ArticleRepository {
        final List<ApiCallback<ArticleContent>> calls = new ArrayList<>(); int cancelled;
        public Cancelable getContent(String title, ApiCallback<ArticleContent> callback) {
            calls.add(callback); return () -> cancelled++;
        }
        public Cancelable getSummary(String title, ApiCallback<ArticleSummary> callback) { return () -> {}; }
    }
    static class Bookmarks implements BookmarkRepository {
        boolean value, fail; PersonalArticle last;
        public List<PersonalArticleEntry> getBookmarks() { return Collections.emptyList(); }
        public boolean isBookmarked(String language, long id) { return value; }
        public void saveBookmark(PersonalArticle article, long now) {
            if (fail) throw new IllegalStateException(); last = article; value = true;
        }
        public void removeBookmark(String language, long id) {
            if (fail) throw new IllegalStateException(); value = false;
        }
    }
    static class History implements HistoryRepository {
        List<HistoryEntry> rows = new ArrayList<>();
        public LiveData<HistoryState> observe() { return new MutableLiveData<>(); }
        public void refresh() { }
        public void record(HistoryEntry entry) { rows.add(entry); }
        public void remove(String language, long id) { }
        public void clear() { }
    }
    private Articles articles; private Bookmarks bookmarks; private History history;
    private SavedStateHandle saved; private ArticleViewModel model;
    @Before public void setup() {
        articles = new Articles(); bookmarks = new Bookmarks(); history = new History();
        saved = new SavedStateHandle(Collections.singletonMap("title", "Alias"));
        model = new ArticleViewModel(articles, bookmarks, history, Runnable::run, saved);
    }
    private void idle() { Shadows.shadowOf(Looper.getMainLooper()).idle(); }
    private ArticleContent load() {
        model.start(); ArticleContent content = FixtureArticles.article("Đích đã resolve");
        articles.calls.get(0).onSuccess(content); idle(); return content;
    }
    @Test public void networkSuccessAloneNeverRecordsHistoryAndResolvedArticleOwnsBookmark() {
        ArticleContent content = load();
        assertTrue(history.rows.isEmpty());
        assertSame(content, model.state().getValue().content);
        model.toggleBookmark(); idle();
        assertSame(content.article, bookmarks.last);
        model.displayed(content);
        assertEquals(content.article.getPageId(), history.rows.get(0).getPageId());
        assertEquals(content.article.getCanonicalUrl(), history.rows.get(0).getCanonicalUrl());
    }
    @Test public void retryCancelsOldRequestAndIgnoresItsLateSuccessOrError() {
        model.start(); model.retry();
        assertEquals(1, articles.cancelled);
        articles.calls.get(0).onSuccess(FixtureArticles.article("Cũ"));
        assertNull(model.state().getValue().content);
        ArticleContent current = FixtureArticles.article("Mới");
        articles.calls.get(1).onSuccess(current); idle();
        articles.calls.get(0).onError(new WikiApiException(WikiApiException.Kind.TIMEOUT, "old", null));
        assertSame(current, model.state().getValue().content);
        assertNull(model.state().getValue().error);
    }
    @Test public void startOnRotationReusesContentInsteadOfReloading() {
        ArticleContent content = load(); model.start();
        assertEquals(1, articles.calls.size()); assertSame(content, model.state().getValue().content);
    }
    @Test public void failedBookmarkWriteHasExplicitRetryOfTheSameAction() {
        load(); bookmarks.fail = true; model.toggleBookmark(); idle();
        assertTrue(model.state().getValue().bookmarkError);
        assertFalse(model.state().getValue().bookmarkReady);
        bookmarks.fail = false; model.retryBookmark(); idle();
        assertTrue(bookmarks.value); assertFalse(model.state().getValue().bookmarkError);
        model.toggleBookmark(); idle(); assertFalse(bookmarks.value);
    }
    @Test public void rapidTogglesAreSerializedAndIgnoreEarlierCallbacks() {
        List<Runnable> tasks = new ArrayList<>();
        model = new ArticleViewModel(articles, bookmarks, history, tasks::add, saved);
        load(); tasks.remove(0).run(); idle();
        model.toggleBookmark(); model.toggleBookmark(); model.toggleBookmark();
        for (Runnable task : tasks) task.run(); idle();
        assertTrue(bookmarks.value); assertTrue(model.state().getValue().bookmarked);
    }
    @Test public void progressLivesInSmallSavedStateAndNewModelCanReloadAfterProcessDeath() {
        load(); model.progress(.42f);
        ArticleViewModel restored = new ArticleViewModel(articles, bookmarks, history, Runnable::run, saved);
        assertEquals(.42f, restored.progress(), .0001f);
        restored.start(); assertEquals(2, articles.calls.size());
        assertFalse(saved.keys().contains("html"));
        articles.calls.get(1).onError(new WikiApiException(WikiApiException.Kind.NO_CONNECTION, "offline", null));
        assertNull(restored.state().getValue().content);
        assertEquals(WikiApiException.Kind.NO_CONNECTION, restored.state().getValue().error.getKind());
    }
    @Test public void clearedModelRejectsNetworkAndQueuedDiskCallbacks() {
        model.start(); model.onCleared();
        articles.calls.get(0).onSuccess(FixtureArticles.article("Late"));
        assertNull(model.state().getValue().content); assertEquals(1, articles.cancelled);
    }
}
