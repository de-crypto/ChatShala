package com.google.firebase.udacity.friendlychat;

/*
  Created by Decrypto on 22/04/17
*/

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = "MainActivity";
    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final String Chatter_message_length = "Chatter_message_length";

    public static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER = 2;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    public String Checkusername;
    private String mUsername;


    // Firebase instance variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;
        Checkusername = ANONYMOUS;

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);


        if(!haveNetworkConnection()){
            Intent i = new Intent(this,NoInternet.class);
            startActivity(i);
            this.finish();
        }

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.VISIBLE);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.custom_dialog);
                dialog.setTitle("Choose Profile photo");

                // set the custom dialog components - text, image and button
                Button dialogButton1 = (Button) dialog.findViewById(R.id.dbtn1);
                Button dialogButton2 = (Button) dialog.findViewById(R.id.dbtn2);
                // if button is clicked, close the custom dialog
                dialogButton1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        gallery_option();
                        dialog.dismiss();
                    }
                });
                dialogButton2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        camera_option();
                        dialog.dismiss();
                    }
                });
                WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
                wmlp.gravity = Gravity.BOTTOM | Gravity.CENTER;
                wmlp.width=900;
                wmlp.height=800;
                wmlp.y = -400;   //y position
                dialog.show();
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null,Checkusername);
                mMessagesDatabaseReference.push().setValue(friendlyMessage);

                // Clear input box
                mMessageEditText.setText("");
            }
        });

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Checkusername = user.getDisplayName();
                    onSignedInInitialize(user.getDisplayName());
                } else {
                    // User is signed out
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setTheme(R.style.AppTheme)
                                    .setIsSmartLockEnabled(false)
                                    .setProviders(
                                            AuthUI.EMAIL_PROVIDER,
                                            AuthUI.GOOGLE_PROVIDER)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        Map<String,Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(Chatter_message_length,DEFAULT_MSG_LENGTH_LIMIT);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);

        //Method to check the update in Remote Config
        fetchconfig();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            return false;
        }
        return false;
    }

    private void fetchconfig() {
        long cacheExpiration = 3600;

        if(mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()){
            cacheExpiration=0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                       mFirebaseRemoteConfig.activateFetched();
                        applyRetrievelengthLimit();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
              Log.w(TAG,"Error Fetching config", e);
                applyRetrievelengthLimit();
            }
        });
    }

    private void applyRetrievelengthLimit() {
        Long chatter_message = mFirebaseRemoteConfig.getLong(Chatter_message_length);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(chatter_message.intValue())});
        Log.d(TAG,Chatter_message_length + " = "+ chatter_message);
    }

    //use gallery
    public void gallery_option(){
        boolean result=Utility.checkPermission(MainActivity.this);
        if(result) {
            galleryIntent();
        } else {return ;}
    }

    public void camera_option(){
        boolean result= Utility.checkPermission(MainActivity.this);
        if(result) {
            cameraIntent();
        } else {return ;}
    }

    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"),RC_PHOTO_PICKER);
    }

    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {

            onSelectFromGalleryResult(data);
        }
        else if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            onCaptureImageResult(data);
        }

    }
    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {

        Bitmap bm=null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Uri selectedImageUri = data.getData();

        // Get a reference to store file at chat_photos/<FILENAME>
        StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());

        // Upload file to Firebase Storage
        photoRef.putFile(selectedImageUri)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // When the image has successfully uploaded, we get its download URL
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();

                        // Set the download URL to the message box, so that the user can send it to the database
                        FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername, downloadUrl.toString(),Checkusername);
                        mMessagesDatabaseReference.push().setValue(friendlyMessage);
                    }
                }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Upload failed
                Log.w(TAG, "uploadFromUri:onFailure", exception);
                Toast.makeText(MainActivity.this, "Error: upload failed",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");

        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + ".jpg");

        // CALL THIS METHOD TO GET THE URI FROM THE BITMAP
        Uri tempUri = getImageUri(getApplicationContext(), thumbnail);

        //Create uri for Camera image
        Uri selectedImageUri = tempUri;

        // Get a reference to store file at chat_photos/<FILENAME>
        StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());

        // Upload file to Firebase Storage
        photoRef.putFile(selectedImageUri)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // When the image has successfully uploaded, we get its download URL
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();

                        // Set the download URL to the message box, so that the user can send it to the database
                        FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername, downloadUrl.toString(),Checkusername);
                        mMessagesDatabaseReference.push().setValue(friendlyMessage);
                    }
                }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Upload failed
                Log.w(TAG, "uploadFromUri:onFailure", exception);
                Toast.makeText(MainActivity.this, "Error: upload failed",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this,R.style.DialogStyle);
                builder1.setMessage("Are you sure you want to Sign Out?");
                builder1.setCancelable(true);

                builder1.setPositiveButton(
                        "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                //sign out
                                AuthUI.getInstance().signOut(MainActivity.this);
                            }

                        });
                builder1.setNegativeButton(
                        "No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }

                        });
                AlertDialog alert11 = builder1.create();
                alert11.show();
                return true;
            case  R.id.about_us_item:
                Intent i = new Intent(MainActivity.this,AboutUs.class);
                startActivity(i);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSignedInInitialize(String username) {
        mUsername = username;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                public void onCancelled(DatabaseError databaseError) {
                }
            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}
