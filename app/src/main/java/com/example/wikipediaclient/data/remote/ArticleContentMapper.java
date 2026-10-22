package com.example.wikipediaclient.data.remote;

import android.net.Uri;
import com.example.wikipediaclient.data.model.ArticleContent;
import com.example.wikipediaclient.data.model.PersonalArticle;
import org.json.JSONException;
import org.json.JSONObject;

public final class ArticleContentMapper {
    private ArticleContentMapper() { }
    public static ArticleContent parse(JSONObject json) throws JSONException {
        if (json.has("error") || json.has("errors")) throw new JSONException("API error");
        JSONObject license = json.getJSONObject("license");
        String key = json.getString("key");
        String url = new Uri.Builder().scheme("https").authority("vi.wikipedia.org")
                .appendPath("wiki").appendPath(key).build().toString();
        String licenseUrl = license.getString("url");
        Uri licenseUri = Uri.parse(licenseUrl);
        if (!"https".equals(licenseUri.getScheme()) || !"creativecommons.org".equals(licenseUri.getHost())) {
            throw new JSONException("Invalid license URL");
        }
        return new ArticleContent(new PersonalArticle("vi", json.getLong("id"),
                json.getString("title"), url), json.getString("html"),
                json.getJSONObject("latest").getLong("id"), license.getString("title"), licenseUrl);
    }
}
