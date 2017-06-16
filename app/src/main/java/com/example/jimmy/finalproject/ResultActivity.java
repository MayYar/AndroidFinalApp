package com.example.jimmy.finalproject;


import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;

import org.w3c.dom.Text;


public class ResultActivity extends AppCompatActivity {


    ImageView downloadImg;
    Button edit;
    TextView emo;
    ImageButton plus, home;
    StorageReference storageReference;
    DatabaseReference myRef;

    static String name, happy_val;
    static Integer count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        storageReference = FirebaseStorage.getInstance().getReference();

        downloadImg = (ImageView)findViewById(R.id.iv_pic);
        emo = (TextView)findViewById(R.id.tv_emo);
        home = (ImageButton)findViewById(R.id.ib_home);
        plus = (ImageButton)findViewById(R.id.ib_plus);
        plus.setOnClickListener(doClick);
        home.setOnClickListener(doClick);

        Bundle bundle = this.getIntent().getExtras();
        name = bundle.getString("name");

        downloadImage();
        showInfo();




    }

    private void showInfo() {
            // Read from the database
        myRef = FirebaseDatabase.getInstance().getReference(name);
        Toast.makeText(ResultActivity.this, "ShowInfo_func", Toast.LENGTH_SHORT).show();

            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.hasChild("happiness")){
                        String value = dataSnapshot.child("happiness").getValue().toString();
                        emo.setText(name + "\n開心指數:" + value);
                        //Toast.makeText(ResultActivity.this, value, Toast.LENGTH_SHORT).show();
                        //Log.d(TAG, "Value is: " + value);
                    }
                }


                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });



    }

    private void downloadImage(){
        storageReference.child(name + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Got the download URL for 'users/me/profile.png'
                Glide.with(ResultActivity.this)
                        .load(uri)
                        .error(R.drawable.no_image)
                        .into(downloadImg);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                //Toast.makeText(ResultActivity.this, "下載失敗", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Button.OnClickListener doClick = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.ib_home:
                    Intent intent2 = new Intent();
                    intent2.setClass(ResultActivity.this, MapsActivity.class);
                    startActivity(intent2);
                    break;
                case R.id.ib_plus:
                    Intent intent = new Intent();
                    intent.setClass(ResultActivity.this, PhotoActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("name", name);
                    intent.putExtras(bundle);
                    startActivity(intent);
                    break;
            }
        }
    };



}

