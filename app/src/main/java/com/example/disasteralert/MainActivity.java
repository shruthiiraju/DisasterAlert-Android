package com.example.disasteralert;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Parcelable;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static Context mContext;
    private static GetLocations locations;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Initialising Firebase authentication
        mAuth = FirebaseAuth.getInstance();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null) {
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
        }

        //Initialising layout components
        FloatingActionButton fab = findViewById(R.id.fab);
        Button DisasterMapButton = findViewById(R.id.disasterMapButton);
        Button ReportButton = findViewById(R.id.reportButton);
        Button DonateButton = findViewById(R.id.donateButton);
        Button LiveFeedButton = findViewById(R.id.liveFeedButton);

        //Initialising locations
        setContext(this);
        locations = new GetLocations();
        locations.getLocation();

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT)
                    .show();
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
        }
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

    public static Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }
}