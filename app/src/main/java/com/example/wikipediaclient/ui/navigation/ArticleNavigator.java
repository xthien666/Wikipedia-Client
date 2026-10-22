package com.example.wikipediaclient.ui.navigation;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.NavController;
import java.util.Objects;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.data.model.ArticleLocator;
public final class ArticleNavigator {
    private ArticleNavigator() {}
    public static void open(Fragment source, ArticleLocator locator) {
        NavController controller = NavHostFragment.findNavController(source);
        Bundle current = controller.getCurrentBackStackEntry() == null ? null
                : controller.getCurrentBackStackEntry().getArguments();
        if (controller.getCurrentDestination() != null
                && controller.getCurrentDestination().getId() == R.id.articleFragment
                && current != null
                && Objects.equals(current.getString("language"), locator.getLanguage())
                && sameArticle(current, locator)) {
            return; // Ignore repeated taps that would push the same article twice.
        }
        Bundle args = new Bundle();
        args.putString("title", locator.getTitle());
        args.putString("language", locator.getLanguage());
        if (locator.getPageId() != null) args.putLong("pageId", locator.getPageId());
        controller.navigate(R.id.action_open_article, args);
    }

    private static boolean sameArticle(Bundle current, ArticleLocator locator) {
        long currentId = current.getLong("pageId", 0L);
        Long requestedId = locator.getPageId();
        if (currentId > 0 && requestedId != null) return currentId == requestedId;
        return currentId == 0 && requestedId == null
                && Objects.equals(current.getString("title"), locator.getTitle());
    }
}
