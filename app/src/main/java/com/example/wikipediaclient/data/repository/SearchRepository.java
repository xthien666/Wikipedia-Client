package com.example.wikipediaclient.data.repository;
import com.example.wikipediaclient.data.model.SearchPage;
import com.example.wikipediaclient.data.remote.ApiCallback;
import com.example.wikipediaclient.data.remote.Cancelable;

/** Interface so the ViewModel can be tested with a fake. Callbacks arrive on the main thread. */
public interface SearchRepository {
    Cancelable search(String query, int offset, ApiCallback<SearchPage> callback);
}
