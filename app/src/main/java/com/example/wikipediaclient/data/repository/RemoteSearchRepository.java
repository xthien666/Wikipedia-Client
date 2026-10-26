package com.example.wikipediaclient.data.repository;
import androidx.annotation.Nullable;
import com.example.wikipediaclient.data.model.SearchContinuation;
import com.example.wikipediaclient.data.model.SearchPage;
import com.example.wikipediaclient.data.remote.ApiCallback;
import com.example.wikipediaclient.data.remote.Cancelable;
import com.example.wikipediaclient.data.remote.SearchJsonMapper;
import com.example.wikipediaclient.data.remote.WikiApiException;
import com.example.wikipediaclient.data.remote.WikipediaApi;
import org.json.JSONException;
import org.json.JSONObject;

public final class RemoteSearchRepository implements SearchRepository {
    static final int PAGE_SIZE = 20;
    private final WikipediaApi api;

    public RemoteSearchRepository(WikipediaApi api) { this.api = api; }

    @Override
    public Cancelable search(String query, @Nullable SearchContinuation continuation,
                             ApiCallback<SearchPage> callback) {
        return api.search(query, continuation, PAGE_SIZE, new ApiCallback<JSONObject>() {
            @Override public void onSuccess(JSONObject json) {
                SearchPage page;
                try {
                    page = SearchJsonMapper.parse(json);
                } catch (JSONException e) {
                    callback.onError(new WikiApiException(WikiApiException.Kind.DATA, e.getMessage(), e));
                    return;
                }
                callback.onSuccess(page);
            }
            @Override public void onError(WikiApiException error) { callback.onError(error); }
        });
    }
}
