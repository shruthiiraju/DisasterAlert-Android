package com.example.disasteralert;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.icu.text.CaseMap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;

public class DonationActivity extends AppCompatActivity {

    final String TAG = "DonationActivity";
    final int UPI_PAYMENT = 0;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    EditText amt, upi, name, note;
    Button send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        final Button makePayment = findViewById(R.id.make_donation);

        makePayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeUPIPayments();
            }
        });

    }

    private void makeUPIPayments() {
        LinearLayout layout_UPI = findViewById(R.id.upi_donations_layout);
        layout_UPI.setVisibility(View.VISIBLE);

        amt = findViewById(R.id.value_donation);
        upi = findViewById(R.id.upi_id_donation);
        name = findViewById(R.id.name_donation);
        note = findViewById(R.id.note_donation);
        send = findViewById(R.id.make_donation);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String amount = amt.getText().toString();
                String upi_id = upi.getText().toString();
                String Name = name.getText().toString();
                String Note = note.getText().toString();
                payUsingUPI(amount, upi_id, Name, Note);
            }
        });
    }

    private void payUsingUPI(String amount, String upi_id, String name, String note) {
        Uri uri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upi_id)
                .appendQueryParameter("pn", name)
                .appendQueryParameter("tn", note)
                .appendQueryParameter("am", amount)
                .appendQueryParameter("cu", "INR")
                .build();

        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW);
        upiPayIntent.setData(uri);

        //dialog to show apps to user
        Intent chooser = Intent.createChooser(upiPayIntent, "Pay with");

        //Check if intent is resolved
        if(chooser.resolveActivity(getPackageManager()) != null){
            startActivityForResult(chooser, UPI_PAYMENT);
        }
        else {
            Toast.makeText(DonationActivity.this, "No UPI app found, make sure you have one", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case UPI_PAYMENT:
                    if(resultCode == RESULT_OK || resultCode == 11){
                        if(data != null){
                            String txt = data.getStringExtra("response");
                            Log.d(TAG, "onActivityResult: " + txt);
                            ArrayList<String> dataList = new ArrayList<>();
                            dataList.add(txt);
                            upiPaymentDataOperation(dataList);
                        }
                        else{
                            Log.d(TAG, "onActivityResult: Data returned is null :(");
                            ArrayList<String> dataList = new ArrayList<>();
                            dataList.add("nothing");
                            upiPaymentDataOperation(dataList);
                        }
                    }
                    else{
                        Log.d(TAG, "onActivityResult: Data returned is null :(");
                        ArrayList<String> dataList = new ArrayList<>();
                        dataList.add("nothing");
                        upiPaymentDataOperation(dataList);
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            Toast.makeText(DonationActivity.this, ex.toString(),
                    Toast.LENGTH_SHORT).show();
        }

    }

    private void upiPaymentDataOperation(ArrayList<String> data) {
        if(isConnectionAvailable(DonationActivity.this)){
            String str = data.get(0);
            Log.d(TAG, "upiPaymentDataOperation: " + str);
            String paymentCancel = "";
            if(str == null)
                str = "discard";
            String status = "";
            String approvalRefNo = "";
            String[] response = str.split("&");
            for (String s : response) {
                String[] equalStr = s.split("=");
                if (equalStr.length >= 2) {
                    if (equalStr[0].toLowerCase().equals("Status".toLowerCase())) {
                        status = equalStr[1].toLowerCase();
                    } else if (equalStr[0].toLowerCase().equals("ApprovalRefNo".toLowerCase())
                            || equalStr[0].toLowerCase().equals("txnRef".toLowerCase())) {
                        approvalRefNo = equalStr[1];
                    }
                } else {
                    paymentCancel = "Payment cancelled by user.";
                }
            }
            if (status.equals("success")) {
                //Code to handle successful transaction here.
                Toast.makeText(DonationActivity.this, "Transaction successful.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "responseStr: "+approvalRefNo);

                //update to database
                HashMap<String, Object> map = new HashMap<>();
                map.put("NDRF", amt.getText().toString());
                map.put("FundType", "Mobile Donation");
                map.put("byPhone", mAuth.getCurrentUser().getPhoneNumber());
                map.put("byUid", mAuth.getCurrentUser().getUid());

                db.collection("funds")
                        .add(map)
                        .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                if(task.isSuccessful()){
                                    Log.d(TAG, "onComplete: Database was updated");
                                }
                            }
                        });

            }
            else if("Payment cancelled by user.".equals(paymentCancel)) {
                Toast.makeText(DonationActivity.this, "Payment cancelled by user.", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(DonationActivity.this, "Transaction failed.Please try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(DonationActivity.this, "Internet connection is not available. Please check and try again", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isConnectionAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()
                    && netInfo.isConnectedOrConnecting()
                    && netInfo.isAvailable()) {
                return true;
            }
        }
        return false;
    }

}