package com.trafficjammap;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.maps.android.PolyUtil;
import com.squareup.picasso.Picasso;
import com.trafficjammap.GoogleDirection.ApiCallback;
import com.trafficjammap.GoogleDirection.Directions;
import com.trafficjammap.GoogleDirection.GoogleDirection;
import com.trafficjammap.Util.CheckRoute;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ApiCallback, SensorEventListener {
    FloatingActionButton menu1,menu2,menu3 ;
    FloatingActionMenu floatMenu;
    Marker startMaker = null, destMaker=null;
    List<Polyline> listLine = new ArrayList<>();
    ArrayList<Directions.Route> route;

    int check = 0;
    private GoogleMap mMap;
    Button btSearch = null;
    GoogleApiClient mGoogleApiClient;
    Location currentLocation;
    EditText etLocation;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    CountDownTimer updateTimer, removeMakerTimer;
    int REQUEST_CODE_IMAGE = 1;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    DatabaseReference mData;
    List<Marker> markers = new ArrayList<>();
    List<Info> listInfo = new ArrayList<>();
    StorageReference storageRef = null;

    Button btSentReport;
    Dialog dialog;
    LatLng startPos, destPos;

    private boolean isApiReady = false;
    Button btCancle;
    Button btFind;
    LinearLayout lnSearch, lnPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        menu1 = (FloatingActionButton)findViewById(R.id.item1) ;
        menu2 = (FloatingActionButton)findViewById(R.id.item2) ;
        menu3 = (FloatingActionButton)findViewById(R.id.item3) ;
        floatMenu = (FloatingActionMenu) findViewById(R.id.floating_action_menu);
        storageRef = storage.getReferenceFromUrl("gs://trafficjammap.appspot.com");

        mData = FirebaseDatabase.getInstance().getReference();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
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

        updateTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                gotoMyLocation();
            }
        };

        updateTimer.start();

        removeInfo();

        mData.child("Info").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Info info = new Info();
                info.key = dataSnapshot.getKey();
                info.pos = new LatLng((double) dataSnapshot.child("pos").child("latitude").getValue(), (double) dataSnapshot.child("pos").child("longitude").getValue());
                info.time = (long) dataSnapshot.child("time").getValue();
                info.nameImage = (String) dataSnapshot.child("nameImage").getValue();
                info.urlImage = (String) dataSnapshot.child("urlImage").getValue();

                //Toast.makeText(getApplication(), "" + ll, Toast.LENGTH_SHORT ).show();
                listInfo.add(info);
                markers.add(mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher_maker))
                        .position(info.pos)
                        .snippet(info.urlImage)));
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //for(int i = 0; i < markers.size(); i++)
                //markers.get(i).remove();
                //markers.clear();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        btCancle = (Button)findViewById(R.id.btCancle);
        btFind = (Button)findViewById(R.id.btFind);
        lnSearch = (LinearLayout) findViewById(R.id.lnSearch);
        lnPath = (LinearLayout) findViewById(R.id.lnPath);

        btFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                check = 1;
                lnSearch.setVisibility(View.VISIBLE);
                lnPath.setVisibility(View.INVISIBLE);
                Log.d("mylog", startPos.toString());
                String origin = "" + startPos.latitude + "," + startPos.longitude;
                String dest = "" + destPos.latitude + "," + destPos.longitude;
                new GoogleDirection(MainActivity.this).foo(origin, dest);

                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(startPos, 15);
                mMap.animateCamera(update);


            }
        });

        btCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                check = 0;
                lnSearch.setVisibility(View.VISIBLE);
                lnPath.setVisibility(View.INVISIBLE);

            }
        });

        removeMakerTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                removeInfo();
                removeMakerTimer.start();

            }
        };

        removeMakerTimer.start();

    }

    public void onClick(View v) {
        removePath();
        floatMenu.close(true);
        switch (v.getId()) {
            case R.id.item1: {
                sentReport("0","0");

                break;
            }

            case R.id.item2: {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, REQUEST_CODE_IMAGE);

                break;
            }

            case R.id.item3: {
                if (!isApiReady) {
                    return;
                }

                lnPath.setVisibility(View.VISIBLE);
                lnSearch.setVisibility(View.INVISIBLE);

                break;
            }

            default:
                break;
        }
    }

    void removeInfo()
    {


        long currentTime = System.currentTimeMillis();

        int i = 0;
        while (i < listInfo.size()) {
            if (currentTime - listInfo.get(i).time > 600000) {
                mData.child("Info").child(listInfo.get(i).key).removeValue();
                if(listInfo.get(i).nameImage.toString() != "0"){
                    RemoveImage(listInfo.get(i).nameImage);
                }
                listInfo.remove(i);
                markers.get(i).remove();
                markers.remove(i);
            } else
                i++;
        }
    }
    void RemoveImage(String name){
        StorageReference desertRef = storageRef.child("/" + name);
       // Toast.makeText(MainActivity.this, "" + name,Toast.LENGTH_SHORT).show();
        desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //Toast.makeText(MainActivity.this,"thanh cong", Toast.LENGTH_LONG).show();
            }
        });
    }
    void sentReport(String url, String name) {
        updateCurrentLocation();
        long time = System.currentTimeMillis();
        Info info = new Info("",new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), time, url,name);
        mData.child("Info").push().setValue(info);
        Toast.makeText(getApplicationContext(), "Sent success!!!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_IMAGE &&  resultCode == RESULT_OK && data != null){
            Calendar calendar = Calendar.getInstance();
            StorageReference mountainsRef = storageRef.child("image" + calendar.getTimeInMillis()+ ".jpg");

            Bitmap bitmap = (Bitmap) data.getExtras().get("data");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

            byte[] data_byte = baos.toByteArray();

            UploadTask uploadTask = mountainsRef.putBytes(data_byte);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(MainActivity.this, "Fail!!!", Toast.LENGTH_SHORT).show();
                    // Handle unsuccessful uploads
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.

                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                    Log.d("AAA", downloadUrl + "");
                    sentReport(downloadUrl.toString(),taskSnapshot.getStorage().getName().toString());
                }
            });
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setPadding(0, 100, 0, 0);



        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        mMap.setMyLocationEnabled(true);

        // Add a marker in Sydney, Australia, and move the camera.
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                LinearLayout info = new LinearLayout(MainActivity.this);
                info.setOrientation(LinearLayout.VERTICAL);

                if(marker.getSnippet() != null && !marker.getSnippet().toString().equals("0")) {
                    ImageView img =  new ImageView(MainActivity.this);
                    img.setImageDrawable(getResources().getDrawable(R.drawable.loading));
                    Picasso.with(getApplicationContext()).load(marker.getSnippet().toString()).into(img);
                    img.setMinimumHeight(300);
                    img.setMinimumWidth(200);

                    info.addView(img);
                    return info;
                }
                //Toast.makeText(getApplicationContext(), ""+marker.getTitle()+" " + marker.getSnippet(), Toast.LENGTH_SHORT).show();
                if (marker.getTitle()== null)
                    return null;

                TextView title = new TextView(MainActivity.this);
                title.setText(marker.getTitle());
                info.addView(title);
                return info;
            }
        });

        mMap.setTrafficEnabled(true);

        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener()
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


    public void updateCurrentLocation()
    {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        currentLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        if(currentLocation == null)
            Toast.makeText(this, "Map not connect", Toast.LENGTH_SHORT ).show();
    }

    void removePath(){
        if(listLine.size() > 0)
            for (int i = 0; i < listLine.size(); i++)
                listLine.get(i).remove();
        listLine.clear();

        if(startMaker!=null)
            startMaker.remove();
        if(destMaker!=null)
            destMaker.remove();
    }
    //call back after get routes
    @Override
    public void onApiResult(Directions result) {
//        Directions res = result;


        route = new ArrayList<Directions.Route>(Arrays.asList(result.routes));

        if (result == null || result.routes == null || result.routes.length == 0) {
            return;
        }

        //create fake report data
        ArrayList<LatLng> res = new ArrayList<>();
        for(int i = 0; i < listInfo.size(); i++)
            res.add(listInfo.get(i).pos);

        /*Random random = new Random();
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
        }*/
        //-----------------------------------------------------------------//
        ArrayList<LatLng> ans = new ArrayList<>();
//        Toast.makeText(this, String.valueOf(result.routes[0].report),Toast.LENGTH_SHORT).show();


        removePath();

        LatLng origin = new LatLng(result.routes[0].legs[0].start_location.getLat(),result.routes[0].legs[0].start_location.getLng());
        LatLng des = new LatLng(result.routes[0].legs[result.routes[0].legs.length-1].end_location.getLat(),result.routes[0].legs[result.routes[0].legs.length-1].end_location.getLng());
        startMaker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_startpos))
                .position(origin));
        destMaker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_destpos))
                .position(des));


        Log.d("log routes", "" + result.routes.length);
        for (int i = 0; i < result.routes.length; i++) {
            ans.addAll(CheckRoute.countReportOnRoute(result.routes[i],res));
            for (Directions.Leg leg : result.routes[i].legs) {
                for (Directions.Leg.Step step : leg.steps){
                    List<LatLng> points = PolyUtil.decode(step.polyline.points);
                    List<LatLng> decodedPath = PolyUtil.decode(step.polyline.points);
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(decodedPath).width(15).color(Directions.getColor(i)));
                    polyline.setClickable(true);
                    listLine.add(polyline);
                    //listLine.add(mMap.addPolyline(new PolylineOptions().addAll(points).width(15).color(Directions.getColor(i))));
                    //listLine.add(mMap.addPolyline(new PolylineOptions().add(new LatLng(step.start_location.getLat(), step.start_location.getLng())
                    //        , new LatLng(step.end_location.getLat(), step.end_location.getLng())).width(15).color(Directions.getColor(i))));
                }
            }
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isApiReady = true;
    }

    public void gotoMyLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("debug","no permiss");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
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
            startPos = latLng;
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
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setHint("Choose location");
        PlaceAutocompleteFragment autocompleteFragmentStart = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment_start);
        autocompleteFragmentStart.setText("My Current Location");
        PlaceAutocompleteFragment autocompleteFragmentDest = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment_destination);
        autocompleteFragmentDest.setHint("Choose Destination");

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i("auto complete", "Place: " + place.getName());
                LatLng latLng = place.getLatLng();
                mMap.addMarker(new MarkerOptions().position(latLng).title("" + place.getName())).showInfoWindow();
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                removePath();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("auto complete", "An error occurred: " + status);

            }
        });

        autocompleteFragmentStart.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i("auto complete", "Place: " + place.getName());
                startPos = place.getLatLng();

            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("auto complete", "An error occurred: " + status);

            }
        });

        autocompleteFragmentDest.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i("auto complete", "Place: " + place.getName());
                destPos = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("auto complete", "An error occurred: " + status);

            }
        });
        //gotoMyLocation();
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
                if ((delta_X + delta_Y +delta_Z)/dif > 0.2) {
                    //send request.
                    sentReport("0","0");
                    Toast.makeText(this, "shake", Toast.LENGTH_SHORT).show();
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
