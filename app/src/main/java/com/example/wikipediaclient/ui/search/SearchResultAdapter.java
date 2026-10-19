package com.example.wikipediaclient.ui.search;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.wikipediaclient.data.model.SearchResult;
import com.example.wikipediaclient.databinding.ItemSearchResultBinding;

public final class SearchResultAdapter extends ListAdapter<SearchResult, SearchResultAdapter.Holder> {
    public interface OnResultClick { void onClick(SearchResult result); }

    private static final DiffUtil.ItemCallback<SearchResult> DIFF = new DiffUtil.ItemCallback<SearchResult>() {
        @Override public boolean areItemsTheSame(@NonNull SearchResult a, @NonNull SearchResult b) {
            return a.getPageId() == b.getPageId();
        }
        @Override public boolean areContentsTheSame(@NonNull SearchResult a, @NonNull SearchResult b) {
            return a.getTitle().equals(b.getTitle()) && a.getSnippet().equals(b.getSnippet());
        }
    };

    private final OnResultClick listener;

    public SearchResultAdapter(OnResultClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemSearchResultBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SearchResult result = getItem(position);
        holder.binding.resultTitle.setText(result.getTitle());
        holder.binding.resultSnippet.setText(result.getSnippet());
        holder.binding.getRoot().setOnClickListener(v -> listener.onClick(result));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemSearchResultBinding binding;
        Holder(ItemSearchResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
