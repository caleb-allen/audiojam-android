package com.torchlighttech.api;

import com.torchlighttech.Config;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.MockRetrofit;

/**
 * Created by caleb on 3/15/17.
 */

public class ApiClient {

    private static ApiInterface client;

    public static ApiInterface getApiClient(){
        if (client == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://someapi.com")
                    .build();


            if (Config.MOCK_API) {
                MockRetrofit mockRetrofit = new MockRetrofit.Builder(retrofit).build();
                BehaviorDelegate<ApiInterface> delegate = mockRetrofit.create(ApiInterface.class);
                MockApiClient mockApiClient = new MockApiClient(delegate);
                client =  mockApiClient;
            }else{
                client = retrofit.create(ApiInterface.class);
            }
        }
        return client;
    }
}
