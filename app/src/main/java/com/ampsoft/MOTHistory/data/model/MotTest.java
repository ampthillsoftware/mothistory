package com.ampsoft.MOTHistory.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class MotTest implements Serializable {

    @SerializedName("completedDate")
    private String completedDate;

    @SerializedName("testResult")
    private String testResult;

    @SerializedName("expiryDate")
    private String expiryDate;

    @SerializedName("odometerValue")
    private String odometerValue;

    @SerializedName("odometerUnit")
    private String odometerUnit;

    @SerializedName("motTestNumber")
    private String motTestNumber;

    @SerializedName("defects")
    private List<Defect> defects;

    public String getCompletedDate() {
        return completedDate;
    }

    public String getTestResult() {
        return testResult;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public String getOdometerValue() {
        return odometerValue;
    }

    public String getOdometerUnit() {
        return odometerUnit;
    }

    public String getMotTestNumber() {
        return motTestNumber;
    }

    public List<Defect> getDefects() {
        return defects;
    }
}
