package com.example.wikipediaclient.data.repository;

import com.example.wikipediaclient.data.model.HistoryEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HistoryState {
    public enum Kind { LOADING, READY, ERROR }
    public enum Failure { LOAD, WRITE }
    private final Kind kind;
    private final List<HistoryEntry> entries;
    private final Failure failure;

    private HistoryState(Kind kind, List<HistoryEntry> entries) {
        this(kind, entries, null);
    }

    private HistoryState(Kind kind, List<HistoryEntry> entries, Failure failure) {
        this.kind = kind;
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        this.failure = failure;
    }
    public static HistoryState loading() { return new HistoryState(Kind.LOADING, Collections.emptyList()); }
    public static HistoryState ready(List<HistoryEntry> entries) { return new HistoryState(Kind.READY, entries); }
    public static HistoryState error(List<HistoryEntry> entries, Failure failure) {
        return new HistoryState(Kind.ERROR, entries, failure);
    }
    public Kind getKind() { return kind; }
    public List<HistoryEntry> getEntries() { return entries; }
    public Failure getFailure() { return failure; }
}
