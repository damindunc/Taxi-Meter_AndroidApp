package com.iotaconcepts.distancecalc;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.lang.reflect.Method;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener
{
    private GoogleMap mMap;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    private LocationRequest mLocationRequest;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters

    Button startWalking, save_fare;
    TextView lat_lon_disp, distanceDisplay, fare_display, ActionbarTitle;
    CheckBox fare_checkbox;
    EditText et_fare_rate;
    FloatingActionButton fab;

    GPSTracker gps;
    PolylineOptions line;

    Double lat_1, lon_1, L1, L2;
    int count, a, fare_value;
    float distanceTravelled;

    boolean startMeasuring;

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        init();

        if (!checkInternetConnection() && !checkLocationAccess()) {
            messagePopup("Required", "This application requires active INTERNET CONNECTION and GPS LOCATION enabled. Please turn them on.");
        }
        else if (!checkInternetConnection()) {
            messagePopup("Required", "This application requires active INTERNET CONNECTION. Please turn on your data.");
        }
        else if (!checkLocationAccess()) {
            messagePopup("Required", "Please turn on your LOCATION in order to use the app.");
        }

        if (checkPlayServices()) {
            buildGoogleApiClient();
            createLocationRequest();
        }

        try {
            initilizeMap();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            initilizeMap();
            startMeasuring = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        fare_checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                et_fare_rate.setVisibility(View.VISIBLE);
                save_fare.setVisibility(View.VISIBLE);
            }
        });

        save_fare.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                save_fare.setVisibility(View.INVISIBLE);
                et_fare_rate.setVisibility(View.INVISIBLE);
                fare_display.setVisibility(View.VISIBLE);
                fare_value = Integer.parseInt(et_fare_rate.getText().toString());
            }
        });

        startWalking.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                GPSFunctionWrapper();

                if (a == 0) {
                    // Snackbar
                    Snackbar snack = Snackbar.make(v, "Started measuring distance travelled.", Snackbar.LENGTH_LONG).setAction("Action", null);
                    ViewGroup group = (ViewGroup) snack.getView();
                    group.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.floroGreen));
                    snack.show();
                    distanceDisplay.setVisibility(View.VISIBLE);
                    fare_checkbox.setVisibility(View.VISIBLE);
                    a = 1;
                }
                else if(a == 1) {
                    // Snackbar
                    Snackbar snack = Snackbar.make(v, "Stoped measuring distance travelled.", Snackbar.LENGTH_LONG).setAction("Action", null);
                    ViewGroup group = (ViewGroup) snack.getView();
                    group.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.floroGreen));
                    snack.show();
                    a = 0;
                }

                // Plot First PolyLine
                line = new PolylineOptions().add(new LatLng(lat_1, lon_1), new LatLng(lat_1, lon_1)).width(13).color(getApplicationContext().getResources().getColor(R.color.floroGreen));
                mMap.addPolyline(line);

                // Watchout for location updates
                togglePeriodicLocationUpdates();
            }
        });

    } // On create

    private void init(){
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        startWalking = (Button)findViewById(R.id.bt_startWalking);
        distanceDisplay = (TextView)findViewById(R.id.tv_distance);
        fare_checkbox = (CheckBox)findViewById(R.id.cb_fare_checkbox);
        et_fare_rate = (EditText)findViewById(R.id.et_fare_rate);
        save_fare = (Button)findViewById(R.id.bt_save_fare);
        fare_display = (TextView)findViewById(R.id.tv_fare);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        ActionbarTitle = (TextView)findViewById(R.id.tv_maps_actionbar_title);

        Typeface Mont = Typeface.createFromAsset(getApplication().getAssets(), "Montserrat-Regular.otf");
        ActionbarTitle.setTypeface(Mont);
        startWalking.setTypeface(Mont);
        distanceDisplay.setTypeface(Mont);
        fare_checkbox.setTypeface(Mont);
        save_fare.setTypeface(Mont);
        fare_display.setTypeface(Mont);
        et_fare_rate.setTypeface(Mont);

        distanceTravelled = 0;
        count = 0;
        a = 0;
        startMeasuring = false;
    }

    /**
     * function to load map. If map is not created it will create it for you
     * */
    private void initilizeMap()
    {
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setZoomGesturesEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setRotateGesturesEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.setMyLocationEnabled(true);

            GPSFunctionWrapper();

            LatLng coordinate = new LatLng(L1, L2);
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 14);
            mMap.animateCamera(yourLocation);

            // check if map is created successfully or not
            if (mMap == null) {
                Toast.makeText(getApplicationContext(), "Sorry! unable to create maps", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            GPSFunctionWrapper();

            LatLng coordinate = new LatLng(L1, L2);
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 14);
            mMap.animateCamera(yourLocation);

            // check if map is created successfully or not
            if (mMap == null) {
                Toast.makeText(getApplicationContext(), "Sorry! unable to create maps", Toast.LENGTH_SHORT).show();
            }
        }
    }

    void GPSFunctionWrapper() {
        int hasPermission = 0;

        // Go for RUNTIME PERMISSION CHECK only if the device is Marshmallow or above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
        {
            // Check LOCATION PERMISSION at runtime
            hasPermission = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasPermission != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);
                Toast.makeText(MapsActivity.this, "Location permission was not granted!", Toast.LENGTH_SHORT).show();

                // return null if permission not granted
                return;
            }

            // permission granted :) lets get location and call API.
            GPSFunction();
        }
        else {
            // For device lower than Marshmallow
            // Call locationAndCall function straight away, no need to check!
            GPSFunction();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    GPSFunction();
                } else {
                    // Permission Denied
                    Toast.makeText(MapsActivity.this, "You denied location permission!", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    void GPSFunction() {
        gps = new GPSTracker(MapsActivity.this);
        if(gps.canGetLocation()) {
            lat_1 = gps.getLatitude();
            lon_1 = gps.getLongitude();
        }
        else {
            //       gps.showSettingsAlert();
        }
    }

    /**
     * Method to display the location on UI ************************************************************ F L A G ************
     * */
    private void displayLocation() {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            String sTemp = String.format("%.2f", distanceTravelled);

            distanceDisplay.setText("Distace travelled: " + sTemp + " km");

            float temp = distanceTravelled * fare_value;
            fare_display.setText("Total fare: " + temp);

            count++;
        }
        else {
            //lat_lon_disp.setText("Couldn't get the location. Make sure location is enabled on the device");
        }
    }


    /**
     * Method to toggle periodic location updates
     * */
    private void togglePeriodicLocationUpdates()
    {
        if (!mRequestingLocationUpdates)
        {
            // Changing the button text
            //startWalking.setText(getString(R.string.btn_stop_location_updates));

            startWalking.setBackgroundColor(getResources().getColor(R.color.floroGreen));
            startWalking.setTextColor(getResources().getColor(R.color.colorPrimary));
            startWalking.setText("STOP JOURNEY");

            mRequestingLocationUpdates = true;
            startLocationUpdates();
        }
        else
        {
            // Changing the button text
            //startWalking.setText(getString(R.string.btn_start_location_updates));

            startWalking.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            startWalking.setTextColor(getResources().getColor(R.color.floroGreen));
            startWalking.setText("START JOURNEY");

            mRequestingLocationUpdates = false;
            // Stopping the location updates
            stopLocationUpdates();
        }
    }


    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Creating location request object
     * */
    protected void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices()
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates()
    {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {

    }

    @Override
    public void onConnected(Bundle arg0)
    {
        // Once connected with google api, get the location
        displayLocation();

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    /**
     * On Location Change ******************************************************************************* F L A G ************
     */
    @Override
    public void onLocationChanged(Location location)
    {

        if (startMeasuring) {
            // Assign the new location
            //this.mLastLocation = location;
            mLastLocation = location;

            Double lat_2, lon_2;
            lat_2 = location.getLatitude();
            lon_2 = location.getLongitude();

            //Toast.makeText(getApplicationContext(), "Location changed!" + lat_2 +"   "+lon_2, Toast.LENGTH_SHORT).show();

            // Plot new Polyline on map
            line = new PolylineOptions().add(new LatLng(lat_1, lon_1), new LatLng(lat_2, lon_2)).width(13).color(getApplicationContext().getResources().getColor(R.color.floroGreen));
            mMap.addPolyline(line);

            // Calculating distance between new location and previous location
            float[] results = new float[1];
            Location.distanceBetween(lat_1, lon_1, lat_2, lon_2, results);
            distanceTravelled = distanceTravelled + (results[0] / 1000);


            lat_1 = lat_2;
            lon_1 = lon_2;

            // Displaying the new location on UI
            displayLocation();
        }
        else {

        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPlayServices();

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }


    void messagePopup(String heading, String message){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
        alertDialog.setTitle(heading);
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
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






}

