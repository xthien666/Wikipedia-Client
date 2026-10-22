package com.example.wikipediaclient;

import static org.junit.Assert.*;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = MainActivityTest.TestApplication.class)
public class MainActivityTest {
    public static class TestApplication extends WikiApplication {
        @Override protected com.example.wikipediaclient.data.AppContainer createContainer() {
            return new com.example.wikipediaclient.data.AppContainer(this,
                    (query, offset, callback) -> () -> {});
        }
    }
    private ActivityController<MainActivity> controller;

    private MainActivity launch() {
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        idle();
        return controller.get();
    }

    private void idle() { Shadows.shadowOf(Looper.getMainLooper()).idle(); }

    private NavController navigation(MainActivity activity) {
        NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        assertNotNull(host);
        return host.getNavController();
    }

    @After public void tearDown() {
        if (controller != null) controller.pause().stop().destroy();
    }

    @Test public void fourTabsOpenAndSearchQuerySurvivesReturning() {
        MainActivity activity = launch();
        assertEquals(R.id.searchFragment, navigation(activity).getCurrentDestination().getId());
        ((EditText) activity.findViewById(R.id.query_input)).setText("Việt Nam");
        BottomNavigationView tabs = activity.findViewById(R.id.bottom_navigation);
        for (int destination : new int[]{R.id.libraryFragment, R.id.historyFragment,
                R.id.settingsFragment, R.id.searchFragment}) {
            tabs.setSelectedItemId(destination);
            idle();
            assertEquals(destination, navigation(activity).getCurrentDestination().getId());
        }
        assertEquals("Việt Nam", ((EditText) activity.findViewById(R.id.query_input)).getText().toString());
    }

