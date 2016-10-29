package com.example.tainguyen.trafficmap;

import android.*;
import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.tainguyen.trafficmap.GoogleDirection.ApiCallback;
import com.example.tainguyen.trafficmap.GoogleDirection.Directions;
import com.example.tainguyen.trafficmap.GoogleDirection.GoogleDirection;
import com.example.tainguyen.trafficmap.Util.CheckRoute;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.example.tainguyen.trafficmap.R.id.map;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ApiCallback, SensorEventListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location currentLocation;
    private static final int ERROR_DIALOG_REQUEST = 9001;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    ArrayList<Directions.Route> route;

    List<Marker> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        if (servicesOK()) {
            mGoogleApiClient = new GoogleApiClient
                    .Builder(this)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .enableAutoManage(this, this)
                    .build();
            mGoogleApiClient.connect();
        }

    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setTrafficEnabled(true);
        googleMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener()
        {

            @Override
            public void onPolylineClick(Polyline polyline)
            {
                List<LatLng> points = polyline.getPoints();
                LatLng start = points.get(0);
                LatLng end = points.get(points.size()-1);

                Double circumferenceOfEarthInMeters = 2 * Math.PI * 6371000;
                // Double tileWidthAtZoomLevelAtEquatorInDegrees = 360.0/Math.pow(2.0, map.getCameraPosition().zoom);
                Double pixelSizeInMetersAtLatitude = (circumferenceOfEarthInMeters * Math.cos(googleMap.getCameraPosition().target.latitude * (Math.PI / 180.0))) / Math.pow(2.0, googleMap.getCameraPosition().zoom + 8.0);
                Double tolerance = pixelSizeInMetersAtLatitude * Math.sqrt(2.0) * 10.0;

                Directions.Route rightRoute = null;
                for(Directions.Route rou : route){
                    for(Directions.Leg leg : rou.legs){
                        for(Directions.Leg.Step step : leg.steps){
                            if (PolyUtil.isLocationOnPath(start, PolyUtil.decode(step.polyline.getPoints()), true, tolerance)) {
                                rightRoute = rou;
                                break;
                            }
                        }
                        if (rightRoute!=null) break;
                    }
                    if (rightRoute!=null) break;
                }
                if (rightRoute!=null){
                    //show detail
                    Toast.makeText(getBaseContext(),"" + rightRoute.reportList.size()+ " reports",Toast.LENGTH_SHORT).show();
                }

            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        new GoogleDirection(this).foo();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onApiResult(Directions result) {
//        Directions res = result;

        route = new ArrayList<Directions.Route>(Arrays.asList(result.routes));
        ArrayList<LatLng> res = new ArrayList<>();

        Random random = new Random();
        for (Directions.Route route : result.routes) {
            for (Directions.Leg leg : route.legs) {
                for (Directions.Leg.Step step : leg.steps) {
                    for (int i = 0; i < 10; i++) {
                        double randlat = (random.nextFloat() - 0.5)*0.01 + step.start_location.getLat();
                        double randlong = (random.nextFloat() - 0.5)*0.01 + step.start_location.getLng();
                        LatLng temp = new LatLng(randlat,randlong);
                        res.add(temp);
                    }
                }
            }
        }
        ArrayList<LatLng> ans = new ArrayList<>();
//        Toast.makeText(this, String.valueOf(result.routes[0].report),Toast.LENGTH_SHORT).show();

        Polyline line = null;
        Log.d("log routes", "" + result.routes.length);
        for (int i = 0; i < result.routes.length; i++) {
            ans.addAll(CheckRoute.countReportOnRoute(result.routes[i],res));
            for (Directions.Leg leg : result.routes[i].legs) {
                for (Directions.Leg.Step step : leg.steps){
                    List<LatLng> decodedPath = PolyUtil.decode(step.polyline.points);
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(decodedPath));
                    polyline.setClickable(true);
                }
            }
        }

        Marker marker = null;
        for (LatLng p : ans) {
            marker = mMap.addMarker(new MarkerOptions().position(p));
        }
    }

    public void gotoMyLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("debug","no permiss");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
        currentLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        if(currentLocation == null){
            Toast.makeText(this, "Map not connect", Toast.LENGTH_SHORT ).show();
            //finish();
        }else{
            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, 15);
            mMap.animateCamera(update);
        }
    }

    public boolean servicesOK(){
        int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if(isAvailable == ConnectionResult.SUCCESS) {
            return true;
        }else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)){
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable, this, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "Can't connect to mapping service", Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this, "Ready to map", Toast.LENGTH_SHORT).show();
        gotoMyLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Connect failed", Toast.LENGTH_SHORT).show();
    }

    long lastTime = System.currentTimeMillis();
    double last_X = -1;
    double last_Y = -1;
    double last_Z = -1;
    double delta_X = 0;
    double delta_Y = 0;
    double delta_Z = 0;
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //Toast.makeText(this,"start listen",Toast.LENGTH_SHORT).show();

        if (sensorEvent.sensor.getType() ==  Sensor.TYPE_ACCELEROMETER){
            //Toast.makeText(this,"small shake" + delta_X +" "+ delta_Y+ " " + delta_Z,Toast.LENGTH_SHORT).show();

            if (last_X == -1 && last_Y == -1 && last_Z == -1){
                last_X = sensorEvent.values[0];
                last_Y = sensorEvent.values[1];
                last_Z = sensorEvent.values[2];
                return ;
            }
            long now = System.currentTimeMillis();
            long dif = now - lastTime;
            double cur_X = sensorEvent.values[0];
            double cur_Y = sensorEvent.values[1];
            double cur_Z = sensorEvent.values[2];
            delta_X += Math.abs(cur_X - last_X);
            delta_Y += Math.abs(cur_Y - last_Y);
            delta_Z += Math.abs(cur_Z - last_Z);
            //Log.d("acc","x"+delta_X + " curX=" +cur_X);
            //Log.d("acc","y"+delta_X + " curY=" +cur_X);
            //Log.d("acc",String.valueOf((delta_X + delta_Y +delta_Z)/dif));

            if (dif >= 2000 ){
                if ((delta_X + delta_Y +delta_Z)/dif > 0.25) {
                    //send request.
                    Toast.makeText(this, "Shake", Toast.LENGTH_SHORT).show();
                    mSensorManager.unregisterListener(this,sensorEvent.sensor);
                }
                lastTime = now;
                delta_X = 0;
                delta_Y = 0;
                delta_Z = 0;
            }
            last_X = cur_X;
            last_Y = cur_Y;
            last_Z = cur_Z;

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

