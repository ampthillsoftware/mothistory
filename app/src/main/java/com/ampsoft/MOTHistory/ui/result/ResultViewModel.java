package com.ampsoft.MOTHistory.ui.result;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ResultViewModel extends ViewModel {

    private final MutableLiveData<String> registration = new MutableLiveData<>("");
    private final MutableLiveData<String> vehicleSummary = new MutableLiveData<>("");

    public LiveData<String> getRegistration() {
        return registration;
    }

    public LiveData<String> getVehicleSummary() {
        return vehicleSummary;
    }

    public void setVehicle(String registrationValue, String make, String model) {
        registration.setValue(registrationValue == null ? "" : registrationValue);
        String makePart = make == null ? "" : make;
        String modelPart = model == null ? "" : model;
        vehicleSummary.setValue((makePart + " " + modelPart).trim());
    }
}
