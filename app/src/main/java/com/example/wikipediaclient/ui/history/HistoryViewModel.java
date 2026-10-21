package com.example.wikipediaclient.ui.history;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.wikipediaclient.data.model.HistoryEntry;
import com.example.wikipediaclient.data.repository.HistoryRepository;
import com.example.wikipediaclient.data.repository.HistoryState;

public final class HistoryViewModel extends ViewModel {
    private final HistoryRepository repository;

    public HistoryViewModel(HistoryRepository repository) { this.repository = repository; }

    public LiveData<HistoryState> observe() { return repository.observe(); }
    public void refresh() { repository.refresh(); }
    public void remove(HistoryEntry entry) { repository.remove(entry.getLanguage(), entry.getPageId()); }
    public void clear() { repository.clear(); }

    public static final class Factory implements ViewModelProvider.Factory {
        private final HistoryRepository repository;
        public Factory(HistoryRepository repository) { this.repository = repository; }
        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(HistoryViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
            }
            return modelClass.cast(new HistoryViewModel(repository));
        }
    }
}
