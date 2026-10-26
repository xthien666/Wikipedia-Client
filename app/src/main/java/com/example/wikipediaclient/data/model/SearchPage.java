package com.example.wikipediaclient.data.model;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/** One page of search results. {@code continuation} is null when there are no more pages. */
public final class SearchPage {
    private final List<SearchResult> results;
    @Nullable private final SearchContinuation continuation;
    private final int totalHits;
    public SearchPage(List<SearchResult> results, @Nullable SearchContinuation continuation, int totalHits) {
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
        this.continuation = continuation;
        this.totalHits = totalHits;
    }
    /** Offset-only form, kept for fakes and fixtures. */
    public SearchPage(List<SearchResult> results, @Nullable Integer nextOffset, int totalHits) {
        this(results, nextOffset == null ? null : SearchContinuation.ofOffset(nextOffset), totalHits);
    }
    public List<SearchResult> getResults() { return results; }
    @Nullable public SearchContinuation getContinuation() { return continuation; }
    @Nullable public Integer getNextOffset() { return continuation == null ? null : continuation.getOffset(); }
    public int getTotalHits() { return totalHits; }
}
