package com.example.wikipediaclient.data.repository;
import com.example.wikipediaclient.data.model.ArticleContent;
import com.example.wikipediaclient.data.model.ArticleSummary;
import com.example.wikipediaclient.data.remote.ApiCallback;
import com.example.wikipediaclient.data.remote.Cancelable;

/** Resolves redirects before exposing metadata and full HTML to the reader. Callbacks on main. */
public interface ArticleRepository {
    Cancelable getContent(String title, ApiCallback<ArticleContent> callback);
    Cancelable getSummary(String title, ApiCallback<ArticleSummary> callback);
}
