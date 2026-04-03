package com.ampsoft.MOTHistory.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.ads.AdsManager;
import com.ampsoft.MOTHistory.data.api.MotApiClient;
import com.ampsoft.MOTHistory.data.local.VehicleStore;
import com.ampsoft.MOTHistory.data.model.Vehicle;
import com.ampsoft.MOTHistory.data.repository.MotRepository;
import com.ampsoft.MOTHistory.data.repository.RepositoryResult;
import com.ampsoft.MOTHistory.ui.common.VehicleCardAdapter;
import com.ampsoft.MOTHistory.util.RegistrationValidator;
import com.google.android.gms.ads.AdView;

public class SearchFragment extends Fragment {

    private SearchViewModel viewModel;
    private AdView bannerAdView;
    private VehicleCardAdapter recentSearchAdapter;
    private TextView recentEmptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MotRepository motRepository = new MotRepository(MotApiClient.create());
        SearchViewModelFactory factory = new SearchViewModelFactory(motRepository);
        viewModel = new ViewModelProvider(this, factory).get(SearchViewModel.class);

        EditText registrationInput = view.findViewById(R.id.et_registration);
        Button searchButton = view.findViewById(R.id.btn_search);
        FrameLayout bannerContainer = view.findViewById(R.id.fl_search_banner_ad_container);
        ProgressBar loadingIndicator = view.findViewById(R.id.pb_loading);
        TextView messageText = view.findViewById(R.id.tv_message);
        recentEmptyText = view.findViewById(R.id.tv_recent_searches_empty);
        RecyclerView recentSearchesList = view.findViewById(R.id.rv_recent_searches);
        recentSearchAdapter = new VehicleCardAdapter(vehicle ->
                openVehicle(view, vehicle)
        );

        recentSearchesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentSearchesList.setNestedScrollingEnabled(false);
        recentSearchesList.setAdapter(recentSearchAdapter);
        attachRecentSearchSwipeToRemove(recentSearchesList);
        bindRecentSearches(recentSearchAdapter, recentEmptyText);

        bannerContainer.post(() -> AdsManager.getInstance().attachAnchoredBannerWhenReady(
                requireActivity(),
                bannerContainer,
                AdsManager.BannerPlacement.SEARCH,
                adView -> bannerAdView = adView
        ));

        searchButton.setOnClickListener(v -> {
            String normalized = RegistrationValidator.normalize(registrationInput.getText().toString());
            registrationInput.setText(normalized);
            viewModel.search(normalized);
        });

        viewModel.getLookupResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                return;
            }

            boolean isLoading = result.getStatus() == RepositoryResult.Status.LOADING;
            loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            searchButton.setEnabled(!isLoading);

            if (result.getStatus() == RepositoryResult.Status.SUCCESS && result.getData() != null) {
                Vehicle vehicle = result.getData();
                messageText.setVisibility(View.GONE);
                VehicleStore.addRecentVehicle(requireContext(), vehicle);
                bindRecentSearches(recentSearchAdapter, recentEmptyText);
                viewModel.clearLookupResult();
                AdsManager.getInstance().maybeShowInterstitialOnLookupSuccess(
                        requireActivity(),
                        () -> openVehicle(view, vehicle)
                );
                return;
            }

            if (result.getStatus() == RepositoryResult.Status.ERROR) {
                messageText.setText(result.getMessage());
                messageText.setTextColor(resolveMessageColor(result.getHttpCode()));
                messageText.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (recentSearchAdapter != null && recentEmptyText != null) {
            bindRecentSearches(recentSearchAdapter, recentEmptyText);
        }
    }

    private int resolveMessageColor(int httpCode) {
        if (httpCode == 429) {
            return ContextCompat.getColor(requireContext(), R.color.status_warning);
        }
        return ContextCompat.getColor(requireContext(), R.color.status_fail);
    }

    private void bindRecentSearches(VehicleCardAdapter adapter, TextView emptyView) {
        adapter.submitList(VehicleStore.getRecentVehicles(requireContext()));
        emptyView.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void openVehicle(@NonNull View view, @NonNull Vehicle vehicle) {
        Bundle args = new Bundle();
        args.putSerializable("vehicle", vehicle);
        Navigation.findNavController(view).navigate(R.id.action_searchFragment_to_resultFragment, args);
    }

    private void attachRecentSearchSwipeToRemove(@NonNull RecyclerView recyclerView) {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Vehicle vehicle = recentSearchAdapter.getVehicleAt(viewHolder.getBindingAdapterPosition());
                if (vehicle != null) {
                    VehicleStore.removeRecentVehicle(requireContext(), vehicle.getRegistration());
                }
                bindRecentSearches(recentSearchAdapter, recentEmptyText);
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onDestroyView() {
        if (bannerAdView != null) {
            bannerAdView.destroy();
            bannerAdView = null;
        }
        super.onDestroyView();
    }
}
