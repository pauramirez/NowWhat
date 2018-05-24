package com.zerostudios.nowwhat;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegistrationActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthStateListener;

    private LinearLayout registerLayout;
    private AnimationDrawable animationDrawable;
    private EditText mFname, mEmail, mPassword, mCPassword;
    private Button mRegister;
    private RadioGroup mRadioGroup;
    private CheckBox mCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        firebaseAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {
                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user != null)
                {
                    Intent intent = new Intent(RegistrationActivity.this, TutorialActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        mFname = findViewById(R.id.fname);
        mEmail = findViewById(R.id.user);
        mPassword = findViewById(R.id.pass);
        mCPassword = findViewById(R.id.confirmpass);
        mCheckBox = findViewById(R.id.mayor);
        mRegister = findViewById(R.id.register);
        mRadioGroup = findViewById(R.id.radioGroup);
        registerLayout = findViewById(R.id.registerLayout);
        animationDrawable = (AnimationDrawable)registerLayout.getBackground();
        animationDrawable.setEnterFadeDuration(1000);
        animationDrawable.setExitFadeDuration(2000);
        animationDrawable.start();

        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int selectedId = mRadioGroup.getCheckedRadioButtonId();
                final RadioButton radioButton = findViewById(selectedId);
                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();
                final String cpassword = mCPassword.getText().toString();
                final String name = mFname.getText().toString();

                if(radioButton== null||radioButton.getText()==null)
                {
                    Toast.makeText(RegistrationActivity.this, "No Gender Selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                else if(email.isEmpty()||password.isEmpty()||name.isEmpty())
                {
                    Toast.makeText(RegistrationActivity.this, " Missing Fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                else if(!password.equals(cpassword))
                {
                    Toast.makeText(RegistrationActivity.this, "Passwords do not correspond", Toast.LENGTH_SHORT).show();
                    return;
                }


                mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(RegistrationActivity.this, new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful())
                        {
                            Toast.makeText(RegistrationActivity.this, "Sign Up Error", Toast.LENGTH_SHORT).show();
                        }

                        else
                        {
                            String mayorEdad;
                            if(mCheckBox.isChecked())
                            {
                                mayorEdad = "Mayor";
                            }
                            else
                            {
                                mayorEdad = "Menor";
                            }

                            String userId = mAuth.getCurrentUser().getUid();
                            DatabaseReference currentUserDb = FirebaseDatabase.getInstance().getReference().child("Users").child(radioButton.getText().toString()).child(mayorEdad).child(userId).child("name");
                            currentUserDb.setValue(name);
                        }
                    }
                });

            }
        });
    }

    public void backToLogin(View view)
    {
        Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
        return;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthStateListener);
    }
}
