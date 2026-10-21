package com.example.wikipediaclient.ui.history;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.data.model.HistoryEntry;
import com.example.wikipediaclient.databinding.ItemHistoryBinding;
import java.util.ArrayList;
import java.util.List;

final class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
    interface Listener {
        void open(HistoryEntry entry);
        void remove(HistoryEntry entry);
    }

    private final Listener listener;
    private final List<HistoryEntry> entries = new ArrayList<>();

    HistoryAdapter(Listener listener) {
        this.listener = listener;
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    void submit(List<HistoryEntry> next) {
        entries.clear();
        entries.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        HistoryEntry entry = entries.get(position);
        holder.binding.historyTitle.setText(entry.getTitle());
        holder.binding.historyDate.setText(DateUtils.getRelativeDateTimeString(
                holder.itemView.getContext(), entry.getLastReadAt(), DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS, 0));
        holder.binding.historyOpen.setContentDescription(entry.getTitle());
        holder.binding.historyOpen.setOnClickListener(v -> listener.open(entry));
        holder.binding.historyRemove.setContentDescription(holder.itemView.getContext().getString(
                R.string.history_remove_article, entry.getTitle()));
        holder.binding.historyRemove.setOnClickListener(v -> listener.remove(entry));
    }

    @Override public int getItemCount() { return entries.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemHistoryBinding binding;
        Holder(ItemHistoryBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
}
