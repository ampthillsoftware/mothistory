package com.ampsoft.MOTHistory.data.api;

import com.ampsoft.MOTHistory.data.model.Vehicle;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface MotApiService {

    @GET("v1/trade/vehicles/registration/{registration}")
    Call<Vehicle> getVehicleByRegistration(@Path("registration") String registration);
}
