package com.example.wikipediaclient.ui.article;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.wikipediaclient.ui.navigation.ArticleNavigator;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.WikiApplication;
import com.example.wikipediaclient.data.AppContainer;
import com.example.wikipediaclient.data.model.ArticleContent;
import com.example.wikipediaclient.data.remote.WikiApiException;
import com.example.wikipediaclient.databinding.FragmentArticleBinding;
import com.example.wikipediaclient.ui.source.ArticleSourceController;

public final class ArticleFragment extends Fragment {
    private FragmentArticleBinding binding;
    private ArticleViewModel model;
    private ArticleContent rendered;
    private ArticleSourceController sourceActions;
    private boolean displayed, restoring, touched, renderFailed, rendererGone;
    private float restoreProgress;
    public ArticleFragment() { super(R.layout.fragment_article); }
    @android.annotation.SuppressLint("ClickableViewAccessibility") // Touch observation returns false; WebView retains all click/accessibility handling.
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        binding = FragmentArticleBinding.bind(view);
        sourceActions = new ArticleSourceController(requireContext(), binding.actionShare,
                binding.btnViewOriginal, binding.articleLicense);
        renderFailed = false; rendererGone = false;
        AppContainer container = ((WikiApplication) requireActivity().getApplication()).getContainer();
        model = new ViewModelProvider(this, ArticleViewModel.factory(this, requireArguments(), container))
                .get(ArticleViewModel.class);
        binding.articleTitle.setText(requireArguments().getString("title"));
        androidx.core.view.ViewCompat.setAccessibilityHeading(binding.articleTitle, true);
        WebView web = binding.articleBody;
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setTextZoom(Math.round(getResources().getConfiguration().fontScale * 100));
        settings.setDefaultFontSize(container.getReaderSettings().getFontSize());
        settings.setUserAgentString(settings.getUserAgentString() + " WikiRead/0.1");
        web.setBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.surface));
        web.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) { touched = true; restoring = false; }
            return false;
        });
        web.setOnScrollChangeListener((v, x, y, oldX, oldY) -> {
            if (displayed && !restoring) saveProgress();
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return true;
                return route(request.getUrl().toString());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return route(url); }
            @Override public void onPageCommitVisible(WebView view, String url) { committed(view); }
            @Override public void onPageFinished(WebView view, String url) { committed(view); }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel(); // Never bypass TLS errors, including those for subresources.
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && binding != null) renderFailure();
            }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (binding != null) {
                    renderFailure();
                    binding.status.showError(R.string.reader_error_title, getString(R.string.reader_renderer_error), null);
                    ((android.view.ViewGroup) view.getParent()).removeView(view);
                    view.destroy(); rendererGone = true;
                }
                return true;
            }
        });
        binding.actionSave.setOnClickListener(v -> model.toggleBookmark());
        binding.bookmarkRetry.setOnClickListener(v -> model.retryBookmark());
        model.state().observe(getViewLifecycleOwner(), this::render);
        model.start();
    }
    private boolean route(String url) {
        if (!displayed || rendered == null) return true;
        ArticleLinks.Target target = ArticleLinks.resolve(rendered.article.getCanonicalUrl(), url);
        switch (target.kind) {
            case ANCHOR: return false;
            case ARTICLE:
                saveProgress();
                if (target.locator.getTitle().equals(rendered.article.getTitle())) {
                    if (target.anchor != null) binding.articleBody.loadUrl(target.url);
                    return true;
                }
                ArticleNavigator.open(this, target.locator, target.anchor);
                return true;
            case EXTERNAL: open(new Intent(Intent.ACTION_VIEW, Uri.parse(target.url))); return true;
            default: Toast.makeText(requireContext(), R.string.reader_unsupported_url, Toast.LENGTH_SHORT).show(); return true;
        }
    }
    private void render(ArticleViewModel.State state) {
        if (binding == null) return;
        if (state.content == null) {
            rendered = null; displayed = false;
            binding.articleBody.setVisibility(View.GONE);
            binding.articleLicense.setVisibility(View.GONE);
            if (state.loading) binding.status.showLoading(R.string.reader_loading);
            else {
                int status = state.error == null ? -1 : state.error.getStatusCode();
                int message = status == 404 ? R.string.reader_missing
                        : status == 429 || status == 503 ? R.string.reader_rate_limit
                        : state.error != null && state.error.getKind() == WikiApiException.Kind.DATA
                        ? R.string.reader_data_error : R.string.reader_network_error;
                binding.status.showError(R.string.reader_error_title, getString(message),
                        status == 404 || status == 429 || status == 503 ? null : model::retry);
            }
        } else if (rendered != state.content) {
            rendered = state.content; displayed = false; restoring = true; touched = false; renderFailed = false;
            restoreProgress = model.progress();
            binding.articleTitle.setText(rendered.article.getTitle());
            binding.status.showLoading(R.string.reader_loading);
            binding.articleBody.setVisibility(View.VISIBLE);
            binding.articleBody.loadDataWithBaseURL(rendered.article.getCanonicalUrl(), ArticleHtml.document(rendered),
                    "text/html", "UTF-8", null);
        }
        binding.bookmarkRetry.setVisibility(state.bookmarkError && displayed ? View.VISIBLE : View.GONE);
        binding.actionSave.setText(state.bookmarked ? R.string.reader_bookmark_remove : R.string.reader_bookmark_add);
        binding.actionSave.setEnabled(displayed && state.bookmarkReady);
        sourceActions.bind(rendered, displayed);
    }
    private void committed(WebView view) {
        if (binding == null || view != binding.articleBody || rendered == null || displayed || renderFailed) return;
        displayed = true;
        binding.status.setVisibility(View.GONE);
        ArticleNavigator.displayed(this, rendered.article);
        model.displayed(rendered);
        render(model.state().getValue());
        String anchor = requireArguments().getString("anchor");
        if (restoreProgress == 0 && anchor != null && !anchor.isEmpty()) {
            restoring = false;
            view.loadUrl(rendered.article.getCanonicalUrl() + "#" + Uri.encode(anchor));
            return;
        }
        // Reapply after late image layout; never move the reader after the user touches it.
        for (int delay : new int[]{0, 200, 600, 1500}) {
            view.postDelayed(() -> {
                if (binding == null || view != binding.articleBody || touched || !restoring) return;
                int range = scrollRange(view);
                view.scrollTo(0, Math.round(restoreProgress * range));
            }, delay);
        }
        view.postDelayed(() -> { if (binding != null && view == binding.articleBody) restoring = false; }, 1600);
    }
    @SuppressWarnings("deprecation") private int scrollRange(WebView view) {
        return Math.max(0, Math.round(view.getContentHeight() * view.getScale()) - view.getHeight());
    }
    private void saveProgress() {
        if (binding == null || !displayed || restoring) return;
        int range = scrollRange(binding.articleBody);
        model.progress(range > 0 ? binding.articleBody.getScrollY() / (float) range : 0);
    }
    private void renderFailure() {
        displayed = false; renderFailed = true;
        binding.actionSave.setEnabled(false); sourceActions.bind(null, false);
        binding.articleBody.setVisibility(View.GONE);
        binding.status.showError(R.string.reader_error_title, getString(R.string.reader_data_error), model::retry);
    }
    @Override public void onResume() { super.onResume(); if (model != null) model.refreshBookmark(); }
    @Override public void onPause() { saveProgress(); super.onPause(); }
    @Override public void onSaveInstanceState(@NonNull Bundle state) { saveProgress(); super.onSaveInstanceState(state); }
    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException exception) { Toast.makeText(requireContext(), R.string.no_external_app, Toast.LENGTH_SHORT).show(); }
    }
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    @Override public void onDestroyView() {
        saveProgress();
        sourceActions.release(); sourceActions = null;
        WebView web = binding.articleBody;
        if (!rendererGone) {
            web.stopLoading(); web.setOnScrollChangeListener(null); web.setOnTouchListener(null);
            web.setWebViewClient(new WebViewClient());
            ((android.view.ViewGroup) web.getParent()).removeView(web); web.destroy();
        }
        binding = null; rendered = null; displayed = false;
        super.onDestroyView();
    }
}
