package com.example.wikipediaclient.ui.search;

import static org.junit.Assert.*;
import androidx.lifecycle.SavedStateHandle;
import com.example.wikipediaclient.data.model.SearchPage;
import com.example.wikipediaclient.data.model.SearchResult;
import com.example.wikipediaclient.data.remote.ApiCallback;
import com.example.wikipediaclient.data.remote.Cancelable;
import com.example.wikipediaclient.data.remote.WikiApiException;
import com.example.wikipediaclient.data.repository.SearchRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// Robolectric is used only for LiveData's main-thread check; no Android UI is involved.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SearchViewModelTest {
    private static final class Call {
        final String query; final int offset; final ApiCallback<SearchPage> callback; boolean cancelled;
        Call(String query, int offset, ApiCallback<SearchPage> callback) {
            this.query = query; this.offset = offset; this.callback = callback;
        }
    }

    private static final class FakeRepository implements SearchRepository {
        final List<Call> calls = new ArrayList<>();
        @Override public Cancelable search(String query, int offset, ApiCallback<SearchPage> callback) {
            Call call = new Call(query, offset, callback);
            calls.add(call);
            return () -> call.cancelled = true;
        }
        Call last() { return calls.get(calls.size() - 1); }
    }

    private static final class FakeExecutor implements DelayedExecutor {
        Runnable pending;
        @Override public void postDelayed(Runnable task, long delayMs) { pending = task; }
        @Override public void cancel(Runnable task) { if (pending == task) pending = null; }
        void fire() { Runnable task = pending; pending = null; if (task != null) task.run(); }
    }

    private FakeRepository repository;
    private FakeExecutor executor;
    private SearchViewModel model;

    @Before public void setUp() {
        repository = new FakeRepository();
        executor = new FakeExecutor();
        model = new SearchViewModel(new SavedStateHandle(), repository, executor);
    }

    private static SearchPage page(Integer next, long... ids) {
        List<SearchResult> results = new ArrayList<>();
        for (long id : ids) results.add(new SearchResult(id, "Bài " + id, "Tóm tắt", null));
        return new SearchPage(results, next, results.size());
    }

    private SearchUiState.Type type() { return model.getState().getValue().getType(); }

    @Test public void startsIdleAndTypingWaitsForDebounce() {
        assertEquals(SearchUiState.Type.IDLE, type());
        model.setQuery("Hà");
        model.setQuery("Hà Nội");
        assertTrue("No request before the debounce fires", repository.calls.isEmpty());
        executor.fire();
        assertEquals(1, repository.calls.size());
        assertEquals("Hà Nội", repository.last().query);
        assertEquals(SearchUiState.Type.LOADING, type());
    }

    @Test public void successEmptyAndErrorStates() {
        model.setQuery("a"); executor.fire();
        repository.last().callback.onSuccess(page(null, 1, 2));
        assertEquals(SearchUiState.Type.SUCCESS, type());
        assertEquals(2, model.getState().getValue().getResults().size());
        assertFalse(model.getState().getValue().hasMore());

        model.setQuery("b"); executor.fire();
        repository.last().callback.onSuccess(page(null));
        assertEquals(SearchUiState.Type.EMPTY, type());

        model.setQuery("c"); executor.fire();
        repository.last().callback.onError(new WikiApiException(WikiApiException.Kind.TIMEOUT, "t", null));
        assertEquals(SearchUiState.Type.ERROR, type());
        assertEquals(WikiApiException.Kind.TIMEOUT, model.getState().getValue().getErrorKind());
    }

    @Test public void newQueryCancelsOldRequestAndLateResultIsIgnored() {
        model.setQuery("cũ"); executor.fire();
        Call old = repository.last();
        model.setQuery("mới");
        assertTrue(old.cancelled);
        executor.fire();
        Call current = repository.last();
        current.callback.onSuccess(page(null, 7));
        old.callback.onSuccess(page(null, 1, 2, 3)); // arrives late
        assertEquals(1, model.getState().getValue().getResults().size());
        assertEquals(7L, model.getState().getValue().getResults().get(0).getPageId());
    }

    @Test public void clearingQueryReturnsToIdleAndCancelsRequest() {
        model.setQuery("a"); executor.fire();
        Call call = repository.last();
        model.setQuery("");
        assertTrue(call.cancelled);
        assertEquals(SearchUiState.Type.IDLE, type());
        call.callback.onSuccess(page(null, 1));
        assertEquals(SearchUiState.Type.IDLE, type());
    }

    @Test public void submitSearchesImmediately() {
        model.setQuery("Hà Nội");
        model.submit();
        assertEquals(1, repository.calls.size());
        assertNull("Debounce task must be dropped", executor.pending);
    }

    @Test public void loadMoreUsesNextOffsetAndDoesNotDuplicateWhileLoading() {
        model.setQuery("a"); executor.fire();
        repository.last().callback.onSuccess(page(20, 1, 2));
        assertTrue(model.getState().getValue().hasMore());

        model.loadMore();
        model.loadMore();
        model.loadMore();
        assertEquals(2, repository.calls.size()); // 1 search + exactly 1 loadMore
        assertEquals(20, repository.last().offset);

        repository.last().callback.onSuccess(page(null, 3));
        assertEquals(3, model.getState().getValue().getResults().size());
        assertFalse(model.getState().getValue().hasMore());
        model.loadMore();
        assertEquals(2, repository.calls.size()); // no more pages
    }

    @Test public void loadMoreFailureKeepsResultsAndBlocksUntilRetry() {
        model.setQuery("a"); executor.fire();
        repository.last().callback.onSuccess(page(20, 1));
        model.loadMore();
        repository.last().callback.onError(new WikiApiException(WikiApiException.Kind.NO_CONNECTION, "x", null));
        SearchUiState ui = model.getState().getValue();
        assertEquals(SearchUiState.Type.SUCCESS, ui.getType());
        assertTrue(ui.isLoadMoreFailed());
        assertEquals(1, ui.getResults().size());

        model.loadMore();
        assertEquals(2, repository.calls.size()); // blocked
        model.retryLoadMore();
        assertEquals(3, repository.calls.size());
        assertEquals(20, repository.last().offset);
    }

    @Test public void restoredQueryTriggersSearchAfterProcessDeath() {
        SavedStateHandle saved = new SavedStateHandle(Collections.singletonMap("query", "Hà Nội"));
        FakeRepository repo = new FakeRepository();
        FakeExecutor exec = new FakeExecutor();
        SearchViewModel restored = new SearchViewModel(saved, repo, exec);
        assertEquals("Hà Nội", restored.getQuery());
        exec.fire();
        assertEquals("Hà Nội", repo.last().query);
    }
}
