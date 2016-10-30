package com.zenbarrier.uberclone;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Switch;

import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

public class MainActivity extends AppCompatActivity {

    Switch switchRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchRole = (Switch) findViewById(R.id.switchRole);

        ParseAnalytics.trackAppOpenedInBackground(getIntent());
    }

    public void logIn(View view){
        boolean isDriver = switchRole.isChecked();
        ParseUser user = ParseUser.getCurrentUser();
        user.put("isDriver", isDriver);
        user.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e==null){
                    Log.i("Sign", "Successful");
                }
                else{
                    e.printStackTrace();
                    Log.i("Sign", "Failed");
                }
            }
        });
    }
}
