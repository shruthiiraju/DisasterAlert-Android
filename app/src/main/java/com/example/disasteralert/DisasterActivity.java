package com.example.disasteralert;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DisasterActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disaster);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert  mapFragment != null;
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

    private void updateHeatMap() {
    }

    private void updateReport() {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng location = MainActivity.locations.myCoordinates;
        mMap = googleMap;
        //mMap.addMarker(new MarkerOptions().position(location).title("default"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(location)
                .zoom(12)
                .bearing(90)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
}