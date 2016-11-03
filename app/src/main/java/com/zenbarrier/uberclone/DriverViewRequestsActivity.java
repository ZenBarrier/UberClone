package com.zenbarrier.uberclone;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DriverViewRequestsActivity extends AppCompatActivity implements LocationListener {
    ListView listViewRequests;
    String provider;
    LocationManager locationManager;
    ArrayList<ParseObject> listRequests;
    requestAdapter adapter;
    ParseGeoPoint driverLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_view_requests);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listRequests = new ArrayList<>();
        listViewRequests = (ListView) findViewById(R.id.listViewRequests);
        adapter = new requestAdapter(this, listRequests);
        listViewRequests.setAdapter(adapter);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);

        listViewRequests.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ParseObject request = listRequests.get(i);
                try {
                    request.fetch();
                    String riderId = request.getString("driverId");
                    if(riderId == null || riderId.length() <= 0){
                        Intent intent = new Intent(DriverViewRequestsActivity.this, DriverMapsActivity.class);
                        intent.putExtra("requestId", request.getObjectId());
                        startActivity(intent);
                    }
                    else{
                        queryRequests(getCurrentLocation());
                        Snackbar.make(findViewById(R.id.CoordinatorDriverRequests),
                                "Not available anymore",
                                Snackbar.LENGTH_SHORT).show();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                request.put("driverId", ParseUser.getCurrentUser().getObjectId());
            }
        });
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
        onLocationChanged(getCurrentLocation());
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

    Location getCurrentLocation() {
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

    @Override
    public void onLocationChanged(Location location) {
        queryRequests(location);
    }

    void queryRequests(Location location){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
        driverLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        query.whereWithinMiles("riderLocation", driverLocation, 35.0);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                listRequests.clear();
                for(ParseObject object:objects){
                    String driverId = object.getString("driverId");
                    if(driverId==null || driverId.length()<=0);{
                        listRequests.add(object);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    class requestAdapter extends ArrayAdapter<ParseObject>{

        requestAdapter(Context context, List<ParseObject> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ParseObject request = getItem(position);
            if(convertView == null){
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            TextView distanceText = (TextView) convertView.findViewById(android.R.id.text1);
            TextView idText = (TextView) convertView.findViewById(android.R.id.text2);
            if (request != null) {
                double distanceMiles = request.getParseGeoPoint("riderLocation").distanceInMilesTo(driverLocation);
                distanceText.setText(String.format(Locale.getDefault(),"%.2f miles", distanceMiles));
                idText.setText(request.getObjectId());
            }
            return convertView;
        }
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
