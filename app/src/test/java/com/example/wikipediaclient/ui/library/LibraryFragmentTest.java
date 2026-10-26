package com.example.wikipediaclient.ui.library;

import static org.junit.Assert.*;
import android.content.DialogInterface;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.wikipediaclient.MainActivity;
import com.example.wikipediaclient.MainActivityTest;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.WikiApplication;
import com.example.wikipediaclient.data.AppContainer;
import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import com.example.wikipediaclient.data.repository.BookmarkRepository;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = MainActivityTest.TestApplication.class)
public class LibraryFragmentTest {
    private ActivityController<MainActivity> controller;
    private AppContainer container;
    private final PersonalArticle article = new PersonalArticle("vi", 123, "Hà Nội", "https://vi.wikipedia.org/wiki/Hà_Nội");

    /** Seeds real SQLite storage; no sample content is added by production code. */
    @Before public void setup() throws Exception {
        container = ((WikiApplication) RuntimeEnvironment.getApplication()).getContainer();
        container.getDiskExecutor().submit(() -> container.getBookmarkRepository()
                .saveBookmark(article, 100)).get(10, TimeUnit.SECONDS);
    }

    /** Waits for disk and UI callbacks without arbitrary sleeps. */
    private void settle() throws Exception {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        container.getDiskExecutor().submit(() -> { }).get(10, TimeUnit.SECONDS);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    /** Opens the actual library destination in the application's activity. */
    private MainActivity launchLibrary() throws Exception {
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        ((BottomNavigationView) controller.get().findViewById(R.id.bottom_navigation))
                .setSelectedItemId(R.id.libraryFragment);
        settle();
        return controller.get();
    }

    /** Returns the current navigation host for assertions. */
    private NavHostFragment host() {
        return (NavHostFragment) controller.get().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
    }

    /** Builds a real bound row so button actions use the production adapter listeners. */
    private BookmarkAdapter.Holder row() {
        RecyclerView list = controller.get().findViewById(R.id.bookmark_list);
        BookmarkAdapter adapter = (BookmarkAdapter) list.getAdapter();
        assertEquals(1, adapter.getItemCount());
        BookmarkAdapter.Holder holder = adapter.onCreateViewHolder(list, 0);
        adapter.onBindViewHolder(holder, 0);
        return holder;
    }

    /** A saved row survives recreation and opens the matching ID, title, and language. */
    @Test public void savedArticleSurvivesRecreationAndOpensCorrectIdentity() throws Exception {
        launchLibrary();
        assertEquals("Hà Nội", row().binding.bookmarkTitle.getText().toString());
        controller.recreate(); settle();
        row().binding.bookmarkOpen.performClick(); settle();
        NavController nav = host().getNavController();
        assertEquals(R.id.articleFragment, nav.getCurrentDestination().getId());
        assertEquals(123, nav.getCurrentBackStackEntry().getArguments().getLong("pageId"));
        assertEquals("Hà Nội", nav.getCurrentBackStackEntry().getArguments().getString("title"));
        assertEquals("vi", nav.getCurrentBackStackEntry().getArguments().getString("language"));
    }

    /** Cancel keeps data; confirmation removes it and the screen stays empty on recreation. */
    @Test public void removalRequiresConfirmationAndPersists() throws Exception {
        launchLibrary();
        row().binding.bookmarkRemove.performClick(); settle();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick(); settle();
        assertEquals("Hà Nội", row().binding.bookmarkTitle.getText().toString());
        row().binding.bookmarkRemove.performClick(); settle();
        dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick(); settle();
        assertEquals(View.VISIBLE, controller.get().findViewById(R.id.status).getVisibility());
        assertFalse(container.getDiskExecutor().submit(() -> container.getBookmarkRepository()
                .isBookmarked("vi", 123)).get(10, TimeUnit.SECONDS));
        controller.recreate(); settle();
        assertEquals(View.GONE, controller.get().findViewById(R.id.bookmark_list).getVisibility());
    }

    /** Rotation preserves the confirmation; cancel never deletes the selected row. */
    @Test public void confirmationSurvivesRecreationAndCanBeCancelled() throws Exception {
        launchLibrary();
        row().binding.bookmarkRemove.performClick(); settle();
        controller.recreate(); settle();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertTrue(dialog.isShowing());
        assertTrue(container.getDiskExecutor().submit(() -> container.getBookmarkRepository()
                .isBookmarked("vi", 123)).get(10, TimeUnit.SECONDS));
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick(); settle();
        assertEquals("Hà Nội", row().binding.bookmarkTitle.getText().toString());
    }

    /** Restored confirmation deletes its original ID even when another article is saved. */
    @Test public void restoredConfirmationDeletesOnlySelectedArticle() throws Exception {
        launchLibrary();
        row().binding.bookmarkRemove.performClick(); settle();
        PersonalArticle other = new PersonalArticle("vi", 456, "Other", "https://vi.wikipedia.org/wiki/Other");
        container.getDiskExecutor().submit(() -> container.getBookmarkRepository()
                .saveBookmark(other, 200)).get(10, TimeUnit.SECONDS);
        controller.recreate(); settle();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertTrue(dialog.isShowing());
        assertTrue(container.getDiskExecutor().submit(() -> container.getBookmarkRepository()
                .isBookmarked("vi", 123)).get(10, TimeUnit.SECONDS));
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick(); settle();
        assertFalse(container.getDiskExecutor().submit(() -> container.getBookmarkRepository()
                .isBookmarked("vi", 123)).get(10, TimeUnit.SECONDS));
        assertEquals("Other", row().binding.bookmarkTitle.getText().toString());
        controller.recreate(); settle();
        assertEquals("Other", row().binding.bookmarkTitle.getText().toString());
    }

    /** A foreground refresh cannot lose a pending deletion error or hide known rows. */
    @Test public void failedDeleteSurvivesBackgroundAndForeground() throws Exception {
        launchLibrary();
        LibraryFragment fragment = (LibraryFragment) host().getChildFragmentManager().getPrimaryNavigationFragment();
        final boolean[] fail = {true};
        BookmarkRepository actual = container.getBookmarkRepository();
        BookmarkRepository failing =
                new BookmarkRepository() {
                    /** Reads real saved rows. */
                    @Override public List<PersonalArticleEntry> getBookmarks() {
                        return actual.getBookmarks();
                    }
                    /** Checks the real database. */
                    @Override public boolean isBookmarked(String language, long id) { return actual.isBookmarked(language, id); }
                    /** Saves through the normal repository. */
                    @Override public void saveBookmark(PersonalArticle value, long time) { actual.saveBookmark(value, time); }
                    /** Fails only the deletion until the test enables writes. */
                    @Override public void removeBookmark(String language, long id) {
                        if (fail[0]) throw new IllegalStateException("Write failed");
                        actual.removeBookmark(language, id);
                    }
                };
        LibraryViewModel model = new LibraryViewModel(failing, container.getDiskExecutor());
        fragment.getViewModelStore().put("androidx.lifecycle.ViewModelProvider.DefaultKey:" + LibraryViewModel.class.getCanonicalName(), model);
        // Recreate the view to bind the injected model through the normal factory lookup.
        host().getChildFragmentManager().beginTransaction().detach(fragment).commitNow();
        host().getChildFragmentManager().beginTransaction().attach(fragment).commitNow();
        settle();
        row().binding.bookmarkRemove.performClick(); settle();
        ((AlertDialog) ShadowDialog.getLatestDialog()).getButton(DialogInterface.BUTTON_POSITIVE).performClick(); settle();
        controller.pause().stop();
        controller.start().resume(); settle();
        assertTrue(model.getState().getValue().error);
        assertEquals(View.VISIBLE, controller.get().findViewById(R.id.bookmark_list).getVisibility());
        fail[0] = false;
        controller.get().findViewById(R.id.status_action).performClick(); settle();
        assertFalse(actualOnDisk());
    }

    /** Checks persistent membership using the application's worker. */
    private boolean actualOnDisk() throws Exception {
        return container.getDiskExecutor().submit(() -> container.getBookmarkRepository()
                .isBookmarked("vi", 123)).get(10, TimeUnit.SECONDS);
    }

    /** Destroys the activity and drains pending work before the test environment is reset. */
    @After public void cleanup() throws Exception {
        if (controller != null) controller.pause().stop().destroy();
        settle();
    }
}
