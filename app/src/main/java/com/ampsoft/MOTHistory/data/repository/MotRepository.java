package com.ampsoft.MOTHistory.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ampsoft.MOTHistory.data.api.MotApiService;
import com.ampsoft.MOTHistory.data.model.Vehicle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class MotRepository {

    private static final String TAG = "MotRepository";

    private final MotApiService motApiService;
    private final ExecutorService executorService;

    public MotRepository(MotApiService motApiService) {
        this.motApiService = motApiService;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<RepositoryResult<Vehicle>> lookupByRegistration(String normalizedRegistration) {
        MutableLiveData<RepositoryResult<Vehicle>> result = new MutableLiveData<>();
        result.setValue(RepositoryResult.loading());

        executorService.execute(() -> {
            try {
                Response<Vehicle> response = motApiService
                        .getVehicleByRegistration(normalizedRegistration)
                        .execute();

                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(RepositoryResult.success(response.body()));
                    return;
                }

                int httpCode = response.code();
                result.postValue(RepositoryResult.error(mapHttpErrorMessage(httpCode), httpCode));
            } catch (Exception e) {
                Log.e(TAG, "DVSA lookup failed", e);
                result.postValue(RepositoryResult.error(
                        "Unable to contact MOT service.",
                        0
                ));
            }
        });

        return result;
    }

    private String mapHttpErrorMessage(int httpCode) {
        switch (httpCode) {
            case 404:
                return "No vehicle found for that registration.";
            case 403:
                return "Access denied by MOT service. Please try again later.";
            case 429:
                return "Rate limit reached. Please try again later.";
            case 500:
                return "MOT service is unavailable right now. Please try again later.";
            default:
                return "Vehicle lookup failed. Please try again.";
        }
    }
}
