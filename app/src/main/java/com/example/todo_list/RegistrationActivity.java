package com.example.todo_list;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class RegistrationActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText emailEditText;
    private EditText nameEditText;
    private EditText passwordEditText;

    private Button registerBtn;

    private TextView loginTextView;

    private ProgressBar progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();

        registerBtn = (Button) findViewById(R.id.registerBtn);
        registerBtn.setOnClickListener(this);

        loginTextView = (TextView) findViewById(R.id.back_to_login);
        loginTextView.setOnClickListener(this);

        emailEditText = (EditText) findViewById(R.id.email);
        nameEditText = (EditText) findViewById(R.id.name);
        passwordEditText = (EditText) findViewById(R.id.password);

        progressBar = (ProgressBar) findViewById(R.id.loading_indicator);

    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back_to_login:
                startActivity(new Intent(this, LoginActivity.class));
                break;
            case R.id.registerBtn:
                registerUser();
                break;
        }
    }

    private void registerUser(){
        String email = emailEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if(email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if(name.isEmpty()) {
            nameEditText.setError("Full name is required");
            nameEditText.requestFocus();
            return;
        }

        if(password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please provide valid email");
            emailEditText.requestFocus();
            return;
        }

        if(password.length() < 6) {
            passwordEditText.setError("Min password length should be 6 characters!");
            passwordEditText.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        emailEditText.setEnabled(false);
        nameEditText.setEnabled(false);
        passwordEditText.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        User user = new User(email, name, password);
                        FirebaseDatabase.getInstance().getReference("Users")
                                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                                .setValue(user)
                                .addOnCompleteListener(task1 -> {
                                    if(task1.isSuccessful()){
                                        Toast.makeText(RegistrationActivity.this, "User has been registered successfully", Toast.LENGTH_LONG).show();
                                        emailEditText.setText("");
                                        nameEditText.setText("");
                                        passwordEditText.setText("");
                                    } else {
                                        Toast.makeText(RegistrationActivity.this, "Failed to register! Try again", Toast.LENGTH_LONG).show();
                                    }
                                    progressBar.setVisibility(View.GONE);
                                    emailEditText.setEnabled(true);
                                    nameEditText.setEnabled(true);
                                    passwordEditText.setEnabled(true);
                                });
                    }
                    else {
                        Toast.makeText(RegistrationActivity.this, "Failed to register", Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                        emailEditText.setEnabled(true);
                        nameEditText.setEnabled(true);
                        passwordEditText.setEnabled(true);
                    }
                });

    }
}