package com.example.wikipediaclient.ui.history;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.data.model.ArticleLocator;
import com.example.wikipediaclient.databinding.FragmentHistoryBinding;
import com.example.wikipediaclient.ui.common.SampleListViewModel;
import com.example.wikipediaclient.ui.navigation.ArticleNavigator;

public final class HistoryFragment extends Fragment {
    private FragmentHistoryBinding binding;
    private SampleListViewModel model;
    public HistoryFragment() { super(R.layout.fragment_history); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        binding = FragmentHistoryBinding.bind(view);
        model = new ViewModelProvider(this).get(SampleListViewModel.class);
        int[] titleIds = {R.string.reader_default_title, R.string.sample_hoan_kiem, R.string.sample_solar_system};
        View[] cards = {binding.cardHistory1, binding.cardHistory2, binding.cardHistory3};
        View[] remove = {binding.btnDelHist1, binding.btnDelHist2, binding.btnDelHist3};
        for (int index = 0; index < cards.length; index++) {
            final int id = index + 1;
            final String title = getString(titleIds[index]);
            cards[index].setOnClickListener(v -> ArticleNavigator.open(this, new ArticleLocator("vi", title, null)));
            remove[index].setContentDescription(getString(R.string.remove_sample, title));
            remove[index].setOnClickListener(v -> { model.remove(id); render(); });
        }
        binding.btnClearHistory.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle(R.string.history_confirm_title)
                .setMessage(R.string.history_confirm_message)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    for (int id = 1; id <= 3; id++) model.remove(id);
                    if (binding != null) render();
                })
                .setNegativeButton(R.string.cancel, null).show());
        render();
    }
    private void render() {
        int count = model.remaining(3);
        binding.cardHistory1.setVisibility(model.isRemoved(1) ? View.GONE : View.VISIBLE);
        binding.cardHistory2.setVisibility(model.isRemoved(2) ? View.GONE : View.VISIBLE);
        binding.cardHistory3.setVisibility(model.isRemoved(3) ? View.GONE : View.VISIBLE);
        int today = (model.isRemoved(1) ? 0 : 1) + (model.isRemoved(2) ? 0 : 1);
        binding.historyToday.setText(getString(R.string.sample_today, today));
        binding.historyToday.setVisibility(today == 0 ? View.GONE : View.VISIBLE);
        binding.historyYesterday.setText(getString(R.string.sample_yesterday, model.isRemoved(3) ? 0 : 1));
        binding.historyYesterday.setVisibility(model.isRemoved(3) ? View.GONE : View.VISIBLE);
        binding.tvHistoryCount.setText(getString(R.string.history_saved_count, count));
        binding.btnClearHistory.setEnabled(count > 0);
        binding.status.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        if (count == 0) binding.status.showEmpty(R.drawable.ic_history, R.string.history_empty_title, R.string.history_empty_message);
    }
    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
