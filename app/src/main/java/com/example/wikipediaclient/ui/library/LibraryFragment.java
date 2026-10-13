package com.example.wikipediaclient.ui.library;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.data.model.ArticleLocator;
import com.example.wikipediaclient.databinding.FragmentLibraryBinding;
import com.example.wikipediaclient.ui.common.SampleListViewModel;
import com.example.wikipediaclient.ui.navigation.ArticleNavigator;

public final class LibraryFragment extends Fragment {
    private FragmentLibraryBinding binding;
    private SampleListViewModel model;
    public LibraryFragment() { super(R.layout.fragment_library); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        binding = FragmentLibraryBinding.bind(view);
        model = new ViewModelProvider(this).get(SampleListViewModel.class);
        View.OnClickListener first = v -> ArticleNavigator.open(this,
                new ArticleLocator("vi", getString(R.string.reader_default_title), null));
        View.OnClickListener second = v -> ArticleNavigator.open(this,
                new ArticleLocator("vi", getString(R.string.sample_hanoi), null));
        binding.btnRead1.setOnClickListener(first);
        binding.cardFav1.setOnClickListener(first);
        binding.btnRead2.setOnClickListener(second);
        binding.cardFav2.setOnClickListener(second);
        binding.btnDelete1.setOnClickListener(v -> { model.remove(1); render(); });
        binding.btnDelete2.setOnClickListener(v -> { model.remove(2); render(); });
        binding.chipFavAll.setOnClickListener(v -> filter("all"));
        binding.chipFavPlaces.setOnClickListener(v -> filter("places"));
        binding.chipFavScience.setOnClickListener(v -> filter("science"));
        binding.btnDelete1.setContentDescription(getString(R.string.remove_sample, getString(R.string.reader_default_title)));
        binding.btnDelete2.setContentDescription(getString(R.string.remove_sample, getString(R.string.sample_hanoi)));
        render();
    }
    private void filter(String filter) { model.setFilter(filter); render(); }
    private void render() {
        String filter = model.getFilter();
        int remaining = model.remaining(2);
        boolean showPlaces = !"science".equals(filter);
        binding.cardFav1.setVisibility(showPlaces && !model.isRemoved(1) ? View.VISIBLE : View.GONE);
        binding.cardFav2.setVisibility(showPlaces && !model.isRemoved(2) ? View.VISIBLE : View.GONE);
        binding.chipFavAll.setText(getString(R.string.tab_all, remaining));
        binding.chipFavPlaces.setText(getString(R.string.tab_places, remaining));
        binding.chipFavScience.setText(getString(R.string.tab_science, 0));
        select(binding.chipFavAll, "all".equals(filter));
        select(binding.chipFavPlaces, "places".equals(filter));
        select(binding.chipFavScience, "science".equals(filter));
        // Keep the filter controls reachable even when the selected category is empty.
        boolean empty = remaining == 0 || !showPlaces;
        binding.status.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) binding.status.showEmpty(R.drawable.ic_bookmark,
                R.string.library_empty_title, remaining == 0 ? R.string.library_empty_message : R.string.filter_empty_message);
    }
    private void select(TextView chip, boolean selected) {
        chip.setSelected(selected);
        chip.setBackgroundResource(selected ? R.drawable.bg_pill_dark : R.drawable.bg_pill_light);
        chip.setTextColor(ContextCompat.getColor(requireContext(), selected ? R.color.on_primary : R.color.on_surface_variant));
        androidx.core.view.ViewCompat.setStateDescription(chip,
                getString(selected ? R.string.option_selected : R.string.option_not_selected));
    }
    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
