package com.example.wikipediaclient.data.repository;
import com.example.wikipediaclient.data.model.ArticleContent;
import com.example.wikipediaclient.data.model.ArticleSummary;
import com.example.wikipediaclient.data.remote.*;
import org.json.JSONException;
import org.json.JSONObject;

public final class RemoteArticleRepository implements ArticleRepository {
    private final WikipediaApi api;
    public RemoteArticleRepository(WikipediaApi api) { this.api = api; }
    @Override public Cancelable getContent(String title, ApiCallback<ArticleContent> callback) {
        return api.content(title, new ApiCallback<JSONObject>() {
            @Override public void onSuccess(JSONObject json) {
                ArticleContent content;
                try { content = ArticleContentMapper.parse(json); }
                catch (JSONException | IllegalArgumentException exception) {
                    callback.onError(new WikiApiException(WikiApiException.Kind.DATA, exception.getMessage(), exception));
                    return;
                }
                callback.onSuccess(content);
            }
            @Override public void onError(WikiApiException error) { callback.onError(error); }
        });
    }
    @Override public Cancelable getSummary(String title, ApiCallback<ArticleSummary> callback) {
        return api.summary(title, new ApiCallback<JSONObject>() {
            @Override public void onSuccess(JSONObject json) {
                ArticleSummary summary;
                try { summary = ArticleSummaryMapper.parse(json); }
                catch (JSONException | IllegalArgumentException exception) {
                    callback.onError(new WikiApiException(WikiApiException.Kind.DATA, exception.getMessage(), exception));
                    return;
                }
                callback.onSuccess(summary);
            }
            @Override public void onError(WikiApiException error) { callback.onError(error); }
        });
    }
}
