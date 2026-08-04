package com.ampsoft.MOTHistory.ui.common;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.data.model.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class VehicleCardAdapter extends RecyclerView.Adapter<VehicleCardAdapter.VehicleViewHolder> {

    public interface OnVehicleClickListener {
        void onVehicleClick(Vehicle vehicle);
    }

    public interface VehicleSupplementProvider {
        String getSupplementText(Vehicle vehicle);
    }

    public interface VehicleCardMetadataProvider {
        CardMetadata getCardMetadata(Vehicle vehicle);
    }

    public static final class CardMetadata {
        @Nullable
        private final StatusChip primaryChip;
        @Nullable
        private final StatusChip secondaryChip;
        @Nullable
        private final StatusChip tertiaryChip;
        @Nullable
        private final String supplementText;

        public CardMetadata(
                @Nullable StatusChip primaryChip,
                @Nullable StatusChip secondaryChip,
                @Nullable StatusChip tertiaryChip,
                @Nullable String supplementText
        ) {
            this.primaryChip = primaryChip;
            this.secondaryChip = secondaryChip;
            this.tertiaryChip = tertiaryChip;
            this.supplementText = supplementText;
        }
    }

    public static final class StatusChip {
        private final String text;
        @ColorRes
        private final int backgroundColorRes;
        @ColorRes
        private final int textColorRes;

        public StatusChip(@NonNull String text, @ColorRes int backgroundColorRes, @ColorRes int textColorRes) {
            this.text = text;
            this.backgroundColorRes = backgroundColorRes;
            this.textColorRes = textColorRes;
        }
    }

    private final List<Vehicle> vehicles = new ArrayList<>();
    private final OnVehicleClickListener onVehicleClickListener;
    private final VehicleSupplementProvider vehicleSupplementProvider;
    private final VehicleCardMetadataProvider vehicleCardMetadataProvider;

    public VehicleCardAdapter(OnVehicleClickListener onVehicleClickListener) {
        this(onVehicleClickListener, null, null);
    }

    public VehicleCardAdapter(
            OnVehicleClickListener onVehicleClickListener,
            VehicleSupplementProvider vehicleSupplementProvider
    ) {
        this(onVehicleClickListener, vehicleSupplementProvider, null);
    }

    public VehicleCardAdapter(
            OnVehicleClickListener onVehicleClickListener,
            VehicleCardMetadataProvider vehicleCardMetadataProvider
    ) {
        this(onVehicleClickListener, null, vehicleCardMetadataProvider);
    }

    public VehicleCardAdapter(
            OnVehicleClickListener onVehicleClickListener,
            VehicleSupplementProvider vehicleSupplementProvider,
            VehicleCardMetadataProvider vehicleCardMetadataProvider
    ) {
        this.onVehicleClickListener = onVehicleClickListener;
        this.vehicleSupplementProvider = vehicleSupplementProvider;
        this.vehicleCardMetadataProvider = vehicleCardMetadataProvider;
    }

    public void submitList(List<Vehicle> vehicleList) {
        vehicles.clear();
        if (vehicleList != null) {
            vehicles.addAll(vehicleList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehicle_card, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        Vehicle vehicle = vehicles.get(position);
        CardMetadata metadata = vehicleCardMetadataProvider != null
                ? vehicleCardMetadataProvider.getCardMetadata(vehicle)
                : null;
        String supplement = metadata != null
                ? metadata.supplementText
                : vehicleSupplementProvider != null ? vehicleSupplementProvider.getSupplementText(vehicle) : null;
        holder.bind(vehicle, supplement, metadata);
        holder.itemView.setOnClickListener(v -> onVehicleClickListener.onVehicleClick(vehicle));
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    public Vehicle getVehicleAt(int position) {
        if (position < 0 || position >= vehicles.size()) {
            return null;
        }
        return vehicles.get(position);
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {

        private final TextView registrationView;
        private final TextView summaryView;
        private final TextView supplementView;
        private final TextView primaryChipView;
        private final TextView secondaryChipView;
        private final TextView tertiaryChipView;

        VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            registrationView = itemView.findViewById(R.id.tv_vehicle_registration);
            summaryView = itemView.findViewById(R.id.tv_vehicle_summary);
            supplementView = itemView.findViewById(R.id.tv_vehicle_supplement);
            primaryChipView = itemView.findViewById(R.id.tv_vehicle_chip_primary);
            secondaryChipView = itemView.findViewById(R.id.tv_vehicle_chip_secondary);
            tertiaryChipView = itemView.findViewById(R.id.tv_vehicle_chip_tertiary);
        }

        void bind(@Nullable Vehicle vehicle, @Nullable String supplement, @Nullable CardMetadata metadata) {
            registrationView.setText(vehicle != null ? fallback(vehicle.getRegistration()) : "-");
            String make = vehicle != null ? fallback(vehicle.getMake()) : "-";
            String model = vehicle != null ? fallback(vehicle.getModel()) : "-";
            summaryView.setText((make + " " + model).trim());
            supplementView.setText(supplement);
            supplementView.setVisibility(TextUtils.isEmpty(supplement) ? View.GONE : View.VISIBLE);
            bindChip(primaryChipView, metadata != null ? metadata.primaryChip : null);
            bindChip(secondaryChipView, metadata != null ? metadata.secondaryChip : null);
            bindChip(tertiaryChipView, metadata != null ? metadata.tertiaryChip : null);
        }

        private String fallback(String value) {
            return value == null || value.trim().isEmpty() ? "-" : value;
        }

        private void bindChip(@NonNull TextView chipView, @Nullable StatusChip chip) {
            if (chip == null || TextUtils.isEmpty(chip.text)) {
                chipView.setVisibility(View.GONE);
                return;
            }
            chipView.setText(chip.text);
            chipView.setTextColor(ContextCompat.getColor(chipView.getContext(), chip.textColorRes));
            chipView.setBackgroundTintList(
                    ContextCompat.getColorStateList(chipView.getContext(), chip.backgroundColorRes)
            );
            chipView.setVisibility(View.VISIBLE);
        }
    }
}
