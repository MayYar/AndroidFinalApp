package com.example.jimmy.finalproject;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    EditText emailEditText, pwdEdiText;
    Button login, exit;
    FirebaseAuth auth;
    FirebaseAuth.AuthStateListener authListener;
    private String userUID;
    private static final String TAG = "EmailPassword";
    private String email;
    private String password;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        auth = FirebaseAuth.getInstance();
        authListener = new FirebaseAuth.AuthStateListener() { //track sign in or out
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user!=null) {
                    Log.d("onAuthStateChanged", "登入:"+ user.getUid());
                    userUID =  user.getUid();
                }else{
                    Log.d("onAuthStateChanged", "已登出");
                }
            }
        };

        emailEditText = (EditText)findViewById(R.id.et_email);
        pwdEdiText = (EditText)findViewById(R.id.et_pwd);
        login = (Button)findViewById(R.id.btn_login);
        exit = (Button)findViewById(R.id.btn_exit);

        login.setOnClickListener(doClick);
        exit.setOnClickListener(doClick);

    }

    private Button.OnClickListener doClick = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.btn_login:
                    auth = FirebaseAuth.getInstance();
                    email = emailEditText.getText().toString();
                    password = pwdEdiText.getText().toString();
                    if(!email.isEmpty()||!password.isEmpty()){
                        email = email.trim();
                        password = password.trim();
                        auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        Log.d("TAG", "signInWithEmail:onComplete:" + task.isSuccessful());
                                        // If sign in fails, display a message to the user. If sign in succeeds
                                        // the auth state listener will be notified and logic to handle the
                                        // signed in user can be handled in the listener.
                                        if (!task.isSuccessful()) {
                                            Log.w("TAG", "signInWithEmail", task.getException());
                                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                                    Toast.LENGTH_SHORT).show();
                                            register(email,password);
                                        }else{
                                            Toast.makeText(MainActivity.this, "Authentication Success.",
                                                    Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent();
                                            intent.setClass(MainActivity.this,MapsActivity.class);
                                            startActivity(intent);
                                        }
                                    }
                                });
                    }

                    break;
                case R.id.btn_exit:
                    finish();
            }
        }
    };

    private void register(final String email, final String password) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("登入問題")
                .setMessage("無此帳號，是否要以此帳號與密碼註冊?")
                .setPositiveButton("註冊",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                createUser(email, password);
                            }
                        })
                .setNeutralButton("取消", null)
                .show();
    }

    private void createUser(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                String message =
                                        task.isSuccessful() ? "註冊成功" : "註冊失敗";
                                // task.isComplete() ? "註冊成功" : "註冊失敗"; (感謝jiaping網友提醒)
                                new AlertDialog.Builder(MainActivity.this)
                                        .setMessage(message)
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        });
    }

    @Override
    protected void onStart() {  //check to see if the user is currently signed in.
        super.onStart();
        auth.addAuthStateListener(authListener);
    }

    @Override
    protected void onStop() {
        if (authListener != null){
            auth.removeAuthStateListener(authListener);
        }
        super.onStop();
    }

//    @Override
//    protected void onDestroy() {    //強制使用者下次執行APP還要再輸入帳密重新驗證
//        auth.signOut();
//        super.onDestroy();
//    }
}
