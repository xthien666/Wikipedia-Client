package com.example.wikipediaclient.data.remote;
import static org.junit.Assert.*;
import com.android.volley.*;
import com.android.volley.toolbox.NoCache;
import com.example.wikipediaclient.data.model.SearchPage;
import com.example.wikipediaclient.ui.search.SearchUiState;
import java.util.*;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ServerWaitIntegrationTest {
    static class Queue extends RequestQueue {
        final List<Request<?>> requests = new ArrayList<>();
        Queue() { super(new NoCache(), request -> null); }
        @Override public <T> Request<T> add(Request<T> request) { requests.add(request); return request; }
    }
    static class Scheduler implements RetryScheduler {
        final List<Runnable> pending = new ArrayList<>();
        public void postDelayed(Runnable r, long delay) { pending.add(r); }
        public void cancel(Runnable r) { pending.removeIf(task -> task == r); }
        void fire() { List<Runnable> work = new ArrayList<>(pending); pending.clear(); for(Runnable r:work)r.run(); }
    }
    final ApiCallback<JSONObject> callback = new ApiCallback<JSONObject>() {
        public void onSuccess(JSONObject value) { }
        public void onError(WikiApiException error) { }
    };
    private void limited(Queue queue) {
        queue.requests.get(0).deliverError(new com.android.volley.ServerError(new NetworkResponse(
                429, new byte[0], false, 0, Collections.singletonList(new Header("Retry-After", "30")))));
    }
    @Test public void manualRetryAndArticleOpeningRespectTheSharedServerDeadline() {
        Queue queue = new Queue(); Scheduler scheduler = new Scheduler(); long[] now = {0};
        WikipediaApi api = new WikipediaApi(queue, scheduler, () -> now[0]);
        api.search("A", null, 20, callback); limited(queue);
        api.search("A", null, 20, callback); api.content("B", callback);
        assertEquals(1, queue.requests.size());
        now[0]=29_999; scheduler.fire(); assertEquals(1, queue.requests.size());
        now[0]=30_000; scheduler.fire(); assertEquals(3, queue.requests.size());
        api.cancelAll();
    }
    @Test public void cancellationAlsoRemovesDeferredRequests() {
        Queue queue = new Queue(); Scheduler scheduler = new Scheduler();
        WikipediaApi api = new WikipediaApi(queue, scheduler, () -> 0L);
        api.content("A", callback); limited(queue);
        Cancelable retry=api.content("A", callback); retry.cancel();
        assertTrue(scheduler.pending.isEmpty());
        api.content("B", callback); api.cancelAll();
        assertTrue(scheduler.pending.isEmpty()); scheduler.fire(); assertEquals(1, queue.requests.size());
    }
    @Test public void extremeRetryAfterNeverOverflowsUiWaitAndLegacyNullPageRemainsUsable() {
        assertEquals(Integer.MAX_VALUE, SearchUiState.error(new WikiApiException(
                WikiApiException.Kind.SERVER, "rate", null, 429, Long.MAX_VALUE)).getRetryAfterSeconds());
        assertNull(new SearchPage(Collections.emptyList(), null, 0).getContinuation());
    }
}
