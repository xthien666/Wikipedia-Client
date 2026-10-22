package com.example.wikipediaclient.data.remote;
import static org.junit.Assert.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import com.example.wikipediaclient.data.model.ArticleContent;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ArticleContentMapperTest {
    private JSONObject response() throws Exception {
        return new JSONObject().put("id", 80).put("key", "Hà_Nội").put("title", "Hà Nội")
                .put("html", "<html><body>Toàn bộ bài</body></html>")
                .put("latest", new JSONObject().put("id", 987))
                .put("license", new JSONObject().put("title", "CC BY-SA 4.0")
                        .put("url", "https://creativecommons.org/licenses/by-sa/4.0/deed.vi"));
    }
    @Test public void resolvedIdentityHtmlRevisionAndLicenseStayTogether() throws Exception {
        ArticleContent content = ArticleContentMapper.parse(response());
        assertEquals(80, content.article.getPageId());
        assertEquals("Hà Nội", content.article.getTitle());
        assertEquals("Hà_Nội", android.net.Uri.parse(content.article.getCanonicalUrl()).getLastPathSegment());
        assertEquals(987, content.revision);
        assertTrue(content.html.contains("Toàn bộ bài"));
        assertEquals("CC BY-SA 4.0", content.licenseTitle);
    }
    @Test public void missingHtmlAndInvalidIdentityAreRejected() throws Exception {
        for (String field : new String[]{"html", "id", "key", "license", "latest", "title"}) {
            JSONObject json = response(); json.remove(field);
            try { ArticleContentMapper.parse(json); fail(field); }
            catch (JSONException | IllegalArgumentException expected) { }
        }
    }
    @Test public void emptyHtmlAndUnsafeLicenseAreRejected() throws Exception {
        try { ArticleContentMapper.parse(response().put("html", " ")); fail(); }
        catch (IllegalArgumentException expected) { }
        JSONObject json = response(); json.getJSONObject("license").put("url", "javascript:alert(1)");
        try { ArticleContentMapper.parse(json); fail(); } catch (JSONException expected) { }
    }
    @Test public void titleIncludingReservedCharactersIsOneEncodedPathSegment() {
        String url = WikipediaApi.buildContentUrl("Hà Nội/?:#");
        android.net.Uri uri = android.net.Uri.parse(url);
        assertEquals("Hà_Nội/?:#", uri.getPathSegments().get(4));
        assertNull(uri.getQuery()); assertNull(uri.getFragment());
        assertEquals("with_html", uri.getLastPathSegment());
    }
}
