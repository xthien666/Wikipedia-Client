package com.example.wikipediaclient.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.data.model.HistoryEntry;
import com.example.wikipediaclient.databinding.ItemHistoryBinding;
import com.example.wikipediaclient.ui.common.WikiThumbnails;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
    interface Listener {
        void open(HistoryEntry entry);
        void remove(HistoryEntry entry);
    }

    static final Locale VIETNAMESE = new Locale("vi", "VN");
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private final Listener listener;
    private final WikiThumbnails thumbnails;
    private final List<HistoryEntry> entries = new ArrayList<>();

    HistoryAdapter(Listener listener) { this(listener, WikiThumbnails.disabled()); }

    HistoryAdapter(Listener listener, WikiThumbnails thumbnails) {
        this.listener = listener;
        this.thumbnails = thumbnails;
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
        Context context = holder.itemView.getContext();
        long now = System.currentTimeMillis();
        long day = startOfDay(entry.getLastReadAt());
        boolean firstOfDay = position == 0 || startOfDay(entries.get(position - 1).getLastReadAt()) != day;
        holder.binding.historySection.setVisibility(firstOfDay ? View.VISIBLE : View.GONE);
        if (firstOfDay) {
            holder.binding.historySection.setText(sectionLabel(context, day, startOfDay(now)));
            ViewCompat.setAccessibilityHeading(holder.binding.historySection, true);
        }
        holder.binding.historyTitle.setText(entry.getTitle());
        String time = DateFormat.getTimeInstance(DateFormat.SHORT, VIETNAMESE).format(new Date(entry.getLastReadAt()));
        holder.binding.historyDate.setText(day >= startOfDay(now) - DAY_MS
                ? context.getString(R.string.history_read_at, time)
                : time + " · " + DateFormat.getDateInstance(DateFormat.SHORT, VIETNAMESE)
                        .format(new Date(entry.getLastReadAt())));
        thumbnails.load(entry.getTitle(), holder.binding.historyThumbnail);
        holder.binding.historyOpen.setContentDescription(entry.getTitle());
        holder.binding.historyOpen.setOnClickListener(v -> listener.open(entry));
        holder.binding.historyRemove.setContentDescription(context.getString(
                R.string.history_remove_article, entry.getTitle()));
        holder.binding.historyRemove.setOnClickListener(v -> listener.remove(entry));
    }

    @Override public void onViewRecycled(@NonNull Holder holder) {
        thumbnails.cancel(holder.binding.historyThumbnail);
    }

    @Override public int getItemCount() { return entries.size(); }

    /** Local midnight of the day containing {@code millis}. */
    static long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static String sectionLabel(Context context, long day, long today) {
        if (day == today) return context.getString(R.string.history_section_today);
        if (day == startOfDay(today - DAY_MS / 2)) return context.getString(R.string.history_section_yesterday);
        return DateFormat.getDateInstance(DateFormat.FULL, VIETNAMESE).format(new Date(day));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemHistoryBinding binding;
        Holder(ItemHistoryBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
}
