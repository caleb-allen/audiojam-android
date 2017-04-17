package io.caleballen.audiojam;

import com.torchlighttech.api.ApiInterface;
import com.torchlighttech.api.TorchlightApiClient;

/**
 * Created by caleb on 4/17/17.
 */

public class ApiClient {

    private static ApiInterface client;

    public static ApiInterface getInstance(){
        if (client == null) {
            if (Application.DEBUG) {
                client = MockApiClient.getInstance();
            }else{
                client = TorchlightApiClient.getInstance();
            }
        }
        return client;
    }
}
