package com.example.wikipediaclient.data.model;
import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
/**
 * The Action API {@code continue} object, kept whole. MediaWiki requires every key in it
 * (e.g. {@code sroffset} and {@code continue}) to be sent back unchanged for the next page.
 */
public final class SearchContinuation {
    private final Map<String, String> params;
    public SearchContinuation(Map<String, String> params) {
        if (params == null || params.isEmpty()) throw new IllegalArgumentException("Continuation is empty.");
        this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }
    /** Continuation carrying only an offset, for callers that do not have the full object. */
    public static SearchContinuation ofOffset(int offset) {
        return new SearchContinuation(Collections.singletonMap("sroffset", String.valueOf(offset)));
    }
    public Map<String, String> getParams() { return params; }
    @Nullable public Integer getOffset() {
        String value = params.get("sroffset");
        if (value == null) return null;
        try { return Integer.valueOf(value.trim()); } catch (NumberFormatException e) { return null; }
    }
    @Override public boolean equals(Object other) {
        return other instanceof SearchContinuation && params.equals(((SearchContinuation) other).params);
    }
    @Override public int hashCode() { return params.hashCode(); }
    @Override public String toString() { return "SearchContinuation" + params; }
}
