package com.example.wikipediaclient.ui.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import com.example.wikipediaclient.BuildConfig;
import com.example.wikipediaclient.R;
import com.example.wikipediaclient.WikiApplication;
import com.example.wikipediaclient.data.local.ReaderSettings;
import com.example.wikipediaclient.databinding.FragmentSettingsBinding;
import com.example.wikipediaclient.ui.source.ExternalActions;

public final class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private ReaderSettings settings;
    public SettingsFragment() { super(R.layout.fragment_settings); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        binding = FragmentSettingsBinding.bind(view);
        settings = ((WikiApplication) requireActivity().getApplication()).getContainer().getReaderSettings();
        binding.btnFontSmall.setOnClickListener(v -> selectFont(14));
        binding.btnFontNormal.setOnClickListener(v -> selectFont(16));
        binding.btnFontLarge.setOnClickListener(v -> selectFont(18));
        binding.btnFontHuge.setOnClickListener(v -> selectFont(22));
        binding.btnClearCache.setEnabled(false);
        binding.tvCacheVal.setVisibility(View.GONE);
        binding.tvVersion.setText(getString(R.string.settings_version_val, BuildConfig.VERSION_NAME));
        binding.btnAboutSource.setOnClickListener(v -> ExternalActions.openUrl(requireContext(), "https://vi.wikipedia.org/"));
        binding.btnAboutTerms.setOnClickListener(v -> ExternalActions.openUrl(requireContext(), "https://foundation.wikimedia.org/wiki/Policy:Terms_of_Use"));
        binding.btnAboutProject.setOnClickListener(v -> ExternalActions.openUrl(requireContext(), "https://github.com/xthien666/Wikipedia-Client"));
        render(settings.getFontSize());
    }
    private void selectFont(int size) { settings.setFontSize(size); render(size); }
    private void render(int size) {
        binding.tvPreviewBody.setTextSize(size);
        binding.tvFontBadge.setText(getString(R.string.settings_font_size_value, size));
        TextView[] options = {binding.btnFontSmall, binding.btnFontNormal, binding.btnFontLarge, binding.btnFontHuge};
        int[] sizes = {14, 16, 18, 22};
        for (int index = 0; index < options.length; index++) {
            TextView option = options[index];
            boolean selected = size == sizes[index];
            option.setSelected(selected);
            if (selected) binding.fontSizeGroup.check(option.getId());
            ViewCompat.setStateDescription(option, getString(selected ? R.string.option_selected : R.string.option_not_selected));
            option.setContentDescription(getString(R.string.font_option_accessible, getString(switch (sizes[index]) {
                case 14 -> R.string.font_small;
                case 18 -> R.string.font_large;
                case 22 -> R.string.font_huge;
                default -> R.string.font_normal;
            }), sizes[index]));
        }
    }
    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
