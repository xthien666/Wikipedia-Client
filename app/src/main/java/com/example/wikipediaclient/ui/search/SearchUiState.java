package com.example.wikipediaclient.ui.search;
import androidx.annotation.Nullable;
import com.example.wikipediaclient.data.model.SearchResult;
import com.example.wikipediaclient.data.remote.WikiApiException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable UI state: IDLE, LOADING, SUCCESS, EMPTY, ERROR. */
public final class SearchUiState {
    public enum Type { IDLE, LOADING, SUCCESS, EMPTY, ERROR }

    private final Type type;
    private final List<SearchResult> results;
    private final boolean hasMore;
    private final boolean loadMoreFailed;
    @Nullable private final WikiApiException.Kind errorKind;

    private SearchUiState(Type type, List<SearchResult> results, boolean hasMore,
                          boolean loadMoreFailed, @Nullable WikiApiException.Kind errorKind) {
        this.type = type;
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
        this.hasMore = hasMore;
        this.loadMoreFailed = loadMoreFailed;
        this.errorKind = errorKind;
    }

    public static SearchUiState idle() { return of(Type.IDLE); }
    public static SearchUiState loading() { return of(Type.LOADING); }
    public static SearchUiState empty() { return of(Type.EMPTY); }
    public static SearchUiState success(List<SearchResult> results, boolean hasMore, boolean loadMoreFailed) {
        return new SearchUiState(Type.SUCCESS, results, hasMore, loadMoreFailed, null);
    }
    public static SearchUiState error(WikiApiException.Kind kind) {
        return new SearchUiState(Type.ERROR, Collections.emptyList(), false, false, kind);
    }
    private static SearchUiState of(Type type) {
        return new SearchUiState(type, Collections.emptyList(), false, false, null);
    }

    public Type getType() { return type; }
    public List<SearchResult> getResults() { return results; }
    public boolean hasMore() { return hasMore; }
    public boolean isLoadMoreFailed() { return loadMoreFailed; }
    @Nullable public WikiApiException.Kind getErrorKind() { return errorKind; }
}
