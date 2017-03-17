package com.torchlighttech.api;

import android.content.Context;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.torchlighttech.data.Show;
import retrofit2.Call;
import retrofit2.mock.BehaviorDelegate;

/**
 * Created by caleb on 3/15/17.
 */

public class MockApiClient implements ApiInterface {

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
        Gson gson = new Gson();
        String s = "{\n" +
                "  \"name\": \"Demo Show\",\n" +
                "  \"id\": \"1\",\n" +
                "  \"events\":[\n" +
                "    {\n" +
                "      \"start_time\": 1000,\n" +
                "      \"duration\": 500,\n" +
                "      \"effect\":{\n" +
                "        \"flash\": true\n" +
                "      },\n" +
                "      \"peripheral\": {\n" +
                "        \"screen\":{\n" +
                "          \"on_color\": \"#00000\",\n" +
                "          \"off_color\": \"#111111\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"start_time\": 1500,\n" +
                "      \"duration\": 500,\n" +
                "      \"effect\":{\n" +
                "        \"strobe\":{\n" +
                "          \"time_on\": 50,\n" +
                "          \"time_off\": 100\n" +
                "        }\n" +
                "      },\n" +
                "      \"peripheral\":{\n" +
                "        \"torch\": true\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"start_time\": 2000,\n" +
                "      \"duration\": 500,\n" +
                "      \"effect\":{\n" +
                "        \"fade\":true\n" +
                "      },\n" +
                "      \"peripheral\":{\n" +
                "        \"screen\": {\n" +
                "          \"on_color\": \"#00000\",\n" +
                "          \"off_color\": \"#111111\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
//        try {
//            s = getStringFromFile(Application.getInstance(), "mock-shows.json");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        Show show = gson.fromJson(s, Show.class);
        return delegate.returningResponse(show).getShow();
    }
}
