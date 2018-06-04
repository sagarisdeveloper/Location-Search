package com.sagar.locationsearch.network;
import com.sagar.locationsearch.model.DistanceResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by sagar on 5/05/2018.
 */

public interface API_SERVICE {
  String URL_DISTANCEMATRIX = "api/directions/json";
  String KEY_DESTINATION = "destination";
  String KEY_ORIGINS = "origin";
  String KEY_API = "key";
  @GET(URL_DISTANCEMATRIX)
  Call<DistanceResponse> getDistance(
          @Query(KEY_ORIGINS) String origins_lat_lon,
          @Query(KEY_DESTINATION) String destination_lat_lon,
          @Query(KEY_API) String key
  );
}

