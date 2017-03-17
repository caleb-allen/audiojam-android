package com.torchlighttech.api;

import com.torchlighttech.data.Show;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by caleb on 3/15/17.
 */

public interface ApiInterface {
    @GET("/show/")
    Call<Show> getShow();
}