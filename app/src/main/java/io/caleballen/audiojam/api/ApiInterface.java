package io.caleballen.audiojam.api;

import io.caleballen.audiojam.data.Show;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by caleb on 3/15/17.
 */

public interface ApiInterface {
    @GET("/show/")
    Call<Show> getShow();
}