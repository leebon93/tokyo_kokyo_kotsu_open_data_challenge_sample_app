package com.tokyo_kokyo_kotsu_open_data_challenge_sample_app.service;

import com.tokyo_kokyo_kotsu_open_data_challenge_sample_app.entity.Station;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface StationService {
    @GET("v4/odpt:Station")
    Call<List<Station>> getStation(
            @Query("acl:consumerKey") String token,
            @Query("odpt:railway") String railway
    );
}
