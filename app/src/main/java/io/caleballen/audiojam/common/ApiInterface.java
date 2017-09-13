package io.caleballen.audiojam.common;

import io.caleballen.audiojam.data.Show;

import retrofit.Callback;
import retrofit.http.GET;

/**
 * Created by caleb on 3/15/17.
 */

public interface ApiInterface {
    @GET("/show/")
    void getShow(Callback<Show> callback);
}