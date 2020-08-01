package com.example.disasteralert;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.longrunning.GetOperationRequest;

import java.net.URL;
import java.util.ArrayList;

public class LiveFeedsActivity extends AppCompatActivity {

    private static final String TAG="LiveFeedsActivity";

    private ArrayList<String> Type, Desc;
    private ArrayList<String> imagesUrls;
    private ArrayList<String> colour;
    private Context mContext;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_feeds);

        //initialising lists
        Type = new ArrayList<>();
        Desc = new ArrayList<>();
        imagesUrls = new ArrayList<>();
        colour = new ArrayList<>();

        //Get data
        db = FirebaseFirestore.getInstance();
        db.collection("reports")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot documentSnapshot: task.getResult()){
                                Type.add((String)documentSnapshot.get("type"));
                                Desc.add((String)documentSnapshot.get("description"));
                                GeoPoint point;
                                try {
                                    point = (GeoPoint) documentSnapshot.get("location");
                                }
                                catch (Exception e){
                                    point = new GeoPoint(documentSnapshot.getDouble("lat"), documentSnapshot.getDouble("lon"));
                                }
                                if(documentSnapshot.get("type").equals("Relief")) {
                                    String URL = "https://maps.googleapis.com/maps/api/staticmap?";
                                    URL = URL + "center=" + point.getLatitude() + "," + point.getLongitude();
                                    URL =URL + "&zoom=12&size=150x200";
                                    URL = URL + "&markers=color:blue%7Clabel:Relief%7C" + point.getLatitude() + "," + point.getLongitude();
                                    URL = URL + "&key=AIzaSyDtTcuOsoWo4dpfO--iNDs_DSaXkN-1UKw";
                                    imagesUrls.add(URL);
                                    colour.add("green");
                                }
                                else if(documentSnapshot.get("type").equals("Critical")){
                                    String URL = "https://maps.googleapis.com/maps/api/staticmap?";
                                    URL = URL + "center=" + point.getLatitude() + "," + point.getLongitude();
                                    URL =URL + "&zoom=12&size=150x200";
                                    URL = URL + "&markers=color:red%7Clabel:Critical%7C" + point.getLatitude() + "," + point.getLongitude();
                                    URL = URL + "&key=AIzaSyDtTcuOsoWo4dpfO--iNDs_DSaXkN-1UKw";
                                    imagesUrls.add(URL);
                                    colour.add("red");
                                }
                                else {
                                    try {
                                        imagesUrls.add((String) documentSnapshot.get("imageUrl"));
                                    } catch (Exception e) {
                                        imagesUrls.add("none");
                                    }
                                    colour.add("orange");
                                }
                            }
                            initRecylerView();
                        }
                        else{
                            Log.d(TAG, "onComplete: That didn't work");
                        }
                    }
                });
    }

    private void initRecylerView(){
        Log.d(TAG, "initRecyclerView: prep recycler view");
        RecyclerView recyclerView = findViewById(R.id.feedsRecyclerView);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, Type, Desc, imagesUrls,colour);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

}