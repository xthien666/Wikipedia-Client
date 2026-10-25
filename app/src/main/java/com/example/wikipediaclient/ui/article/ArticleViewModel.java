package com.example.wikipediaclient.ui.article;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import androidx.annotation.NonNull;
import androidx.savedstate.SavedStateRegistryOwner;
import android.os.Bundle;
import com.example.wikipediaclient.data.AppContainer;
import com.example.wikipediaclient.data.model.ArticleContent;
import com.example.wikipediaclient.data.remote.*;
import com.example.wikipediaclient.data.repository.*;
import java.util.concurrent.Executor;

/** Owns content and canonical identity together. No View, Activity or HTML in saved state. */
public final class ArticleViewModel extends ViewModel {
    public static final class State {
        public final ArticleContent content;
        public final WikiApiException error;
        public final boolean loading, bookmarked, bookmarkReady, bookmarkError;
        State(ArticleViewModel model) {
            content = model.content; error = model.error; loading = model.loading;
            bookmarked = model.bookmarked; bookmarkReady = model.bookmarkReady; bookmarkError = model.bookmarkError;
        }
    }
    private final ArticleRepository articles;
    private final BookmarkRepository bookmarks;
    private final HistoryRepository history;
    private final Executor disk;
    private final SavedStateHandle saved;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private ArticleContent content;
    private WikiApiException error;
    private boolean loading, bookmarked, bookmarkReady, bookmarkError, bookmarkPending;
    private volatile boolean cleared;
    private int generation, bookmarkGeneration;
    private Cancelable request;
    private Boolean failedBookmark;
    public ArticleViewModel(ArticleRepository articles, BookmarkRepository bookmarks,
            HistoryRepository history, Executor disk, SavedStateHandle saved) {
        this.articles = articles; this.bookmarks = bookmarks; this.history = history;
        this.disk = disk; this.saved = saved;
    }
    public LiveData<State> state() { return state; }
    public void start() { if (state.getValue() == null) retry(); }
    public void retry() {
        int token = ++generation;
        if (request != null) request.cancel();
        content = null; error = null; loading = true; bookmarkReady = false;
        bookmarkGeneration++; bookmarkPending = false; bookmarkError = false; failedBookmark = null;
        publish();
        String title = saved.get("title");
        request = articles.getContent(title, new ApiCallback<ArticleContent>() {
            @Override public void onSuccess(ArticleContent value) {
                if (cleared || generation != token) return;
                content = value;
                saved.set("title", value.article.getTitle());
                saved.set("pageId", value.article.getPageId());
                loading = false; publish(); refreshBookmark();
            }
            @Override public void onError(WikiApiException value) {
                if (cleared || generation != token) return;
                error = value; loading = false; publish();
            }
        });
    }
    public void refreshBookmark() {
        // Lifecycle refresh must not replace an in-flight write or discard its retry intent.
        if (bookmarkPending || bookmarkError) return;
        bookmarkReady = false; bookmark(null);
    }
    public void toggleBookmark() { if (content != null && bookmarkReady) bookmark(!bookmarked); }
    public void retryBookmark() { bookmark(failedBookmark); }
    private void bookmark(Boolean desired) {
        if (content == null) return;
        ArticleContent selected = content;
        int token = ++bookmarkGeneration;
        bookmarkError = false; bookmarkPending = true;
        if (desired != null) bookmarked = desired;
        publish();
        disk.execute(() -> {
            try {
                String language = selected.article.getLanguage(); long id = selected.article.getPageId();
                if (desired != null) {
                    if (desired) bookmarks.saveBookmark(selected.article, System.currentTimeMillis());
                    else bookmarks.removeBookmark(language, id);
                }
                boolean actual = bookmarks.isBookmarked(language, id);
                // Refresh metadata for an already saved page after a redirect or rename.
                if (desired == null && actual) bookmarks.saveBookmark(selected.article, System.currentTimeMillis());
                main.post(() -> {
                    if (cleared || token != bookmarkGeneration || content != selected) return;
                    bookmarkPending = false; bookmarked = actual; bookmarkReady = true; failedBookmark = null; publish();
                });
            } catch (RuntimeException exception) {
                main.post(() -> {
                    if (cleared || token != bookmarkGeneration || content != selected) return;
                    bookmarkPending = false; bookmarkError = true; bookmarkReady = false; failedBookmark = desired; publish();
                });
            }
        });
    }
    /** Called only after the WebView has committed the content, never on HTTP success alone. */
    public void displayed(ArticleContent displayed) {
        if (!cleared && content == displayed) history.recordRead(displayed.article, System.currentTimeMillis());
    }
    public float progress() { Float value = saved.get("progress"); return value == null ? 0 : value; }
    public void progress(float value) { saved.set("progress", Math.max(0, Math.min(1, value))); }
    private void publish() { state.setValue(new State(this)); }
    @Override protected void onCleared() { cleared = true; generation++; if (request != null) request.cancel(); }
    public static ViewModelProvider.Factory factory(SavedStateRegistryOwner owner, Bundle args, AppContainer container) {
        return new AbstractSavedStateViewModelFactory(owner, args) {
            @NonNull @Override protected <T extends ViewModel> T create(@NonNull String key,
                    @NonNull Class<T> type, @NonNull SavedStateHandle handle) {
                return type.cast(new ArticleViewModel(container.getArticleRepository(),
                        container.getBookmarkRepository(), container.getHistoryRepository(), container.getDiskExecutor(), handle));
            }
        };
    }
}
