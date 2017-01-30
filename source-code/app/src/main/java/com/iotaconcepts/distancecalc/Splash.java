package com.iotaconcepts.distancecalc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;

public class Splash extends Activity{

    Button getStarted;
    TextView description;

    ProgressDialog rd;
    GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();

        checkRequirements();

        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                rd = new ProgressDialog(Splash.this);
                rd.setTitle("Please Wait!");
                rd.setMessage("Setting Map for you..");
                rd.setCancelable(false);
                rd.show();

                testGPSRun();

                runTimer();
            }
        });
    }

    private void init(){
        setContentView(R.layout.splash_layout);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getStarted = (Button)findViewById(R.id.bt_splash_get_started);
        description = (TextView)findViewById(R.id.tv_splash_descriptionOfApp);

        Typeface Mont = Typeface.createFromAsset(getApplication().getAssets(), "Montserrat-Regular.otf");
        getStarted.setTypeface(Mont);
        description.setTypeface(Mont);
    }

    void checkRequirements() {
        if (!checkInternetConnection() && !checkLocationAccess()) {
            messagePopup("Required", "This application requires active INTERNET CONNECTION and GPS LOCATION enabled. Please turn them on.");
        }
        else if (!checkInternetConnection()) {
            messagePopup("Required", "This application requires active INTERNET CONNECTION. Please turn on your data.");
        }
        else if (!checkLocationAccess()) {
            messagePopup("Required", "Please turn on your LOCATION in order to use the app.");
        }
        else {
            Toast.makeText(this, "yay!", Toast.LENGTH_SHORT).show();
        }
    }

    void messagePopup(String heading, String message){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Splash.this);
        alertDialog.setTitle(heading);
        alertDialog.setMessage(message);
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                checkRequirements();
            }
        });
        alertDialog.show();
    }

    void runTimer() {
        Thread timer = new Thread() {
            public void run() {
                try {
                    sleep(3000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finally {
                    rd.cancel();
                    startActivity(new Intent(Splash.this, MapsActivity.class));
                }
            }
        };
        timer.start();
    }

    boolean checkInternetConnection() {
        boolean mobileDataEnabled = false;
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean)method.invoke(cm);
        } catch (Exception e) {
            // Some problem accessible private API
            // TODO do whatever error handling you want here
        }
        return mobileDataEnabled;
    }

    boolean checkLocationAccess() {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }else{
            locationProviders = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    void testGPSRun(){
        gps = new GPSTracker(Splash.this);
        if(gps.canGetLocation()) {
            Double L1 = gps.getLatitude();
            Double L2 = gps.getLongitude();
        }
        else {
            //gps.showSettingsAlert();
        }
    }
}
