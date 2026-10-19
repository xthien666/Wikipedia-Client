package com.example.wikipediaclient.data.repository;
import com.example.wikipediaclient.data.model.ArticleSummary;
import com.example.wikipediaclient.data.remote.ApiCallback;
import com.example.wikipediaclient.data.remote.Cancelable;

/** Resolves a title (following redirects) to its canonical summary. Callbacks on the main thread. */
public interface ArticleRepository {
    Cancelable getSummary(String title, ApiCallback<ArticleSummary> callback);
}
