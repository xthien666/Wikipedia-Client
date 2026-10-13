package com.example.wikipediaclient.ui.navigation;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.data.model.ArticleLocator;
public final class ArticleNavigator {
    private ArticleNavigator() {}
    public static void open(Fragment source, ArticleLocator locator) {
        Bundle args = new Bundle();
        args.putString("title", locator.getTitle());
        args.putString("language", locator.getLanguage());
        if (locator.getPageId() != null) args.putLong("pageId", locator.getPageId());
        NavHostFragment.findNavController(source).navigate(R.id.action_open_article, args);
    }
}
