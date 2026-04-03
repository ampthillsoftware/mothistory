package com.ampsoft.MOTHistory.data.api;

import android.text.TextUtils;

import com.ampsoft.MOTHistory.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class BackendAuthInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder requestBuilder = chain.request().newBuilder();
        if (!TextUtils.isEmpty(BuildConfig.MOT_BACKEND_APP_KEY)) {
            requestBuilder.header("x-app-key", BuildConfig.MOT_BACKEND_APP_KEY);
        }
        return chain.proceed(requestBuilder.build());
    }
}
