package com.ampsoft.MOTHistory.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.ampsoft.MOTHistory.data.model.Vehicle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class VehicleStore {

    private static final String PREFS_NAME = "vehicle_store";
    private static final String KEY_RECENT_VEHICLES = "recent_vehicles";
    private static final String KEY_SAVED_VEHICLES = "saved_vehicles";
    private static final int MAX_RECENT_VEHICLES = 5;

    private static final Gson GSON = new Gson();
    private static final Type VEHICLE_LIST_TYPE = new TypeToken<List<Vehicle>>() { }.getType();

    private VehicleStore() {
    }

    public static List<Vehicle> getRecentVehicles(Context context) {
        return loadVehicles(context, KEY_RECENT_VEHICLES);
    }

    public static void addRecentVehicle(Context context, Vehicle vehicle) {
        List<Vehicle> vehicles = loadVehicles(context, KEY_RECENT_VEHICLES);
        upsertVehicle(vehicles, vehicle);
        while (vehicles.size() > MAX_RECENT_VEHICLES) {
            vehicles.remove(vehicles.size() - 1);
        }
        saveVehicles(context, KEY_RECENT_VEHICLES, vehicles);
    }

    public static boolean removeRecentVehicle(Context context, String registration) {
        List<Vehicle> vehicles = loadVehicles(context, KEY_RECENT_VEHICLES);
        Vehicle existing = findVehicleByRegistration(vehicles, registration);
        if (existing == null) {
            return false;
        }
        vehicles.remove(existing);
        saveVehicles(context, KEY_RECENT_VEHICLES, vehicles);
        return true;
    }

    public static List<Vehicle> getSavedVehicles(Context context) {
        return loadVehicles(context, KEY_SAVED_VEHICLES);
    }

    public static Vehicle getSavedVehicleByRegistration(Context context, String registration) {
        return findVehicleByRegistration(loadVehicles(context, KEY_SAVED_VEHICLES), registration);
    }

    public static boolean isVehicleSaved(Context context, String registration) {
        return findVehicleByRegistration(loadVehicles(context, KEY_SAVED_VEHICLES), registration) != null;
    }

    public static boolean toggleSavedVehicle(Context context, Vehicle vehicle) {
        List<Vehicle> vehicles = loadVehicles(context, KEY_SAVED_VEHICLES);
        Vehicle existing = findVehicleByRegistration(vehicles, vehicle != null ? vehicle.getRegistration() : null);
        if (existing != null) {
            vehicles.remove(existing);
            saveVehicles(context, KEY_SAVED_VEHICLES, vehicles);
            return false;
        }
        upsertVehicle(vehicles, vehicle);
        saveVehicles(context, KEY_SAVED_VEHICLES, vehicles);
        return true;
    }

    private static void upsertVehicle(List<Vehicle> vehicles, Vehicle vehicle) {
        if (vehicle == null || isBlank(vehicle.getRegistration())) {
            return;
        }
        Iterator<Vehicle> iterator = vehicles.iterator();
        while (iterator.hasNext()) {
            Vehicle existing = iterator.next();
            if (sameRegistration(existing, vehicle.getRegistration())) {
                iterator.remove();
                break;
            }
        }
        vehicles.add(0, vehicle);
    }

    private static Vehicle findVehicleByRegistration(List<Vehicle> vehicles, String registration) {
        if (isBlank(registration)) {
            return null;
        }
        for (Vehicle vehicle : vehicles) {
            if (sameRegistration(vehicle, registration)) {
                return vehicle;
            }
        }
        return null;
    }

    private static boolean sameRegistration(Vehicle vehicle, String registration) {
        return vehicle != null
                && vehicle.getRegistration() != null
                && vehicle.getRegistration().equalsIgnoreCase(registration);
    }

    private static List<Vehicle> loadVehicles(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(key, "");
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Vehicle> vehicles = GSON.fromJson(json, VEHICLE_LIST_TYPE);
        return vehicles != null ? new ArrayList<>(vehicles) : new ArrayList<>();
    }

    private static void saveVehicles(Context context, String key, List<Vehicle> vehicles) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(key, GSON.toJson(vehicles)).apply();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
