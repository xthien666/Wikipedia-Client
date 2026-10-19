package com.example.wikipediaclient.data.remote;

import static org.junit.Assert.*;
import android.net.Uri;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class WikipediaApiTest {
    @Test public void searchUrlEncodesVietnameseQueryAndPaging() {
        String url = WikipediaApi.buildSearchUrl("Hà Nội", 40, 20);
        assertTrue(url.startsWith("https://vi.wikipedia.org/w/api.php?"));
        Uri uri = Uri.parse(url);
        assertEquals("Hà Nội", uri.getQueryParameter("srsearch"));
        assertEquals("40", uri.getQueryParameter("sroffset"));
        assertEquals("20", uri.getQueryParameter("srlimit"));
        assertEquals("search", uri.getQueryParameter("list"));
        assertEquals("2", uri.getQueryParameter("formatversion"));
        assertFalse("Query must be percent-encoded", url.contains("Hà"));
    }

    @Test public void volleyErrorsMapToSharedKinds() {
        assertEquals(WikiApiException.Kind.NO_CONNECTION,
                WikipediaApi.toException(new NoConnectionError()).getKind());
        assertEquals(WikiApiException.Kind.TIMEOUT,
                WikipediaApi.toException(new TimeoutError()).getKind());
        assertEquals(WikiApiException.Kind.DATA,
                WikipediaApi.toException(new ParseError(new Exception("bad json"))).getKind());
        WikiApiException server = WikipediaApi.toException(
                new ServerError(new NetworkResponse(503, new byte[0], false, 0, java.util.Collections.emptyList())));
        assertEquals(WikiApiException.Kind.SERVER, server.getKind());
        assertEquals(503, server.getStatusCode());
    }
}
