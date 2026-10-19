package com.example.wikipediaclient.data;
import android.content.Context;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/** Shared dependencies; feature owners add their repositories here. */
public final class AppContainer {
    private final Context applicationContext;
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();
    private RequestQueue requestQueue;
    private final com.example.wikipediaclient.data.local.ReaderSettings readerSettings;
    public AppContainer(Context context) {
        applicationContext = context.getApplicationContext();
        readerSettings = new com.example.wikipediaclient.data.local.ReaderSettings(applicationContext);
    }
    public synchronized RequestQueue getRequestQueue() {
        if (requestQueue == null) requestQueue = Volley.newRequestQueue(applicationContext);
        return requestQueue;
    }
    private com.example.wikipediaclient.data.remote.WikipediaApi wikipediaApi;
    private com.example.wikipediaclient.data.repository.SearchRepository searchRepository;
    private com.example.wikipediaclient.data.repository.ArticleRepository articleRepository;
    private synchronized com.example.wikipediaclient.data.remote.WikipediaApi getWikipediaApi() {
        if (wikipediaApi == null) {
            wikipediaApi = new com.example.wikipediaclient.data.remote.WikipediaApi(getRequestQueue());
        }
        return wikipediaApi;
    }
    public synchronized com.example.wikipediaclient.data.repository.SearchRepository getSearchRepository() {
        if (searchRepository == null) {
            searchRepository = new com.example.wikipediaclient.data.repository.RemoteSearchRepository(getWikipediaApi());
        }
        return searchRepository;
    }
    public synchronized com.example.wikipediaclient.data.repository.ArticleRepository getArticleRepository() {
        if (articleRepository == null) {
            articleRepository = new com.example.wikipediaclient.data.repository.RemoteArticleRepository(getWikipediaApi());
        }
        return articleRepository;
    }
    public com.example.wikipediaclient.data.local.ReaderSettings getReaderSettings() { return readerSettings; }
    public ExecutorService getDiskExecutor() { return diskExecutor; }
}
