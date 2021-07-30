package com.example.uber;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;


public class RiderMap extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    EditText destination;
    LocationManager locationManager;
    LocationListener locationListener;
    Button callUberButton;
    boolean requested;
    TextView infoText;
    LatLng userLocation;
    Button finish;

    Marker marker=null;

    int i=1;

    public void checkForUpdatesToRequest()
    {
        ParseQuery<ParseObject>query=ParseQuery.getQuery("Requests");
        query.whereEqualTo("username",ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e==null)
                {
                    for(final ParseObject object:objects)
                    {
                        if(object.getBoolean("AcceptedOrNot"))//Request has been accepted.
                        {
                            callUberButton.setVisibility(View.INVISIBLE);
                            destination.setVisibility(View.INVISIBLE);
                            ParseQuery<ParseUser>query1=ParseUser.getQuery();
                            query1.whereEqualTo("username",object.getString("driverUsername"));
                            query1.findInBackground(new FindCallback<ParseUser>() {
                                @Override
                                public void done(List<ParseUser> objects, ParseException e) {
                                    for(ParseUser user:objects)
                                    {
                                        LatLng driverLoc=new LatLng(user.getParseGeoPoint("currentDriverLocation").getLatitude(),
                                                user.getParseGeoPoint("currentDriverLocation").getLongitude());
                                        if(marker!=null)
                                        marker.remove();
                                        marker=mMap.addMarker(new MarkerOptions().position(driverLoc).title("Driver's location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                                        infoText.setText("Driver is on the way...");

                                        if(i==1) {
                                            new AlertDialog.Builder(RiderMap.this)
                                                    .setIcon(android.R.drawable.alert_light_frame)
                                                    .setTitle("Request accepted")
                                                    .setMessage("Your driver is on the way!")
                                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {

                                                        }
                                                    })
                                                    .show();
                                            i=2;
                                        }

                                        Location driverLocLocation=new Location(LocationManager.GPS_PROVIDER);
                                        driverLocLocation.setLongitude(driverLoc.longitude);
                                        driverLocLocation.setLatitude(driverLoc.latitude);
                                        Location userLocLocation=new Location(LocationManager.GPS_PROVIDER);
                                        userLocLocation.setLongitude(userLocation.longitude);
                                        userLocLocation.setLatitude(userLocation.latitude);

                                        Double distance=(double)userLocLocation.distanceTo(driverLocLocation);
                                        Double distanceTDkm=(double)Math.round(distance*10)/10;
                                        distanceTDkm/=1000;
                                        infoText.setText("Driver is currently "+distanceTDkm+" km away...");

                                        if(distance<10)
                                        {
                                            infoText.setText("Driver has arrived!");
                                            finish.setVisibility(View.VISIBLE);
                                            if(i==2)
                                            {
                                                new AlertDialog.Builder(RiderMap.this)
                                                        .setIcon(android.R.drawable.alert_light_frame)
                                                        .setTitle("Driver has arrived!")
                                                        .setMessage("Your driver has arrived")
                                                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                try {
                                                                    object.delete();
                                                                } catch (ParseException ex) {
                                                                    ex.printStackTrace();
                                                                }
                                                            }
                                                        })
                                                        .show();

                                                finish.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        infoText.setText("");
                                                        finish.setVisibility(View.INVISIBLE);
                                                        callUberButton.setVisibility(View.VISIBLE);
                                                        callUberButton.setText("Call Uber");
                                                        requested=false;
                                                        destination.setVisibility(View.VISIBLE);
                                                    }
                                                });
                                                i=1;
                                                handler.removeCallbacks(runnable);
                                            }
                                        }

                                    }
                                }
                            });
                        }
                    }
                }
            }
        });
    }
    Handler handler = new Handler();
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            checkForUpdatesToRequest();
            handler.postDelayed(this,1000);
        }
    };
    public void handlerFunction()
    {
        handler.postDelayed(runnable,1000);
    }


    public void requestUber(View view)
    {

        if(!requested&&destination.getText().toString().matches(""))
        {
            Toast.makeText(this, "Please enter your desired destination", Toast.LENGTH_SHORT).show();
        }
        else {
            if (!requested) {

                ParseObject request = new ParseObject("Requests");
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                ParseGeoPoint userLocationParse = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                request.put("location", userLocationParse);
                Log.i("destination",destination.getText().toString());
                request.put("destination", destination.getText().toString());
                request.put("username", ParseUser.getCurrentUser().getUsername());
                request.put("AcceptedOrNot",false);
                request.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            callUberButton.setText("Requested. Tap to cancel");
                            infoText.setText("Looking for Uber drivers nearby...");
                        }
                    }
                });
                callUberButton.setText("Requested. Tap to cancel");
                requested = true;
                handlerFunction();

            } else {
                ParseQuery<ParseObject>query=ParseQuery.getQuery("Requests");
                query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if(e==null)
                        {
                            for(ParseObject object:objects)
                            {
                                object.deleteInBackground();
                            }
                            callUberButton.setText("Request Uber");
                            Toast.makeText(RiderMap.this, "Request cancelled", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                requested = false;
                infoText.setText("");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rider_activity);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_rider);
        mapFragment.getMapAsync(this);
        callUberButton=(Button)findViewById(R.id.button);
        destination=(EditText)findViewById(R.id.destination);
        infoText=findViewById(R.id.infoText);
        finish=findViewById(R.id.finish);

        ParseQuery<ParseObject>query=ParseQuery.getQuery("Requests");
        query.whereEqualTo("username",ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e==null)
                {
                    if(objects.size()>0)
                    {
                        for(ParseObject object:objects)
                        {
                            handlerFunction();
                            if(!object.getBoolean("AcceptedOrNot"))
                            {
                                infoText.setText("Looking for Uber drivers nearby...");
                                callUberButton.setText("Requested. Tap to cancel");
                                destination.setText(object.getString("destination"));
                                requested = true;
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==1)
        {
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,locationListener);
                Location lastKnownLocation=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                LatLng lastKnown=new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                if(lastKnownLocation!=null)
                {
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(lastKnown).title("Last known location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(lastKnown));
                }
            }
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
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mMap.clear();
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,13));
                mMap.addMarker(new MarkerOptions().position(userLocation).title("Your location"));
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if(Build.VERSION.SDK_INT<23)
        {
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
        else
        {
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            }
            else
            {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                LatLng userLocation=new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                if(lastKnownLocation!=null)
                {
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(userLocation).title("Last known location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,13));
                }
            }
        }

    }
}
