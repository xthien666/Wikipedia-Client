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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.data.model.ArticleLocator;
import com.example.wikipediaclient.databinding.FragmentSearchBinding;
import com.example.wikipediaclient.ui.navigation.ArticleNavigator;

public final class SearchFragment extends Fragment {
    private FragmentSearchBinding binding;
    private TextWatcher watcher;

    public SearchFragment() {
        super(R.layout.fragment_search);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        binding = FragmentSearchBinding.bind(view);
        SearchViewModel model = new ViewModelProvider(this).get(SearchViewModel.class);

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
                    Toast.makeText(requireContext(), R.string.search_pending, Toast.LENGTH_SHORT).show();
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
    }

    @Override
    public void onDestroyView() {
        if (binding != null && watcher != null) {
            binding.queryInput.removeTextChangedListener(watcher);
        }
        watcher = null;
        binding = null;
        super.onDestroyView();
    }
}
