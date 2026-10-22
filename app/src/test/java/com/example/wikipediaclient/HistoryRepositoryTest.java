package com.example.wikipediaclient;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import com.example.wikipediaclient.data.local.LocalHistoryRepository;
import com.example.wikipediaclient.data.model.HistoryEntry;
import com.example.wikipediaclient.data.repository.HistoryState;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class HistoryRepositoryTest {
    private static class ControlledDatabase extends com.example.wikipediaclient.data.local.WikiDatabaseHelper {
        volatile boolean failWrites;
        ControlledDatabase(Context context) { super(context); }
        @Override public android.database.sqlite.SQLiteDatabase getWritableDatabase() {
            if (failWrites) throw new android.database.sqlite.SQLiteException("Simulated disk failure");
            return super.getWritableDatabase();
        }
    }
    private ExecutorService disk;
    @Before public void setup() {
        disk = Executors.newSingleThreadExecutor();
        RuntimeEnvironment.getApplication().deleteDatabase("personal_articles.db");
        RuntimeEnvironment.getApplication().getSharedPreferences("reading_history", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }
    @After public void cleanup() throws Exception {
        disk.shutdown();
        assertTrue(disk.awaitTermination(10, TimeUnit.SECONDS));
    }
    private void drain() {
        try { disk.submit(() -> {}).get(10, TimeUnit.SECONDS); }
        catch (Exception error) { throw new AssertionError(error); }
    }
    private HistoryState state(LiveData<HistoryState> liveData) {
        drain();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        return liveData.getValue();
    }

    @Test public void unreadableHistoryIsNotSilentlyOverwrittenAndCanBeCleared() {
        Context context = RuntimeEnvironment.getApplication();
        android.content.SharedPreferences preferences = context.getSharedPreferences(
                "reading_history", Context.MODE_PRIVATE);
        preferences.edit().putString("entries", "broken-json").commit();
        LocalHistoryRepository repository = new LocalHistoryRepository(context, disk);
        assertEquals(HistoryState.Failure.LOAD, state(repository.observe()).getFailure());
        repository.record(new HistoryEntry("vi", 1, "A", "https://vi.wikipedia.org/wiki/A", 100));
        assertEquals(HistoryState.Failure.WRITE, state(repository.observe()).getFailure());
        assertEquals("broken-json", preferences.getString("entries", null));
        repository.clear();
        assertEquals(HistoryState.Kind.READY, state(repository.observe()).getKind());
        repository.record(new HistoryEntry("vi", 1, "A", "https://vi.wikipedia.org/wiki/A", 100));
        assertEquals(1, state(repository.observe()).getEntries().size());
    }

    @Test public void visitsAreUniqueOrderedPersistentAndClearDoesNotTouchOtherPreferences() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("reading_history", Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences("other_data", Context.MODE_PRIVATE).edit()
                .putString("bookmark", "saved").commit();
        LocalHistoryRepository repository = new LocalHistoryRepository(context, disk);
        repository.record(new HistoryEntry("vi", 1, "A", "https://vi.wikipedia.org/wiki/A", 100));
        repository.record(new HistoryEntry("vi", 2, "B", "https://vi.wikipedia.org/wiki/B", 200));
        repository.record(new HistoryEntry("vi", 1, "A mới", "https://vi.wikipedia.org/wiki/A_moi", 300));
        HistoryState current = state(repository.observe());
        assertEquals(HistoryState.Kind.READY, current.getKind());
        assertEquals(2, current.getEntries().size());
        assertEquals("A mới", current.getEntries().get(0).getTitle());
        assertEquals(1, current.getEntries().get(0).getPageId());

        LocalHistoryRepository reopened = new LocalHistoryRepository(context, disk);
        assertEquals(2, state(reopened.observe()).getEntries().size());
        assertFalse(context.getSharedPreferences("reading_history", Context.MODE_PRIVATE).contains("entries"));
        reopened.remove("vi", 1);
        assertEquals(2, state(repository.observe()).getEntries().size());
        assertEquals(1, state(reopened.observe()).getEntries().size());
        reopened.clear();
        assertTrue(state(reopened.observe()).getEntries().isEmpty());
        assertEquals("saved", context.getSharedPreferences("other_data", Context.MODE_PRIVATE)
                .getString("bookmark", null));
    }

    @Test public void retryRepeatsFailedRemovalAndRefreshDoesNotHideIt() {
        Context context = RuntimeEnvironment.getApplication();
        ControlledDatabase database = new ControlledDatabase(context);
        LocalHistoryRepository repository = new LocalHistoryRepository(context, disk, database);
        repository.record(new HistoryEntry("vi", 1, "A", "https://vi.wikipedia.org/wiki/A", 100));
        assertEquals(1, state(repository.observe()).getEntries().size());
        database.failWrites = true;
        repository.remove("vi", 1);
        assertEquals(HistoryState.Failure.WRITE, state(repository.observe()).getFailure());
        assertEquals(1, state(repository.observe()).getEntries().size());
        database.failWrites = false;
        repository.refresh();
        assertEquals(HistoryState.Failure.WRITE, state(repository.observe()).getFailure());
        assertEquals(1, state(repository.observe()).getEntries().size());
        repository.retry();
        assertEquals(HistoryState.Kind.READY, state(repository.observe()).getKind());
        assertTrue(state(repository.observe()).getEntries().isEmpty());
        drain();
        disk.execute(database::close);
    }

    @Test public void retryRepeatsConfirmedClearWithoutRemovingBookmarks() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ControlledDatabase database = new ControlledDatabase(context);
        LocalHistoryRepository repository = new LocalHistoryRepository(context, disk, database);
        repository.record(new HistoryEntry("vi", 1, "A", "https://vi.wikipedia.org/wiki/A", 100));
        state(repository.observe());
        disk.submit(() -> new com.example.wikipediaclient.data.repository.LocalBookmarkRepository(database)
                .saveBookmark(new com.example.wikipediaclient.data.model.PersonalArticle(
                        "vi", 1, "A", "https://vi.wikipedia.org/wiki/A"), 100)).get();
        database.failWrites = true;
        repository.clear();
        assertEquals(HistoryState.Failure.WRITE, state(repository.observe()).getFailure());
        database.failWrites = false;
        repository.retry();
        assertEquals(HistoryState.Kind.READY, state(repository.observe()).getKind());
        assertTrue(state(repository.observe()).getEntries().isEmpty());
        assertTrue(disk.submit(() -> new com.example.wikipediaclient.data.repository.LocalBookmarkRepository(database)
                .isBookmarked("vi", 1)).get());
        disk.execute(database::close);
    }

    @Test public void explicitClearSupersedesFailedRecordSoRetryCannotRecreateIt() {
        Context context = RuntimeEnvironment.getApplication();
        ControlledDatabase database = new ControlledDatabase(context);
        LocalHistoryRepository repository = new LocalHistoryRepository(context, disk, database);
        state(repository.observe());
        database.failWrites = true;
        repository.record(new HistoryEntry("vi", 1, "A", "https://vi.wikipedia.org/wiki/A", 100));
        assertEquals(HistoryState.Failure.WRITE, state(repository.observe()).getFailure());
        database.failWrites = false;
        repository.clear();
        assertEquals(HistoryState.Kind.READY, state(repository.observe()).getKind());
        repository.retry();
        assertTrue(state(repository.observe()).getEntries().isEmpty());
        disk.execute(database::close);
    }

    @Test public void retryOfLoadFailureReloadsRepairedData() {
        Context context = RuntimeEnvironment.getApplication();
        android.content.SharedPreferences preferences = context.getSharedPreferences("reading_history", Context.MODE_PRIVATE);
        preferences.edit().putString("entries", "broken-json").commit();
        LocalHistoryRepository repository = new LocalHistoryRepository(context, disk);
        assertEquals(HistoryState.Failure.LOAD, state(repository.observe()).getFailure());
        preferences.edit().remove("entries").commit();
        repository.retry();
        assertEquals(HistoryState.Kind.READY, state(repository.observe()).getKind());
    }
}
