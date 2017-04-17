package io.caleballen.audiojam;

import android.content.Context;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.torchlighttech.api.ApiInterface;
import com.torchlighttech.api.TorchlightApiClient;
import com.torchlighttech.data.Show;
import com.torchlighttech.data.gson.SGson;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.MockRetrofit;

/**
 * Created by caleb on 3/15/17.
 */

public class MockApiClient implements ApiInterface {

    public static ApiInterface getInstance(){
        Retrofit retrofit = TorchlightApiClient.getRetrofit();
        MockRetrofit mockRetrofit = new MockRetrofit.Builder(retrofit).build();
        BehaviorDelegate<ApiInterface> delegate = mockRetrofit.create(ApiInterface.class);
        MockApiClient mockApiClient = new MockApiClient(delegate);
        return mockApiClient;
    }

    private final BehaviorDelegate<ApiInterface> delegate;

    public MockApiClient(BehaviorDelegate<ApiInterface> delegate) {
        this.delegate = delegate;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(Context context, String filePath) throws Exception {
        final InputStream stream = context.getResources().getAssets().open(filePath);

        String ret = convertStreamToString(stream);
        //Make sure you close all streams.
        stream.close();
        return ret;
    }

    @Override
    public Call<Show> getShow() {
        Gson gson = SGson.getInstance();
        String s = "";
        try {
            s = getStringFromFile(Application.getInstance(), "mock-shows.json");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Show show = gson.fromJson(s, Show.class);
        return delegate.returningResponse(show).getShow();
    }
}
