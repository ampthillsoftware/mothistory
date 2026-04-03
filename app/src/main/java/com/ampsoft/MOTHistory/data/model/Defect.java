package com.ampsoft.MOTHistory.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Defect implements Serializable {

    @SerializedName("text")
    private String text;

    @SerializedName("type")
    private String type;

    @SerializedName("dangerous")
    private boolean dangerous;

    public String getText() {
        return text;
    }

    public String getType() {
        return type;
    }

    public boolean isDangerous() {
        return dangerous;
    }
}
