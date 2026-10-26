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
    private final boolean loadingMore;
    @Nullable private final WikiApiException.Kind errorKind;
    private final boolean rateLimited;
    private final int retryAfterSeconds;

    private SearchUiState(Type type, List<SearchResult> results, boolean hasMore,
                          boolean loadMoreFailed, boolean loadingMore, @Nullable WikiApiException.Kind errorKind) {
        this(type, results, hasMore, loadMoreFailed, loadingMore, errorKind, false, -1);
    }

    private SearchUiState(Type type, List<SearchResult> results, boolean hasMore, boolean loadMoreFailed,
                          boolean loadingMore, @Nullable WikiApiException.Kind errorKind,
                          boolean rateLimited, int retryAfterSeconds) {
        this.rateLimited = rateLimited;
        this.retryAfterSeconds = retryAfterSeconds;
        this.type = type;
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
        this.hasMore = hasMore;
        this.loadMoreFailed = loadMoreFailed;
        this.loadingMore = loadingMore;
        this.errorKind = errorKind;
    }

    public static SearchUiState idle() { return of(Type.IDLE); }
    public static SearchUiState loading() { return of(Type.LOADING); }
    public static SearchUiState empty() { return of(Type.EMPTY); }
    public static SearchUiState success(List<SearchResult> results, boolean hasMore, boolean loadMoreFailed) {
        return success(results, hasMore, loadMoreFailed, false);
    }
    public static SearchUiState success(List<SearchResult> results, boolean hasMore, boolean loadMoreFailed,
                                        boolean loadingMore) {
        return new SearchUiState(Type.SUCCESS, results, hasMore, loadMoreFailed, loadingMore, null);
    }
    public static SearchUiState error(WikiApiException.Kind kind) {
        return new SearchUiState(Type.ERROR, Collections.emptyList(), false, false, false, kind);
    }
    /** Error that keeps the 429/503 detail so the screen can say how long to wait. */
    public static SearchUiState error(WikiApiException error) {
        long ms = error.getRetryAfterMs();
        int seconds = ms < 0 ? -1 : (int) Math.min(Integer.MAX_VALUE, ms / 1000 + (ms % 1000 == 0 ? 0 : 1));
        return new SearchUiState(Type.ERROR, Collections.emptyList(), false, false, false,
                error.getKind(), error.isRateLimited(), seconds);
    }
    private static SearchUiState of(Type type) {
        return new SearchUiState(type, Collections.emptyList(), false, false, false, null);
    }

    public Type getType() { return type; }
    public List<SearchResult> getResults() { return results; }
    public boolean hasMore() { return hasMore; }
    public boolean isLoadMoreFailed() { return loadMoreFailed; }
    public boolean isLoadingMore() { return loadingMore; }
    @Nullable public WikiApiException.Kind getErrorKind() { return errorKind; }
    /** The server answered 429/503 even after the automatic retries. */
    public boolean isRateLimited() { return rateLimited; }
    /** Seconds the server asked us to wait, or -1 when unknown. */
    public int getRetryAfterSeconds() { return retryAfterSeconds; }
}
