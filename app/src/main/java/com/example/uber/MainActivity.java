package com.example.uber;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.PrintWriterPrinter;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    CheckBox driver;
    CheckBox rider;
    int riderOrDriver=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        driver=(CheckBox)findViewById(R.id.driver);
        rider=(CheckBox)findViewById(R.id.rider);

        if(ParseUser.getCurrentUser()==null)
        {
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    Toast.makeText(MainActivity.this, "Anonymous Login Success", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else
        {
            if(ParseUser.getCurrentUser().getString("riderOrDriver")!=null)
            {
                if(ParseUser.getCurrentUser().getString("riderOrDriver").matches("rider"))
                {
                    Toast.makeText(this, "Welcome rider", Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(getApplicationContext(),RiderMap.class);
                    startActivity(intent);
                }
                else if(ParseUser.getCurrentUser().getString("riderOrDriver").matches("driver"))
                {
                    Toast.makeText(this, "Welcome driver", Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(getApplicationContext(),DriverMap.class);
                    startActivity(intent);
                }
            }
        }
        rider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rider.setChecked(true);
                driver.setChecked(false);
                riderOrDriver=0;
            }
        });
        driver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rider.setChecked(false);
                driver.setChecked(true);
                riderOrDriver=1;
            }
        });
    }

    public void next(View view)
    {
        if(riderOrDriver==0)
        {
            ParseUser.getCurrentUser().put("riderOrDriver","rider");
            ParseUser.getCurrentUser().saveInBackground();
            Intent intent=new Intent(getApplicationContext(),RiderMap.class);
            startActivity(intent);
        }
        else
        {
            ParseUser.getCurrentUser().put("riderOrDriver","driver");
            ParseUser.getCurrentUser().saveInBackground();
            Intent intent=new Intent(getApplicationContext(),DriverMap.class);
            startActivity(intent);
        }
    }
}
