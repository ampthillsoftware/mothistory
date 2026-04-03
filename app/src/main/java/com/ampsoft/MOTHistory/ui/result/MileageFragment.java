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

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.ads.AdsManager;
import com.ampsoft.MOTHistory.data.model.Vehicle;
import com.ampsoft.MOTHistory.util.BundleUtils;
import com.google.android.gms.ads.AdView;

public class MileageFragment extends Fragment {

    private static final String ARG_VEHICLE = "vehicle";
    private AdView bannerAdView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mileage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Vehicle vehicle = getVehicle();
        MotHistoryInsights.MileageStats stats = MotHistoryInsights.buildMileageStats(vehicle);

        TextView registrationView = view.findViewById(R.id.tv_mileage_registration);
        TextView latestView = view.findViewById(R.id.tv_mileage_latest);
        TextView emptyStateView = view.findViewById(R.id.tv_mileage_empty_state);
        MileageGraphView graphView = view.findViewById(R.id.view_mileage_graph);
        FrameLayout bannerContainer = view.findViewById(R.id.fl_mileage_banner_ad_container);

        registrationView.setText(vehicle != null ? vehicle.getRegistration() : "");

        String unit = stats.getUnit();
        String latestMileage = stats.getLatestPoint() == null
                ? null
                : MotHistoryInsights.formatMileage(stats.getLatestPoint().getMileageValue(), unit);
        latestView.setText(fallback(latestMileage));

        if (stats.getPointsAscending().isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            graphView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            graphView.setVisibility(View.VISIBLE);
            graphView.setPoints(stats.getPointsAscending());
        }

        bannerContainer.post(() -> AdsManager.getInstance().attachAnchoredBannerWhenReady(
                requireActivity(),
                bannerContainer,
                AdsManager.BannerPlacement.MILEAGE,
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

    private String fallback(String value) {
        return value == null || value.trim().isEmpty()
                ? getString(R.string.generic_not_available)
                : value;
    }

    private Vehicle getVehicle() {
        Bundle args = getArguments();
        return BundleUtils.getSerializable(args, ARG_VEHICLE, Vehicle.class);
    }
}
