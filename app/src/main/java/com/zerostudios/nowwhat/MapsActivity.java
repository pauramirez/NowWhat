package com.zerostudios.nowwhat;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.zerostudios.nowwhat.Model.MyPlaces;
import com.zerostudios.nowwhat.Model.Results;
import com.zerostudios.nowwhat.Remote.IGoogleAPIService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    private static final int MY_PERMISSION_CODE = 1000;
    private GoogleMap mMap;

    private FirebaseAuth mAuth;
    private DatabaseReference currentUserDb;
    private double latitude,longitude;
    private Location mLastLocation;
    private Marker mMarker;
    private ImageView mCompass;
    private float[]  mGravity  = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimuth = 0f;
    private int count;
    private float currentAzimth = 0f;
    private boolean hasChanged = false;

    private FirebaseAnalytics mFirebaseAnalytics;

    SensorManager sensorManager;
    Sensor sensor;


    MyPlaces currentPlace;
    IGoogleAPIService mService;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        sensorManager =  (SensorManager)getSystemService(Service.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mCompass = findViewById(R.id.compass);
        mAuth = FirebaseAuth.getInstance();
        mService = Common.getGoogleAPIService();


        //Request RuntimePermission
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
        {
            checkLocationPermission();
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setItemIconTintList(null);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                //Code Late

                switch (item.getItemId())
                {
                    case R.id.restaurant:
                        nearByPlace("restaurant");
                        break;

                    case R.id.museums:
                        nearByPlace("museum");
                        break;

                    case R.id.parks:
                        nearByPlace("parks");
                        break;


                    default:
                        break;
                }
                return true;
            }
        });


        buildLocationCallBack();
        buildLocationRequest();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest,locationCallback, Looper.myLooper());

    }

    @Override
    protected void onStop()
    {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    private void buildLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setSmallestDisplacement(10f);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    private void buildLocationCallBack()
    {
        locationCallback = new LocationCallback()
        {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mLastLocation = locationResult.getLastLocation();
                if(mMarker != null)
                {
                    mMarker.remove();
                }


                latitude = mLastLocation.getLatitude();
                longitude =mLastLocation.getLongitude();

                LatLng latLng = new LatLng(latitude,longitude);

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title("Your Position")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                mMarker = mMap.addMarker(markerOptions);
                // Move Camera
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

            }
        };
    }


    private void nearByPlace(final String placeType)
    {
        mMap.clear();
        String url = getUrl(latitude,longitude,placeType);
        mService.getNearByPlaces(url)
                .enqueue(new Callback<MyPlaces>() {
                    @Override
                    public void onResponse(Call<MyPlaces> call, Response<MyPlaces> response) {
                        currentPlace = response.body();

                        if(response.isSuccessful())
                        {
                            for (int i = 0; i < response.body().getResults().length ; i++)
                            {
                                MarkerOptions markerOptions = new MarkerOptions();
                                Results googlePlace = response.body().getResults()[i];
                                double lat = Double.parseDouble(googlePlace.getGeometry().getLocation().getLat());
                                double lgn = Double.parseDouble(googlePlace.getGeometry().getLocation().getLng());
                                String placeName  = googlePlace.getName();
                                String vicinity = googlePlace.getVicinity();
                                LatLng latLng = new LatLng(lat,lgn);
                                markerOptions.position(latLng);
                                markerOptions.title(placeName);
                                if(placeType.equals("restaurant"))
                                {
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
                                    currentUserDb = FirebaseDatabase.getInstance().getReference().child("Places").child("restaurant").child(googlePlace.getPlace_id());
                                    currentUserDb.setValue(placeName);
                                }


                                else if(placeType.equals("museum"))
                                {
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                                    currentUserDb = FirebaseDatabase.getInstance().getReference().child("Places").child("museum").child(googlePlace.getPlace_id());
                                    currentUserDb.setValue(placeName);
                                }


                                else
                                {
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                    currentUserDb = FirebaseDatabase.getInstance().getReference().child("Places").child("parks").child(googlePlace.getPlace_id());
                                    currentUserDb.setValue(placeName);
                                }


                                markerOptions.snippet(String.valueOf(i)); //Asign index of marker
                                mMap.addMarker(markerOptions);
                                //Move Camera
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));


                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<MyPlaces> call, Throwable t) {

                    }
                });
    }

    private String getUrl(double latitude, double longitude, String placeType)
    {
        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location="+latitude+","+longitude);
        googlePlacesUrl.append("&radius="+1000);
        googlePlacesUrl.append("&type="+placeType);
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key="+"AIzaSyBzFxZlTXSDWc0Sz7whTsvCS7si_knnWPs");
        Log.d("getUrl",googlePlacesUrl.toString());
        return googlePlacesUrl.toString();
    }

    private boolean checkLocationPermission()
    {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION))
            {
                ActivityCompat.requestPermissions(this,new String[]{

                        android.Manifest.permission.ACCESS_FINE_LOCATION
                },MY_PERMISSION_CODE);
            }
            else
                ActivityCompat.requestPermissions(this,new String[]{

                        android.Manifest.permission.ACCESS_FINE_LOCATION
                },MY_PERMISSION_CODE);

            return false;
        }
        else
            return true;

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case MY_PERMISSION_CODE:
            {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
                    {

                        mMap.setMyLocationEnabled(true);
                        buildLocationCallBack();
                        buildLocationRequest();

                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest,locationCallback, Looper.myLooper());
                    }
                }

            }
            break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        }
        else
        {
            mMap.setMyLocationEnabled(true);
        }

        // Make event
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker)
            {
                if(marker.getSnippet()!=null) {
                    Common.currentResult = currentPlace.getResults()[Integer.parseInt(marker.getSnippet())];
                    //New Activity
                    startActivity(new Intent(MapsActivity.this, ViewPlace.class));
                }
                return true;
            }
        });
    }


    public void logOutUser(View view)
    {
        mAuth.signOut();
        Intent intent = new Intent(MapsActivity.this,LoginActivity.class);
        startActivity(intent);
        finish();
        return;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_GAME);


    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;

        synchronized (this){
            if(sensorEvent.sensor.getType()==Sensor.TYPE_ACCELEROMETER)
            {
                mGravity[0] =  alpha*mGravity[0] + (1-alpha)*sensorEvent.values[0];
                mGravity[1] =  alpha*mGravity[1] + (1-alpha)*sensorEvent.values[1];
                mGravity[2] =  alpha*mGravity[2] + (1-alpha)*sensorEvent.values[2];
            }
            if(sensorEvent.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD)
            {
                mGeomagnetic[0] =  alpha*mGeomagnetic[0] + (1-alpha)*sensorEvent.values[0];
                mGeomagnetic[1] =  alpha*mGeomagnetic[1] + (1-alpha)*sensorEvent.values[1];
                mGeomagnetic[2] =  alpha*mGeomagnetic[2] + (1-alpha)*sensorEvent.values[2];
            }

            float R[] =  new float[9];
            float I[]  = new float[9];
            boolean succes = SensorManager.getRotationMatrix(R,I,mGravity,mGeomagnetic);
            if(succes)
            {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R,orientation);
                azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth+360)%360;
                Animation anim = new RotateAnimation(-currentAzimth,-azimuth,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
                currentAzimth = azimuth;

                anim.setDuration(500);
                anim.setRepeatCount(0);
                anim.setFillAfter(true);

                mCompass.startAnimation(anim);
            }
        }

        if(sensorEvent.sensor.getType()==Sensor.TYPE_LIGHT)
        {
            if(sensorEvent.values[0]<30)
            {
                SensorLight(true);
                if(!hasChanged)
                {
                    hasChanged = true;
                    count++;
                    currentUserDb = FirebaseDatabase.getInstance().getReference().child("Sensores").child("noche");
                    currentUserDb.setValue(count);
                }


            }
            else{
                hasChanged = false;
                SensorLight(false);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void SensorLight(boolean  light)
    {
        if(light && mMap != null)
        {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle));
        }
        else  if(mMap != null)
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyleday));
    }
}

