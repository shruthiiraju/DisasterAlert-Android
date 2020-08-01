package com.example.disasteralert;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.disasteralert.MainActivity.FASTEST_UPDATE_INTERVAL;
import static com.example.disasteralert.MainActivity.MAX_UPDATE_INTERVAL;
import static com.example.disasteralert.MainActivity.UPDATE_INTERVAL;

public class ReportCreationActivity extends AppCompatActivity {

    /*
    *1.Upload Photos/Video
     * 2. Type: Fire, flood, injury, illness, etc.
     * 3. Description: free form text area
     * 4.Location+Timestamp (implicit)
     * 5.Should we ask for contact number?
     * 6.Number of People Affected?
    *
    */
    private static final String TAG = "DisasterAlert";

    private static final String[] PEOPLE_AFFECTED_PICKER_CHOICES = new String[]{"None", "1", "5", "10", "<50", "50+"};
    private static final String[] eventTypes = { "Flood", "Fire", "Injury", "Illness", "Earthquake" };
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private NumberPicker peopleAffectedPicker;
    private Button addPictureButton, submitReportButton;
    private ImageView reportImageView;
    private Spinner reportTypeSpinner;
    private EditText descriptionEditText;
    private ProgressBar reportUploadProgressBar;

    private File photoFile;
    private Uri photoFileUri;
    private Bitmap photoBitmap;
    private Location location;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_creation);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        //Initialising layout components
        reportTypeSpinner = (Spinner) findViewById(R.id.spinnerReportType);
        peopleAffectedPicker = findViewById(R.id.picker_people_affected);
        addPictureButton = findViewById(R.id.button_add_picture);
        reportImageView = findViewById(R.id.image_view_report);
        submitReportButton = findViewById(R.id.button_submit_report);
        descriptionEditText = findViewById(R.id.textReportDesc);
        reportUploadProgressBar = findViewById(R.id.progress_bar_report_upload);

        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setMaxWaitTime(MAX_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);

                        submitReportButton.setText(R.string.submit_report);
                        submitReportButton.setEnabled(true);
                        location = locationResult.getLastLocation();
                    }
                },
                ReportCreationActivity.this.getMainLooper()
        );

        peopleAffectedPicker.setMinValue(0);
        peopleAffectedPicker.setMaxValue(PEOPLE_AFFECTED_PICKER_CHOICES.length - 1);
        peopleAffectedPicker.setDisplayedValues(PEOPLE_AFFECTED_PICKER_CHOICES);
        peopleAffectedPicker.setValue(1);
        peopleAffectedPicker.setWrapSelectorWheel(false);

        //Setting Report Type spinner dropdown values
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, eventTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportTypeSpinner.setAdapter(adapter);

        addPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        submitReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reportUploadProgressBar.setVisibility(View.VISIBLE);

                final LinkedHashMap<String, Object> report = new LinkedHashMap<>();
                report.put("byPhone", mAuth.getCurrentUser().getPhoneNumber());
                //report.put("byEmail", mAuth.getCurrentUser().getEmail());
                report.put("byUid", mAuth.getCurrentUser().getUid().toString());
                report.put("location", new GeoPoint(location.getLatitude(), location.getLongitude()));
                report.put("time", String.valueOf(new Date().getTime()));
                report.put("type", eventTypes[reportTypeSpinner.getSelectedItemPosition()]);
                report.put("numberOfPeopleAffected", PEOPLE_AFFECTED_PICKER_CHOICES[peopleAffectedPicker.getValue()]);
                report.put("description", descriptionEditText.getText().toString());
                String smsBody = "New Report from App\n";
                for (Map.Entry mapElement : report.entrySet()) {
                    if((String)mapElement.getKey()!="location") {
                        String key = (String) mapElement.getValue();
                        Log.e(TAG, key);
                        smsBody += key + "\n";
                    }
                }
                smsBody += location.getLatitude() + "," + location.getLongitude();
                    final ConnectionChecker connectionChecker = new ConnectionChecker();
                if(connectionChecker.isOnline()){
                    report.put("layer", "first");
                } else if (!connectionChecker.isOnline() && connectionChecker.isMobileAvailable(getApplicationContext())) {
                    report.put("layer", "second");
                    sendSMS(smsBody);
                    return;
                }
                else {
                    report.put("layer", "third");
                }
                Log.e(TAG, report.toString());
                final StorageReference imageRef = storage.getReference()
                        .child(String.valueOf(new Date().getTime()));


                if (photoFileUri != null) {
                    imageRef.putFile(photoFileUri)
                            .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                @Override
                                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                    if (!task.isSuccessful()) {
                                        Log.e(TAG, "then: ", task.getException());
                                        Toast.makeText(ReportCreationActivity.this, "Couldn't upload image", Toast.LENGTH_SHORT).show();
                                        throw task.getException();
                                    }

                                    return imageRef.getDownloadUrl();
                                }
                            })
                            .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    report.put("imageUrl", uri.toString());

                                    uploadReport(report);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "onFailure: ", e);
                                    reportUploadProgressBar.setVisibility(View.GONE);
                                }
                            });
                } else {
                    uploadReport(report);
                }
            }
        });
    }

    private void uploadReport(HashMap<String, Object> report) {
        db.collection("reports")
                .document(String.valueOf(report.get("time")))

                .set(report)

                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        reportUploadProgressBar.setVisibility(View.GONE);
                        Toast.makeText(ReportCreationActivity.this, "Report Created", Toast.LENGTH_SHORT).show();
                        if (photoFile != null) {
                            if (!photoFile.delete()) {
                                Log.e(TAG, "onSuccess: FILE NOT DELETED");
                            }
                        }
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        reportUploadProgressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            photoFile = new File(this.getFilesDir(), "Report" + new Date().getTime() + ".jpg");
            String authorities = getApplicationContext().getPackageName() + ".fileprovider";
            photoFileUri = FileProvider.getUriForFile(this, authorities, photoFile);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFileUri);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Glide.with(ReportCreationActivity.this)
                    .load(photoFile)
                    .into(reportImageView);
            reportImageView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (photoFile != null) {
            if (photoFile.exists()) {
                if (!photoFile.delete()) {
                    Log.e(TAG, "onStop: FILE NOT DELETED");
                }
            }
        }
    }

    public void sendSMS(String body) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage("+12059272598", null, body, null, null);
            Toast.makeText(getApplicationContext(), "Message Sent",
                    Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(),ex.getMessage().toString(),
                    Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }
}