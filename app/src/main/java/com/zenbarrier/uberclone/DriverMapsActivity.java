package com.zenbarrier.uberclone;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

public class DriverMapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    ParseObject request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    public void acceptRequest(View view){
        try {
            request.fetch();
            String driverId = request.getString("driverId");
            if(driverId == null || driverId.length()<=0){
                request.put("driverId", ParseUser.getCurrentUser().getObjectId());
                request.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e==null){
                            //todo Launch maps directions
                        }
                    }
                });
            }
            else{
                Toast.makeText(this, "Not available anymore", Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Intent i = getIntent();
        final LatLng driverLocation = new LatLng(i.getDoubleExtra("driverLat", 0), i.getDoubleExtra("driverLng", 0));
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
        query.whereEqualTo("objectId", i.getStringExtra("requestId"));

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e==null && objects.size() > 0){
                    request = objects.get(0);
                    ParseGeoPoint requestGeo = request.getParseGeoPoint("riderLocation");
                    LatLng requestLatLng = new LatLng(requestGeo.getLatitude(), requestGeo.getLongitude());
                    Marker driverMarker = mMap.addMarker(new MarkerOptions().position(driverLocation).title("Driver"));
                    Marker requestMarker = mMap.addMarker(new MarkerOptions().position(requestLatLng).title("Rider"));
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(driverMarker.getPosition());
                    builder.include(requestMarker.getPosition());
                    LatLngBounds bound = builder.build();
                    int padding = 50;

                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bound, padding));
                }
            }
        });
    }
}
