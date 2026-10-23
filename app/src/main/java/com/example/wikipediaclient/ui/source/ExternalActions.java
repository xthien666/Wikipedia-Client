package com.example.wikipediaclient.ui.source;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import com.example.wikipediaclient.R;

/** TV5 opens the browser or Sharesheet; choosing a recipient remains a user action. */
public final class ExternalActions {
    private ExternalActions() { }
    public static boolean isHttps(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        Uri uri = Uri.parse(value);
        return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                && !uri.getHost().isEmpty() && uri.getUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443);
    }
    public static boolean isArticleUrl(String value) {
        return isHttps(value) && "vi.wikipedia.org".equalsIgnoreCase(Uri.parse(value).getHost())
                && Uri.parse(value).getPath() != null && Uri.parse(value).getPath().startsWith("/wiki/");
    }
    public static void openUrl(Context context, String value) {
        if (!isHttps(value)) {
            Toast.makeText(context, R.string.reader_unsupported_url, Toast.LENGTH_SHORT).show();
            return;
        }
        open(context, new Intent(Intent.ACTION_VIEW, Uri.parse(value)));
    }
    public static void share(Context context, String title, String canonicalUrl) {
        if (!isArticleUrl(canonicalUrl)) return;
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, canonicalUrl).putExtra(Intent.EXTRA_SUBJECT, title);
        open(context, Intent.createChooser(send, context.getString(R.string.share_article)));
    }
    private static void open(Context context, Intent intent) {
        if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { context.startActivity(intent); }
        catch (ActivityNotFoundException error) {
            Toast.makeText(context, R.string.no_external_app, Toast.LENGTH_SHORT).show();
        }
    }
}
