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
    ParseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchRole = (Switch) findViewById(R.id.switchRole);

        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        user = ParseUser.getCurrentUser();

        if(user == null){
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser userLogged, ParseException e) {
                    if(e==null){
                        user = userLogged;
                        Log.i("Anon", "Successful");
                    }
                    else{
                        Log.i("Anon", "failed to create anon user");
                    }
                }
            });
        }else{
            if(user.get("isDriver")!=null){
                try {
                    user.fetch();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if(user.getBoolean("isDriver")){
                    startUberActivity(DriverViewRequestsActivity.class);
                }
                else{
                    startUberActivity(RiderMapsActivity.class);
                }
            }
        }
    }

    private void startUberActivity(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);
    }

    public void logIn(View view){
        final boolean isDriver = switchRole.isChecked();
        user.put("isDriver", isDriver);
        Log.i("isDriver", isDriver+"");
        user.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e==null){
                    if(isDriver){
                        startUberActivity(DriverViewRequestsActivity.class);
                    }
                    else{
                        startUberActivity(RiderMapsActivity.class);
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
