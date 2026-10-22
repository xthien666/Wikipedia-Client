package com.example.wikipediaclient.ui.article;
import com.example.wikipediaclient.data.model.*;
import com.example.wikipediaclient.data.remote.*;
import com.example.wikipediaclient.data.repository.ArticleRepository;

public final class FixtureArticles implements ArticleRepository {
    public static ArticleContent article(String title) {
        return new ArticleContent(new PersonalArticle("vi", Math.abs((long) title.hashCode()) + 1, title,
                "https://vi.wikipedia.org/wiki/" + android.net.Uri.encode(title.replace(' ', '_'))),
                "<html><head></head><body><p>" + title + "</p></body></html>", 10,
                "CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/");
    }
    @Override public Cancelable getContent(String title, ApiCallback<ArticleContent> callback) {
        callback.onSuccess(article(title)); return () -> {};
    }
    @Override public Cancelable getSummary(String title, ApiCallback<ArticleSummary> callback) { return () -> {}; }
}
