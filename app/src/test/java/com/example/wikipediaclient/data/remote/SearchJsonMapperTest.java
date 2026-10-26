package com.example.wikipediaclient.data.remote;

import static org.junit.Assert.*;
import com.example.wikipediaclient.data.model.SearchPage;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// Robolectric only provides a real org.json / HtmlCompat; no network is involved.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SearchJsonMapperTest {
    @Test public void normalResponseWithHtmlSnippetAndAccents() throws Exception {
        SearchPage page = SearchJsonMapper.parse(new JSONObject("{"
                + "\"continue\":{\"sroffset\":20,\"continue\":\"-||\"},"
                + "\"query\":{\"searchinfo\":{\"totalhits\":1234},\"search\":["
                + "{\"ns\":0,\"title\":\"Hà Nội\",\"pageid\":2330,"
                + "\"snippet\":\"<span class=\\\"searchmatch\\\">Hà Nội</span> là thủ đô &quot;nghìn năm&quot; &amp; lớn\"},"
                + "{\"ns\":0,\"title\":\"Nguyễn Trãi\",\"pageid\":77,\"snippet\":\"Nhà thơ\"}]}}"));
        assertEquals(2, page.getResults().size());
        assertEquals(1234, page.getTotalHits());
        assertEquals(Integer.valueOf(20), page.getNextOffset());
        assertEquals("Every continue key is kept", "-||", page.getContinuation().getParams().get("continue"));
        assertEquals("20", page.getContinuation().getParams().get("sroffset"));
        assertEquals(2330L, page.getResults().get(0).getPageId());
        assertEquals("Hà Nội", page.getResults().get(0).getTitle());
        assertEquals("Hà Nội là thủ đô \"nghìn năm\" & lớn", page.getResults().get(0).getSnippet());
        assertNull(page.getResults().get(0).getThumbnailUrl());
    }

    @Test public void emptyResultsHaveNoNextPage() throws Exception {
        SearchPage page = SearchJsonMapper.parse(new JSONObject(
                "{\"query\":{\"searchinfo\":{\"totalhits\":0},\"search\":[]}}"));
        assertTrue(page.getResults().isEmpty());
        assertNull(page.getNextOffset());
        assertNull(page.getContinuation());
        assertEquals(0, page.getTotalHits());
    }

    @Test public void entriesMissingIdOrTitleAreSkippedAndMissingSnippetIsEmpty() throws Exception {
        SearchPage page = SearchJsonMapper.parse(new JSONObject("{\"query\":{\"search\":["
                + "{\"title\":\"Không có id\"},"
                + "{\"pageid\":5},"
                + "{\"title\":\"Hợp lệ\",\"pageid\":9}]}}"));
        assertEquals(1, page.getResults().size());
        assertEquals("Hợp lệ", page.getResults().get(0).getTitle());
        assertEquals("", page.getResults().get(0).getSnippet());
        assertEquals(1, page.getTotalHits()); // falls back to the parsed size
    }

    @Test(expected = JSONException.class)
    public void apiErrorPayloadIsRejected() throws Exception {
        SearchJsonMapper.parse(new JSONObject("{\"error\":{\"code\":\"badvalue\"}}"));
    }

    @Test(expected = JSONException.class)
    public void missingSearchArrayIsRejected() throws Exception {
        SearchJsonMapper.parse(new JSONObject("{\"query\":{}}"));
    }
}
