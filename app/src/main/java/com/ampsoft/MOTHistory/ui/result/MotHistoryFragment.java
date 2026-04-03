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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.ads.AdsManager;
import com.ampsoft.MOTHistory.data.model.MotTest;
import com.ampsoft.MOTHistory.data.model.Vehicle;
import com.ampsoft.MOTHistory.util.BundleUtils;
import com.google.android.gms.ads.AdView;

import java.util.List;

public class MotHistoryFragment extends Fragment {

    private static final String ARG_VEHICLE = "vehicle";
    private AdView bannerAdView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mot_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Vehicle vehicle = getVehicle();
        List<MotTest> tests = MotHistoryInsights.getSortedTestsDescending(vehicle);

        TextView registrationView = view.findViewById(R.id.tv_history_registration);
        TextView subtitleView = view.findViewById(R.id.tv_history_subtitle);
        TextView emptyStateView = view.findViewById(R.id.tv_history_empty_state);
        RecyclerView recyclerView = view.findViewById(R.id.rv_history_tests);
        FrameLayout bannerContainer = view.findViewById(R.id.fl_history_banner_ad_container);

        registrationView.setText(vehicle != null ? vehicle.getRegistration() : "");
        subtitleView.setText(getString(R.string.mot_history_subtitle, tests.size()));

        MotTestAdapter adapter = new MotTestAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        if (tests.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            adapter.submitList(tests);
        }

        bannerContainer.post(() -> AdsManager.getInstance().attachAnchoredBannerWhenReady(
                requireActivity(),
                bannerContainer,
                AdsManager.BannerPlacement.MOT_HISTORY,
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

    private Vehicle getVehicle() {
        Bundle args = getArguments();
        return BundleUtils.getSerializable(args, ARG_VEHICLE, Vehicle.class);
    }
}
