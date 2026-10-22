package com.example.wikipediaclient.ui.article;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ArticleLinksTest {
    private final String base = "https://vi.wikipedia.org/wiki/H%C3%A0_N%E1%BB%99i";
    @Test public void fragmentsAndRelativeVietnameseArticlesAreSeparated() {
        assertEquals(ArticleLinks.Kind.ANCHOR, ArticleLinks.resolve(base, "#cite_note-1").kind);
        ArticleLinks.Target next = ArticleLinks.resolve(base, "./Vi%E1%BB%87t_Nam#Lịch_sử");
        assertEquals(ArticleLinks.Kind.ARTICLE, next.kind);
        assertEquals("Việt Nam", next.locator.getTitle());
        assertEquals("Lịch_sử", next.anchor);
        assertEquals(ArticleLinks.Kind.ARTICLE, ArticleLinks.resolve(base, "/wiki/Trái_Đất").kind);
        assertEquals(ArticleLinks.Kind.ARTICLE, ArticleLinks.resolve(base, "/wiki/Star_Trek:_Voyager").kind);
    }
    @Test public void namespacesOtherLanguagesAndQueriesGoOutsideReader() {
        for (String href : new String[]{"//en.wikipedia.org/wiki/Hanoi", "/wiki/Đặc_biệt:Tìm_kiếm",
                "/wiki/Tập_tin:Photo.jpg", "/w/index.php?title=Hanoi&action=edit",
                "/wiki/A?redlink=1", "https://example.org/", "https://vi.wikipedia.org.evil.test/wiki/A"}) {
            assertEquals(href, ArticleLinks.Kind.EXTERNAL, ArticleLinks.resolve(base, href).kind);
        }
    }
    @Test public void activeSchemesFilesAndCredentialsAreBlocked() {
        for (String href : new String[]{"javascript:alert(1)", "data:text/html,hi", "file:///sdcard/a",
                "content://a", "intent://a", "https://name@vi.wikipedia.org/wiki/A", "%XX"}) {
            assertEquals(href, ArticleLinks.Kind.BLOCKED, ArticleLinks.resolve(base, href).kind);
        }
    }
    @Test public void fullDocumentKeepsBodyButRemovesActiveBaseAndRefresh() {
        com.example.wikipediaclient.data.model.ArticleContent fixture = FixtureArticles.article("Việt Nam");
        String html = ArticleHtml.document(fixture);
        assertTrue(html.contains("Content-Security-Policy"));
        assertTrue(html.contains("overflow-x:auto"));
        assertTrue(html.contains("Việt Nam"));
        assertTrue(html.indexOf("Content-Security-Policy") < html.indexOf("<body>"));
    }
    @Test public void attributedHeadGetsPolicyBeforeOriginalMetadataAndBaseIsRemoved() {
        com.example.wikipediaclient.data.model.ArticleContent fixture = new com.example.wikipediaclient.data.model.ArticleContent(
                FixtureArticles.article("A").article, "<html><head prefix='mw:'><base href='https://evil.test/'><meta http-equiv='refresh' content='0;url=https://evil.test/'></head><body>Content</body></html>",
                1, "CC BY-SA", "https://creativecommons.org/licenses/by-sa/4.0/");
        String html = ArticleHtml.document(fixture);
        assertTrue(html.startsWith("<html><head prefix='mw:'><meta http-equiv=\"Content-Security-Policy\""));
        assertFalse(html.contains("evil.test"));
        assertTrue(html.contains("<body>Content</body>"));
    }}
