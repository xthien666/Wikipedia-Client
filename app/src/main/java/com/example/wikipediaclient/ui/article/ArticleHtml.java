package com.example.wikipediaclient.ui.article;

import com.example.wikipediaclient.data.model.ArticleContent;

/** The API HTML is a complete document. Scripts and active embeds are disabled by CSP and WebSettings. */
public final class ArticleHtml {
    private ArticleHtml() { }
    public static String document(ArticleContent content) {
        String policy = "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; "
                + "img-src https: data:; style-src 'unsafe-inline'; font-src 'none'; form-action 'none'; base-uri 'none'\">";
        String style = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><style>"
                + "html{color:#1b1c1a;background:#fbf9f6;overflow-wrap:anywhere}"
                + "body{font-family:Georgia,'Times New Roman',serif;line-height:1.7;margin:16px auto;padding:0 16px;max-width:70ch}"
                + "a{color:#145fab;text-decoration:underline;text-underline-offset:2px}"
                + "h1,h2,h3{line-height:1.25;color:#00142f;margin:1.5em 0 .6em}"
                + "p{margin:.8em 0} img,video,svg{max-width:100%;height:auto} figure{margin:1em 0}"
                + "table{display:block;max-width:100%!important;width:auto!important;overflow-x:auto;border-collapse:collapse;font-size:.9em}"
                + "td,th{padding:6px;border:1px solid #c4c6cf} .infobox{float:none!important;margin:1em 0!important}"
                + ".mw-editsection,.noprint,script,iframe,object,embed,form{display:none!important}"
                + "pre{overflow-x:auto;white-space:pre} blockquote{margin:1em;padding-left:1em;border-left:3px solid #c4c6cf}"
                + "sup{line-height:0} :target{background:#d5e3ff} ::selection{background:#d5e3ff;color:#00142f}"
                + "</style>";
        String html = content.html.replaceAll("(?is)<base\\b[^>]*>", "")
                .replaceAll("(?is)<meta\\b[^>]*http-equiv\\s*=\\s*['\"]?refresh[^>]*>", "");
        java.util.regex.Matcher head = java.util.regex.Pattern.compile("(?is)<head\\b[^>]*>").matcher(html);
        if (head.find()) return html.substring(0, head.end()) + policy + style + html.substring(head.end());
        return "<!doctype html><html><head>" + policy + style + "</head><body>" + html + "</body></html>";
    }
}
