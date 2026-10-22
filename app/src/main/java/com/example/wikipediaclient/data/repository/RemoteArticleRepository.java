package com.example.wikipediaclient.data.repository;
import com.example.wikipediaclient.data.model.ArticleSummary;
import com.example.wikipediaclient.data.remote.ApiCallback;
import com.example.wikipediaclient.data.remote.ArticleSummaryMapper;
import com.example.wikipediaclient.data.remote.Cancelable;
import com.example.wikipediaclient.data.remote.WikiApiException;
import com.example.wikipediaclient.data.remote.WikipediaApi;
import org.json.JSONException;
import org.json.JSONObject;

public final class RemoteArticleRepository implements ArticleRepository {
    private final WikipediaApi api;

    public RemoteArticleRepository(WikipediaApi api) { this.api = api; }

    @Override
    public Cancelable getSummary(String title, ApiCallback<ArticleSummary> callback) {
        return api.summary(title, new ApiCallback<JSONObject>() {
            @Override public void onSuccess(JSONObject json) {
                ArticleSummary summary;
                try {
                    summary = ArticleSummaryMapper.parse(json);
                } catch (JSONException | IllegalArgumentException e) {
                    callback.onError(new WikiApiException(WikiApiException.Kind.DATA, e.getMessage(), e));
                    return;
                }
                callback.onSuccess(summary);
            }
            @Override public void onError(WikiApiException error) { callback.onError(error); }
        });
    }
}
