package com.ampsoft.MOTHistory.ui.result;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.ads.AdsManager;
import com.ampsoft.MOTHistory.data.local.VehicleStore;
import com.ampsoft.MOTHistory.data.model.MotTest;
import com.ampsoft.MOTHistory.data.model.Vehicle;
import com.ampsoft.MOTHistory.util.BundleUtils;
import com.ampsoft.MOTHistory.util.DateFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.gms.ads.AdView;

import java.util.List;

public class ResultFragment extends Fragment {

    private static final String ARG_VEHICLE = "vehicle";
    private AdView bannerAdView;

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
        viewModel.setVehicle(
                vehicle != null ? vehicle.getRegistration() : "",
                vehicle != null ? vehicle.getMake() : "",
                vehicle != null ? vehicle.getModel() : ""
        );

        TextView registrationText = view.findViewById(R.id.tv_result_registration);
        TextView summaryText = view.findViewById(R.id.tv_result_summary);
        MaterialButton saveVehicleButton = view.findViewById(R.id.btn_save_vehicle);
        TextView statusText = view.findViewById(R.id.tv_result_status);
        TextView expiryText = view.findViewById(R.id.tv_result_expiry);
        TextView testsCountText = view.findViewById(R.id.tv_result_tests_count);
        TextView passesText = view.findViewById(R.id.tv_result_passes);
        TextView passesAdvText = view.findViewById(R.id.tv_result_passes_adv);
        TextView failuresText = view.findViewById(R.id.tv_result_failures);
        TextView passRateText = view.findViewById(R.id.tv_result_pass_rate);
        TextView latestMileageText = view.findViewById(R.id.tv_result_latest_mileage);
        TextView mileageChangeText = view.findViewById(R.id.tv_result_mileage_change);
        TextView emptyStateText = view.findViewById(R.id.tv_result_empty_state);
        RecyclerView motHistoryList = view.findViewById(R.id.rv_mot_history);
        MaterialButton fullHistoryButton = view.findViewById(R.id.btn_view_full_history);
        MaterialButton mileageButton = view.findViewById(R.id.btn_view_mileage);
        FrameLayout bannerContainer = view.findViewById(R.id.fl_banner_ad_container);
        MotTestAdapter motTestAdapter = new MotTestAdapter();

        motHistoryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        motHistoryList.setNestedScrollingEnabled(false);
        motHistoryList.setAdapter(motTestAdapter);

        viewModel.getRegistration().observe(getViewLifecycleOwner(), registrationText::setText);
        viewModel.getVehicleSummary().observe(getViewLifecycleOwner(), summaryText::setText);

        List<MotTest> recentTests = MotHistoryInsights.getRecentTests(vehicle, 3);
        MotTest latestTest = MotHistoryInsights.getLatestTest(vehicle);
        MotHistoryInsights.MileageStats mileageStats = MotHistoryInsights.buildMileageStats(vehicle);
        MotHistoryInsights.ResultBreakdown resultBreakdown = MotHistoryInsights.buildResultBreakdown(vehicle);

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
            updateSaveButton(saveVehicleButton, saved);
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

    private void updateSaveButton(@NonNull MaterialButton button, @Nullable Vehicle vehicle) {
        boolean saved = vehicle != null
                && VehicleStore.isVehicleSaved(requireContext(), vehicle.getRegistration());
        updateSaveButton(button, saved);
    }

    private void updateSaveButton(@NonNull MaterialButton button, boolean saved) {
        button.setText(saved ? R.string.result_remove_vehicle : R.string.result_save_vehicle);
    }
}
