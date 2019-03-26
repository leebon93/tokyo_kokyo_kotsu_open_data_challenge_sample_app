package com.tokyo_kokyo_kotsu_open_data_challenge_sample_app;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.tokyo_kokyo_kotsu_open_data_challenge_sample_app.entity.Station;
import com.tokyo_kokyo_kotsu_open_data_challenge_sample_app.service.StationService;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public String baseUrl = "https://api-tokyochallenge.odpt.org/api/";
    public String token = ""; // FIXME: 東京公共交通オープンデータチャレンジにエントリーし、token を記入
    public String railway = "odpt.Railway:Keisei.Main";
    public List<Station> stations;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(MapsActivity.this);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        final StationService service = retrofit.create(StationService.class);
        Call<List<Station>> call = service.getStation(token, railway);
        call.enqueue(new Callback<List<Station>>() {
            @Override
            public void onResponse(Call<List<Station>> call, Response<List<Station>> response) {
                // サーバーから京成本線の駅情報が入っている response が返ってくる
                stations = response.body();
                if (stations == null) {
                    Toast.makeText(MapsActivity.this, "駅情報が探せませんでした！m(_ _)m", Toast.LENGTH_LONG).show();
                    return;
                }

                // 京成本線の駅リストを stationCode 順に並び替える
                Comparator<Station> comparator = new Comparator<Station>() {
                    @Override
                    public int compare(Station station1, Station station2) {
                        // 京成本線の stationCode は KS22 のように KS＋数字で構成されている
                        String[] split1 = station1.stationCode.split("KS");
                        String[] split2 = station2.stationCode.split("KS");
                        String trimStationCode1 = split1[1];
                        String trimStationCode2 = split2[1];

                        return Integer.valueOf(trimStationCode1).compareTo(Integer.valueOf(trimStationCode2));
                    }
                };
                Collections.sort(stations, comparator);

                PolylineOptions polyOptions = new PolylineOptions();
                for (int i = 0; i < stations.size(); i++) {
                    Station station = stations.get(i);

                    // 緯度経度を取得する
                    double longitude = station.geoLong;
                    double latitude = station.geoLat;
                    // markerをつける
                    BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.icon_station);
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(latitude, longitude))
                            .title(station.title)
                            .snippet(station.railway)
                            .icon(bitmapDescriptor)
                            .anchor(0.5f, 0.5f));
                    station.markerId = marker.getId();

                    // 得した緯度経度で線を引く
                    polyOptions.add(new LatLng(latitude, longitude));
                    polyOptions.color(getColor(R.color.keisei_color));
                    polyOptions.width(15);
                }

                mMap.addPolyline(polyOptions);
            }

            @Override
            public void onFailure(Call<List<Station>> call, Throwable t) {
                Log.e("## onFailure ##", "## 駅レスポンスの取得失敗 : " + t.getMessage());
            }
        });

        // 上野駅を含んだ幾つの駅がちょうどいい感じに表示されるように指定する
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(35.72934631855064, 139.83522657305), 12.5F));
        // マップの右下に出る toolbar を非表示にする
        mMap.getUiSettings().setMapToolbarEnabled(false);
        // 駅をタップすると駅の名前と乗り換え線を表示
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                View view = getLayoutInflater().inflate(R.layout.info_window_view, null);
                // 駅名表示
                TextView title = view.findViewById(R.id.station_name_view);
                title.setText(marker.getTitle() + "駅");

                // 乗り換え線表示
                for (int i = 0; i < stations.size(); i++) {
                    if (marker.getId().equals(stations.get(i).markerId)) {
                        List<String> connectingRailwayList = stations.get(i).connectingRailwayList;
                        LinearLayout railwayView = view.findViewById(R.id.railway_container);
                        if (connectingRailwayList != null && connectingRailwayList.size() > 0) {
                            for (int j = 0; j < connectingRailwayList.size(); j++) {
                                View itemView = getLayoutInflater().inflate(R.layout.item_railway, null);

                                // 線路の名前は日本語で返ってこないのでハードコーディングで表示させる
                                String railwayName = connectingRailwayList.get(j);
                                if (railwayName.contains("JR-East.NaritaAirportBranch")) {
                                    setRailwayNameAndImage(itemView, "成田空港支線", R.drawable.line_jr); // JR
                                } else if (railwayName.contains("Keisei.Main")) {
                                    setRailwayNameAndImage(itemView, "本線", R.drawable.line_ks); // KS
                                } else if (railwayName.contains("Hokuso.Hokuso")) {
                                    setRailwayNameAndImage(itemView, "北総線", R.drawable.line_hs); // HS
                                } else if (railwayName.contains("Tobu.TobuUrbanPark")) {
                                    setRailwayNameAndImage(itemView, "東武アーバンパークライン", R.drawable.line_td); // TD
                                } else if (railwayName.contains("Keisei.Kanamachi")) {
                                    setRailwayNameAndImage(itemView, "京成金町線", R.drawable.line_ks); // KS
                                } else if (railwayName.contains("JR-East.Musashino")) {
                                    setRailwayNameAndImage(itemView, "武蔵野線", R.drawable.line_jm); // JM
                                } else if (railwayName.contains("Keisei.NaritaSkyAccess")) {
                                    setRailwayNameAndImage(itemView, "成田空港線", R.drawable.line_ks_ske_access); // KS(オレンジ)
                                } else if (railwayName.contains("JR-East.Narita")) {
                                    setRailwayNameAndImage(itemView, "成田線", R.drawable.line_jr); // JR
                                } else if (railwayName.contains("JR-East.NaritaAbikoBranch")) {
                                    setRailwayNameAndImage(itemView, "成田我孫子支線", R.drawable.line_jr); // JR
                                } else if (railwayName.contains("Keisei.HigashiNarita")) {
                                    setRailwayNameAndImage(itemView, "東成田線", R.drawable.line_ks); // KS
                                } else if (railwayName.contains("ToyoRapid.ToyoRapid")) {
                                    setRailwayNameAndImage(itemView, "東葉高速線", R.drawable.line_tr); // TR
                                } else if (railwayName.contains("Keisei.Chiba")) {
                                    setRailwayNameAndImage(itemView, "千葉線", R.drawable.line_ks); // KS
                                } else if (railwayName.contains("ShinKeisei.ShinKeisei")) {
                                    setRailwayNameAndImage(itemView, "新京成線", R.drawable.line_sl); // SL
                                } else if (railwayName.contains("JR-East.ChuoSobuLocal")) {
                                    setRailwayNameAndImage(itemView, "中央・総武線", R.drawable.line_jb); // JB
                                } else if (railwayName.contains("JR-East.Keiyo")) {
                                    setRailwayNameAndImage(itemView, "京葉線", R.drawable.line_je); // JE
                                } else if (railwayName.contains("TokyoMetro.Tozai")) {
                                    setRailwayNameAndImage(itemView, "東西線", R.drawable.line_t); // T
                                } else if (railwayName.contains("Toei.Shinjuku")) {
                                    setRailwayNameAndImage(itemView, "新宿線", R.drawable.line_s); // S
                                } else if (railwayName.contains("JR-East.SobuRapid")) {
                                    setRailwayNameAndImage(itemView, "総武線快速線", R.drawable.line_jo); // JO
                                } else if (railwayName.contains("Keisei.Oshiage")) {
                                    setRailwayNameAndImage(itemView, "押上線", R.drawable.line_ks); // KS
                                } else if (railwayName.contains("Tobu.TobuSkytree")) {
                                    setRailwayNameAndImage(itemView, "東武スカイツリーライン", R.drawable.line_ts); // TS
                                } else if (railwayName.contains("Toei.Arakawa")) {
                                    setRailwayNameAndImage(itemView, "東京さくらトラム", R.drawable.line_sa); // SA
                                } else if (railwayName.contains("TokyoMetro.Chiyoda")) {
                                    setRailwayNameAndImage(itemView, "千代田線", R.drawable.line_c); // C
                                } else if (railwayName.contains("JR-East.JobanRapid")) {
                                    setRailwayNameAndImage(itemView, "常磐線快速線", R.drawable.line_jj); // JJ
                                } else if (railwayName.contains("JR-East.KeihinTohokuNegishi")) {
                                    setRailwayNameAndImage(itemView, "京浜東北線・根岸線", R.drawable.line_jk); // JK
                                } else if (railwayName.contains("JR-East.Yamanote")) {
                                    setRailwayNameAndImage(itemView, "山手線", R.drawable.line_jy); // JY
                                } else if (railwayName.contains("Toei.NipporiToneri")) {
                                    setRailwayNameAndImage(itemView, "日暮里・舎人ライナー", R.drawable.line_nt); // NT
                                } else if (railwayName.contains("JR-East.Takasaki")) {
                                    setRailwayNameAndImage(itemView, "高崎線", R.drawable.line_ju); // JU
                                } else if (railwayName.contains("JR-East.Utsunomiya")) {
                                    setRailwayNameAndImage(itemView, "宇都宮線", R.drawable.line_ju); // JU
                                } else if (railwayName.contains("TokyoMetro.Ginza")) {
                                    setRailwayNameAndImage(itemView, "銀座線", R.drawable.line_g); // G
                                } else if (railwayName.contains("TokyoMetro.Hibiya")) {
                                    setRailwayNameAndImage(itemView, "日比谷線", R.drawable.line_h); // H
                                } else {
                                    setRailwayNameAndImage(itemView, connectingRailwayList.get(j), 0);
                                }

                                railwayView.addView(itemView);
                            }
                        } else {
                            railwayView.setVisibility(View.GONE);
                        }
                    }
                }
                return view;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });
    }

    /**
     * 乗り換え線とその線のマークをセットする
     *
     * @param itemView    View
     * @param railwayName 乗り換え線名
     * @param drawableId  マーク画像 ID
     */
    private void setRailwayNameAndImage(View itemView, String railwayName, int drawableId) {
        TextView railwayNameView = itemView.findViewById(R.id.railway_name_view);
        ImageView railwayIconView = itemView.findViewById(R.id.railway_icon);

        if (drawableId == 0) {
            railwayNameView.setText(railwayName);
            return;
        }

        railwayNameView.setText(railwayName);
        railwayIconView.setImageDrawable(ContextCompat.getDrawable(MapsActivity.this, drawableId));
    }
}
