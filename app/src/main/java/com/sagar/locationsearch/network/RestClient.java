package com.sagar.locationsearch.network;


import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
/**
 * Created by sagar on 5/05/2018.
 */
public class RestClient {
  private static RestClient self;
  private Retrofit retrofit;
  private String API_BASE_URL = "https://maps.googleapis.com/maps/";
  public RestClient() {
    HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
    interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
    OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(interceptor).build();

    Retrofit.Builder builder =
            new Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create());
    retrofit = builder.client(httpClient).build();
  }

  public static RestClient getInstance() {
    if (self == null) {
      synchronized(RestClient.class) {
        if (self == null) {
          self = new RestClient();
        }
      }
    }
    return self;
  }

  public Retrofit getRetrofit() {
    return retrofit;
  }
}
