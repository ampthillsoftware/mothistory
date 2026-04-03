package com.ampsoft.MOTHistory.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.ads.AdsManager;
import com.google.android.material.button.MaterialButton;
import com.ampsoft.MOTHistory.util.ThemePreferences;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RadioGroup themeGroup = view.findViewById(R.id.rg_theme_mode);
        TextView privacyTitle = view.findViewById(R.id.tv_privacy_section_title);
        TextView privacySummary = view.findViewById(R.id.tv_privacy_summary);
        MaterialButton privacyButton = view.findViewById(R.id.btn_privacy_options);
        int currentMode = ThemePreferences.getSavedMode(requireContext());
        themeGroup.check(toRadioId(currentMode));

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int selectedMode = fromRadioId(checkedId);
            if (selectedMode == ThemePreferences.getSavedMode(requireContext())) {
                return;
            }
            ThemePreferences.saveMode(requireContext(), selectedMode);
            AppCompatDelegate.setDefaultNightMode(ThemePreferences.toNightMode(selectedMode));
        });

        boolean privacyOptionsRequired = AdsManager.getInstance().isPrivacyOptionsRequired();
        int privacyVisibility = privacyOptionsRequired ? View.VISIBLE : View.GONE;
        privacyTitle.setVisibility(privacyVisibility);
        privacySummary.setVisibility(privacyVisibility);
        privacyButton.setVisibility(privacyVisibility);
        privacyButton.setOnClickListener(v -> AdsManager.getInstance().showPrivacyOptionsForm(requireActivity()));
    }

    private int toRadioId(int mode) {
        switch (mode) {
            case ThemePreferences.MODE_LIGHT:
                return R.id.rb_theme_light;
            case ThemePreferences.MODE_DARK:
                return R.id.rb_theme_dark;
            case ThemePreferences.MODE_SYSTEM:
            default:
                return R.id.rb_theme_system;
        }
    }

    private int fromRadioId(int radioId) {
        if (radioId == R.id.rb_theme_light) {
            return ThemePreferences.MODE_LIGHT;
        }
        if (radioId == R.id.rb_theme_dark) {
            return ThemePreferences.MODE_DARK;
        }
        return ThemePreferences.MODE_SYSTEM;
    }
}
