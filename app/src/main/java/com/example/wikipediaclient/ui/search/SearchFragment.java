package com.example.wikipediaclient.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.WikiApplication;
import com.example.wikipediaclient.data.model.ArticleLocator;
import com.example.wikipediaclient.data.remote.WikiApiException;
import com.example.wikipediaclient.databinding.FragmentSearchBinding;
import com.example.wikipediaclient.ui.navigation.ArticleNavigator;
import com.google.android.material.snackbar.Snackbar;

public final class SearchFragment extends Fragment {
    private FragmentSearchBinding binding;
    private TextWatcher watcher;
    private SearchViewModel model;
    private SearchResultAdapter adapter;
    @Nullable private Snackbar loadMoreSnackbar;

    public SearchFragment() {
        super(R.layout.fragment_search);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        binding = FragmentSearchBinding.bind(view);
        WikiApplication app = (WikiApplication) requireContext().getApplicationContext();
        model = new ViewModelProvider(this, new SearchViewModelFactory(app.getContainer()))
                .get(SearchViewModel.class);

        setUpResults();

        binding.queryInput.setText(model.getQuery());
        watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                model.setQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable editable) {}
        };
        binding.queryInput.addTextChangedListener(watcher);

        // Search submit on keyboard
        binding.queryInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_UP)) {
                String query = binding.queryInput.getText() != null ?
                        binding.queryInput.getText().toString().trim() : "";
                if (!query.isEmpty()) {
                    model.submit();
                    return true;
                }
            }
            return false;
        });

        // Clear search button
        binding.btnClearSearch.setOnClickListener(v -> binding.queryInput.setText(""));

        // Voice search placeholder
        binding.btnVoiceSearch.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.voice_search_pending, Toast.LENGTH_SHORT).show());

        // Topic chips click
        View.OnClickListener chipListener = v -> {
            if (v.getId() == R.id.chip_history) {
                binding.queryInput.setText(R.string.query_history);
            } else if (v.getId() == R.id.chip_places) {
                binding.queryInput.setText(R.string.query_places);
            } else if (v.getId() == R.id.chip_science) {
                binding.queryInput.setText(R.string.query_science);
            } else if (v.getId() == R.id.chip_arts) {
                binding.queryInput.setText(R.string.query_arts);
            } else if (v.getId() == R.id.chip_nature) {
                binding.queryInput.setText(R.string.query_nature);
            }
        };
        binding.chipHistory.setOnClickListener(chipListener);
        binding.chipPlaces.setOnClickListener(chipListener);
        binding.chipScience.setOnClickListener(chipListener);
        binding.chipArts.setOnClickListener(chipListener);
        binding.chipNature.setOnClickListener(chipListener);

        // Featured card and sample button
        binding.openSample.setOnClickListener(v -> ArticleNavigator.open(
                this, new ArticleLocator("vi", getString(R.string.featured_title), null)));
        binding.cardFeatured.setOnClickListener(v -> ArticleNavigator.open(
                this, new ArticleLocator("vi", getString(R.string.featured_title), null)));

        // Suggested article cards
        binding.cardArticle1.setOnClickListener(v -> ArticleNavigator.open(
                this, new ArticleLocator("vi", getString(R.string.card_1_title), null)));
        binding.cardArticle2.setOnClickListener(v -> ArticleNavigator.open(
                this, new ArticleLocator("vi", getString(R.string.card_2_title), null)));
        binding.cardArticle3.setOnClickListener(v -> ArticleNavigator.open(
                this, new ArticleLocator("vi", getString(R.string.card_3_title), null)));

        model.getState().observe(getViewLifecycleOwner(), this::render);
    }

    private void setUpResults() {
        adapter = new SearchResultAdapter(result -> ArticleNavigator.open(
                this, new ArticleLocator("vi", result.getTitle(), result.getPageId())));
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.searchResults.setLayoutManager(layoutManager);
        binding.searchResults.setAdapter(adapter);
        binding.btnLoadMore.setOnClickListener(v -> {
            SearchUiState ui = model.getState().getValue();
            if (ui != null && ui.isLoadMoreFailed()) model.retryLoadMore();
            else model.loadMore();
        });
    }

    private void render(SearchUiState ui) {
        boolean showSuggestions = ui.getType() == SearchUiState.Type.IDLE;
        binding.searchSuggestions.setVisibility(showSuggestions ? View.VISIBLE : View.GONE);
        binding.searchResults.setVisibility(ui.getType() == SearchUiState.Type.SUCCESS ? View.VISIBLE : View.GONE);
        binding.btnLoadMore.setVisibility(ui.getType() == SearchUiState.Type.SUCCESS && ui.hasMore()
                ? View.VISIBLE : View.GONE);
        binding.btnLoadMore.setEnabled(!ui.isLoadingMore());
        binding.btnLoadMore.setText(ui.isLoadingMore() ? R.string.search_loading
                : ui.isLoadMoreFailed() ? R.string.retry : R.string.search_load_more);
        if (loadMoreSnackbar != null && !(ui.getType() == SearchUiState.Type.SUCCESS && ui.isLoadMoreFailed())) {
            loadMoreSnackbar.dismiss();
            loadMoreSnackbar = null;
        }

        switch (ui.getType()) {
            case LOADING:
                binding.status.showLoading(R.string.search_loading);
                break;
            case EMPTY:
                binding.status.showEmpty(R.drawable.ic_search,
                        R.string.search_empty_title, R.string.search_empty_message);
                break;
            case ERROR:
                binding.status.showError(R.string.search_error_title,
                        getString(errorMessage(ui.getErrorKind())), model::retry);
                break;
            case SUCCESS:
                binding.status.setVisibility(View.GONE);
                adapter.submitList(ui.getResults());
                if (ui.isLoadMoreFailed() && loadMoreSnackbar == null) {
                    loadMoreSnackbar = Snackbar.make(binding.getRoot(),
                            R.string.search_load_more_failed, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.retry, v -> model.retryLoadMore());
                    loadMoreSnackbar.show();
                }
                break;
            case IDLE:
            default:
                binding.status.setVisibility(View.GONE);
                adapter.submitList(null);
                break;
        }
    }

    @StringRes
    private static int errorMessage(@Nullable WikiApiException.Kind kind) {
        if (kind == null) return R.string.search_error_unknown;
        switch (kind) {
            case NO_CONNECTION: return R.string.search_error_no_connection;
            case TIMEOUT: return R.string.search_error_timeout;
            case SERVER: return R.string.search_error_server;
            case DATA: return R.string.search_error_data;
            default: return R.string.search_error_unknown;
        }
    }

    @Override
    public void onDestroyView() {
        if (loadMoreSnackbar != null) loadMoreSnackbar.dismiss();
        loadMoreSnackbar = null;
        if (binding != null && watcher != null) {
            binding.queryInput.removeTextChangedListener(watcher);
        }
        watcher = null;
        binding = null;
        super.onDestroyView();
    }
}
