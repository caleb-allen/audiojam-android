package io.caleballen.audiojam;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;

import retrofit.client.Client;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import timber.log.Timber;

/**
 * Created by caleb on 3/15/17.
 */

public class MockApiClient implements Client {

    @Override
    public Response execute(Request request) throws IOException {
        String url = request.getUrl();

        Log.d("MOCK SERVER", "fetching uri: " + url);

        String s = "";

        switch (url) {
            case "http://bogus.com/show/":
                try {
                    s = getStringFromFile(Application.getInstance(), "mock-shows.json");
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }

        Timber.d("Response:");
        Timber.d(s);

        return new Response(request.getUrl(), 200, "nothing", Collections.EMPTY_LIST, new TypedByteArray("application/json", s.getBytes()));
    }

    /*public static ApiInterface getInstance(){
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
    }*/

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

}
