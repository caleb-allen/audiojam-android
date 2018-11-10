package io.caleballen.audiojam;

import com.torchlighttech.api.ApiInterface;
import com.torchlighttech.api.TorchlightApiClient;
import com.torchlighttech.data.gson.SGson;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Created by caleb on 4/17/17.
 */

public class ApiClient {

    private static ApiInterface client;

    public static ApiInterface getInstance(){
        if (client == null) {
            if (Application.DEBUG) {
                RestAdapter.Builder builder = new RestAdapter.Builder();
                client = builder
                        .setClient(new MockApiClient())
                        .setEndpoint("http://bogus.com")
                        .setConverter(new GsonConverter(SGson.getInstance()))
                        .build()
                        .create(ApiInterface.class);
            }else{
                client = TorchlightApiClient.getInstance();
            }
        }
        return client;
    }
}
