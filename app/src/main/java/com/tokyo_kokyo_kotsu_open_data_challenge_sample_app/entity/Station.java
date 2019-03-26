package com.tokyo_kokyo_kotsu_open_data_challenge_sample_app.entity;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Station {

    @SerializedName("geo:lat")
    public double geoLat;

    @SerializedName("geo:long")
    public double geoLong;

    @SerializedName("dc:title")
    public String title;

    @SerializedName("odpt:railway")
    public String railway;

    @SerializedName("odpt:stationCode")
    public String stationCode;

    @SerializedName("odpt:connectingRailway")
    public List<String> connectingRailwayList;

    public String markerId;

}
