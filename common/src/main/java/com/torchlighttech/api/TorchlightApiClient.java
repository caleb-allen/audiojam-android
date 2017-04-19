package com.torchlighttech.api;

import com.torchlighttech.Config;
import com.torchlighttech.data.gson.SGson;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.MockRetrofit;

/**
 * Created by caleb on 3/15/17.
 */

public class TorchlightApiClient {

    private static ApiInterface client;
    public static ApiInterface getInstance(){
        if (client == null) {

            Retrofit retrofit = getRetrofit();
            client = retrofit.create(ApiInterface.class);
        }
        return client;
    }

    public static Retrofit getRetrofit(){
        return new Retrofit.Builder()
                .baseUrl("http://someapi.com")
                .addConverterFactory(GsonConverterFactory.create(SGson.getInstance()))
                .build();
    }
}
