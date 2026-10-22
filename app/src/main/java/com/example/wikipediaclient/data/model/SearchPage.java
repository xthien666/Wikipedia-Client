package com.example.wikipediaclient.data.model;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/** One page of search results. {@code nextOffset} is null when there are no more pages. */
public final class SearchPage {
    private final List<SearchResult> results;
    @Nullable private final Integer nextOffset;
    private final int totalHits;
    public SearchPage(List<SearchResult> results, @Nullable Integer nextOffset, int totalHits) {
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
        this.nextOffset = nextOffset;
        this.totalHits = totalHits;
    }
    public List<SearchResult> getResults() { return results; }
    @Nullable public Integer getNextOffset() { return nextOffset; }
    public int getTotalHits() { return totalHits; }
}
