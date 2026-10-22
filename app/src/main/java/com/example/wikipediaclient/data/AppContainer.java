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
    /** Creates shared services without opening the database or starting networking. */
    public AppContainer(Context context) {
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
    /** Returns the existing reader settings service. */
    public com.example.wikipediaclient.data.local.ReaderSettings getReaderSettings() { return readerSettings; }
    /** Serializes local reads and writes in user-action order. */
    public ExecutorService getDiskExecutor() { return diskExecutor; }
    /** Supplies persistent bookmarks to the library and reader. */
    public BookmarkRepository getBookmarkRepository() { return bookmarkRepository; }
    /** Supplies persistent history to TV1 and TV3. */
    public HistoryRepository getHistoryRepository() { return historyRepository; }
}
