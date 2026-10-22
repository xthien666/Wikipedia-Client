package com.example.wikipediaclient.ui.search;
import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.CreationExtras;
import com.example.wikipediaclient.data.AppContainer;

/** Supplies the repository from {@link AppContainer} while keeping the SavedStateHandle. */
public final class SearchViewModelFactory implements ViewModelProvider.Factory {
    private final AppContainer container;

    public SearchViewModelFactory(AppContainer container) { this.container = container; }

    @NonNull @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass, @NonNull CreationExtras extras) {
        if (!modelClass.isAssignableFrom(SearchViewModel.class)) {
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass);
        }
        SavedStateHandle handle = SavedStateHandleSupport.createSavedStateHandle(extras);
        return modelClass.cast(new SearchViewModel(handle, container.getSearchRepository(),
                DelayedExecutor.mainThread()));
    }
}
