package com.example.wikipediaclient.data;
import android.content.Context;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.wikipediaclient.data.local.LocalHistoryRepository;
import com.example.wikipediaclient.data.repository.HistoryRepository;
/** Shared dependencies; feature owners add their repositories here. */
public final class AppContainer {
    private final Context applicationContext;
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();
    private RequestQueue requestQueue;
    private final com.example.wikipediaclient.data.local.ReaderSettings readerSettings;
    private final HistoryRepository historyRepository;
    public AppContainer(Context context) {
        applicationContext = context.getApplicationContext();
        readerSettings = new com.example.wikipediaclient.data.local.ReaderSettings(applicationContext);
        historyRepository = new LocalHistoryRepository(applicationContext, diskExecutor);
    }
    public synchronized RequestQueue getRequestQueue() {
        if (requestQueue == null) requestQueue = Volley.newRequestQueue(applicationContext);
        return requestQueue;
    }
    public com.example.wikipediaclient.data.local.ReaderSettings getReaderSettings() { return readerSettings; }
    public ExecutorService getDiskExecutor() { return diskExecutor; }
    public HistoryRepository getHistoryRepository() { return historyRepository; }
}
