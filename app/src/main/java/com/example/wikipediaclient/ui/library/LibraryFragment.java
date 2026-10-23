package com.example.wikipediaclient.ui.library;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.WikiApplication;
import com.example.wikipediaclient.data.AppContainer;
import com.example.wikipediaclient.data.model.ArticleLocator;
import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.databinding.FragmentLibraryBinding;
import com.example.wikipediaclient.ui.navigation.ArticleNavigator;

/** The real bookmark library; it starts empty until the reader saves an article. */
public final class LibraryFragment extends Fragment implements BookmarkAdapter.Listener {
    private FragmentLibraryBinding binding;
    private LibraryViewModel model;
    private BookmarkAdapter adapter;

    /** Uses the project's library destination and layout. */
    public LibraryFragment() { super(R.layout.fragment_library); }

    /** Connects the list and lifecycle-aware observation to application dependencies. */
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        binding = FragmentLibraryBinding.bind(view);
        AppContainer container = ((WikiApplication) requireActivity().getApplication()).getContainer();
        model = new ViewModelProvider(this, LibraryViewModel.factory(
                container.getBookmarkRepository(), container.getDiskExecutor())).get(LibraryViewModel.class);
        getChildFragmentManager().setFragmentResultListener(
                RemoveBookmarkDialog.RESULT, getViewLifecycleOwner(), (key, result) ->
                        model.remove(RemoveBookmarkDialog.articleFrom(result)));
        adapter = new BookmarkAdapter(this);
        adapter.setStateRestorationPolicy(BookmarkAdapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        binding.bookmarkList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.bookmarkList.setAdapter(adapter);
        binding.chipFavAll.setSelected(true);
        binding.chipFavAll.setText(getString(R.string.bookmark_all, 0));
        binding.chipFavAll.setOnClickListener(v -> model.refresh());
        // Keep the team's filter controls, but do not infer categories from article titles.
        binding.chipFavPlaces.setEnabled(false);
        binding.chipFavScience.setEnabled(false);
        binding.chipFavPlaces.setContentDescription(getString(R.string.bookmark_categories_unavailable));
        binding.chipFavScience.setContentDescription(getString(R.string.bookmark_categories_unavailable));
        model.getState().observe(getViewLifecycleOwner(), this::render);
    }

    /** Refreshes when returning from the reader so saved changes become visible. */
    @Override public void onResume() { super.onResume(); model.refresh(); }

    /** Shows one of loading, error, empty, or populated states. */
    private void render(LibraryViewModel.State state) {
        adapter.submitList(state.entries);
        binding.chipFavAll.setText(getString(R.string.bookmark_all, state.entries.size()));
        binding.bookmarkList.setVisibility(state.entries.isEmpty() ? View.GONE : View.VISIBLE);

        if (state.loading) {
            binding.status.showLoading(R.string.bookmark_loading);
        } else if (state.error) {
            binding.status.showError(R.string.bookmark_error_title, getString(R.string.bookmark_error_message), model::retry);
            ((android.widget.TextView) binding.status.findViewById(R.id.status_action))
                    .setText(R.string.bookmark_retry);
        } else {
            if (state.entries.isEmpty()) {
                binding.status.showEmpty(R.drawable.ic_bookmark, R.string.bookmark_empty_title, R.string.bookmark_empty_message);
            } else {
                binding.status.setVisibility(View.GONE);
                binding.bookmarkList.setVisibility(View.VISIBLE);
            }
        }
    }

    /** Sends the saved ID to the existing reader; opening does not itself record history. */
    @Override public void onOpen(PersonalArticle article) {
        ArticleNavigator.open(this, new ArticleLocator(article.getLanguage(), article.getTitle(), article.getPageId()));
    }

    /** Confirms deletion while keeping the operation on the ViewModel's disk executor. */
    @Override public void onRemove(PersonalArticle article) {
        if (getChildFragmentManager().findFragmentByTag(RemoveBookmarkDialog.TAG) == null) {
            RemoveBookmarkDialog.newInstance(article)
                    .showNow(getChildFragmentManager(), RemoveBookmarkDialog.TAG);
        }
    }

    /** Releases view references when navigation destroys the screen. */
    @Override public void onDestroyView() {
        binding.bookmarkList.setAdapter(null);
        adapter = null;
        binding = null;
        super.onDestroyView();
    }
}
