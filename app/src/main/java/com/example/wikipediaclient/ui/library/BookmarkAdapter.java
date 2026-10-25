package com.example.wikipediaclient.ui.library;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.data.model.PersonalArticle;
import com.example.wikipediaclient.data.model.PersonalArticleEntry;
import com.example.wikipediaclient.databinding.ItemBookmarkBinding;
import com.example.wikipediaclient.ui.common.WikiThumbnails;
import java.text.DateFormat;
import java.util.Date;

/** Displays real saved metadata, without sample images or invented categories. */
public final class BookmarkAdapter extends ListAdapter<PersonalArticleEntry, BookmarkAdapter.Holder> {
    public interface Listener {
        /** Opens the article using its canonical identity. */
        void onOpen(PersonalArticle article);
        /** Requests confirmation before deleting the bookmark. */
        void onRemove(PersonalArticle article);
    }
    private final Listener listener;
    private final WikiThumbnails thumbnails;
    private static final DiffUtil.ItemCallback<PersonalArticleEntry> DIFF = new DiffUtil.ItemCallback<PersonalArticleEntry>() {
        /** Identifies a row independently of title changes. */
        @Override public boolean areItemsTheSame(@NonNull PersonalArticleEntry a, @NonNull PersonalArticleEntry b) {
            return a.getArticle().getPageId() == b.getArticle().getPageId()
                    && a.getArticle().getLanguage().equals(b.getArticle().getLanguage());
        }
        /** Rebinds only when visible metadata or the saved time has changed. */
        @Override public boolean areContentsTheSame(@NonNull PersonalArticleEntry a, @NonNull PersonalArticleEntry b) {
            return a.getTimestamp() == b.getTimestamp()
                    && a.getArticle().getTitle().equals(b.getArticle().getTitle())
                    && a.getArticle().getCanonicalUrl().equals(b.getArticle().getCanonicalUrl());
        }
    };

    /** Keeps navigation and confirmation actions in the owning Fragment. */
    public BookmarkAdapter(Listener listener) { this(listener, WikiThumbnails.disabled()); }

    /** Also loads each article's Wikipedia lead image as the card cover. */
    public BookmarkAdapter(Listener listener, WikiThumbnails thumbnails) {
        super(DIFF);
        this.listener = listener;
        this.thumbnails = thumbnails;
    }

    /** Inflates one reusable bookmark card. */
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemBookmarkBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    /** Binds a row to the immutable article rather than a potentially stale adapter position. */
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        PersonalArticleEntry entry = getItem(position);
        PersonalArticle article = entry.getArticle();
        holder.binding.bookmarkTitle.setText(article.getTitle());
        holder.binding.bookmarkSource.setText(R.string.bookmark_source);
        holder.binding.bookmarkAvailability.setText(R.string.bookmark_availability);
        holder.binding.getRoot().setOnClickListener(v -> listener.onOpen(article));
        holder.binding.bookmarkUrl.setText(android.net.Uri.decode(article.getCanonicalUrl()));
        String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, VIETNAMESE)
                .format(new Date(entry.getTimestamp()));
        holder.binding.bookmarkDate.setText(holder.itemView.getContext().getString(R.string.bookmark_saved_at, date));
        thumbnails.load(article.getTitle(), holder.binding.bookmarkCover);
        holder.binding.bookmarkOpen.setContentDescription(holder.itemView.getContext()
                .getString(R.string.bookmark_open_description, article.getTitle()));
        holder.binding.bookmarkRemove.setContentDescription(holder.itemView.getContext()
                .getString(R.string.bookmark_remove_description, article.getTitle()));
        holder.binding.bookmarkOpen.setOnClickListener(v -> listener.onOpen(article));
        holder.binding.bookmarkRemove.setOnClickListener(v -> listener.onRemove(article));
    }

    private static final java.util.Locale VIETNAMESE = new java.util.Locale("vi", "VN");

    @Override public void onViewRecycled(@NonNull Holder holder) {
        thumbnails.cancel(holder.binding.bookmarkCover);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemBookmarkBinding binding;
        /** Holds the generated binding for one card. */
        Holder(ItemBookmarkBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
}
