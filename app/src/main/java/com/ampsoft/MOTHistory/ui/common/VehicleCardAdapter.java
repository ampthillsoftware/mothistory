package com.ampsoft.MOTHistory.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.data.model.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class VehicleCardAdapter extends RecyclerView.Adapter<VehicleCardAdapter.VehicleViewHolder> {

    public interface OnVehicleClickListener {
        void onVehicleClick(Vehicle vehicle);
    }

    private final List<Vehicle> vehicles = new ArrayList<>();
    private final OnVehicleClickListener onVehicleClickListener;

    public VehicleCardAdapter(OnVehicleClickListener onVehicleClickListener) {
        this.onVehicleClickListener = onVehicleClickListener;
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
        holder.bind(vehicle);
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

        VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            registrationView = itemView.findViewById(R.id.tv_vehicle_registration);
            summaryView = itemView.findViewById(R.id.tv_vehicle_summary);
        }

        void bind(Vehicle vehicle) {
            registrationView.setText(vehicle != null ? fallback(vehicle.getRegistration()) : "-");
            String make = vehicle != null ? fallback(vehicle.getMake()) : "-";
            String model = vehicle != null ? fallback(vehicle.getModel()) : "-";
            summaryView.setText((make + " " + model).trim());
        }

        private String fallback(String value) {
            return value == null || value.trim().isEmpty() ? "-" : value;
        }
    }
}
