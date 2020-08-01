package com.example.disasteralert;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Parcelable;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DisasterAlert";

    protected static final long FASTEST_UPDATE_INTERVAL = 1 * 1000L;
    protected static final long UPDATE_INTERVAL = 5 * 1000L;
    protected static final long MAX_UPDATE_INTERVAL = 10 * 1000L;
    protected static final int STATUS_PING_REQUEST_CODE = 1;
    protected static final long STATUS_PING_INTERVAL = 5 * 60 * 1000;  // 5 minutes

    public static GetLocations locations;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Initialising Firebase authentication
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Check if user is signed in (non-null) and update UI accordingly.
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if(currentUser == null) {
//            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
//            startActivity(loginIntent);
//            finish();
//        }

        //Initialising layout components
        FloatingActionButton fab = findViewById(R.id.fab);
        Button DisasterMapButton = findViewById(R.id.disasterMapButton);
        Button ReportButton = findViewById(R.id.reportButton);
        Button DonateButton = findViewById(R.id.donateButton);
        Button LiveFeedButton = findViewById(R.id.liveFeedButton);

        //Initialising locations

        Dexter.withContext(this)
                .withPermissions(getRequiredPermissions())
                .withListener(new MultiplePermissionsListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        FusedLocationProviderClient fusedLocationProviderClient =
                                LocationServices.getFusedLocationProviderClient(MainActivity.this);

                        locationRequest = new LocationRequest();
                        locationRequest.setInterval(UPDATE_INTERVAL);
                        locationRequest.setMaxWaitTime(MAX_UPDATE_INTERVAL);
                        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                        fusedLocationProviderClient.requestLocationUpdates(
                                locationRequest,
                                getLocationCallback(),
                                MainActivity.this.getMainLooper()
                        );
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                })
                .withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError dexterError) {
                        Log.e(TAG, "onError: " + dexterError.toString());
                    }
                })
                .check();

//        setContext(this);
//        locations = new GetLocations();
//        locations.getLocation();

        //OnClick: ReportButton should take you to ReportCreationActivity.
        ReportButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent goToReportActivity = new Intent(MainActivity.this, ReportCreationActivity.class);
                goToReportActivity.putExtra("key", "Shruthiiii"); //Optional parameters
                MainActivity.this.startActivity(goToReportActivity);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with something", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //OnClick: DonateButton Should take you to DonateActivity(Currently used for testing locations(no longer))
        DonateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //Toast.makeText(MainActivity.this, locations.myCoordinates.toString(), Toast.LENGTH_LONG).show();
                Intent goToDonationActivity = new Intent(MainActivity.this, DonationActivity.class);
                startActivity(goToDonationActivity);
            }
        });

        //OnClick: DisasterMapButton Should take you to DisasterActivity
        DisasterMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, DisasterActivity.class));
            }
        });

        //OnClick: LiveFeedsButton Should take you to FeedsActivity
        LiveFeedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, LiveFeedsActivity.class));
            }
        });
    }

    private LocationCallback getLocationCallback() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                HashMap<String, Object> updateMap = new HashMap<>();
                updateMap.put(
                        "currentLocation",
                        new GeoPoint(
                            locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude()
                        )
                );

                mAuth = FirebaseAuth.getInstance();
                db  = FirebaseFirestore.getInstance();

                db.collection("users")
                        .document(mAuth.getCurrentUser().getUid())
                        .update(updateMap)

                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "onFailure: ", e);
                            }
                        });
            }
        };
    }

    private ArrayList<String> getRequiredPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.SEND_SMS);
        }
        return permissions;
    }

    @Override
    protected void onStart() {
        super.onStart();
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if(currentUser == null) {
//            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT)
//                    .show();
//            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
//            startActivity(loginIntent);
//            finish();
//        } else {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                        @Override
                        public void onSuccess(InstanceIdResult instanceIdResult) {
                            db.collection("users")
                                    .document(mAuth.getCurrentUser().getUid())
                                    .update("token", instanceIdResult.getToken())

                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG, "onFailure: ", e);
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "onFailure: ", e);
                        }
                    });

            setUpStatusPing();
//        }
    }

    private void setUpStatusPing() {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, StatusPingReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, STATUS_PING_REQUEST_CODE, intent, 0);
        alarmManager.setRepeating(
                AlarmManager.RTC,
                Calendar.getInstance().getTimeInMillis(),
                STATUS_PING_INTERVAL,
                pendingIntent
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    public static Context getContext() {
//        return mContext;
//    }
//
//    public void setContext(Context mContext) {
//        this.mContext = mContext;
//    }
}