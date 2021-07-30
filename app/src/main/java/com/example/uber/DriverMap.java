package com.example.uber;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
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
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DriverMap extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    Location currentLocation;
    ArrayList<LatLng>requestLocs=new ArrayList<>();
    ArrayList<String>destinations=new ArrayList<>();
    ArrayList<String>usernames=new ArrayList<>();


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
                    mMap.addMarker(new MarkerOptions().position(lastKnown).title("Driver"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(lastKnown));
                }
            }
        }

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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

    ArrayList<Marker>markers=new ArrayList<>();
    @Override public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(!marker.getTitle().matches("Driver")) {
                    LatLng markerPos=marker.getPosition();
                    for (int j = 0; j < requestLocs.size(); j++) {
                        if(requestLocs.get(j).latitude==markerPos.latitude && requestLocs.get(j).longitude==markerPos.longitude)
                        {
                            Intent intent=new Intent(getApplicationContext(),ShowRiderInfo.class);
                            intent.putExtra("destination",destinations.get(j));
                            intent.putExtra("username",usernames.get(j));
                            intent.putExtra("riderLatitude",requestLocs.get(j).latitude);
                            intent.putExtra("riderLongitude",requestLocs.get(j).longitude);
                            intent.putExtra("driverLatitude",currentLocation.getLatitude());
                            intent.putExtra("driverLongitude",currentLocation.getLongitude());
                            startActivity(intent);
                        }
                    }
                }
                return true;
            }
        });

        locationManager = (LocationManager) DriverMap.this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,12));
                mMap.addMarker(new MarkerOptions().position(userLocation).title("Driver"));
                currentLocation=location;
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
                LatLng lastKnown=new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                currentLocation=lastKnownLocation;
                if(lastKnownLocation!=null)
                {
                    mMap.clear();
                    Marker driverMarker=mMap.addMarker(new MarkerOptions().position(lastKnown).title("Driver"));
                    markers.add(driverMarker);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnown,13));
                }
            }
        }
        refresh();
    }

    public void refreshForView(View view)
    {
        refresh();
    }

    public void refresh()
    {
        ParseQuery<ParseObject>query= ParseQuery.getQuery("Requests");
        //query.whereNear("location",new ParseGeoPoint(currentLocation.getLatitude(),currentLocation.getLongitude()));
        query.setLimit(10);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                requestLocs.clear();
                destinations.clear();
                if(e==null)
                {
                    for(ParseObject object:objects)
                    {
                        if(object.getBoolean("AcceptedOrNot")==false)
                        {
                            LatLng requestLoc = new LatLng(object.getParseGeoPoint("location").getLatitude(), object.getParseGeoPoint("location").getLongitude());
                            Marker marker = mMap.addMarker(new MarkerOptions().position(requestLoc).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                            marker.setTitle(requestLoc.toString());
                            markers.add(marker);

                            LatLngBounds.Builder builder=new LatLngBounds.Builder();
                            for(Marker boundMarker:markers)
                            {
                                builder.include(boundMarker.getPosition());
                            }
                            LatLngBounds bounds=builder.build();
                            int padding=60;
                            CameraUpdate cu=CameraUpdateFactory.newLatLngBounds(bounds,padding);
                            mMap.animateCamera(cu);

                            requestLocs.add(requestLoc);
                            String destination = object.getString("destination");
                            destinations.add(destination);
                            String username = object.getString("username");
                            usernames.add(username);
                        }
                    }
                }
            }
        });
    }
}