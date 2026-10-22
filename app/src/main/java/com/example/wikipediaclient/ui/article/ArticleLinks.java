package com.example.wikipediaclient.ui.article;

import android.net.Uri;
import java.net.URI;
import java.util.Locale;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import com.example.wikipediaclient.data.model.ArticleLocator;

/** Explicit navigation policy: WebView owns only fragments in the current document. */
public final class ArticleLinks {
    public enum Kind { ANCHOR, ARTICLE, EXTERNAL, BLOCKED }
    public static final class Target {
        public final Kind kind;
        public final String url;
        public final ArticleLocator locator;
        public final String anchor;
        Target(Kind kind, String url, ArticleLocator locator, String anchor) {
            this.kind = kind; this.url = url; this.locator = locator; this.anchor = anchor;
        }
    }
    private static final Set<String> NAMESPACES = new HashSet<>(Arrays.asList(
            "special", "media", "talk", "user", "user talk", "wikipedia", "wikipedia talk", "file", "file talk",
            "mediawiki", "mediawiki talk", "template", "template talk", "help", "help talk", "category", "category talk",
            "portal", "portal talk", "module", "module talk", "timedtext", "gadget", "gadget definition",
            "đặc biệt", "phương tiện", "thảo luận", "thành viên", "người dùng", "thảo luận thành viên",
            "thảo luận wikipedia", "tập tin", "hình", "thảo luận tập tin", "bản mẫu", "thảo luận bản mẫu",
            "trợ giúp", "thảo luận trợ giúp", "thể loại", "thảo luận thể loại", "chủ đề", "thảo luận chủ đề",
            "mô đun", "mô-đun", "thảo luận mô đun", "thảo luận mediawiki"));
    private static boolean functionalTitle(String title) {
        int colon = title.indexOf(':');
        if (colon < 0) return false;
        String prefix = title.substring(0, colon).trim().toLowerCase(Locale.ROOT);
        return NAMESPACES.contains(prefix) || prefix.matches("[a-z-]{2,12}");
    }
    private ArticleLinks() { }
    public static Target resolve(String base, String href) {
        try {
            URI resolved = new URI(base).resolve(href).normalize();
            String scheme = resolved.getScheme();
            if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    || resolved.getHost() == null || resolved.getRawUserInfo() != null
                    || (resolved.getPort() != -1 && resolved.getPort() != 443 && resolved.getPort() != 80)) {
                return new Target(Kind.BLOCKED, href, null, null);
            }
            Uri uri = Uri.parse(resolved.toASCIIString());
            Uri current = Uri.parse(base);
            if (uri.getFragment() != null && uri.buildUpon().fragment(null).build().toString()
                    .equals(current.buildUpon().fragment(null).build().toString())) {
                return new Target(Kind.ANCHOR, uri.toString(), null, uri.getFragment());
            }
            String path = uri.getPath();
            if ("https".equalsIgnoreCase(scheme) && "vi.wikipedia.org".equalsIgnoreCase(uri.getHost())
                    && resolved.getPort() == -1 && uri.getQuery() == null && path != null
                    && path.startsWith("/wiki/")) {
                String title = path.substring(6).replace('_', ' ').trim();
                // Namespaces and interwiki prefixes belong in a browser, never in reader state.
                if (!title.isEmpty() && !functionalTitle(title)) {
                    return new Target(Kind.ARTICLE, uri.toString(), new ArticleLocator("vi", title, null), uri.getFragment());
                }
            }
            return new Target(Kind.EXTERNAL, uri.toString(), null, null);
        } catch (Exception exception) { return new Target(Kind.BLOCKED, href, null, null); }
    }
}
