package com.example.wikipediaclient.ui.article;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.WikiApplication;
import com.example.wikipediaclient.databinding.FragmentArticleBinding;

public final class ArticleFragment extends Fragment {
    private FragmentArticleBinding binding;
    public ArticleFragment() { super(R.layout.fragment_article); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        binding = FragmentArticleBinding.bind(view);
        String requestedTitle = requireArguments().getString("title");
        final String title = requestedTitle == null || requestedTitle.trim().isEmpty()
                ? getString(R.string.reader_default_title) : requestedTitle.trim();
        binding.articleTitle.setText(title);
        // The prototype has no article API yet: never substitute another article's facts.
        binding.articleBody.setText(getString(R.string.reader_sample_body, title));
        binding.articleBody.setTextSize(((WikiApplication) requireActivity().getApplication())
                .getContainer().getReaderSettings().getFontSize());
        binding.actionListen.setEnabled(false);
        binding.actionSave.setEnabled(false);
        final String url = "https://vi.wikipedia.org/wiki/" + Uri.encode(title);
        binding.btnViewOriginal.setOnClickListener(v -> open(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
        binding.actionShare.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, url);
            open(Intent.createChooser(share, getString(R.string.share_article)));
        });
    }
    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException exception) {
            Toast.makeText(requireContext(), R.string.no_external_app, Toast.LENGTH_SHORT).show();
        }
    }
    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
