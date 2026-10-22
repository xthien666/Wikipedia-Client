package com.example.wikipediaclient.data;
import android.content.Context;
import com.example.wikipediaclient.data.local.WikiDatabaseHelper;
import com.example.wikipediaclient.data.repository.BookmarkRepository;
import com.example.wikipediaclient.data.repository.HistoryRepository;
import com.example.wikipediaclient.data.repository.LocalBookmarkRepository;
import com.example.wikipediaclient.data.local.LocalHistoryRepository;
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
    private final BookmarkRepository bookmarkRepository;
    private final HistoryRepository historyRepository;
    /** Creates shared services; history initializes on the disk executor and networking remains lazy. */
    public AppContainer(Context context) {
        this(context, null);
    }
    /** Allows deterministic UI tests to supply a search repository without real networking. */
    public AppContainer(Context context, com.example.wikipediaclient.data.repository.SearchRepository searchRepository) {
        this(context, searchRepository, null);
    }
    /** Injects full article loading in tests without opening real network connections. */
    public AppContainer(Context context, com.example.wikipediaclient.data.repository.SearchRepository searchRepository,
            com.example.wikipediaclient.data.repository.ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
        this.searchRepository = searchRepository;
        applicationContext = context.getApplicationContext();
        readerSettings = new com.example.wikipediaclient.data.local.ReaderSettings(applicationContext);
        WikiDatabaseHelper database = new WikiDatabaseHelper(applicationContext);
        bookmarkRepository = new LocalBookmarkRepository(database);
        historyRepository = new LocalHistoryRepository(applicationContext, diskExecutor, database);
    }
    /** Lazily starts the shared network queue when a network feature needs it. */
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
    /** Returns the existing reader settings service. */
    public com.example.wikipediaclient.data.local.ReaderSettings getReaderSettings() { return readerSettings; }
    /** Serializes local reads and writes in user-action order. */
    public ExecutorService getDiskExecutor() { return diskExecutor; }
    /** Supplies persistent bookmarks to the library and reader. */
    public BookmarkRepository getBookmarkRepository() { return bookmarkRepository; }
    /** Supplies persistent history to TV1 and TV3. */
    public HistoryRepository getHistoryRepository() { return historyRepository; }
}
