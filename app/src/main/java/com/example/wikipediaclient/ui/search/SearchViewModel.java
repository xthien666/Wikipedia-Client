package com.example.wikipediaclient.ui.search;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.wikipediaclient.data.model.SearchPage;
import com.example.wikipediaclient.data.model.SearchResult;
import com.example.wikipediaclient.data.remote.ApiCallback;
import com.example.wikipediaclient.data.remote.Cancelable;
import com.example.wikipediaclient.data.remote.WikiApiException;
import com.example.wikipediaclient.data.repository.SearchRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SearchViewModel extends ViewModel {
    static final long DEBOUNCE_MS = 350;
    private static final String KEY_QUERY = "query";

    private final SavedStateHandle savedState;
    private final SearchRepository repository;
    private final DelayedExecutor executor;
    private final MutableLiveData<SearchUiState> state = new MutableLiveData<>(SearchUiState.idle());
    private final Runnable debounced = () -> startSearch(getQuery().trim());

    private Cancelable inFlight;
    /** Bumped on every new search/cancel so a late callback of an old request is ignored. */
    private int generation;
    private boolean loadingMore;
    private boolean loadMoreFailed;
    private String activeQuery = "";
    private final List<SearchResult> loaded = new ArrayList<>();
    private Integer nextOffset;

    public SearchViewModel(SavedStateHandle savedState, SearchRepository repository, DelayedExecutor executor) {
        this.savedState = savedState;
        this.repository = repository;
        this.executor = executor;
        // Restored after process death: the query survives but results do not.
        if (!getQuery().trim().isEmpty()) executor.postDelayed(debounced, 0);
    }

    public LiveData<SearchUiState> getState() { return state; }

    public String getQuery() {
        String value = savedState.get(KEY_QUERY);
        return value == null ? "" : value;
    }

    /** Called on every keystroke; the request fires after the user pauses typing. */
    public void setQuery(String query) {
        if (query.equals(getQuery())) return;
        savedState.set(KEY_QUERY, query);
        executor.cancel(debounced);
        cancelInFlight();
        if (query.trim().isEmpty()) {
            state.setValue(SearchUiState.idle());
        } else {
            state.setValue(SearchUiState.loading());
            executor.postDelayed(debounced, DEBOUNCE_MS);
        }
    }

    /** Keyboard "search" action: skip the debounce. */
    public void submit() {
        executor.cancel(debounced);
        String query = getQuery().trim();
        if (!query.isEmpty()) startSearch(query);
    }

    public void retry() { submit(); }

    public void loadMore() {
        SearchUiState current = state.getValue();
        if (current == null || current.getType() != SearchUiState.Type.SUCCESS
                || nextOffset == null || loadingMore || loadMoreFailed) return;
        loadingMore = true;
        final int token = generation;
        final int requestedOffset = nextOffset;
        state.setValue(SearchUiState.success(loaded, true, false, true));
        inFlight = repository.search(activeQuery, nextOffset, new ApiCallback<SearchPage>() {
            @Override public void onSuccess(SearchPage page) {
                if (token != generation) return;
                loadingMore = false;
                appendUnique(page.getResults());
                Integer offset = page.getNextOffset();
                nextOffset = offset != null && offset > requestedOffset ? offset : null;
                state.setValue(SearchUiState.success(loaded, nextOffset != null, false));
            }
            @Override public void onError(WikiApiException error) {
                if (token != generation) return;
                loadingMore = false;
                loadMoreFailed = true;
                state.setValue(SearchUiState.success(loaded, true, true));
            }
        });
    }

    public void retryLoadMore() {
        if (!loadMoreFailed) return;
        loadMoreFailed = false;
        state.setValue(SearchUiState.success(loaded, nextOffset != null, false));
        loadMore();
    }

    private void startSearch(String query) {
        cancelInFlight();
        final int token = generation;
        activeQuery = query;
        state.setValue(SearchUiState.loading());
        inFlight = repository.search(query, 0, new ApiCallback<SearchPage>() {
            @Override public void onSuccess(SearchPage page) {
                if (token != generation) return;
                loaded.clear();
                appendUnique(page.getResults());
                Integer offset = page.getNextOffset();
                nextOffset = offset != null && offset > 0 ? offset : null;
                state.setValue(loaded.isEmpty() ? SearchUiState.empty()
                        : SearchUiState.success(loaded, nextOffset != null, false));
            }
            @Override public void onError(WikiApiException error) {
                if (token != generation) return;
                state.setValue(SearchUiState.error(error.getKind()));
            }
        });
    }

    private void cancelInFlight() {
        generation++;
        loadingMore = false;
        loadMoreFailed = false;
        loaded.clear();
        nextOffset = null;
        if (inFlight != null) {
            inFlight.cancel();
            inFlight = null;
        }
    }

    private void appendUnique(List<SearchResult> results) {
        Map<Long, SearchResult> unique = new LinkedHashMap<>();
        for (SearchResult result : loaded) unique.put(result.getPageId(), result);
        for (SearchResult result : results) unique.put(result.getPageId(), result);
        loaded.clear();
        loaded.addAll(unique.values());
    }

    @Override
    protected void onCleared() {
        executor.cancel(debounced);
        generation++;
        if (inFlight != null) inFlight.cancel();
    }
}
