package com.example.wikipediaclient.ui.history;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.wikipediaclient.R;

/** FragmentManager restores the confirmation when the Activity is recreated. */
public final class ClearHistoryDialogFragment extends DialogFragment {
    static final String TAG = "clear_history_dialog";
    static final String CONFIRMED = "clear_history_confirmed";

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.history_confirm_title)
                .setMessage(R.string.history_confirm_message)
                .setPositiveButton(R.string.remove, (dialog, which) ->
                        getParentFragmentManager().setFragmentResult(CONFIRMED, new Bundle()))
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}
