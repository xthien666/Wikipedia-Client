package com.example.wikipediaclient.ui.library;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import com.example.wikipediaclient.data.repository.BookmarkRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/** Loads persistent bookmarks without retaining an Activity or a View. */
public final class LibraryViewModel extends ViewModel {
    public static final class State {
        public final boolean loading;
        public final boolean error;
        public final List<PersonalArticleEntry> entries;
        /** Copies rows so observers cannot mutate repository or ViewModel state. */
        State(boolean loading, boolean error, List<PersonalArticleEntry> entries) {
            this.loading = loading;
            this.error = error;
            this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        }
    }

    private final BookmarkRepository repository;
    private final Executor diskExecutor;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<State> state = new MutableLiveData<>(
            new State(true, false, Collections.emptyList()));
    private Runnable retryAction;
    private int generation;
    private volatile boolean cleared;

    /** Receives shared dependencies; constructing the model does not access disk. */
    public LibraryViewModel(BookmarkRepository repository, Executor diskExecutor) {
        this.repository = repository;
        this.diskExecutor = diskExecutor;
    }

    /** Exposes read-only observable state to the Fragment. */
    public LiveData<State> getState() { return state; }

    /** Reloads on entering the screen, including after a bookmark changes in the reader. */
    public void refresh() { execute(() -> { }); }

    /** Removes one resolved bookmark, then reloads the remaining records. */
    public void remove(PersonalArticle article) {
        execute(() -> repository.removeBookmark(article.getLanguage(), article.getPageId()));
    }

    /** Repeats the failed read or deletion instead of silently discarding the user's action. */
    public void retry() {
        if (retryAction != null) execute(retryAction);
    }

    /** Runs disk work in order and ignores results superseded by a newer request. */
    private void execute(Runnable action) {
        int request = ++generation;
        retryAction = action;
        state.setValue(new State(true, false, Collections.emptyList()));
        diskExecutor.execute(() -> {
            try {
                action.run();
                List<PersonalArticleEntry> rows = repository.getBookmarks();
                main.post(() -> {
                    if (!cleared && request == generation) {
                        retryAction = null;
                        state.setValue(new State(false, false, rows));
                    }
                });
            } catch (RuntimeException exception) {
                Log.e("LibraryViewModel", "Bookmark operation failed", exception);
                main.post(() -> {
                    if (!cleared && request == generation) {
                        state.setValue(new State(false, true, Collections.emptyList()));
                    }
                });
            }
        });
    }

    /** Drops late callbacks while leaving the application-owned executor running. */
    @Override protected void onCleared() { cleared = true; }

    /** Creates models with the production repository, or a controlled repository in tests. */
    public static ViewModelProvider.Factory factory(BookmarkRepository repository, Executor executor) {
        return new ViewModelProvider.Factory() {
            /** Builds only the model this factory supports. */
            @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass != LibraryViewModel.class) throw new IllegalArgumentException("Unknown ViewModel.");
                return modelClass.cast(new LibraryViewModel(repository, executor));
            }
        };
    }
}
