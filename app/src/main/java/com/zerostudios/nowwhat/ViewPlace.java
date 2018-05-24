package com.zerostudios.nowwhat;

import android.Manifest;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.zerostudios.nowwhat.Model.Photos;
import com.zerostudios.nowwhat.Model.PlaceDetail;
import com.zerostudios.nowwhat.Remote.IGoogleAPIService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewPlace extends AppCompatActivity {

    ImageView photo;
    RatingBar ratingBar;
    TextView place_address,place_name;
    Button btnViewOnMap, btnViewDirections, btnTakePhoto;
    ListView listView;
    FirebaseListAdapter adapter;

    private static final int CAMERA_REQUES_CODE =1;
    private String mCurrentPhotoPath, placeType;
    private Uri photoURI;

    private StorageReference mStorage;

    private DatabaseReference mDatabase;
    private DatabaseReference mDatabaseLoad;

    RecyclerView recyclerView;

    RecyclerView.Adapter recyclerAdapter ;

    List<ImageUploadInfo> list = new ArrayList<>();

    private ProgressDialog mProgress;

    IGoogleAPIService mService;

    PlaceDetail mPlace;

    String placeNameForFire;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_place);

        // Assign id to RecyclerView.
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        // Setting RecyclerView size true.
        recyclerView.setHasFixedSize(true);

        // Setting RecyclerView layout as LinearLayout.
        recyclerView.setLayoutManager(new LinearLayoutManager(ViewPlace.this));

        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Photos");
        mProgress = new ProgressDialog(this);
        mService = Common.getGoogleAPIService();

        photo = findViewById(R.id.photo);
        ratingBar = findViewById(R.id.ratingBar);
        place_address = findViewById(R.id.place_address);
        place_name = findViewById(R.id.place_name);
        btnViewOnMap = findViewById(R.id.btn_show_map);
        btnViewDirections = findViewById(R.id.btn_view_directions);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        place_name.setText("");
        place_address.setText("");



        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
        {
            checkCameraPermission();
        }

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();

            }
        });


        btnViewOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mPlace.getResult().getUrl()));
                startActivity(mapIntent);
            }
        });


        btnViewDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mapIntent = new Intent(ViewPlace.this, ViewDirections.class);
                startActivity(mapIntent);

            }
        });

        //Photo
        if(Common.currentResult.getPhotos()!= null && Common.currentResult.getPhotos().length > 0)
        {
            Picasso.with(this)
                    .load(getPhotoOfPlace(Common.currentResult.getPhotos()[0].getPhoto_reference(),1000))
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.ic_email_white_24dp)
                    .into(photo);
        }

        //Rating
        if(Common.currentResult.getRating()!= null && !TextUtils.isEmpty(Common.currentResult.getRating()))
        {
            ratingBar.setRating(Float.parseFloat(Common.currentResult.getRating()));
        }

        else
        {
            ratingBar.setVisibility(View.GONE);
        }



        //Address and Name
        mService.getDetailPlace(getPlaceDetailUrl(Common.currentResult.getPlace_id()))
                .enqueue(new Callback<PlaceDetail>() {
                    @Override
                    public void onResponse(Call<PlaceDetail> call, Response<PlaceDetail> response)
                    {
                        mPlace = response.body();

                        place_address.setText(mPlace.getResult().getFormatted_address());
                        place_name.setText(mPlace.getResult().getName());
                        placeNameForFire = mPlace.getResult().getName();
                        placeType = mPlace.getResult().getTypes()[0].toString();
                        mDatabaseLoad = FirebaseDatabase.getInstance().getReference().child("Photos").child(placeType).child(placeNameForFire);

                        // Adding Add Value Event Listener to databaseReference.
                        mDatabaseLoad.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {

                                for (DataSnapshot postSnapshot : snapshot.getChildren()) {

                                    ImageUploadInfo imageUploadInfo = postSnapshot.getValue(ImageUploadInfo.class);

                                    list.add(imageUploadInfo);
                                }

                                recyclerAdapter = new RecyclerViewAdapter(getApplicationContext(), list);

                                recyclerView.setAdapter(recyclerAdapter);

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                                // Hiding the progress dialog.


                            }
                        });

                    }

                    @Override
                    public void onFailure(Call<PlaceDetail> call, Throwable t) {

                    }
                });

    }


    private boolean checkCameraPermission()
    {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA))
            {
                ActivityCompat.requestPermissions(this,new String[]{

                        android.Manifest.permission.CAMERA
                },CAMERA_REQUES_CODE);
            }
            else
                ActivityCompat.requestPermissions(this,new String[]{

                        Manifest.permission.CAMERA
                },CAMERA_REQUES_CODE);

            return false;
        }
        else
            return true;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_REQUES_CODE && resultCode == RESULT_OK){
            mProgress.setMessage("Uploading...");
            mProgress.show();

            StorageReference filepath = mStorage.child("Photos").child(placeNameForFire).child(photoURI.getLastPathSegment());

            filepath.putFile(photoURI).addOnSuccessListener(new    OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Uri downloadUri = taskSnapshot.getDownloadUrl();
                   // Picasso.with(ViewPlace.this).load(downloadUri).fit().centerCrop().into(mImageView);
                    Toast.makeText(ViewPlace.this, "Upload Successful!",    Toast.LENGTH_SHORT).show();
                    mProgress.dismiss();
                    @SuppressWarnings("VisibleForTests")
                    ImageUploadInfo imageUploadInfo = new ImageUploadInfo("Photo at  " + placeNameForFire ,downloadUri.toString());

                    // Getting image upload ID.
                    String ImageUploadId = mDatabase.push().getKey();

                    // Adding image upload id s child element into databaseReference.
                    mDatabase.child(placeType).child(placeNameForFire).child(ImageUploadId).setValue(imageUploadInfo);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ViewPlace.this, "Upload Failed!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private File createImageFile() throws IOException {
// Create an image file name
        String timeStamp = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        }
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

// Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
// Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File...
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.zerostudios.nowwhat.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUES_CODE);
            }
        }
    }

    private String getPlaceDetailUrl(String place_id)
    {
        StringBuilder url = new StringBuilder("https://maps.googleapis.com/maps/api/place/details/json") ;
        url.append("?placeid="+place_id);
        url.append("&key="+"AIzaSyBzFxZlTXSDWc0Sz7whTsvCS7si_knnWPs");
        return url.toString();
    }

    private String getPhotoOfPlace(String photo_reference,int maxWidth)
    {
        StringBuilder url = new StringBuilder("https://maps.googleapis.com/maps/api/place/photo");
        url.append("?maxwidth="+maxWidth);
        url.append("&photoreference="+photo_reference);
        url.append("&key="+"AIzaSyBzFxZlTXSDWc0Sz7whTsvCS7si_knnWPs");
        return url.toString();
    }
}
