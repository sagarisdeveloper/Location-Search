package com.sagar.locationsearch;

import android.annotation.SuppressLint;

import android.app.Service;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.Manifest;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;

import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.sagar.locationsearch.model.DistanceResponse;

import com.sagar.locationsearch.model.Helper;
import com.sagar.locationsearch.model.Leg;
import com.sagar.locationsearch.model.Polyline;
import com.sagar.locationsearch.model.Route;
import com.sagar.locationsearch.model.Step;
import com.sagar.locationsearch.network.API_SERVICE;
import com.sagar.locationsearch.network.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.google.android.gms.location.places.Place.*;


/**
 * Created by sagar on 5/05/2018.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {


    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String CROSS_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private static final int LOCATION_REQ_CODE = 1245;
    private static final float DEFAULT_ZOOM = 15f;
    private static final String TAG = "MapActivity";

    LatLngBounds latLngBounds = new LatLngBounds(
            new LatLng(47.64299816, -122.14351988),
            new LatLng(50.64299816, -122.14351988));

    private boolean mLocationPermisstionGranted = false;

    //widgets
    private AutoCompleteTextView acFrom, acTo;
    ImageView ivClear, ivClear2, ivGPS,ivBack;
    Button buttonDone;
    LinearLayout bottomView;
    TextView tvTo, tvFrom, tvTime, tvKm;
    ImageView ivHideDialog;
    //variables
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private PlaceAutocompleteAdapter adapterPlaceFrom, adapterPlaceTo;
    private GoogleApiClient mGoogleApiClient;
    final int sdk = android.os.Build.VERSION.SDK_INT;
    private Place mPlace;
    private LatLng latLngFrom, latLngTo;
    private String addForm = null, addTo = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        acFrom = findViewById(R.id.autocomplete_from);
        acTo = findViewById(R.id.autocomplete_to);
        ivClear = findViewById(R.id.cross);
        ivClear2 = findViewById(R.id.cross2);
        ivBack = findViewById(R.id.goback);
        ivGPS = findViewById(R.id.mylocation);
        buttonDone = findViewById(R.id.buttonDone);
        bottomView = findViewById(R.id.bottomView);
        tvFrom = findViewById(R.id.textForm);
        tvTo = findViewById(R.id.textTo);
        tvTime = findViewById(R.id.textTime);
        tvKm = findViewById(R.id.textKm);
        ivHideDialog = findViewById(R.id.hideDialog);

        getLocationPermisstion();
    }

    void initMap() {
        Log.i(TAG, "initMap: ");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    void init() {
        Log.d(TAG, "init: ");
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        acFrom.setOnItemClickListener(onItemClickListenerForm);
        acTo.setOnItemClickListener(onItemClickListenerTo);

        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(TYPE_ROUTE)
                .setTypeFilter(TYPE_NEIGHBORHOOD)
                .setTypeFilter(TYPE_STREET_ADDRESS)
                .setTypeFilter(TYPE_TRAIN_STATION)
                .setTypeFilter(TYPE_LOCALITY)
                .setTypeFilter(TYPE_SUBLOCALITY)
                .build();
        adapterPlaceFrom = new PlaceAutocompleteAdapter(this, mGoogleApiClient, latLngBounds, typeFilter);
        adapterPlaceTo = new PlaceAutocompleteAdapter(this, mGoogleApiClient, latLngBounds, typeFilter);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapActivity.this.finish();
            }
        });
        acFrom.setAdapter(adapterPlaceFrom);
        acTo.setAdapter(adapterPlaceTo);

        // on done or enter button zoom map and add the marker on map
        acFrom.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                try {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH
                            || actionId == EditorInfo.IME_ACTION_DONE
                            || event.getAction() == KeyEvent.ACTION_DOWN
                            || event.getAction() == KeyEvent.KEYCODE_ENTER)
                        geoLocate();
                } catch (Exception e) {
                    Log.e(TAG, "onEditorAction: " + e.getMessage());
                }
                return false;
            }

        });
        acTo.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                try {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH
                            || actionId == EditorInfo.IME_ACTION_DONE
                            || event.getAction() == KeyEvent.ACTION_DOWN
                            || event.getAction() == KeyEvent.KEYCODE_ENTER)
                        geoLocate();
                } catch (Exception e) {
                    Log.e(TAG, "onEditorAction: " + e.getMessage());
                }

                return false;
            }

        });
        //Clear icon visibility set
        acFrom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                adapterPlaceFrom.getFilter().filter(s.toString());
                try {
                    if (acFrom.getText().length() > 0)
                        ivClear.setVisibility(View.VISIBLE);
                    else
                        ivClear.setVisibility(View.GONE);
                } catch (Exception e) {
                    Log.e(TAG, "beforeTextChanged: " + e.getMessage());
                }
            }
        });
        //Clear icon visibility set
        acTo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                try {
                    if (acTo.getText().length() > 0)
                        ivClear2.setVisibility(View.VISIBLE);
                    else
                        ivClear2.setVisibility(View.GONE);
                } catch (Exception e) {
                    Log.e(TAG, "beforeTextChanged: " + e.getMessage());
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                adapterPlaceTo.getFilter().filter(s.toString());
            }
        });
        //clear text from from search bar
        ivClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acFrom.setText("");
                ivClear.setVisibility(View.GONE);
            }
        });
        //clear text from 'to' search bar
        ivClear2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acTo.setText("");
                ivClear2.setVisibility(View.GONE);
            }
        });
        //Done button event
        buttonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateDistance();

            }
        });
        ivHideDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomView.setVisibility(View.GONE);
                buttonDone.setVisibility(View.VISIBLE);
            }
        });
        //focus on current location
        ivGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDeviceLocation();
            }
        });

    }

    void geoLocate() {
        Log.d(TAG, "geoLocate: ");
        String serachKey = acFrom.getText().toString();
        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> addressList = new ArrayList<>();
        try {
            addressList = geocoder.getFromLocationName(serachKey, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: Error" + e.getMessage());
        }

        if (addressList.size() > 0) {
            Address address = addressList.get(0);
            Log.d(TAG, "geoLocate: location found" + address.toString());
            moveCameraFrom(new LatLng(address.getLatitude(), address.getLatitude()), DEFAULT_ZOOM, address.getLocality());
        }
    }


    //getting device current location
    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: ");
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermisstionGranted) {
                @SuppressLint("MissingPermission")
                Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location ");
                            Location actualLocation = (Location) task.getResult();
                            if (acFrom.getText().length() == 0) {
                                try {
                                    acFrom.setText(new Geocoder(MapActivity.this, Locale.getDefault()).getFromLocation(actualLocation.getLatitude(), actualLocation.getLongitude(), 1).get(0).getAddressLine(0));
                                    //acFrom.setSelection(acFrom.getText().toString().length());
                                    acTo.requestFocus();
                                } catch (IOException e) {
                                    Log.e(TAG, "onComplete: error " + e.getMessage());
                                }
                            }
                            moveCameraFrom(new LatLng(actualLocation.getLatitude(), actualLocation.getLongitude()), DEFAULT_ZOOM, "My Location");
                        } else {
                            Log.e(TAG, "onComplete: current location is null");
                            Toast.makeText(MapActivity.this, "unable to get the current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void moveCameraFrom(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: " + zoom);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        if (!title.equals("My Location")) {

            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin))
                    .title(title);
            mMap.addMarker(options);

        }
        latLngFrom = latLng;
        hideKeyboard();
    }

    void moveCameraTo(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: " + zoom);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        if (!title.equals("My Location")) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin2))
                    .title(title);
            mMap.addMarker(options);
            latLngTo = latLng;
            hideKeyboard();

            acTo.clearFocus();
            calculateDistance();
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "onMapReady: ");
        mMap = googleMap;
        if (mLocationPermisstionGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    hideKeyboard();
                    acFrom.clearFocus();
                    acTo.clearFocus();
                }
            });
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_style));

            if (!success) {
                Log.e("MapsActivityRaw", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapsActivityRaw", "Can't find style.", e);
        }
    }

    //Getting device permission

    void getLocationPermisstion() {
        Log.i(TAG, "getLocationPermisstion: ");
        String[] permisstion = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermisstionGranted = true;
                initMap();
                init();
            } else {
                ActivityCompat.requestPermissions(this, permisstion, LOCATION_REQ_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permisstion, LOCATION_REQ_CODE);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult: ");
        mLocationPermisstionGranted = false;
        switch (requestCode) {

            case LOCATION_REQ_CODE: {
                if (grantResults.length > 0) {

                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "onRequestPermissionsResult: permisstion Fail");
                            mLocationPermisstionGranted = false;
                            return;
                            //inti map
                        }

                    }
                    Log.d(TAG, "onRequestPermissionsResult: Permisstion granted");
                    mLocationPermisstionGranted = true;
                    initMap();
                    init();
                }
            }


        }

    }

    //Hide soft keyboard
    void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(acTo.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(acFrom.getWindowToken(), 0);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    private AdapterView.OnItemClickListener onItemClickListenerForm = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final AutocompletePrediction prediction = adapterPlaceFrom.getItem(position);
            String placeId = prediction.getPlaceId();
            PendingResult<PlaceBuffer> pendingResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            pendingResult.setResultCallback(mUpdateCallBackFrom);
        }
    };
    private AdapterView.OnItemClickListener onItemClickListenerTo = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final AutocompletePrediction prediction = adapterPlaceTo.getItem(position);
            String placeId = prediction.getPlaceId();
            PendingResult<PlaceBuffer> pendingResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            pendingResult.setResultCallback(mUpdateCallBackTo);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdateCallBackFrom = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.d(TAG, "onResult: Place query did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);
            try {
                mPlace = place;
                addForm = place.getAddress().toString();
            } catch (NullPointerException e) {
                Log.e(TAG, "onResult: NullPointerException: " + e.getMessage());
            }
            moveCameraFrom(new LatLng(place.getViewport().getCenter().latitude, place.getViewport().getCenter().longitude), DEFAULT_ZOOM, mPlace.getName().toString());
            places.release();
        }
    };
    private ResultCallback<PlaceBuffer> mUpdateCallBackTo = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.d(TAG, "onResult: Place query did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);
            try {
                mPlace = place;
                addTo = place.getAddress().toString();
            } catch (NullPointerException e) {
                Log.e(TAG, "onResult: NullPointerException: " + e.getMessage());
            }
            moveCameraTo(new LatLng(place.getViewport().getCenter().latitude, place.getViewport().getCenter().longitude), DEFAULT_ZOOM, mPlace.getName().toString());
            places.release();
        }
    };

    private void calculateDistance() {
        if (mLocationPermisstionGranted) {
            if (latLngFrom != null && acFrom.length() > 0) {
                if (latLngTo != null && acTo.length() > 0) {
                    LatLng origin = latLngFrom;
                    LatLng destination = latLngTo;
                    //use Google Direction API to get the route between these Locations
                    String directionApiPath = Helper.getUrl(String.valueOf(origin.latitude), String.valueOf(origin.longitude),
                            String.valueOf(destination.latitude), String.valueOf(destination.longitude));
                    Log.d(TAG, "Path " + directionApiPath);

                    getDistanceInfo();

                    buttonDone.setVisibility(View.GONE);
                    tvFrom.setText(acFrom.getText());
                    tvTo.setText(acTo.getText());
                    bottomView.setVisibility(View.VISIBLE);
                } else Toast.makeText(this, "Select Start Location", Toast.LENGTH_SHORT).show();
            } else Toast.makeText(this, "Select End Location", Toast.LENGTH_SHORT).show();
        }
    }

    private String getCurrentCoordinate(LatLng latLng) {
        Log.d(TAG, "getCurrentCoordinate: ");
        if (latLng == null) return "";
        return latLng.latitude + "," + latLng.longitude;
    }

    private void getDistanceInfo() {
        Log.d(TAG, "getDistanceInfo: ");
        API_SERVICE client = RestClient.getInstance().getRetrofit().create(API_SERVICE.class);

        Call<DistanceResponse> call = client.getDistance(
                getCurrentCoordinate(latLngFrom),
                getCurrentCoordinate(latLngTo),
                getString(R.string.api_key)
        );
        call.enqueue(new Callback<DistanceResponse>() {
            @Override
            public void onResponse(Call<DistanceResponse> call, Response<DistanceResponse> response) {
                if (response.body() != null) {
                    Log.d(TAG, "onResponse: Success");
                    List<LatLng> mDirections = getDirectionPolylines(response.body().getRoutes());
                    drawRouteOnMap(mMap, mDirections);
                }
            }

            @Override
            public void onFailure(Call<DistanceResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    /********************************************************************************************************/
    private void showTravelDistance(String km, String time) {
        Log.d(TAG, "showTravelDistance: km : " + km);
        tvKm.setText(km);
        tvTime.setText(time);
    }

    private List<LatLng> getDirectionPolylines(List<Route> routes) {
        Log.d(TAG, "getDirectionPolylines: ");
        List<LatLng> directionList = new ArrayList<LatLng>();
        for (Route route : routes) {
            List<Leg> legs = route.getLegs();
            for (Leg leg : legs) {
                String routeDistance = leg.getDistance().getText();
                String routeDuration = leg.getDuration().getText();
                showTravelDistance(routeDistance, routeDuration);
                List<Step> steps = leg.getSteps();
                for (Step step : steps) {
                    Polyline polyline = step.getPolyline();
                    String points = polyline.getPoints();
                    List<LatLng> singlePolyline = decodePoly(points);
                    for (LatLng direction : singlePolyline) {
                        directionList.add(direction);
                    }
                }
            }
        }
        return directionList;
    }

    private void drawRouteOnMap(GoogleMap map, List<LatLng> positions) {
        Log.d(TAG, "drawRouteOnMap: ");
        PolylineOptions options = new PolylineOptions().width(5).color(Color.RED).geodesic(true);
        options.addAll(positions);
        com.google.android.gms.maps.model.Polyline polyline = map.addPolyline(options);
    }

    private List<LatLng> decodePoly(String encoded) {
        Log.d(TAG, "decodePoly: ");
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
}