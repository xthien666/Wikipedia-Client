package com.example.wikipediaclient.data.remote;

import static org.junit.Assert.*;
import com.example.wikipediaclient.data.model.ArticleSummary;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ArticleSummaryMapperTest {
    @Test public void fullSummary() throws Exception {
        ArticleSummary s = ArticleSummaryMapper.parse(new JSONObject("{"
                + "\"title\":\"Hà Nội\",\"pageid\":2330,\"description\":\"Thủ đô của Việt Nam\","
                + "\"extract\":\"Hà Nội là thủ đô.\","
                + "\"thumbnail\":{\"source\":\"https://upload.wikimedia.org/a.jpg\"},"
                + "\"content_urls\":{\"desktop\":{\"page\":\"https://vi.wikipedia.org/wiki/H%C3%A0_N%E1%BB%99i\"}}}"));
        assertEquals(2330L, s.getPageId());
        assertEquals("Hà Nội", s.getTitle());
        assertEquals("Thủ đô của Việt Nam", s.getDescription());
        assertEquals("https://upload.wikimedia.org/a.jpg", s.getThumbnailUrl());
        assertEquals("https://vi.wikipedia.org/wiki/H%C3%A0_N%E1%BB%99i", s.getPageUrl());
    }

    @Test public void optionalFieldsMayBeMissing() throws Exception {
        ArticleSummary s = ArticleSummaryMapper.parse(new JSONObject("{"
                + "\"title\":\"A\",\"pageid\":1,"
                + "\"content_urls\":{\"desktop\":{\"page\":\"https://vi.wikipedia.org/wiki/A\"}}}"));
        assertNull(s.getDescription());
        assertNull(s.getThumbnailUrl());
        assertEquals("", s.getExtract());
    }

    @Test(expected = JSONException.class)
    public void missingIdentityIsRejected() throws Exception {
        ArticleSummaryMapper.parse(new JSONObject("{\"title\":\"A\"}"));
    }

    @Test public void summaryUrlEncodesTitle() {
        String url = WikipediaApi.buildSummaryUrl("Nguyễn Trãi");
        assertTrue(url.startsWith("https://vi.wikipedia.org/api/rest_v1/page/summary/Nguy"));
        assertFalse(url.contains(" "));
        assertFalse(url.contains("ễ"));
        assertTrue(url.endsWith("Tr%C3%A3i") && url.contains("_"));
    }
}
