package com.example.disasteralert;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.annotations.NotNull;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class LovedOnesActivity extends AppCompatActivity {

    private final int REQUEST_CODE=99;
    private final int CONTACTS_PERMISSION_CODE = 10;
    private final String TAG = "LovedOnesActivity";
    private ArrayList<String> numbers, names, images, safes, userNumbers, userSafes, userIds;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private boolean flag = false, isFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loved_ones);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getIntent().hasExtra("isSafe")) {
            updateDBWithSafeStatus(getIntent().getBooleanExtra("isSafe", true), getIntent().getIntExtra("notificationId", 0));
        }

        //initialising local variables
        final Button getContacts = findViewById(R.id.addNumber);
        numbers = new ArrayList<>();
        names = new ArrayList<>();
        images = new ArrayList<>();
        userNumbers = new ArrayList<>();
        safes = new ArrayList<>();
        userSafes = new ArrayList<>();
        userIds = new ArrayList<>();

        //setting onClicks
        getContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                getContact();
            }
        });
        checkPermission(Manifest.permission.READ_CONTACTS, CONTACTS_PERMISSION_CODE);
        getDataBaseContacts();
    }

    private void updateDBWithSafeStatus(boolean isSafe, final int notificationId) {
        HashMap<String, Object> updateMap = new HashMap<>();
        updateMap.put("isSafe", isSafe);

        db.collection("users")
                .document(mAuth.getCurrentUser().getUid())
                .collection("isSafe")
                .document(mAuth.getCurrentUser().getPhoneNumber())

                .set(updateMap)

                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        NotificationManagerCompat.from(LovedOnesActivity.this).cancel(notificationId);

                        Toast.makeText(LovedOnesActivity.this, "Status updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                    }
                });
    }

    private void getDataBaseContacts() {
        try{
            db.collection("users")
                    .document(mAuth.getCurrentUser().getUid())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.isSuccessful()){
                                DocumentSnapshot document = task.getResult();
                                try {
                                    ArrayList<String> nums = (ArrayList<String>) document.get("lovedOnes");
                                    for (String num : nums) {
                                        num = num.replaceAll("\\s", "");
                                        numbers.add(num);
                                        names.add(num);
                                        images.add(num);
                                        Log.d(TAG, "onComplete: " + num);
                                    }
                                    getContactFromNumber();
                                }
                                catch (Exception e){
                                    Toast.makeText(LovedOnesActivity.this, "Add contacts", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
        }
        catch (Exception e){

        }
    }

    private void getContactFromNumber() {
        if(!numbers.isEmpty()){
            Cursor contacts = getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
            );

            // Loop through the contacts
            outer: while (contacts.moveToNext()) {
            // Get the current contact name
                String name = contacts.getString(
                contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY));

                // Get the current contact phone number
                String phoneNumber = contacts.getString(
                contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                phoneNumber = phoneNumber.replaceAll("\\s", "");

                // Display the contact to text view
                String image = contacts.getString(
                        contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI));
                //Log.d(TAG, "getContactFromNumber: " + phoneNumber + " " + name + " " + image);
                if(numbers.contains(phoneNumber)){
                    Log.d(TAG, "getContactFromNumber: " + phoneNumber + " " + name + " " + image);
                    int i = numbers.indexOf(phoneNumber);
                    names.set(i, name);
                    images.set(i, image);
                }
            }
            contacts.close();
            addToDataBase(false);

        }
    }

    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(LovedOnesActivity.this, permission)
                == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(LovedOnesActivity.this,
                    new String[] { permission },
                    requestCode);
        }
        else {
//            Toast.makeText(LovedOnesActivity.this,
//                    "Permission already granted",
//                    Toast.LENGTH_SHORT)
//                    .show();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,
                        permissions,
                        grantResults);

        if (requestCode == CONTACTS_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(LovedOnesActivity.this,
                        "Camera Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            else {
                Toast.makeText(LovedOnesActivity.this,
                        "Camera Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void getContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        switch (reqCode) {
            case (REQUEST_CODE):
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor c = getContentResolver().query(contactData, null, null, null, null);
                    if (c.moveToFirst()) {
                        String contactId = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
                        String hasNumber = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                        String num = "", name = "", image = "";
                        if (Integer.valueOf(hasNumber) == 1) {
                            flag = false;
                            Cursor numbers = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
                            while (numbers.moveToNext()) {
                                num = numbers.getString(numbers.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                num = num.replaceAll("\\s", "");
                                name = numbers.getString(numbers.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                                image = numbers.getString(numbers.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI));
                                //Toast.makeText(LovedOnesActivity.this, "Number=" + num, Toast.LENGTH_LONG).show();
                                if(!this.numbers.contains(num)) {
                                    Log.d(TAG, "onActivityResult: Number is not there" + num);
                                    this.numbers.add(num);
                                    this.names.add(name);
                                    this.images.add(image);
                                    Log.d(TAG, "onActivityResult: " + image);
                                    flag = true;
                                }
                            }
                            if(flag == false){
                                Toast.makeText(LovedOnesActivity.this, "Number is already added", Toast.LENGTH_SHORT).show();
                            }
                            addToDataBase(true);
                        }
                    }
                    break;
                }
                else{
                    Log.d(TAG, "onActivityResult: Something happened");
                }
        }
    }

    private void addToDataBase(final boolean stage) {
        db.collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot document: task.getResult()) {
                                userNumbers.add("+91" + document.get("number"));
                                userIds.add((String)document.get("uid"));
                            }
                            //make second request
                            addToDataBase2(stage);
                        }
                    }
                });
    }

    private void addToDataBase2(final boolean stage) {
        for(String id: userIds) {
            db.collection("users")
                    .document(id)
                    .collection("isSafe")
                    .document(userNumbers.get(userIds.indexOf(id)))
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.isSuccessful())
                                userSafes.add(String.valueOf(task.getResult().get("isSafe")));
                            if(userSafes.size() == userIds.size()){
                                if(stage)
                                    addToDataBaseContd();
                                else
                                    loadRecyclerView();
                            }
                        }
                    });
        }
    }

    private void addToDataBaseContd() {
        if(userNumbers.contains(numbers.get(numbers.size()-1))){
            HashMap<String, Object> map = new HashMap<>();
            map.put("lovedOnes", numbers);
            db.collection("users")
                    .document(mAuth.getCurrentUser().getUid())
                    .update(map)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            loadRecyclerView();
                        }
                    });
        }
        else{
            numbers.remove(numbers.size() - 1);
            names.remove(names.size() - 1);
            images.remove(images.size() - 1);
            Toast.makeText(this, "The contact you are trying to add is not using the application", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRecyclerView() {
        getsafes();
        Log.d(TAG, "loadRecyclerView: prep recycler view");
        RecyclerView recyclerView = findViewById(R.id.lovedRecycler);
        RecyclerViewAdapterLovedOnesPage adapterLovedOnesPage = new RecyclerViewAdapterLovedOnesPage(this, numbers, names, images, safes);
        recyclerView.setAdapter(adapterLovedOnesPage);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
//        numbers.clear();
//        names.clear();
//        images.clear();
    }

    private void getsafes() {
        Log.d(TAG, numbers.toString());
        for (String num : numbers) {
            int i = userNumbers.indexOf(num);
            safes.add(userSafes.get(i));
        }
        Log.d(TAG, "getsafes: " + safes);
    }
}