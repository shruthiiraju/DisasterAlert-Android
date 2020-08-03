package com.example.disasteralert;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CreateAccActivity extends AppCompatActivity {
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private final String TAG = "CreateAccActivity";
    EditText name, email, number, code;
    Button send, create;
    ProgressBar pb;
    String CodeSent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_acc);
        
        mAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();
        
        //Initialise Content
        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        number = findViewById(R.id.number);
        send = findViewById(R.id.create_send);
        create = findViewById(R.id.create_acc);
        code = findViewById(R.id.create_code);
        pb = findViewById(R.id.create_pb);
        
        //Set onClick
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pb.setVisibility(View.VISIBLE);
                try{
                    SendVerificationCode();
                }
                catch (Exception e){
                    pb.setVisibility(View.GONE);
                    Toast.makeText(CreateAccActivity.this, "Fill each field", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        //Set onClick
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckOnVerification();
            }
        });
    }

    private void CheckOnVerification() {
        String CodeReceived = code.getText().toString();

        if(CodeReceived.isEmpty()) {
            code.setText("Code is Required");
            code.requestFocus();
            return;
        }

        pb.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(CodeSent, CodeReceived);
        signInWithPhoneAuthCredential(credential);
    }

    private void SendVerificationCode() {
        String Number = "+91" + number.getText().toString();

        Log.d(TAG, "SendVerificationCode: " + Number);

        if(number.getText().toString().isEmpty()){
            number.setHint("Phone Number is Required");
            pb.setVisibility(View.GONE);
            return;
        }

        if (Number.length() < 10){
            number.setHint("Please Enter a Valid Phone Number");
            number.requestFocus();
            pb.setVisibility(View.GONE);
            return;
        }

        pb.setVisibility(View.VISIBLE);

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                Number,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks

    }

    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
            Log.d(TAG, "onVerificationCompleted:" + phoneAuthCredential);

            signInWithPhoneAuthCredential(phoneAuthCredential);
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            pb.setVisibility(View.GONE);
            Toast.makeText(CreateAccActivity.this, "Failed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            pb.setVisibility(View.GONE);
            Toast.makeText(CreateAccActivity.this, "Code Sent", Toast.LENGTH_SHORT).show();
            CodeSent = s;
        }
    };

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        pb.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            Toast.makeText(CreateAccActivity.this, "Signed In", Toast.LENGTH_SHORT).show();
                            dbUpdate(mAuth.getCurrentUser(), name.getText().toString(), email.getText().toString(), number.getText().toString());

                            startActivity(new Intent(CreateAccActivity.this, MainActivity.class));
                            finish();
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Log.d(TAG, "onComplete: For whatever reason it happened");
                                finish();
                            }
                        }
                    }
                });
    }

    private void dbUpdate(final FirebaseUser currentUser, String name, String email, String number) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("number", number);
        user.put("uid", currentUser.getUid());

        db.collection("users")
                .document(currentUser.getUid())
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("isSafe", "true");
                        db.collection("users")
                                .document(currentUser.getUid())
                                .collection("isSafe")
                                .document(currentUser.getPhoneNumber())
                                .set(map)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "onSuccess: User Created");
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: User not Created :(");
                    }
                });
    }

}