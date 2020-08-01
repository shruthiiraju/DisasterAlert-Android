package com.example.disasteralert;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

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
    private ArrayList<String> type = new ArrayList<>();
    private String address = "";
    private String city;
    private ArrayList<WeightedLatLng> latLngArrayListFlood = new ArrayList<>();
    private ArrayList<WeightedLatLng> latLngArrayListFire = new ArrayList<>();
    private ArrayList<WeightedLatLng> latLngArrayListInjury = new ArrayList<>();
    private ArrayList<WeightedLatLng> latLngArrayListIllness = new ArrayList<>();
    private ArrayList<WeightedLatLng> latLngArrayListEarthquake = new ArrayList<>();
    private TileOverlay FloodOverlay, FireOverlay, InjuryOverlay, IllnessOverlay, EarthquakeOverlay;
    private boolean flag = false;

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
                        if(!flag)
                            updateMap();
                    }
                },
                DisasterActivity.this.getMainLooper()
        );

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        //Initialise the content
        final Spinner spinner = findViewById(R.id.spinnerMaps);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.maps, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(spinner.getSelectedItem().equals("Flood")){
                    updateReportFlood();
                }
                else if(spinner.getSelectedItem().equals("Fire")){
                    updateReportFire();
                }
                else if(spinner.getSelectedItem().equals("Injury")){
                    updateReportInjury();
                }
                else if(spinner.getSelectedItem().equals("Illness")){
                    updateReportIllness();
                }
                else if(spinner.getSelectedItem().equals("Earthquake")) {
                    updateReportEarthquake();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        updateHeatMap();
    }

    private void updateReportFlood() {

        if(FireOverlay != null)
            FireOverlay.remove();
        if(InjuryOverlay != null)
            InjuryOverlay.remove();
        if(EarthquakeOverlay != null)
            EarthquakeOverlay.remove();
        if(IllnessOverlay != null)
            IllnessOverlay.remove();
        try {
            HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                    .weightedData(latLngArrayListFlood)
                    .radius(50)
                    .build();
            FloodOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
            mMap.setMinZoomPreference(5.0f);
        }
        catch (Exception e){
            Toast.makeText(this, "No reports of this disaster available in this area", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateReportEarthquake() {
        if(FireOverlay != null)
            FireOverlay.remove();
        if(InjuryOverlay != null)
            InjuryOverlay.remove();
        if(FloodOverlay != null)
            FloodOverlay.remove();
        if(IllnessOverlay != null)
            IllnessOverlay.remove();
        try {
            HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                    .weightedData(latLngArrayListEarthquake)
                    .radius(50)
                    .build();
            EarthquakeOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
            mMap.setMinZoomPreference(5.0f);
        }
        catch (Exception e){
            Toast.makeText(this, "No reports of this disaster available in this area", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateReportInjury() {
        if(FireOverlay != null)
            FireOverlay.remove();
        if(FloodOverlay != null)
            FloodOverlay.remove();
        if(EarthquakeOverlay != null)
            EarthquakeOverlay.remove();
        if(IllnessOverlay != null)
            IllnessOverlay.remove();
        try {
            HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                    .weightedData(latLngArrayListInjury)
                    .radius(50)
                    .build();
            InjuryOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
            mMap.setMinZoomPreference(5.0f);
        }
        catch (Exception e){
            Toast.makeText(this, "No reports of this disaster available in this area", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateReportIllness() {
        if(FireOverlay != null)
            FireOverlay.remove();
        if(InjuryOverlay != null)
            InjuryOverlay.remove();
        if(EarthquakeOverlay != null)
            EarthquakeOverlay.remove();
        if(FloodOverlay != null)
            FloodOverlay.remove();
        try {
            HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                    .weightedData(latLngArrayListIllness)
                    .radius(50)
                    .build();
            IllnessOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
            mMap.setMinZoomPreference(5.0f);
        }
        catch (Exception e){
            Toast.makeText(this, "No reports of this disaster available in this area", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateReportFire() {
        if(FloodOverlay != null)
            FloodOverlay.remove();
        if(InjuryOverlay != null)
            InjuryOverlay.remove();
        if(EarthquakeOverlay != null)
            EarthquakeOverlay.remove();
        if(IllnessOverlay != null)
            IllnessOverlay.remove();
        try {
            HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                    .weightedData(latLngArrayListFire)
                    .radius(50)
                    .build();
            FireOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
            mMap.setMinZoomPreference(5.0f);
        }
        catch (Exception e){
            Toast.makeText(this, "No reports of this disaster available in this area", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMap() {
        flag = true;
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
                                try {
                                    locations.add((GeoPoint) documentSnapshot.get("location"));
                                }
                                catch (Exception e){
                                    Location location = new Location("");
                                    location.setLatitude((Double)documentSnapshot.get("lat"));
                                    location.setLongitude((Double)documentSnapshot.get("lon"));
                                }
                                affected.add((String)documentSnapshot.get("numberOfPeopleAffected"));
                                type.add((String)documentSnapshot.get("type"));
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
            Log.d(TAG, "loadHeatMap: " + address + "\nCity: " + city);
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
                if(type.get(i).equals("Flood"))
                    latLngArrayListFlood.add(latLng);
                else if(type.get(i).equals("Fire"))
                    latLngArrayListFire.add(latLng);
                else if(type.get(i).equals("Injury"))
                    latLngArrayListInjury.add(latLng);
                else if(type.get(i).equals("Illness"))
                    latLngArrayListIllness.add(latLng);
                else if(type.get(i).equals("Earthquake"))
                    latLngArrayListEarthquake.add(latLng);
            }
            else if(Integer.parseInt(total_affected.get(postals.get(i))) > 10 && Integer.parseInt(total_affected.get(postals.get(i))) < 30){
                WeightedLatLng latLng = new WeightedLatLng(new LatLng(locations.get(i).getLatitude(), locations.get(i).getLongitude()), 0.6);
                if(type.get(i).equals("Flood"))
                    latLngArrayListFlood.add(latLng);
                else if(type.get(i).equals("Fire"))
                    latLngArrayListFire.add(latLng);
                else if(type.get(i).equals("Injury"))
                    latLngArrayListInjury.add(latLng);
                else if(type.get(i).equals("Illness"))
                    latLngArrayListIllness.add(latLng);
                else if(type.get(i).equals("Earthquake"))
                    latLngArrayListEarthquake.add(latLng);
            }
            else if(Integer.parseInt(total_affected.get(postals.get(i))) > 30 && Integer.parseInt(total_affected.get(postals.get(i))) < 70){
                WeightedLatLng latLng = new WeightedLatLng(new LatLng(locations.get(i).getLatitude(), locations.get(i).getLongitude()), 0.8);
                if(type.get(i).equals("Flood"))
                    latLngArrayListFlood.add(latLng);
                else if(type.get(i).equals("Fire"))
                    latLngArrayListFire.add(latLng);
                else if(type.get(i).equals("Injury"))
                    latLngArrayListInjury.add(latLng);
                else if(type.get(i).equals("Illness"))
                    latLngArrayListIllness.add(latLng);
                else if(type.get(i).equals("Earthquake"))
                    latLngArrayListEarthquake.add(latLng);
            }
            else{
                WeightedLatLng latLng = new WeightedLatLng(new LatLng(locations.get(i).getLatitude(), locations.get(i).getLongitude()), 1.0);
                if(type.get(i).equals("Flood"))
                    latLngArrayListFlood.add(latLng);
                else if(type.get(i).equals("Fire"))
                    latLngArrayListFire.add(latLng);
                else if(type.get(i).equals("Injury"))
                    latLngArrayListInjury.add(latLng);
                else if(type.get(i).equals("Illness"))
                    latLngArrayListIllness.add(latLng);
                else if(type.get(i).equals("Earthquake"))
                    latLngArrayListEarthquake.add(latLng);
            }
        }
        updateReportFlood();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
}