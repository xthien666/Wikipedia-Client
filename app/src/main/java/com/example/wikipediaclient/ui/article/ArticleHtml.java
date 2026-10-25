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
                + "body{font-family:Georgia,'Times New Roman',serif;line-height:1.7;margin:12px auto 32px;padding:0 16px;max-width:70ch}"
                + "a{color:#145fab;text-decoration:underline;text-decoration-color:rgba(20,95,171,.35);text-underline-offset:3px}"
                + "h1,h2,h3{line-height:1.25;color:#00142f;margin:1.6em 0 .6em}"
                + "h2{font-size:1.4em;padding-bottom:.3em;border-bottom:1px solid #e2e8f0}"
                + "h3{font-family:sans-serif;font-size:1.05em;letter-spacing:.01em}"
                + "p{margin:.8em 0} img,video,svg{max-width:100%;height:auto} img{border-radius:8px} figure{margin:1.2em 0}"
                + "figcaption,.thumbcaption,.gallerytext{font-family:sans-serif;font-size:.82em;line-height:1.45;color:#44474e;margin-top:.4em}"
                + ".hatnote,.dablink{font-family:sans-serif;font-size:.85em;color:#44474e;font-style:italic;margin:.4em 0 1em}"
                + "table{display:block;max-width:100%!important;width:auto!important;overflow-x:auto;border-collapse:collapse;"
                + "font-family:sans-serif;font-size:.85em;line-height:1.45;background:#fff;border:1px solid #e2e8f0;border-radius:12px}"
                + "td,th{padding:8px 10px;border:1px solid #edf0f4;vertical-align:top} th{background:#f5f3f0;color:#00142f;text-align:left}"
                + "caption{font-family:Georgia,serif;font-weight:bold;font-size:1.15em;color:#00142f;padding:.6em}"
                + ".infobox{float:none!important;margin:1.2em 0!important;border-radius:16px;box-shadow:0 1px 3px rgba(0,20,47,.06)}"
                + ".mw-editsection,.noprint,script,iframe,object,embed,form{display:none!important}"
                + "pre{overflow-x:auto;white-space:pre}"
                + "blockquote{margin:1.2em 0;padding:.8em 1em;background:#e8eef8;border-left:4px solid #145fab;border-radius:0 12px 12px 0;color:#00407a}"
                + "sup{line-height:0} sup a{text-decoration:none} :target{background:#d5e3ff} ::selection{background:#d5e3ff;color:#00142f}"
                + "</style>";
        String html = content.html.replaceAll("(?is)<base\\b[^>]*>", "")
                .replaceAll("(?is)<meta\\b[^>]*http-equiv\\s*=\\s*['\"]?refresh[^>]*>", "");
        java.util.regex.Matcher head = java.util.regex.Pattern.compile("(?is)<head\\b[^>]*>").matcher(html);
        if (head.find()) return html.substring(0, head.end()) + policy + style + html.substring(head.end());
        return "<!doctype html><html><head>" + policy + style + "</head><body>" + html + "</body></html>";
    }
}
