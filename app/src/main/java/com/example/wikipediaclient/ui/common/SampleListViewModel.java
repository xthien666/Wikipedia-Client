package com.example.wikipediaclient.ui.common;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

/** Session state for the prototype fixtures; this is not the personal-data repository. */
public final class SampleListViewModel extends ViewModel {
    private final SavedStateHandle state;
    public SampleListViewModel(SavedStateHandle state) { this.state = state; }
    public boolean isRemoved(int id) { return Boolean.TRUE.equals(state.get("removed_" + id)); }
    public void remove(int id) { state.set("removed_" + id, true); }
    public int remaining(int total) {
        int count = 0;
        for (int id = 1; id <= total; id++) if (!isRemoved(id)) count++;
        return count;
    }
    public String getFilter() {
        String filter = state.get("filter");
        return filter == null ? "all" : filter;
    }
    public void setFilter(String filter) { state.set("filter", filter); }
}
