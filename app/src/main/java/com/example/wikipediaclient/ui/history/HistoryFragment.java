package com.example.wikipediaclient.ui.history;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.WikiApplication;
import com.example.wikipediaclient.data.repository.HistoryState;
import com.example.wikipediaclient.databinding.FragmentHistoryBinding;
import com.example.wikipediaclient.ui.navigation.ArticleNavigator;

public final class HistoryFragment extends Fragment {
    private FragmentHistoryBinding binding;

    public HistoryFragment() { super(R.layout.fragment_history); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedState) {
        super.onViewCreated(view, savedState);
        binding = FragmentHistoryBinding.bind(view);
        HistoryViewModel model = new ViewModelProvider(this, new HistoryViewModel.Factory(
                ((WikiApplication) requireActivity().getApplication())
                        .getContainer().getHistoryRepository())).get(HistoryViewModel.class);
        HistoryAdapter adapter = new HistoryAdapter(new HistoryAdapter.Listener() {
            @Override public void open(com.example.wikipediaclient.data.model.HistoryEntry entry) {
                ArticleNavigator.open(HistoryFragment.this, entry.toLocator());
            }
            @Override public void remove(com.example.wikipediaclient.data.model.HistoryEntry entry) {
                model.remove(entry);
            }
        });
        binding.historyList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.historyList.setAdapter(adapter);
        getChildFragmentManager().setFragmentResultListener(ClearHistoryDialogFragment.CONFIRMED,
                getViewLifecycleOwner(), (key, result) -> model.clear());
        binding.btnClearHistory.setOnClickListener(v -> {
            if (getChildFragmentManager().findFragmentByTag(ClearHistoryDialogFragment.TAG) == null) {
                new ClearHistoryDialogFragment().showNow(getChildFragmentManager(), ClearHistoryDialogFragment.TAG);
            }
        });
        model.observe().observe(getViewLifecycleOwner(), state -> render(state, adapter, model));
        model.refresh();
    }

    private void render(HistoryState state, HistoryAdapter adapter, HistoryViewModel model) {
        if (binding == null) return;
        int count = state.getEntries().size();
        binding.tvHistoryCount.setText(getString(R.string.history_saved_count, count));
        binding.btnClearHistory.setEnabled(count > 0 || state.getKind() == HistoryState.Kind.ERROR);
        binding.historyList.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        adapter.submit(state.getEntries());
        if (state.getKind() == HistoryState.Kind.LOADING) {
            binding.status.showLoading(R.string.history_loading);
        } else if (state.getKind() == HistoryState.Kind.ERROR) {
            boolean writeFailed = state.getFailure() == HistoryState.Failure.WRITE;
            binding.status.showError(writeFailed ? R.string.history_write_error_title : R.string.history_error_title,
                    getString(writeFailed ? R.string.history_write_error_message : R.string.history_error_message),
                    model::refresh);
        } else if (count == 0) {
            binding.status.showEmpty(R.drawable.ic_history,
                    R.string.history_empty_title, R.string.history_empty_message);
        } else {
            binding.status.setVisibility(View.GONE);
        }
    }

    @Override public void onDestroyView() {
        binding.historyList.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }
}
