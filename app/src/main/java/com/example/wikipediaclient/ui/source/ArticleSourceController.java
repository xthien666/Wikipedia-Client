package com.example.wikipediaclient.ui.source;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.data.model.ArticleContent;

/** View-lifecycle TV5 component. TV3 supplies the exact metadata currently committed on screen. */
public final class ArticleSourceController {
    private final Context context;
    private final View share, original;
    private final TextView license;
    @Nullable private ArticleContent current;
    public ArticleSourceController(Context context, View share, View original, TextView license) {
        this.context = context; this.share = share; this.original = original; this.license = license;
        share.setOnClickListener(v -> {
            if (current != null) ExternalActions.share(context, current.article.getTitle(), current.article.getCanonicalUrl());
        });
        original.setOnClickListener(v -> {
            if (current != null) ExternalActions.openUrl(context, current.article.getCanonicalUrl());
        });
        license.setOnClickListener(v -> {
            if (current != null && ExternalActions.isHttps(current.licenseUrl)) ExternalActions.openUrl(context, current.licenseUrl);
        });
        ViewCompat.setAccessibilityDelegate(license, new AccessibilityDelegateCompat() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                if (host.isClickable()) info.setClassName(android.widget.Button.class.getName());
            }
        });
        bind(null, false);
    }
    /** Every state change is atomic: pending/error content must not retain an older article's actions. */
    public void bind(@Nullable ArticleContent content, boolean committed) {
        current = committed && content != null && content.article != null
                && ExternalActions.isArticleUrl(content.article.getCanonicalUrl()) ? content : null;
        share.setEnabled(current != null); original.setEnabled(current != null);
        license.setVisibility(current != null ? View.VISIBLE : View.GONE);
        boolean linked = current != null && ExternalActions.isHttps(current.licenseUrl);
        license.setClickable(linked); license.setFocusable(linked);
        if (current == null) {
            license.setText(""); license.setContentDescription(null);
            share.setContentDescription(context.getString(R.string.share_article));
            original.setContentDescription(context.getString(R.string.reader_view_original));
            return;
        }
        String name = current.licenseTitle == null || current.licenseTitle.trim().isEmpty()
                ? context.getString(R.string.source_license_unknown) : current.licenseTitle;
        license.setText(context.getString(R.string.reader_license_format, name, current.revision));
        license.setContentDescription(context.getString(linked ? R.string.source_license_accessible
                : R.string.source_license_accessible_unlinked, current.article.getTitle(), name, current.revision));
        share.setContentDescription(context.getString(R.string.source_share_accessible, current.article.getTitle()));
        original.setContentDescription(context.getString(R.string.source_original_accessible, current.article.getTitle()));
    }
    public void release() {
        bind(null, false);
        share.setOnClickListener(null); original.setOnClickListener(null); license.setOnClickListener(null);
    }
}
