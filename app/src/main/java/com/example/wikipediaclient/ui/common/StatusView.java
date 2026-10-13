package com.example.wikipediaclient.ui.common;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.example.wikipediaclient.databinding.ViewStatusBinding;
/** Reusable loading/empty/error UI. The feature ViewModel owns state. */
public final class StatusView extends LinearLayout {
    private final ViewStatusBinding binding;
    public StatusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        binding = ViewStatusBinding.inflate(LayoutInflater.from(context), this, true);
    }
    public void showLoading(@StringRes int message) {
        setVisibility(VISIBLE);
        binding.statusProgress.setVisibility(VISIBLE);
        binding.statusIcon.setVisibility(GONE);
        binding.statusTitle.setVisibility(GONE);
        binding.statusMessage.setText(message);
        binding.statusAction.setVisibility(GONE);
        binding.statusAction.setOnClickListener(null);
    }
    public void showEmpty(@DrawableRes int icon, @StringRes int title, @StringRes int message) {
        setVisibility(VISIBLE);
        binding.statusProgress.setVisibility(GONE);
        binding.statusIcon.setVisibility(VISIBLE);
        binding.statusIcon.setImageResource(icon);
        binding.statusTitle.setVisibility(VISIBLE);
        binding.statusTitle.setText(title);
        binding.statusMessage.setText(message);
        binding.statusAction.setVisibility(GONE);
        binding.statusAction.setOnClickListener(null);
    }
    public void showError(@StringRes int title, String message, @Nullable Runnable retry) {
        setVisibility(VISIBLE);
        binding.statusProgress.setVisibility(GONE);
        binding.statusIcon.setVisibility(GONE);
        binding.statusTitle.setVisibility(VISIBLE);
        binding.statusTitle.setText(title);
        binding.statusMessage.setText(message);
        binding.statusAction.setVisibility(retry == null ? GONE : VISIBLE);
        binding.statusAction.setOnClickListener(retry == null ? null : v -> retry.run());
    }
}
