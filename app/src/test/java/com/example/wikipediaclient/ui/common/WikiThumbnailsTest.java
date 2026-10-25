package com.example.wikipediaclient.ui.common;

import static org.junit.Assert.*;

import android.net.Uri;
import android.os.Looper;
import android.widget.ImageView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class WikiThumbnailsTest {
    private final List<Request<?>> requests = new ArrayList<>();
    private final RequestQueue queue = new RequestQueue(new NoCache(), request -> {
        throw new AssertionError("Thumbnail tests must not use the network");
    }) {
        @Override public <T> Request<T> add(Request<T> request) {
            requests.add(request);
            return request;
        }
    };
    private final WikiThumbnails thumbnails = new WikiThumbnails(queue);

    private ImageView image() { return new ImageView(RuntimeEnvironment.getApplication()); }

    private void flush() {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50));
    }

    @Test public void cancelledViewDoesNotStartMetadataRequest() {
        ImageView target = image();
        thumbnails.load("Hà Nội", target);
        thumbnails.cancel(target);
        flush();
        assertTrue(requests.isEmpty());
    }

    @Test public void recycledViewRequestsOnlyItsCurrentArticle() {
        ImageView target = image();
        thumbnails.load("Hà Nội", target);
        thumbnails.load("Đà Nẵng", target);
        flush();
        assertEquals(1, requests.size());
        assertEquals("Đà Nẵng", Uri.parse(requests.get(0).getUrl()).getQueryParameter("titles"));
    }

    @Test public void cancellingOneViewKeepsTheOtherViewsRequest() {
        ImageView first = image();
        ImageView second = image();
        thumbnails.load("Hà Nội", first);
        thumbnails.load("Hà Nội", second);
        thumbnails.cancel(first);
        flush();
        assertEquals(1, requests.size());
        assertEquals("Hà Nội", Uri.parse(requests.get(0).getUrl()).getQueryParameter("titles"));
    }
}
