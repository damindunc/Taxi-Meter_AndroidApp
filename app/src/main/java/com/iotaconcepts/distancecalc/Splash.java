package com.iotaconcepts.distancecalc;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class Splash extends Activity{

    Button getStarted;
    TextView description;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Splash.this, MapsActivity.class);
                startActivity(i);
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
}
