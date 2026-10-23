package com.example.wikipediaclient.ui.library;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.data.model.PersonalArticle;

/** Restores the selected article and confirmation after recreation. */
public final class RemoveBookmarkDialog extends DialogFragment {
    static final String TAG = "remove_bookmark";
    static final String RESULT = "bookmark_removal_confirmed";

    /** Stores the selected identity instead of retaining a Fragment or callback. */
    static RemoveBookmarkDialog newInstance(PersonalArticle article) {
        Bundle args = new Bundle();
        args.putString("language", article.getLanguage());
        args.putLong("pageId", article.getPageId());
        args.putString("title", article.getTitle());
        args.putString("canonicalUrl", article.getCanonicalUrl());
        RemoveBookmarkDialog dialog = new RemoveBookmarkDialog();
        dialog.setArguments(args);
        return dialog;
    }

    /** Reads the same article from restored arguments or a confirmed result. */
    static PersonalArticle articleFrom(Bundle args) {
        return new PersonalArticle(args.getString("language"), args.getLong("pageId"),
                args.getString("title"), args.getString("canonicalUrl"));
    }

    /** Sends a deletion request only when the positive button is pressed. */
    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        PersonalArticle article = articleFrom(requireArguments());
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.bookmark_remove_title)
                .setMessage(getString(R.string.bookmark_remove_message, article.getTitle()))
                .setNegativeButton(R.string.bookmark_cancel, null)
                .setPositiveButton(R.string.bookmark_remove, (dialog, which) ->
                        getParentFragmentManager().setFragmentResult(RESULT, new Bundle(requireArguments())))
                .create();
    }
}
