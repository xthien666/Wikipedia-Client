package com.example.wikipediaclient;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import com.example.wikipediaclient.data.local.LocalHistoryRepository;
import com.example.wikipediaclient.data.model.HistoryEntry;
import com.example.wikipediaclient.data.repository.HistoryState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class HistoryRepositoryTest {
    private HistoryState state(LiveData<HistoryState> liveData) {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        return liveData.getValue();
    }

    @Test public void unreadableHistoryIsNotSilentlyOverwrittenAndCanBeCleared() {
        Context context = RuntimeEnvironment.getApplication();
        android.content.SharedPreferences preferences = context.getSharedPreferences(
                "reading_history", Context.MODE_PRIVATE);
        preferences.edit().putString("entries", "broken-json").commit();
        LocalHistoryRepository repository = new LocalHistoryRepository(context, Runnable::run);
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
        LocalHistoryRepository repository = new LocalHistoryRepository(context, Runnable::run);
        repository.record(new HistoryEntry("vi", 1, "A", "https://vi.wikipedia.org/wiki/A", 100));
        repository.record(new HistoryEntry("vi", 2, "B", "https://vi.wikipedia.org/wiki/B", 200));
        repository.record(new HistoryEntry("vi", 1, "A mới", "https://vi.wikipedia.org/wiki/A_moi", 300));
        HistoryState current = state(repository.observe());
        assertEquals(HistoryState.Kind.READY, current.getKind());
        assertEquals(2, current.getEntries().size());
        assertEquals("A mới", current.getEntries().get(0).getTitle());
        assertEquals(1, current.getEntries().get(0).getPageId());

        LocalHistoryRepository reopened = new LocalHistoryRepository(context, Runnable::run);
        assertEquals(2, state(reopened.observe()).getEntries().size());
        reopened.remove("vi", 1);
        assertEquals(2, state(repository.observe()).getEntries().size());
        assertEquals(1, state(reopened.observe()).getEntries().size());
        reopened.clear();
        assertTrue(state(reopened.observe()).getEntries().isEmpty());
        assertEquals("saved", context.getSharedPreferences("other_data", Context.MODE_PRIVATE)
                .getString("bookmark", null));
    }
}
