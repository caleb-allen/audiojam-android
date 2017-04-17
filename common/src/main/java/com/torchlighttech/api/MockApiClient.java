package com.torchlighttech.api;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.torchlighttech.data.Show;
import com.torchlighttech.data.gson.SGson;

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

/*    public static String getStringFromFile(Context context, String filePath) throws Exception {
        final InputStream stream = context.getResources().getAssets().open(filePath);

        String ret = convertStreamToString(stream);
        //Make sure you close all streams.
        stream.close();
        return ret;
    }*/

    @Override
    public Call<Show> getShow() {
        Gson gson = SGson.getInstance();
        String s = "{\n" +
                "  \"name\": \"Demo Show\",\n" +
                "  \"id\": \"1\",\n" +
                "  \"events\":[\n" +
                "    {\n" +
                "      \"start_time\": 5000,\n" +
                "      \"effect\":{\n" +
                "        \"duration\": 2000,\n" +
                "        \"type\": \"flash\"\n" +
                "      },\n" +
                "      \"peripheral\": {\n" +
                "        \"screen\":{\n" +
                "          \"on_color\": \"#00000\",\n" +
                "          \"off_color\": \"#111111\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"start_time\": 8000,\n" +
                "      \"effect\":{\n" +
                "        \"duration\": 2000,\n" +
                "        \"type\":\"strobe\",\n" +
                "        \"time_on\": 200,\n" +
                "        \"time_off\": 200\n" +
                "      },\n" +
                "      \"peripheral\":{\n" +
                "        \"torch\": true\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"start_time\": 11000,\n" +
                "      \"effect\":{\n" +
                "        \"duration\": 2000,\n" +
                "        \"type\":\"strobe\",\n" +
                "        \"time_on\": 500,\n" +
                "        \"time_off\": 500\n" +
                "      },\n" +
                "      \"peripheral\":{\n" +
                "        \"screen\": {\n" +
                "          \"on_color\": \"#00000\",\n" +
                "          \"off_color\": \"#111111\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"start_time\": 14000,\n" +
                "      \"effect\":{\n" +
                "        \"duration\": 3000,\n" +
                "        \"type\":\"strobe\",\n" +
                "        \"time_on\": 50,\n" +
                "        \"time_off\": 50\n" +
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
