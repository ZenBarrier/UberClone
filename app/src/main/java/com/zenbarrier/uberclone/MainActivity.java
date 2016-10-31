package com.zenbarrier.uberclone;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class MainActivity extends AppCompatActivity {

    Switch switchRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchRole = (Switch) findViewById(R.id.switchRole);

        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        if(ParseUser.getCurrentUser() == null){
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if(e==null){
                        Log.i("Anon", "Successful");
                    }
                    else{
                        Log.i("Anon", "failed to create anon user");
                    }
                }
            });
        }else{
            if(ParseUser.getCurrentUser().get("isDriver")!=null){
                if(ParseUser.getCurrentUser().getBoolean("isDriver")){
                    Log.i("isDriver", "Launch Driver Activity");
                }
                else{
                    startRiderActivity();
                }
            }
        }
    }

    private void startRiderActivity() {
        Intent intent = new Intent(this, RiderMapsActivity.class);
        startActivity(intent);
    }

    public void logIn(View view){
        final boolean isDriver = switchRole.isChecked();
        ParseUser user = ParseUser.getCurrentUser();
        user.put("isDriver", isDriver);
        user.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e==null){
                    Log.i("Sign", "Successful");
                    if(isDriver){
                        //todo driveractivity
                    }
                    else{
                        startRiderActivity();
                    }
                }
                else{
                    e.printStackTrace();
                    Log.i("Sign", "Failed");
                }
            }
        });
    }
}
