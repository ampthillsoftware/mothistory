package com.ampsoft.MOTHistory.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Vehicle implements Serializable {

    @SerializedName("registration")
    private String registration;

    @SerializedName("make")
    private String make;

    @SerializedName("model")
    private String model;

    @SerializedName("primaryColour")
    private String primaryColour;

    @SerializedName("fuelType")
    private String fuelType;

    @SerializedName("engineSize")
    private String engineSize;

    @SerializedName("firstUsedDate")
    private String firstUsedDate;

    @SerializedName("manufactureDate")
    private String manufactureDate;

    @SerializedName("registrationDate")
    private String registrationDate;

    @SerializedName("motTests")
    private List<MotTest> motTests;

    public String getRegistration() {
        return registration;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public String getPrimaryColour() {
        return primaryColour;
    }

    public String getFuelType() {
        return fuelType;
    }

    public String getEngineSize() {
        return engineSize;
    }

    public String getFirstUsedDate() {
        return firstUsedDate;
    }

    public String getManufactureDate() {
        return manufactureDate;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public List<MotTest> getMotTests() {
        return motTests;
    }
}
