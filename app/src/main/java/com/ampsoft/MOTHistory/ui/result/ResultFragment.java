package com.ampsoft.MOTHistory.ui.result;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.ads.AdsManager;
import com.ampsoft.MOTHistory.data.local.ReminderStore;
import com.ampsoft.MOTHistory.data.local.VehicleStore;
import com.ampsoft.MOTHistory.data.model.MotTest;
import com.ampsoft.MOTHistory.data.model.Vehicle;
import com.ampsoft.MOTHistory.reminders.MotReminderScheduler;
import com.ampsoft.MOTHistory.util.BundleUtils;
import com.ampsoft.MOTHistory.util.DateFormatter;
import com.ampsoft.MOTHistory.util.AppReviewManager;
import com.ampsoft.MOTHistory.util.ReviewPromptStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.gms.ads.AdView;

import java.util.List;

public class ResultFragment extends Fragment {

    private static final String ARG_VEHICLE = "vehicle";
    private static final String ARG_PROMPT_FOR_REVIEW = "prompt_for_review";
    private AdView bannerAdView;
    private Vehicle currentVehicle;
    private View rootContentView;
    private TextView reminderStatusText;
    private MaterialButton reminderToggleButton;
    private MaterialButton reminderTimingButton;
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && currentVehicle != null) {
                    enableReminderWithOffset(currentVehicle, MotReminderScheduler.DEFAULT_OFFSET_DAYS);
                } else if (!granted && isAdded()) {
                    showReminderMessage(getString(R.string.reminder_permission_denied), Snackbar.LENGTH_LONG);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ResultViewModel viewModel = new ViewModelProvider(this).get(ResultViewModel.class);

        Bundle args = getArguments();
        Vehicle vehicle = BundleUtils.getSerializable(args, ARG_VEHICLE, Vehicle.class);
        currentVehicle = vehicle;
        rootContentView = view;
        viewModel.setVehicle(
                vehicle != null ? vehicle.getRegistration() : "",
                vehicle != null ? vehicle.getMake() : "",
                vehicle != null ? vehicle.getModel() : ""
        );

        TextView registrationText = view.findViewById(R.id.tv_result_registration);
        TextView summaryText = view.findViewById(R.id.tv_result_summary);
        MaterialButton saveVehicleButton = view.findViewById(R.id.btn_save_vehicle);
        MaterialCardView motRecordSummaryCard = view.findViewById(R.id.card_mot_record_summary);
        TextView motRecordSummaryTitle = view.findViewById(R.id.tv_mot_record_summary_title);
        TextView motRecordSummaryBody = view.findViewById(R.id.tv_mot_record_summary_body);
        TextView motRecordSummarySignals = view.findViewById(R.id.tv_mot_record_summary_signals);
        TextView motRecordChangesTitle = view.findViewById(R.id.tv_mot_record_changes_title);
        TextView motRecordChanges = view.findViewById(R.id.tv_mot_record_changes);
        TextView motRecordSummaryDisclaimer = view.findViewById(R.id.tv_mot_record_summary_disclaimer);
        MaterialButton motRecordSummaryToggle = view.findViewById(R.id.btn_toggle_mot_record_summary);
        TextView statusText = view.findViewById(R.id.tv_result_status);
        TextView expiryText = view.findViewById(R.id.tv_result_expiry);
        TextView testsCountText = view.findViewById(R.id.tv_result_tests_count);
        View breakdownContent = view.findViewById(R.id.layout_result_breakdown_content);
        MaterialButton breakdownToggle = view.findViewById(R.id.btn_toggle_breakdown);
        TextView passesText = view.findViewById(R.id.tv_result_passes);
        TextView passesAdvText = view.findViewById(R.id.tv_result_passes_adv);
        TextView failuresText = view.findViewById(R.id.tv_result_failures);
        TextView passRateText = view.findViewById(R.id.tv_result_pass_rate);
        View mileageContent = view.findViewById(R.id.layout_result_mileage_content);
        MaterialButton mileageSummaryToggle = view.findViewById(R.id.btn_toggle_mileage_summary);
        TextView latestMileageText = view.findViewById(R.id.tv_result_latest_mileage);
        TextView mileageChangeText = view.findViewById(R.id.tv_result_mileage_change);
        TextView emptyStateText = view.findViewById(R.id.tv_result_empty_state);
        RecyclerView motHistoryList = view.findViewById(R.id.rv_mot_history);
        MaterialButton fullHistoryButton = view.findViewById(R.id.btn_view_full_history);
        MaterialButton mileageButton = view.findViewById(R.id.btn_view_mileage);
        reminderStatusText = view.findViewById(R.id.tv_result_reminder_status);
        reminderToggleButton = view.findViewById(R.id.btn_toggle_reminder);
        reminderTimingButton = view.findViewById(R.id.btn_change_reminder_timing);
        FrameLayout bannerContainer = view.findViewById(R.id.fl_banner_ad_container);
        MotTestAdapter motTestAdapter = new MotTestAdapter();

        motHistoryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        motHistoryList.setNestedScrollingEnabled(false);
        motHistoryList.setAdapter(motTestAdapter);

        viewModel.getRegistration().observe(getViewLifecycleOwner(), registrationText::setText);
        viewModel.getVehicleSummary().observe(getViewLifecycleOwner(), summaryText::setText);

        List<MotTest> recentTests = MotHistoryInsights.getRecentTests(vehicle, 1);
        MotTest latestTest = MotHistoryInsights.getLatestTest(vehicle);
        MotHistoryInsights.MileageStats mileageStats = MotHistoryInsights.buildMileageStats(vehicle);
        MotHistoryInsights.ResultBreakdown resultBreakdown = MotHistoryInsights.buildResultBreakdown(vehicle);
        MotRecordSummary.Summary motRecordSummary = MotRecordSummary.build(requireContext(), vehicle);

        motRecordSummaryTitle.setText(motRecordSummary.getTitle());
        motRecordSummaryBody.setText(motRecordSummary.getBody());
        motRecordSummarySignals.setText(formatMotRecordSignals(motRecordSummary.getSignals()));
        motRecordChanges.setText(formatMotRecordSignals(motRecordSummary.getChanges()));
        int summaryColor = colorForMotRecordTone(motRecordSummary.getTone());
        motRecordSummaryTitle.setTextColor(summaryColor);
        motRecordSummaryCard.setStrokeColor(summaryColor);
        setupMultiViewToggle(
                motRecordSummaryToggle,
                R.string.result_show_record_details,
                R.string.result_hide_record_details,
                motRecordSummarySignals,
                motRecordChangesTitle,
                motRecordChanges,
                motRecordSummaryDisclaimer
        );
        setupExpandableSection(
                breakdownToggle,
                breakdownContent,
                R.string.result_show_breakdown,
                R.string.result_hide_breakdown
        );
        setupExpandableSection(
                mileageSummaryToggle,
                mileageContent,
                R.string.result_show_mileage_summary,
                R.string.result_hide_mileage_summary
        );

        statusText.setText(getString(
                R.string.result_status_latest,
                fallback(latestTest != null ? latestTest.getTestResult() : null)
        ));
        expiryText.setText(getString(
                R.string.result_status_expiry,
                fallback(latestTest != null ? DateFormatter.asDisplayDate(latestTest.getExpiryDate()) : null)
        ));
        testsCountText.setText(getString(
                R.string.result_status_tests,
                vehicle != null && vehicle.getMotTests() != null ? vehicle.getMotTests().size() : 0
        ));
        passesText.setText(getString(R.string.result_breakdown_passes, resultBreakdown.getPasses()));
        passesAdvText.setText(getString(
                R.string.result_breakdown_passes_adv,
                resultBreakdown.getPassesWithAdvisories()
        ));
        failuresText.setText(getString(R.string.result_breakdown_failures, resultBreakdown.getFailures()));
        passRateText.setText(getString(
                R.string.result_breakdown_pass_rate,
                resultBreakdown.getPassRatePercent()
        ));

        String unit = mileageStats.getUnit();
        String latestMileage = mileageStats.getLatestPoint() == null
                ? null
                : MotHistoryInsights.formatMileage(mileageStats.getLatestPoint().getMileageValue(), unit);
        latestMileageText.setText(getString(
                R.string.result_mileage_latest,
                fallback(latestMileage)
        ));
        mileageChangeText.setText(getString(
                R.string.result_mileage_change,
                fallback(MotHistoryInsights.formatDelta(mileageStats.getLatestDelta(), unit))
        ));

        if (!recentTests.isEmpty()) {
            emptyStateText.setVisibility(View.GONE);
            motTestAdapter.submitList(recentTests);
        } else {
            emptyStateText.setVisibility(View.VISIBLE);
            motTestAdapter.submitList(null);
        }

        updateSaveButton(saveVehicleButton, vehicle);
        saveVehicleButton.setOnClickListener(v -> {
            boolean saved = VehicleStore.toggleSavedVehicle(requireContext(), vehicle);
            if (!saved) {
                MotReminderScheduler.cancelReminder(requireContext(), vehicle.getRegistration());
            }
            updateSaveButton(saveVehicleButton, saved);
            updateReminderUi(vehicle);
        });

        reminderToggleButton.setOnClickListener(v -> {
            if (vehicle == null) {
                return;
            }
            if (!VehicleStore.isVehicleSaved(requireContext(), vehicle.getRegistration())) {
                showReminderMessage(getString(R.string.reminder_save_first), Snackbar.LENGTH_LONG);
                return;
            }
            ReminderStore.ReminderConfig config =
                    ReminderStore.getReminder(requireContext(), vehicle.getRegistration());
            if (config == null) {
                ensureNotificationPermissionAndEnable(vehicle);
                return;
            }
            MotReminderScheduler.cancelReminder(requireContext(), vehicle.getRegistration());
            updateReminderUi(vehicle);
            showReminderMessage(getString(R.string.reminder_disabled), Snackbar.LENGTH_SHORT);
        });
        reminderTimingButton.setOnClickListener(v -> {
            if (vehicle != null) {
                showReminderOffsetPicker(vehicle);
            }
        });

        fullHistoryButton.setOnClickListener(v -> navigateWithVehicle(
                view,
                R.id.action_resultFragment_to_motHistoryFragment,
                vehicle
        ));
        mileageButton.setOnClickListener(v -> navigateWithVehicle(
                view,
                R.id.action_resultFragment_to_mileageFragment,
                vehicle
        ));

        bannerContainer.post(() -> AdsManager.getInstance().attachAnchoredBannerWhenReady(
                requireActivity(),
                bannerContainer,
                AdsManager.BannerPlacement.RESULT,
                adView -> bannerAdView = adView
        ));

        updateReminderUi(vehicle);

        if (args != null && args.getBoolean(ARG_PROMPT_FOR_REVIEW, false)) {
            showReviewPrompt();
        }
    }

    @Override
    public void onDestroyView() {
        if (bannerAdView != null) {
            bannerAdView.destroy();
            bannerAdView = null;
        }
        super.onDestroyView();
    }

    private void navigateWithVehicle(@NonNull View rootView, int actionId, @Nullable Vehicle vehicle) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_VEHICLE, vehicle);
        Navigation.findNavController(rootView).navigate(actionId, args);
    }

    private String fallback(@Nullable String value) {
        return value == null || value.trim().isEmpty()
                ? getString(R.string.generic_not_available)
                : value;
    }

    private void setupExpandableSection(
            @NonNull MaterialButton toggleButton,
            @NonNull View contentView,
            int collapsedTextRes,
            int expandedTextRes
    ) {
        contentView.setVisibility(View.GONE);
        toggleButton.setText(collapsedTextRes);
        toggleButton.setOnClickListener(v -> {
            boolean expanded = contentView.getVisibility() == View.VISIBLE;
            contentView.setVisibility(expanded ? View.GONE : View.VISIBLE);
            toggleButton.setText(expanded ? collapsedTextRes : expandedTextRes);
        });
    }

    private void setupMultiViewToggle(
            @NonNull MaterialButton toggleButton,
            int collapsedTextRes,
            int expandedTextRes,
            @NonNull View... contentViews
    ) {
        for (View contentView : contentViews) {
            contentView.setVisibility(View.GONE);
        }
        toggleButton.setText(collapsedTextRes);
        toggleButton.setOnClickListener(v -> {
            boolean expanded = contentViews.length > 0 && contentViews[0].getVisibility() == View.VISIBLE;
            for (View contentView : contentViews) {
                contentView.setVisibility(expanded ? View.GONE : View.VISIBLE);
            }
            toggleButton.setText(expanded ? collapsedTextRes : expandedTextRes);
        });
    }

    private String formatMotRecordSignals(@NonNull List<String> signals) {
        if (signals.isEmpty()) {
            return getString(R.string.mot_record_signal_none);
        }
        StringBuilder builder = new StringBuilder();
        for (String signal : signals) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("- ").append(signal);
        }
        return builder.toString();
    }

    private int colorForMotRecordTone(@NonNull MotRecordSummary.Tone tone) {
        switch (tone) {
            case IMPORTANT:
                return ContextCompat.getColor(requireContext(), R.color.status_fail);
            case REVIEW:
                return ContextCompat.getColor(requireContext(), R.color.status_warning);
            case LIMITED:
                return ContextCompat.getColor(requireContext(), R.color.status_neutral);
            case CLEAN:
            default:
                return ContextCompat.getColor(requireContext(), R.color.status_pass);
        }
    }

    private void updateSaveButton(@NonNull MaterialButton button, @Nullable Vehicle vehicle) {
        boolean saved = vehicle != null
                && VehicleStore.isVehicleSaved(requireContext(), vehicle.getRegistration());
        updateSaveButton(button, saved);
    }

    private void updateSaveButton(@NonNull MaterialButton button, boolean saved) {
        button.setText(saved ? R.string.result_remove_vehicle : R.string.result_save_vehicle);
    }

    private void updateReminderUi(@Nullable Vehicle vehicle) {
        if (reminderStatusText == null || reminderToggleButton == null || reminderTimingButton == null) {
            return;
        }
        String expiryDateIso = DateFormatter.extractLatestExpiryDate(vehicle);
        if (vehicle == null || expiryDateIso == null || expiryDateIso.trim().isEmpty()) {
            reminderStatusText.setText(R.string.reminder_unavailable);
            reminderToggleButton.setEnabled(false);
            reminderToggleButton.setVisibility(View.GONE);
            reminderTimingButton.setVisibility(View.GONE);
            return;
        }
        boolean saved = VehicleStore.isVehicleSaved(requireContext(), vehicle.getRegistration());
        if (!saved) {
            reminderStatusText.setText(R.string.reminder_save_first);
            reminderToggleButton.setEnabled(false);
            reminderToggleButton.setText(R.string.reminder_enable_button);
            reminderToggleButton.setVisibility(View.GONE);
            reminderTimingButton.setVisibility(View.GONE);
            return;
        }

        ReminderStore.ReminderConfig config =
                ReminderStore.getReminder(requireContext(), vehicle.getRegistration());
        reminderStatusText.setText(MotReminderScheduler.buildStatusText(requireContext(), vehicle));
        reminderToggleButton.setEnabled(true);
        reminderToggleButton.setVisibility(View.VISIBLE);
        reminderToggleButton.setText(config == null
                ? R.string.reminder_enable_button
                : R.string.reminder_disable_button);
        reminderTimingButton.setVisibility(config == null ? View.GONE : View.VISIBLE);
        if (config != null) {
            reminderTimingButton.setText(getString(
                    R.string.reminder_change_timing_button,
                    MotReminderScheduler.describeReminderOffset(requireContext(), config.getOffsetDays())
            ));
        }
    }

    private void ensureNotificationPermissionAndEnable(@NonNull Vehicle vehicle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        enableReminderWithOffset(vehicle, MotReminderScheduler.DEFAULT_OFFSET_DAYS);
    }

    private void enableReminderWithOffset(@NonNull Vehicle vehicle, int offsetDays) {
        boolean scheduled = MotReminderScheduler.scheduleReminder(requireContext(), vehicle, offsetDays);
        if (scheduled) {
            updateReminderUi(vehicle);
            showReminderMessage(getString(R.string.reminder_enabled), Snackbar.LENGTH_SHORT);
        } else {
            updateReminderUi(vehicle);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.reminder_unavailable_title)
                    .setMessage(R.string.reminder_unavailable_schedule)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void showReminderOffsetPicker(@NonNull Vehicle vehicle) {
        String[] labels = new String[MotReminderScheduler.SUPPORTED_OFFSETS_DAYS.length];
        int checkedItem = 0;
        ReminderStore.ReminderConfig existing =
                ReminderStore.getReminder(requireContext(), vehicle.getRegistration());
        for (int i = 0; i < MotReminderScheduler.SUPPORTED_OFFSETS_DAYS.length; i++) {
            int offsetDays = MotReminderScheduler.SUPPORTED_OFFSETS_DAYS[i];
            labels[i] = MotReminderScheduler.describeReminderOffset(requireContext(), offsetDays);
            if (existing != null && existing.getOffsetDays() == offsetDays) {
                checkedItem = i;
            }
        }
        final int[] selectedIndex = new int[] {checkedItem};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reminder_picker_title)
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton(R.string.reminder_picker_save, (dialog, which) ->
                        enableReminderWithOffset(
                                vehicle,
                                MotReminderScheduler.SUPPORTED_OFFSETS_DAYS[selectedIndex[0]]
                        ))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showReminderMessage(@NonNull String message, int duration) {
        View anchor = rootContentView != null ? rootContentView : getView();
        if (anchor == null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            return;
        }
        Snackbar.make(anchor, message, duration).show();
    }

    private void showReviewPrompt() {
        if (!isAdded()) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.review_prompt_title)
                .setMessage(R.string.review_prompt_message)
                .setPositiveButton(R.string.review_prompt_rate_now, (dialog, which) -> {
                    ReviewPromptStore.markRateRequested(requireContext());
                    AppReviewManager.openPlayStoreListing(requireContext());
                })
                .setNeutralButton(R.string.review_prompt_later, (dialog, which) ->
                        ReviewPromptStore.snooze(requireContext()))
                .setNegativeButton(R.string.review_prompt_no_thanks, (dialog, which) ->
                        ReviewPromptStore.declinePermanently(requireContext()))
                .show();
    }
}
