package com.zenbarrier.uberclone;

import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;
import java.util.Locale;

public class RiderMapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    LocationManager locationManager;
    String provider;
    Marker riderMarker;
    Marker driverMarker;
    boolean isRequesting = false;
    Button buttonRiderRequest;
    TextView feedback;
    ParseObject request;
    ParseUser driver;
    Handler handler = new Handler();
    Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buttonRiderRequest = (Button) findViewById(R.id.buttonRiderRequest);
        feedback = (TextView) findViewById(R.id.textRiderFeedback);

        runnable = new Runnable() {
            @Override
            public void run() {
                driverUpdates();
                handler.postDelayed(this, 2000);
            }
        };

        checkActiveRequests();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.removeUpdates(this);
    }

    public void requestUber(View view) {
        isRequesting = !isRequesting;

        if (isRequesting) {
            feedback.setText(R.string.rider_activity_feedback_finding);
            feedback.animate().cancel();
            feedback.setAlpha(1f);
            buttonRiderRequest.setText(R.string.rider_activity_button_cancel);
            request = new ParseObject("Requests");
            request.put("riderId", ParseUser.getCurrentUser().getObjectId());
            Location location = getUserLocation();
            ParseGeoPoint geoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            request.put("riderMarker", geoPoint);
            ParseACL acl = new ParseACL();
            acl.setPublicReadAccess(true);
            acl.setPublicWriteAccess(true);
            request.setACL(acl);
            request.saveInBackground();
            handler.postDelayed(runnable, 2000);
        } else {
            handler.removeCallbacks(runnable);
            feedback.setText(R.string.rider_activity_feedback_canceled);
            feedback.animate().alpha(0f).setStartDelay(2000).setDuration(2000);
            buttonRiderRequest.setText(R.string.rider_activity_button_request);
            ParseQuery<ParseObject> query = new ParseQuery<>("Requests");
            query.whereEqualTo("riderId", ParseUser.getCurrentUser().getObjectId());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        try {
                            ParseObject.deleteAll(objects);
                        } catch (ParseException e1) {
                            e1.printStackTrace();
                        }
                    } else {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    void checkActiveRequests(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
        query.whereEqualTo("riderId", ParseUser.getCurrentUser().getObjectId());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e==null){
                    if(objects.size()>0) {
                        request = objects.get(0);
                        isRequesting = true;
                        buttonRiderRequest.setText(R.string.rider_activity_button_cancel);
                        feedback.setText(R.string.rider_activity_feedback_finding);
                        handler.postDelayed(runnable, 2000);
                        objects.remove(0);
                        try {
                            ParseObject.deleteAll(objects);
                        } catch (ParseException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    void driverUpdates(){
        if(driver == null){
            request.fetchInBackground(new GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject object, ParseException e) {
                    if(e==null){
                        final String driverId = request.getString("driverId");
                        if(driverId==null || driverId.length()<=0){
                            Log.i("Driver","none");
                        }
                        else{
                            Log.i("Driver","found");
                            handler.removeCallbacks(runnable);
                            ParseQuery<ParseUser> queryDriver = ParseUser.getQuery();
                            queryDriver.whereEqualTo("objectId", driverId);
                            queryDriver.findInBackground(new FindCallback<ParseUser>() {
                                @Override
                                public void done(List<ParseUser> objects, ParseException e) {
                                    if(e==null){
                                        if(objects.size()>0) {
                                            driver = objects.get(0);
                                            buttonRiderRequest.setVisibility(View.INVISIBLE);
                                            feedback.setText(R.string.rider_activity_feedback_on_the_way);
                                            Log.i("Driver", driver.getObjectId());
                                        }
                                        else{
                                            request.remove("driverId");
                                            request.saveInBackground();
                                        }
                                        handler.postDelayed(runnable, 2000);
                                    }
                                    else{
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
        else{
            try {
                driver.fetch();
                ParseGeoPoint geoPoint = driver.getParseGeoPoint("location");
                double distance = geoPoint.distanceInMilesTo(request.getParseGeoPoint("riderLocation"));
                feedback.setText(String.format(Locale.ENGLISH,"Your driver is %.2f miles away", distance));
                LatLng driverLocation = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

                if(driverMarker != null){
                    driverMarker.remove();
                }

                driverMarker = mMap.addMarker(new MarkerOptions().position(driverLocation).title("You")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(driverMarker.getPosition());
                builder.include(riderMarker.getPosition());
                LatLngBounds bound = builder.build();
                int padding = 100;

                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bound, padding));

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    Location getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        return locationManager.getLastKnownLocation(provider);
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
        onLocationChanged(getUserLocation());
    }

    @Override
    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        LatLng position = new LatLng(lat, lng);
        if(riderMarker != null){
            riderMarker.remove();
        }



        riderMarker = mMap.addMarker(new MarkerOptions().position(position).title("Your Location"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
