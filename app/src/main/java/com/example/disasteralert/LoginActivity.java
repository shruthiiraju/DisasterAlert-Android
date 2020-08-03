package com.example.disasteralert;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    public final String TAG = "LoginActivity";
    EditText number, code;
    TextView create;
    Button send, verify;
    ProgressBar pb;
    String CodeSent,phoneNumber;
    boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        //Initialise the content
        number = findViewById(R.id.acc_user);
        code = findViewById(R.id.acc_password);
        send = findViewById(R.id.acc_sign);
        verify = findViewById(R.id.acc_verify);
        create = findViewById(R.id.acc_create);
        pb = findViewById(R.id.login_pb);

        //Set onClick for Button
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check is user exists in database
                checkUsers();
            }
        });

        //set OnClick for verify
        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckOnVerification();
            }
        });

        //Set onClick for create goes to create account
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, CreateAccActivity.class));
                finish();
            }
        });
    }

    private void checkUsers() {
        phoneNumber = number.getText().toString();
        if(phoneNumber.length() < 10)
            return;

        db.collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            pb.setVisibility(View.VISIBLE);
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                Map<String, Object> map = document.getData();
                                try {
                                    if (map.get("number").toString().equals(phoneNumber)) {
                                        flag = true;
                                    }
                                }
                                catch (Exception e) {
                                    flag = false;
                                }
                            }
                            checkUsersComplete();
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private void checkUsersComplete() {
        Log.d(TAG, "checkUsers: " + flag);
        if(flag){
            try{
                SendVerificationCode();
            }
            catch (Exception e){
                pb.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Fill each field", Toast.LENGTH_SHORT)
                        .show();
            }
        }
        else{
            Toast.makeText(LoginActivity.this, "Sign Up to Enter Credentials", Toast.LENGTH_LONG)
                    .show();
        }
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
        pb.setVisibility(View.GONE);
        phoneNumber = "+91" + number.getText().toString();

        if(number.getText().toString().isEmpty()){
            number.setText("Phone Number is Required");
            return;
        }

        if (phoneNumber.length() < 10){
            number.setText("Please Enter a Valid Phone Number");
            number.requestFocus();
            return;
        }

        pb.setVisibility(View.VISIBLE);

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks

    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        pb.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            Toast.makeText(LoginActivity.this, "Signed In", Toast.LENGTH_SHORT).show();

                            FirebaseUser currentUser = mAuth.getCurrentUser();

                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
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


    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
            Log.d(TAG, "onVerificationCompleted:" + phoneAuthCredential);

            signInWithPhoneAuthCredential(phoneAuthCredential);
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            pb.setVisibility(View.GONE);
            Log.d(TAG, "onVerificationFailed: failed");
            Toast.makeText(LoginActivity.this, "Failed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            pb.setVisibility(View.GONE);
            Toast.makeText(LoginActivity.this, "Code Sent", Toast.LENGTH_SHORT).show();
            CodeSent = s;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}