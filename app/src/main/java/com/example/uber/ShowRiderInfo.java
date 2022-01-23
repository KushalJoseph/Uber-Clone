package com.example.uber;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.w3c.dom.Text;

import java.sql.Driver;
import java.util.List;
import java.util.Locale;

public class ShowRiderInfo extends AppCompatActivity {

    TextView riderLocation;
    TextView destinationText;
    TextView distanceText;
    String username;
    Location riderLocLocation = new Location(LocationManager.GPS_PROVIDER);
    Location driverLoc = new Location(LocationManager.GPS_PROVIDER);

    public void acceptRequest(View view)
    {
        ParseQuery<ParseObject>query=ParseQuery.getQuery("Requests");
        query.whereEqualTo("username", username);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null && objects.size() > 0)
                {
                    for(ParseObject object:objects)
                    {
                        if(object.getBoolean("AcceptedOrNot") == true)
                        {
                            Toast.makeText(ShowRiderInfo.this, "Sorry, this request was already accepted by another driver.", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            object.put("driverUsername", ParseUser.getCurrentUser().getUsername());
                            object.put("AcceptedOrNot", true);
                            object.saveInBackground();
                            ParseUser.getCurrentUser().put("currentDriverLocation", new ParseGeoPoint(driverLoc.getLatitude(),driverLoc.getLongitude()));
                            ParseUser.getCurrentUser().saveInBackground();
                            Toast.makeText(ShowRiderInfo.this, "Request accepted. Opening navigation...", Toast.LENGTH_SHORT).show();
                            Intent directionsIntent = new Intent(android.content.Intent.ACTION_VIEW,
                                    Uri.parse("http://maps.google.com/maps?saddr="+driverLoc.getLatitude()+","+driverLoc.getLongitude()
                                            +"&daddr="+riderLocLocation.getLatitude()+","+riderLocLocation.getLongitude()));
                            startActivity(directionsIntent);
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_rider_info);

        riderLocation = findViewById(R.id.riderLoc);
        destinationText = findViewById(R.id.destination);
        distanceText = findViewById(R.id.distance);

        Intent intent = getIntent();

        username = intent.getStringExtra("username");
        String destination = intent.getStringExtra("destination");
        LatLng riderLoc = new LatLng(intent.getDoubleExtra("riderLatitude", 0), intent.getDoubleExtra("riderLongitude", 0));


        riderLocLocation.setLatitude(intent.getDoubleExtra("riderLatitude", 0));
        riderLocLocation.setLongitude(intent.getDoubleExtra("riderLongitude", 0));

        driverLoc.setLatitude(intent.getDoubleExtra("driverLatitude", 0));
        driverLoc.setLongitude(intent.getDoubleExtra("driverLongitude", 0));

        Double distance = (double) riderLocLocation.distanceTo(driverLoc);
        destinationText.setText("Desired destination: " + destination);
        distanceText.setText("Distance to Rider: " + Double.toString(maniDouble(distance)) + " km.");

        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try
        {
            List<Address>addresses = geocoder.getFromLocation(intent.getDoubleExtra("riderLatitude", 0),
                    intent.getDoubleExtra("riderLongitude", 0), 1);
            if(addresses != null && addresses.size() > 0)
            {
                if(addresses.get(0).getAddressLine(0) != null)
                {
                    riderLocation.setText(addresses.get(0).getAddressLine(0));
                }
            }

        }
        catch(Exception e)
        {
            riderLocation.setText("Could not find a proper address");
        }

    }

    public double maniDouble(double num)
    {
        num = Math.round(num * 10)/10;
        return num/1000;
    }

    public void cancel(View view)
    {
        Intent intent = new Intent(getApplicationContext(), DriverMap.class);
        startActivity(intent);
    }
}
