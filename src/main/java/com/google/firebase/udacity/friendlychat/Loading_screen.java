package com.google.firebase.udacity.friendlychat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
public class Loading_screen extends AppCompatActivity {

    /** Duration of wait **/
    private final int SPLASH_DISPLAY_LENGTH = 2000;
    //FirebaseAnalytics mFirebaseAnalytics;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_loading_screen);
        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        // Obtain the FirebaseAnalytics instance.
        //mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                Intent mainIntent = new Intent(Loading_screen.this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(mainIntent);
                //startActivity(new Intent(MainActivity.this, Test_1.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                Loading_screen.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}