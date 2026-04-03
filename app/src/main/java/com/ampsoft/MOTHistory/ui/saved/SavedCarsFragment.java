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

public class SavedCarsFragment extends Fragment {

    private VehicleCardAdapter adapter;
    private TextView emptyView;
    private AdView bannerAdView;

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
        RecyclerView recyclerView = view.findViewById(R.id.rv_saved_cars);
        FrameLayout bannerContainer = view.findViewById(R.id.fl_saved_banner_ad_container);
        adapter = new VehicleCardAdapter(vehicle -> {
            Bundle args = new Bundle();
            args.putSerializable("vehicle", vehicle);
            Navigation.findNavController(view).navigate(R.id.resultFragment, args);
        });

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
        adapter.submitList(VehicleStore.getSavedVehicles(requireContext()));
        emptyView.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
}
