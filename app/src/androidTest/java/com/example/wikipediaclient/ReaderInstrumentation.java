package com.example.wikipediaclient;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.*;
import android.webkit.*;
import androidx.navigation.fragment.NavHostFragment;
import com.example.wikipediaclient.ui.article.*;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;

/** Real device acceptance runner. No Espresso, JS bridge, debug endpoint or production bypass. */
public final class ReaderInstrumentation extends Instrumentation {
    private final AtomicReference<MainActivity> current = new AtomicReference<>();
    private final JSONArray checks = new JSONArray();
    private File output;
    private String stage = "launch";
    private final Application.ActivityLifecycleCallbacks lifecycle = new Application.ActivityLifecycleCallbacks() {
        public void onActivityCreated(Activity a, Bundle b) { }
        public void onActivityStarted(Activity a) { }
        public void onActivityResumed(Activity a) { if (a instanceof MainActivity) current.set((MainActivity)a); }
        public void onActivityPaused(Activity a) { }
        public void onActivityStopped(Activity a) { }
        public void onActivitySaveInstanceState(Activity a, Bundle b) { }
        public void onActivityDestroyed(Activity a) { }
    };
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    interface Read<T> { T get(); }
    private <T> T ui(Read<T> action) {
        AtomicReference<T> value = new AtomicReference<>();
        runOnMainSync(() -> value.set(action.get())); return value.get();
    }
    private MainActivity activity() { return current.get(); }
    private WebView web() { return activity().findViewById(R.id.article_body); }
    private String title() { return ui(() -> ((android.widget.TextView) activity().findViewById(R.id.article_title)).getText().toString()); }
    private void check(boolean value, String name) throws Exception {
        if (!value) throw new AssertionError(stage + ": " + name);
        checks.put(name); Bundle status = new Bundle(); status.putString("stream", "PASS: " + name + "\n"); sendStatus(0, status);
    }
    private void openArticle(String title) {
        runOnMainSync(() -> {
            Bundle args = new Bundle(); args.putString("title", title); args.putString("language", "vi");
            ((NavHostFragment) activity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment))
                    .getNavController().navigate(R.id.action_open_article, args);
        });
    }
    private void ready() throws Exception {
        long deadline = System.currentTimeMillis() + 45000;
        while (System.currentTimeMillis() < deadline) {
            if (ui(() -> activity() != null && activity().findViewById(R.id.action_share) != null
                    && activity().findViewById(R.id.action_share).isEnabled()
                    && web().getContentHeight() > 1000)) { Thread.sleep(1800); return; }
            Thread.sleep(200);
        }
        throw new AssertionError(stage + ": article did not render (network or WebView error)");
    }
    private void route(String url) {
        runOnMainSync(() -> { if (!web().getWebViewClient().shouldOverrideUrlLoading(web(), url)) web().loadUrl(url); });
        waitForIdleSync();
    }
    private void screenshot(String name) throws Exception {
        Bitmap bitmap = getUiAutomation().takeScreenshot();
        try (FileOutputStream stream = new FileOutputStream(new File(output, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        bitmap.recycle();
    }
    @SuppressWarnings("deprecation") private float progress() {
        return ui(() -> web().getScrollY() / (float)Math.max(1, Math.round(web().getContentHeight() * web().getScale()) - web().getHeight()));
    }
    @SuppressWarnings("deprecation") private boolean withinScreen(float saved) {
        float currentProgress = progress();
        return ui(() -> Math.abs(currentProgress - saved) * Math.max(1,
                Math.round(web().getContentHeight() * web().getScale()) - web().getHeight()) <= web().getHeight() + 2);
    }
    private void back() {
        runOnMainSync(() -> activity().getOnBackPressedDispatcher().onBackPressed()); waitForIdleSync();
    }
    private void drainDisk() throws Exception {
        ((WikiApplication)activity().getApplication()).getContainer().getDiskExecutor().submit(() -> {}).get();
        waitForIdleSync();
    }
    @Override public void onStart() {
        JSONObject report = new JSONObject(); Throwable failure = null;
        try {
            output = new File(getTargetContext().getFilesDir(), "tv3-qa"); output.mkdirs();
            ((Application)getTargetContext().getApplicationContext()).registerActivityLifecycleCallbacks(lifecycle);
            MainActivity launched = (MainActivity)startActivitySync(new Intent(getTargetContext(), MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            current.set(launched); waitForIdleSync();
            stage = "Hà Nội full HTML"; openArticle("Hà Nội"); ready();
            check(title().equals("Hà Nội"), "T06 full Vietnamese article resolves title");
            report.put("contentHeight", ui(() -> web().getContentHeight()));
            report.put("webView", WebView.getCurrentWebViewPackage().versionName);
            report.put("android", Build.VERSION.RELEASE);
            check(ui(() -> !web().getSettings().getJavaScriptEnabled() && !web().getSettings().getAllowFileAccess()
                    && !web().getSettings().getAllowContentAccess()), "WebView active content and local access disabled");
            screenshot("reader-hanoi");
            drainDisk();
            check(((WikiApplication)activity().getApplication()).getContainer().getHistoryRepository()
                    .observe().getValue().getEntries().stream().anyMatch(e -> e.getPageId() == 80), "History records committed canonical page 80");
            stage = "anchor";
            route("https://vi.wikipedia.org/wiki/H%C3%A0_N%E1%BB%99i#cite_note-hanoimoi.com.vn-1");
            Thread.sleep(1000);
            check(title().equals("Hà Nội") && ui(() -> web().getScrollY() > 0), "T06 real citation anchor scrolls within native article entry");
            // Scroll a long article, saving a proportional reading position before navigation.
            runOnMainSync(() -> web().scrollTo(0, 18000));
            Thread.sleep(200); float saved = progress();
            check(saved > 0, "Long article is scrollable");
            stage = "A to B"; route("https://vi.wikipedia.org/wiki/H%E1%BB%87_M%E1%BA%B7t_Tr%E1%BB%9Di"); ready();
            check(title().equals("Hệ Mặt Trời"), "T07 internal link opens full article B");
            drainDisk();
            runOnMainSync(() -> {
                if (!((android.widget.TextView) activity().findViewById(R.id.action_save)).getText().toString().equals("Bỏ yêu thích"))
                    activity().findViewById(R.id.action_save).performClick();
            }); drainDisk();
            List<PersonalArticleEntry> bookmarks = ((WikiApplication)activity().getApplication()).getContainer().getDiskExecutor()
                    .submit(() -> ((WikiApplication)activity().getApplication()).getContainer().getBookmarkRepository().getBookmarks()).get();
            check(bookmarks.stream().anyMatch(e -> e.getArticle().getTitle().equals("Hệ Mặt Trời")), "T07 reader saves canonical B, never A");
            screenshot("reader-solar-system");
            stage = "Back A"; back(); ready();
            check(title().equals("Hà Nội"), "T07 Back restores article A metadata");
            check(withinScreen(saved), "T07 Back restores A reading position");
            screenshot("reader-restored");
            stage = "rotate";
            MainActivity before = activity();
            runOnMainSync(() -> activity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            long deadline = System.currentTimeMillis()+12000;
            while (current.get() == before && System.currentTimeMillis()<deadline) Thread.sleep(100);
            ready();
            check(title().equals("Hà Nội"), "T08 rotation keeps article identity");
            check(withinScreen(saved), "T08 rotation restores reading position");
            screenshot("reader-landscape");
            stage = "external links";
            IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW); filter.addDataScheme("https");
            ActivityMonitor monitor = addMonitor(filter, new ActivityResult(Activity.RESULT_CANCELED, null), true);
            route("https://en.wikipedia.org/wiki/Hanoi");
            route("https://vi.wikipedia.org/wiki/Đặc_biệt:Tìm_kiếm");
            route("https://example.com/");
            check(monitor.getHits() == 3 && title().equals("Hà Nội"), "T16 external/language/namespace links dispatch browser and preserve article");
            removeMonitor(monitor);
            route("javascript:alert(1)");
            check(title().equals("Hà Nội"), "T16 unsupported scheme cannot replace article");
            stage = "rotate portrait";
            runOnMainSync(() -> activity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            Thread.sleep(1500); ready();
            back();
            check(ui(() -> activity().findViewById(R.id.query_input) != null), "T07 second Back returns to search");
            stage = "alias and font";
            runOnMainSync(() -> ((WikiApplication)activity().getApplication()).getContainer().getReaderSettings().setFontSize(22));
            openArticle("Hà Thành"); ready(); drainDisk();
            check(title().equals("Hà Nội"), "T14 live alias resolves to canonical Hà Nội");
            check(ui(() -> web().getSettings().getDefaultFontSize() == 22), "T11 persisted font size applies to real HTML");
            runOnMainSync(() -> {
                if (!((android.widget.TextView)activity().findViewById(R.id.action_save)).getText().toString().equals("Bỏ yêu thích"))
                    activity().findViewById(R.id.action_save).performClick();
            }); drainDisk();
            back(); openArticle("Hà Nội"); ready(); drainDisk();
            bookmarks = ((WikiApplication)activity().getApplication()).getContainer().getDiskExecutor()
                    .submit(() -> ((WikiApplication)activity().getApplication()).getContainer().getBookmarkRepository().getBookmarks()).get();
            check(bookmarks.stream().filter(e -> e.getArticle().getPageId() == 80).count() == 1,
                    "T14 alias and canonical title share one bookmark");
            check(((WikiApplication)activity().getApplication()).getContainer().getHistoryRepository().observe().getValue()
                    .getEntries().stream().filter(e -> e.getPageId() == 80).count() == 1,
                    "T14 alias and canonical title share one history row");
            screenshot("reader-large-font");
            stage = "wide table and broken image fixture";
            AtomicReference<WebView> fixture = new AtomicReference<>();
            java.util.concurrent.CountDownLatch loaded = new java.util.concurrent.CountDownLatch(1);
            runOnMainSync(() -> {
                WebView test = new WebView(activity()); fixture.set(test);
                test.getSettings().setJavaScriptEnabled(false);
                test.setWebViewClient(new WebViewClient() {
                    @Override public void onPageFinished(WebView v, String url) { loaded.countDown(); }
                });
                activity().addContentView(test, new android.view.ViewGroup.LayoutParams(-1, -1));
                String body = "<html><head prefix='test:'></head><body><h1>Fixture kiểm thử TV3</h1>"
                        + "<p>Ảnh lỗi bên dưới không che chữ.</p><img src='data:image/png;base64,broken' alt='Ảnh không tải được'>"
                        + "<table><tr><td style='min-width:1800px'>Bảng rộng cuộn ngang độc lập</td></tr></table>"
                        + "<p>Văn bản sau ảnh lỗi và bảng vẫn đọc được.</p></body></html>";
                com.example.wikipediaclient.data.model.ArticleContent content = new com.example.wikipediaclient.data.model.ArticleContent(
                        new com.example.wikipediaclient.data.model.PersonalArticle("vi", 1, "Fixture", "https://vi.wikipedia.org/wiki/Fixture"),
                        body, 1, "Fixture", "https://creativecommons.org/licenses/by-sa/4.0/");
                test.loadDataWithBaseURL(content.article.getCanonicalUrl(), ArticleHtml.document(content), "text/html", "UTF-8", null);
            });
            check(loaded.await(10, java.util.concurrent.TimeUnit.SECONDS), "T06 adversarial HTML fixture renders");
            Thread.sleep(500);
            check(ui(() -> !fixture.get().canScrollHorizontally(1)), "T06 wide table does not widen article viewport");
            java.util.concurrent.CountDownLatch found = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicInteger matches = new java.util.concurrent.atomic.AtomicInteger();
            runOnMainSync(() -> {
                fixture.get().setFindListener((index, count, done) -> { if (done) { matches.set(count); found.countDown(); } });
                fixture.get().findAllAsync("Văn bản sau ảnh lỗi");
            });
            check(found.await(10, java.util.concurrent.TimeUnit.SECONDS) && matches.get() > 0,
                    "T06 text remains available after broken image and wide table");
            screenshot("reader-adversarial-fixture");
            runOnMainSync(() -> { WebView test = fixture.get(); ((android.view.ViewGroup)test.getParent()).removeView(test); test.destroy(); });
            // Keep the real article open for the host's process-death check.
        } catch (Throwable exception) { failure = exception; }
        try {
            report.put("checks", checks); report.put("result", failure == null ? "PASS" : "FAIL");
            report.put("stage", stage);
            if (failure != null) report.put("failure", android.util.Log.getStackTraceString(failure));
            try (FileWriter writer = new FileWriter(new File(output, "report.json"))) { writer.write(report.toString(2)); }
        } catch (Exception exception) { failure = exception; }
        Bundle result = new Bundle(); result.putString("stream", report.toString());
        finish(failure == null ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
    }
}
