package com.example.wikipediaclient.data.model;

/** Full HTML and resolved metadata travel together; never put this object in a Bundle. */
public final class ArticleContent {
    public final PersonalArticle article;
    public final String html;
    public final long revision;
    public final String licenseTitle;
    public final String licenseUrl;
    public ArticleContent(PersonalArticle article, String html, long revision,
                          String licenseTitle, String licenseUrl) {
        if (html == null || html.trim().isEmpty()) throw new IllegalArgumentException("Empty article HTML");
        this.article = article;
        this.html = html;
        this.revision = revision;
        this.licenseTitle = licenseTitle;
        this.licenseUrl = licenseUrl;
    }
}
