package com.example.wikipediaclient.data.remote;

import static org.junit.Assert.*;
import android.net.Uri;
import com.android.volley.ClientError;
import com.android.volley.Header;
import com.android.volley.NetworkResponse;
import com.example.wikipediaclient.data.model.SearchContinuation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
        assertEquals("0", uri.getQueryParameter("srnamespace"));
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
        assertTrue(server.isRateLimited());
        assertEquals(-1, server.getRetryAfterMs());
    }

    @Test public void firstPageHasNoContinuationParameters() {
        Uri uri = Uri.parse(WikipediaApi.buildSearchUrl("Hà Nội", (SearchContinuation) null, 20));
        assertNull(uri.getQueryParameter("sroffset"));
        assertNull(uri.getQueryParameter("continue"));
    }

    @Test public void nextPageSendsBackEveryContinuationKey() {
        Map<String, String> cont = new LinkedHashMap<>();
        cont.put("sroffset", "20");
        cont.put("continue", "-||");
        Uri uri = Uri.parse(WikipediaApi.buildSearchUrl("Hà Nội", new SearchContinuation(cont), 20));
        assertEquals("20", uri.getQueryParameter("sroffset"));
        assertEquals("-||", uri.getQueryParameter("continue"));
        assertEquals("Hà Nội", uri.getQueryParameter("srsearch"));
    }

    @Test public void continuationCannotOverrideTheQuery() {
        Map<String, String> cont = new LinkedHashMap<>();
        cont.put("sroffset", "20");
        cont.put("srsearch", "khác");
        cont.put("srlimit", "500");
        Uri uri = Uri.parse(WikipediaApi.buildSearchUrl("Hà Nội", new SearchContinuation(cont), 20));
        assertEquals(Collections.singletonList("Hà Nội"), uri.getQueryParameters("srsearch"));
        assertEquals(Collections.singletonList("20"), uri.getQueryParameters("srlimit"));
    }

    @Test public void tooManyRequestsCarriesRetryAfter() {
        WikiApiException limited = WikipediaApi.toException(new ClientError(new NetworkResponse(
                429, new byte[0], false, 0, Collections.singletonList(new Header("Retry-After", "30")))));
        assertEquals(WikiApiException.Kind.SERVER, limited.getKind());
        assertEquals(429, limited.getStatusCode());
        assertTrue(limited.isRateLimited());
        assertEquals(30_000, limited.getRetryAfterMs());
    }

    @Test public void notFoundIsNotRateLimited() {
        WikiApiException missing = WikipediaApi.toException(new ClientError(new NetworkResponse(
                404, new byte[0], false, 0, Collections.emptyList())));
        assertFalse(missing.isRateLimited());
    }
}