    @Test public void articleOpensAndToolbarUpReturnsToQuery() {
        MainActivity activity = launch();
        ((EditText) activity.findViewById(R.id.query_input)).setText("Hà Nội");
        activity.findViewById(R.id.open_sample).performClick();
        idle();
        assertEquals(R.id.articleFragment, navigation(activity).getCurrentDestination().getId());
        assertEquals(View.GONE, activity.findViewById(R.id.bottom_navigation).getVisibility());
        assertEquals(activity.getString(R.string.sample_title),
                ((TextView) activity.findViewById(R.id.article_title)).getText().toString());
        assertTrue(activity.onSupportNavigateUp());
        idle();
        assertEquals(R.id.searchFragment, navigation(activity).getCurrentDestination().getId());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.bottom_navigation).getVisibility());
        assertEquals("Hà Nội", ((EditText) activity.findViewById(R.id.query_input)).getText().toString());
    }

    @Test public void querySurvivesActivityRecreation() {
        MainActivity activity = launch();
        ((EditText) activity.findViewById(R.id.query_input)).setText("Đà Nẵng");
        controller.recreate();
        idle();
        assertEquals("Đà Nẵng", ((EditText) controller.get().findViewById(R.id.query_input))
                .getText().toString());
    }

    @Test public void savedInstanceStateRestoresArticleAndQueryInNewActivity() {
        MainActivity activity = launch();
        ((EditText) activity.findViewById(R.id.query_input)).setText("Lịch sử");
        activity.findViewById(R.id.open_sample).performClick();
        idle();
        Bundle saved = new Bundle();
        controller.pause().saveInstanceState(saved).stop().destroy();
        controller = Robolectric.buildActivity(MainActivity.class).setup(saved);
        idle();
        activity = controller.get();
        assertEquals(R.id.articleFragment, navigation(activity).getCurrentDestination().getId());
        assertEquals(View.GONE, activity.findViewById(R.id.bottom_navigation).getVisibility());
        activity.getOnBackPressedDispatcher().onBackPressed();
        idle();
        assertEquals(R.id.searchFragment, navigation(activity).getCurrentDestination().getId());
        assertEquals("Lịch sử", ((EditText) activity.findViewById(R.id.query_input)).getText().toString());
    }

    @Test @Config(qualifiers = "night")
    public void lightAppKeepsDarkSystemBarIconsWhenDeviceIsInDarkMode() {
        MainActivity activity = launch();
        WindowInsetsControllerCompat bars = new WindowInsetsControllerCompat(
                activity.getWindow(), activity.getWindow().getDecorView());
        assertTrue("Light app requires dark status bar icons", bars.isAppearanceLightStatusBars());
        assertTrue("Light app requires dark navigation bar icons", bars.isAppearanceLightNavigationBars());
    }

    @Test public void readerPreviewNeverSubstitutesFactsFromAnotherArticle() {
        MainActivity activity = launch();
        activity.findViewById(R.id.open_sample).performClick();
        idle();
        String title = ((TextView) activity.findViewById(R.id.article_title)).getText().toString();
        String body = ((TextView) activity.findViewById(R.id.article_body)).getText().toString();
        assertTrue(body.contains(title));
        assertFalse(body.contains("Vịnh Hạ Long"));
        assertFalse(activity.findViewById(R.id.action_save).isEnabled());
        assertFalse(activity.findViewById(R.id.action_listen).isEnabled());
        activity.findViewById(R.id.action_share).performClick();
        android.content.Intent chooser = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(android.content.Intent.ACTION_CHOOSER, chooser.getAction());
        android.content.Intent share = chooser.getParcelableExtra(android.content.Intent.EXTRA_INTENT);
        assertNotNull(share);
        assertEquals(android.content.Intent.ACTION_SEND, share.getAction());
        assertEquals("https://vi.wikipedia.org/wiki/" + android.net.Uri.encode(title),
                share.getStringExtra(android.content.Intent.EXTRA_TEXT));
    }

    @Test public void submittedSearchKeepsQueryHidesSuggestionsAndOpensNoArticle() {
        MainActivity activity = launch();
        EditText input = activity.findViewById(R.id.query_input);
        assertEquals(View.VISIBLE, activity.findViewById(R.id.search_suggestions).getVisibility());
        input.setText("Hà Nội");
        input.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        idle();
        assertEquals(R.id.searchFragment, navigation(activity).getCurrentDestination().getId());
        assertEquals("Hà Nội", input.getText().toString());
        // Whatever the network does (loading, results or error), the sample cards are replaced.
        assertEquals(View.GONE, activity.findViewById(R.id.search_suggestions).getVisibility());
        input.setText("");
        idle();
        assertEquals(View.VISIBLE, activity.findViewById(R.id.search_suggestions).getVisibility());
    }

    /** Verifies an empty real library instead of the removed sample cards. */
    @Test public void emptyLibraryUsesPersistentRepository() throws Exception {
        MainActivity activity = launch();
        ((BottomNavigationView) activity.findViewById(R.id.bottom_navigation)).setSelectedItemId(R.id.libraryFragment);
        idle();
        ((WikiApplication) activity.getApplication()).getContainer().getDiskExecutor()
                .submit(() -> { }).get(10, java.util.concurrent.TimeUnit.SECONDS);
        idle();
        assertEquals(View.VISIBLE, activity.findViewById(R.id.status).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.bookmark_list).getVisibility());
    }

    @Test public void historyShowsResolvedVisitsAndClearRequiresConfirmation() throws Exception {
        MainActivity activity = launch();
        com.example.wikipediaclient.data.repository.HistoryRepository history =
                ((WikiApplication) activity.getApplication()).getContainer().getHistoryRepository();
        history.clear();
        drainDisk(activity);
        history.record(new com.example.wikipediaclient.data.model.HistoryEntry(
                "vi", 10, "Bài A", "https://vi.wikipedia.org/wiki/Bai_A", 1000));
        history.record(new com.example.wikipediaclient.data.model.HistoryEntry(
                "vi", 11, "Bài B", "https://vi.wikipedia.org/wiki/Bai_B", 2000));
        drainDisk(activity);
        assertEquals(2, history.observe().getValue().getEntries().size());
        history.refresh();
        drainDisk(activity);
        assertEquals("Bài B", history.observe().getValue().getEntries().get(0).getTitle());
        ((BottomNavigationView) activity.findViewById(R.id.bottom_navigation)).setSelectedItemId(R.id.historyFragment);
        drainDisk(activity);
        assertEquals(activity.getString(R.string.history_saved_count, 2),
                ((TextView) activity.findViewById(R.id.tv_history_count)).getText().toString());
        androidx.recyclerview.widget.RecyclerView list = activity.findViewById(R.id.history_list);
        assertEquals(2, list.getAdapter().getItemCount());
        assertEquals("Bài B", ((TextView) list.getChildAt(0).findViewById(R.id.history_title))
                .getText().toString());
        list.getChildAt(0).findViewById(R.id.history_open).performClick();
        idle();
        assertEquals(R.id.articleFragment, navigation(activity).getCurrentDestination().getId());
        assertEquals("Bài B", ((TextView) activity.findViewById(R.id.article_title)).getText().toString());
        activity.getOnBackPressedDispatcher().onBackPressed();
        idle();
        assertEquals(R.id.historyFragment, navigation(activity).getCurrentDestination().getId());
        list = activity.findViewById(R.id.history_list);

        activity.findViewById(R.id.btn_clear_history).performClick();
        idle();
        androidx.appcompat.app.AlertDialog dialog = (androidx.appcompat.app.AlertDialog)
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick();
        idle();
        assertEquals(2, list.getAdapter().getItemCount());

        activity.findViewById(R.id.btn_clear_history).performClick();
        idle();
        controller.recreate();
        idle();
        activity = controller.get();
        dialog = (androidx.appcompat.app.AlertDialog) org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertTrue("Confirmation must survive rotation", dialog.isShowing());
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
        drainDisk(activity);
        controller.recreate();
        idle();
        activity = controller.get();
        assertEquals(View.VISIBLE, activity.findViewById(R.id.status).getVisibility());
        assertEquals(0, ((androidx.recyclerview.widget.RecyclerView)
                activity.findViewById(R.id.history_list)).getAdapter().getItemCount());
        assertFalse(activity.findViewById(R.id.btn_clear_history).isEnabled());
    }

    private void drainDisk(MainActivity activity) throws Exception {
        ((WikiApplication) activity.getApplication()).getContainer().getDiskExecutor()
                .submit(() -> {}).get();
        idle();
    }

    @Test public void articleStackRestoresAAfterBAndIgnoresRepeatedOpen() {
        MainActivity activity = launch();
        activity.findViewById(R.id.open_sample).performClick();
        idle();
        String titleA = ((TextView) activity.findViewById(R.id.article_title)).getText().toString();
        NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        androidx.fragment.app.Fragment article = host.getChildFragmentManager().getPrimaryNavigationFragment();
        com.example.wikipediaclient.data.model.ArticleLocator locator =
                new com.example.wikipediaclient.data.model.ArticleLocator("vi", "Bài B", 42L);
        com.example.wikipediaclient.ui.navigation.ArticleNavigator.open(article, locator);
        com.example.wikipediaclient.ui.navigation.ArticleNavigator.open(article, locator);
        com.example.wikipediaclient.ui.navigation.ArticleNavigator.open(article,
                new com.example.wikipediaclient.data.model.ArticleLocator("vi", "Tên khác của B", 42L));
        idle();
        controller.recreate();
        idle();
        activity = controller.get();
        assertEquals("Bài B", ((TextView) activity.findViewById(R.id.article_title)).getText().toString());
        assertEquals(42L, navigation(activity).getCurrentBackStackEntry().getArguments().getLong("pageId"));
        activity.getOnBackPressedDispatcher().onBackPressed();
        idle();
        assertEquals(titleA, ((TextView) activity.findViewById(R.id.article_title)).getText().toString());
        activity.getOnBackPressedDispatcher().onBackPressed();
        idle();
        assertEquals(R.id.searchFragment, navigation(activity).getCurrentDestination().getId());
    }

    @Test public void articlesWithSameTitleButDifferentResolvedIdsKeepSeparateBackEntries() {
        MainActivity activity = launch();
        activity.findViewById(R.id.open_sample).performClick();
        idle();
        NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        androidx.fragment.app.Fragment source = host.getChildFragmentManager().getPrimaryNavigationFragment();
        com.example.wikipediaclient.ui.navigation.ArticleNavigator.open(source,
                new com.example.wikipediaclient.data.model.ArticleLocator("vi", "Same title", 41L));
        idle();
        source = host.getChildFragmentManager().getPrimaryNavigationFragment();
        com.example.wikipediaclient.ui.navigation.ArticleNavigator.open(source,
                new com.example.wikipediaclient.data.model.ArticleLocator("vi", "Same title", 42L));
        idle();
        assertEquals(42L, navigation(activity).getCurrentBackStackEntry().getArguments().getLong("pageId"));
        activity.getOnBackPressedDispatcher().onBackPressed();
        idle();
        assertEquals(41L, navigation(activity).getCurrentBackStackEntry().getArguments().getLong("pageId"));
    }

    @Test public void corruptedHistoryOffersRetryAndConfirmedReset() throws Exception {
        MainActivity activity = launch();
        drainDisk(activity);
        activity.getSharedPreferences("reading_history", android.content.Context.MODE_PRIVATE)
                .edit().putString("entries", "broken-json").commit();
        ((BottomNavigationView) activity.findViewById(R.id.bottom_navigation)).setSelectedItemId(R.id.historyFragment);
        idle();
        drainDisk(activity);
        assertEquals(activity.getString(R.string.history_error_title),
                ((TextView) activity.findViewById(R.id.status_title)).getText().toString());
        activity.findViewById(R.id.status_action).performClick();
        drainDisk(activity);
        assertTrue(activity.findViewById(R.id.btn_clear_history).isEnabled());
        activity.findViewById(R.id.btn_clear_history).performClick();
        idle();
        androidx.appcompat.app.AlertDialog dialog = (androidx.appcompat.app.AlertDialog)
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
        drainDisk(activity);
        assertEquals(activity.getString(R.string.history_empty_title),
                ((TextView) activity.findViewById(R.id.status_title)).getText().toString());
        assertFalse(activity.findViewById(R.id.btn_clear_history).isEnabled());
    }
    @Test public void fontPreferenceSurvivesAColdLaunchAndAppliesToReader() {
        MainActivity activity = launch();
        ((BottomNavigationView) activity.findViewById(R.id.bottom_navigation)).setSelectedItemId(R.id.settingsFragment);
        idle();
        activity.findViewById(R.id.btn_font_huge).performClick();
        assertTrue(activity.findViewById(R.id.btn_font_huge).isSelected());
        assertFalse(activity.findViewById(R.id.btn_clear_cache).isEnabled());
        // Recreate the settings service and the Activity without a saved Bundle.
        assertEquals(22, new com.example.wikipediaclient.data.local.ReaderSettings(activity).getFontSize());
        controller.pause().stop().destroy();
        activity = launch();
        ((BottomNavigationView) activity.findViewById(R.id.bottom_navigation)).setSelectedItemId(R.id.settingsFragment);
        idle();
        assertTrue(activity.findViewById(R.id.btn_font_huge).isSelected());
        ((BottomNavigationView) activity.findViewById(R.id.bottom_navigation)).setSelectedItemId(R.id.searchFragment);
        idle();
        activity.findViewById(R.id.open_sample).performClick();
        idle();
        assertEquals(22 * activity.getResources().getDisplayMetrics().scaledDensity,
                ((TextView) activity.findViewById(R.id.article_body)).getTextSize(), 0.01f);
    }

    @Test public void resizedWindowDoesNotApplyKeyboardPaddingTwice() {
        MainActivity activity = launch();
        // On API 28 IME is encoded in the native system-window bottom inset. The 300px
        // resized window/IME must not also become root padding above the keyboard.
        androidx.core.view.WindowInsetsCompat insets = new androidx.core.view.WindowInsetsCompat.Builder()
                .setSystemWindowInsets(androidx.core.graphics.Insets.of(0, 24, 0, 300)).build();
        assertEquals(300, insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom);
        View root = activity.findViewById(R.id.app_root);
        androidx.core.view.ViewCompat.dispatchApplyWindowInsets(root, insets);
        assertEquals(0, root.getPaddingBottom());
    }
}
