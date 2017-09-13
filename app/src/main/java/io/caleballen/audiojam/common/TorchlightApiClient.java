package io.caleballen.audiojam.common;

import io.caleballen.audiojam.data.gson.SGson;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;


/**
 * Created by caleb on 3/15/17.
 */

public class TorchlightApiClient {

    private static ApiInterface client;
    public static ApiInterface getInstance(){
        if (client == null) {
            RestAdapter.Builder builder = new RestAdapter.Builder()
                    .setEndpoint("http://someapi.com")
                    .setLogLevel(RestAdapter.LogLevel.BASIC)
                    .setConverter(new GsonConverter(SGson.getInstance()));
            client = builder.build().create(ApiInterface.class);
        }
        return client;
    }

    /*public static Retrofit getRetrofit(){
        return new Retrofit.Builder()
                .baseUrl("http://someapi.com")
                .addConverterFactory(GsonConverterFactory.create(SGson.getInstance()))
                .build();
    }*/
}
