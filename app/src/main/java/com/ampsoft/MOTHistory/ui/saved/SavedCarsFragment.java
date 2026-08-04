package com.ampsoft.MOTHistory.ui.saved;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.ads.AdsManager;
import com.ampsoft.MOTHistory.data.local.VehicleStore;
import com.ampsoft.MOTHistory.ui.common.VehicleCardAdapter;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;

public class SavedCarsFragment extends Fragment {

    private VehicleCardAdapter adapter;
    private TextView emptyView;
    private TextView dashboardDueSoonView;
    private TextView dashboardOverdueView;
    private TextView dashboardRemindersView;
    private TextView dashboardNextDueView;
    private AdView bannerAdView;
    private final List<SavedCarsDashboardFormatter.SavedCarCardData> cardData = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_cars, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyView = view.findViewById(R.id.tv_saved_cars_empty);
        dashboardDueSoonView = view.findViewById(R.id.tv_saved_dashboard_due_soon_value);
        dashboardOverdueView = view.findViewById(R.id.tv_saved_dashboard_overdue_value);
        dashboardRemindersView = view.findViewById(R.id.tv_saved_dashboard_reminders_value);
        dashboardNextDueView = view.findViewById(R.id.tv_saved_dashboard_next_due_value);
        RecyclerView recyclerView = view.findViewById(R.id.rv_saved_cars);
        FrameLayout bannerContainer = view.findViewById(R.id.fl_saved_banner_ad_container);
        adapter = new VehicleCardAdapter(vehicle -> {
            Bundle args = new Bundle();
            args.putSerializable("vehicle", vehicle);
            Navigation.findNavController(view).navigate(R.id.resultFragment, args);
        }, this::findCardMetadata);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        refreshSavedVehicles();

        bannerContainer.post(() -> AdsManager.getInstance().attachAnchoredBannerWhenReady(
                requireActivity(),
                bannerContainer,
                AdsManager.BannerPlacement.SAVED_CARS,
                adView -> bannerAdView = adView
        ));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSavedVehicles();
    }

    @Override
    public void onDestroyView() {
        if (bannerAdView != null) {
            bannerAdView.destroy();
            bannerAdView = null;
        }
        super.onDestroyView();
    }

    private void refreshSavedVehicles() {
        if (adapter == null || emptyView == null) {
            return;
        }
        cardData.clear();
        cardData.addAll(SavedCarsDashboardFormatter.buildCardDataList(
                requireContext(),
                VehicleStore.getSavedVehicles(requireContext())
        ));
        List<com.ampsoft.MOTHistory.data.model.Vehicle> vehicles = new ArrayList<>();
        for (SavedCarsDashboardFormatter.SavedCarCardData item : cardData) {
            vehicles.add(item.getVehicle());
        }
        adapter.submitList(vehicles);
        bindDashboardSummary();
        emptyView.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private VehicleCardAdapter.CardMetadata findCardMetadata(
            com.ampsoft.MOTHistory.data.model.Vehicle vehicle
    ) {
        if (vehicle == null || vehicle.getRegistration() == null) {
            return null;
        }
        for (SavedCarsDashboardFormatter.SavedCarCardData item : cardData) {
            if (vehicle.getRegistration().equalsIgnoreCase(item.getVehicle().getRegistration())) {
                return item.getCardMetadata();
            }
        }
        return null;
    }

    private void bindDashboardSummary() {
        if (dashboardDueSoonView == null
                || dashboardOverdueView == null
                || dashboardRemindersView == null
                || dashboardNextDueView == null) {
            return;
        }
        SavedCarsDashboardFormatter.DashboardSummary summary =
                SavedCarsDashboardFormatter.buildSummary(requireContext(), cardData);
        dashboardDueSoonView.setText(String.valueOf(summary.getDueSoonCount()));
        dashboardOverdueView.setText(String.valueOf(summary.getOverdueCount()));
        dashboardRemindersView.setText(String.valueOf(summary.getRemindersOnCount()));
        dashboardNextDueView.setText(summary.getNextDueSummary());
    }
}
