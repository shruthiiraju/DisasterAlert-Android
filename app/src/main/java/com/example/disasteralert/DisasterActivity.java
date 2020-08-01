package com.example.disasteralert;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.disasteralert.MainActivity.FASTEST_UPDATE_INTERVAL;
import static com.example.disasteralert.MainActivity.MAX_UPDATE_INTERVAL;
import static com.example.disasteralert.MainActivity.UPDATE_INTERVAL;

public class DisasterActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "DisasterActivity";

    private GoogleMap mMap;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Location location;
    private ArrayList<GeoPoint> locations = new ArrayList<>();
    private ArrayList<String> postals = new ArrayList<>();
    private ArrayList<String> affected = new ArrayList<>();
    private HashMap<String, String> total_affected = new HashMap<>();
    private String address = "";

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disaster);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setMaxWaitTime(MAX_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        location = locationResult.getLastLocation();
                        updateMap();
                    }
                },
                DisasterActivity.this.getMainLooper()
        );

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        //Initialise the content
        final FloatingActionButton floatingActionButton = findViewById(R.id.check);


        //onClicks
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateReport();
            }
        });

        updateHeatMap();

    }

    private void updateMap() {
        LatLng location = new LatLng(this.location.getLatitude(), this.location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(location)
                .zoom(12)
                .bearing(90)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void updateHeatMap() {
        db.collection("reports")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot documentSnapshot : task.getResult()){
                                locations.add((GeoPoint) documentSnapshot.get("location"));
                                affected.add((String)documentSnapshot.get("numberOfPeopleAffected"));
                            }
                            loadHeatMap();
                        }
                        else{
                            Log.d(TAG, "onComplete: " + task.getException());
                        }
                    }
                });
    }

    private void loadHeatMap() {
        ArrayList<WeightedLatLng> latLngArrayList = new ArrayList<>();
        for(GeoPoint location : locations){
            try{
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                address = addresses.get(0).getAddressLine(0);
                if (addresses.get(0).getAddressLine(1) != null)
                    address = "," + addresses.get(0).getAddressLine(1);
                if (addresses.get(0).getAddressLine(1) != null)
                    address = "," + addresses.get(0).getAddressLine(2);
            } catch (Exception e) {
//            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
            }
            Log.d(TAG, "loadHeatMap: " + address);
            String listadd[] = address.split(",");
            int i = listadd.length;
            int j = listadd[i - 2].split(" ").length;
            postals.add(listadd[i - 2].split(" ")[j - 1]);
        }
        for(int i=0; i<postals.size(); i++){
            Pattern p = Pattern.compile("\\d+");
            Matcher m = p.matcher(affected.get(i));
            if(total_affected.containsKey(postals.get(i))){
                while(m.find()) {
                    int num = Integer.parseInt(total_affected.get(postals.get(i)));
                    total_affected.put(postals.get(i), String.valueOf(num * Integer.parseInt(m.group())));
                }
            }
            else{
                while(m.find()) {
                    total_affected.put(postals.get(i), m.group());
                }
            }
        }
        for(int i=0; i<postals.size(); i++){
            if(Integer.parseInt(total_affected.get(postals.get(i))) > 0 && Integer.parseInt(total_affected.get(postals.get(i))) < 10){
                WeightedLatLng latLng = new WeightedLatLng(new LatLng(locations.get(i).getLatitude(), locations.get(i).getLongitude()), 0.2);

                latLngArrayList.add(latLng);
            }
            else if(Integer.parseInt(total_affected.get(postals.get(i))) > 10 && Integer.parseInt(total_affected.get(postals.get(i))) < 30){
                WeightedLatLng latLng = new WeightedLatLng(new LatLng(locations.get(i).getLatitude(), locations.get(i).getLongitude()), 0.6);

                latLngArrayList.add(latLng);
            }
            else if(Integer.parseInt(total_affected.get(postals.get(i))) > 30 && Integer.parseInt(total_affected.get(postals.get(i))) < 70){
                WeightedLatLng latLng = new WeightedLatLng(new LatLng(locations.get(i).getLatitude(), locations.get(i).getLongitude()), 0.8);

                latLngArrayList.add(latLng);
            }
            else{
                WeightedLatLng latLng = new WeightedLatLng(new LatLng(locations.get(i).getLatitude(), locations.get(i).getLongitude()), 1.0);

                latLngArrayList.add(latLng);
            }
        }
        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                .weightedData(latLngArrayList)
                .radius(50)
                .build();
        TileOverlay mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        mMap.setMinZoomPreference(5.0f);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(14.9,77.6)));
    }

    private void updateReport() {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
}