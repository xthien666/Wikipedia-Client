package com.example.wikipediaclient.data.repository;
import androidx.annotation.Nullable;
import com.example.wikipediaclient.data.model.SearchContinuation;
import com.example.wikipediaclient.data.model.SearchPage;
import com.example.wikipediaclient.data.remote.ApiCallback;
import com.example.wikipediaclient.data.remote.Cancelable;

/** Interface so the ViewModel can be tested with a fake. Callbacks arrive on the main thread. */
public interface SearchRepository {
    /** {@code continuation} is null for the first page, else the previous page's {@link SearchPage#getContinuation()}. */
    Cancelable search(String query, @Nullable SearchContinuation continuation, ApiCallback<SearchPage> callback);
}
