package com.example.wikipediaclient.ui.search;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
public final class SearchViewModel extends ViewModel {
    private final SavedStateHandle savedState;
    public SearchViewModel(SavedStateHandle savedState) { this.savedState = savedState; }
    public String getQuery() {
        String value = savedState.get("query");
        return value == null ? "" : value;
    }
    public void setQuery(String query) { savedState.set("query", query); }
}
