package com.example.wikipediaclient.ui.navigation;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.NavController;
import java.util.Objects;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.data.model.ArticleLocator;
import com.example.wikipediaclient.data.model.PersonalArticle;
import androidx.lifecycle.SavedStateHandle;
public final class ArticleNavigator {
    private ArticleNavigator() {}
    public static void open(Fragment source, ArticleLocator locator) {
        open(source, locator, null);
    }
    public static void open(Fragment source, ArticleLocator locator, String anchor) {
        NavController controller = NavHostFragment.findNavController(source);
        Bundle current = controller.getCurrentBackStackEntry() == null ? null
                : controller.getCurrentBackStackEntry().getArguments();
        if (controller.getCurrentDestination() != null
                && controller.getCurrentDestination().getId() == R.id.articleFragment
                && current != null
                && Objects.equals(current.getString("language"), locator.getLanguage())
                && sameArticle(current, controller.getCurrentBackStackEntry().getSavedStateHandle(), locator)) {
            return; // Ignore repeated taps that would push the same article twice.
        }
        Bundle args = new Bundle();
        args.putString("title", locator.getTitle());
        args.putString("language", locator.getLanguage());
        args.putString("anchor", anchor);
        if (locator.getPageId() != null) args.putLong("pageId", locator.getPageId());
        controller.navigate(R.id.action_open_article, args);
    }

    /** Records only the identity of the article whose HTML is now displayed on this entry. */
    public static void displayed(Fragment source, PersonalArticle article) {
        if (source.getParentFragmentManager().getPrimaryNavigationFragment() != source) return;
        NavController controller = NavHostFragment.findNavController(source);
        if (controller.getCurrentDestination() == null || controller.getCurrentDestination().getId() != R.id.articleFragment) return;
        SavedStateHandle state = controller.getCurrentBackStackEntry().getSavedStateHandle();
        state.set("resolvedLanguage", article.getLanguage());
        state.set("resolvedPageId", article.getPageId());
        state.set("resolvedTitle", article.getTitle());
    }

    private static boolean sameArticle(Bundle current, SavedStateHandle resolved, ArticleLocator locator) {
        Long resolvedId = resolved.get("resolvedPageId");
        if (Objects.equals(resolved.get("resolvedLanguage"), locator.getLanguage()) && resolvedId != null) {
            if (locator.getPageId() != null) return resolvedId.equals(locator.getPageId());
            return Objects.equals(resolved.get("resolvedTitle"), locator.getTitle());
        }
        long currentId = current.getLong("pageId", 0L);
        Long requestedId = locator.getPageId();
        if (currentId > 0 && requestedId != null) return currentId == requestedId;
        return currentId == 0 && requestedId == null
                && Objects.equals(current.getString("title"), locator.getTitle());
    }
}
