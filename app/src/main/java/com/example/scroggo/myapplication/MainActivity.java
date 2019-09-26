package com.example.scroggo.myapplication;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {
    TextView mAddress, mRealAddress;
    private static final int REQUEST_CODE = 73;
    private LocationManager mLocationManager;
    private Location mBestLocation = null;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAddress = findViewById(R.id.address);
        mRealAddress = findViewById(R.id.realAddress);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            accessLocation();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE);
            } else {
                Toast.makeText(this, "Check permission settings", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE && permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                accessLocation();
            } else {
                Toast.makeText(this, "Did not get permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void accessLocation() {
        if (!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
        
        try {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    10000, 10, this);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000, 10, this);
        } catch (SecurityException e) {
            Toast.makeText(this, "Could not get updates", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClick(View view) {
        if (view == mAddress && mBestLocation != null) {
            Uri uri = Uri.parse("geo:" + mBestLocation.getLatitude()
            + "," + mBestLocation.getLongitude());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "No activity to view", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(intent);
            }
        } else if (view == mRealAddress && mBestLocation != null) {
            final String string = "com.example.scroggo.myapplication.PROXIMITY";
            Intent intent = new Intent(string);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                    intent, 0);
            registerReceiver(new ProximityAlertReceiver(), new IntentFilter(string));
            try {
                mLocationManager.addProximityAlert(mBestLocation.getLatitude(),
                        mBestLocation.getLongitude(), 1, -1, pendingIntent);
            } catch (SecurityException e) {
                Toast.makeText(this, "Give me back permissions!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (mBestLocation != null) {
                if (location.getElapsedRealtimeNanos() > mBestLocation.getElapsedRealtimeNanos() + 10000
                    || location.getAccuracy() > mBestLocation.getAccuracy()){
                    updateRealAddress(location);
                }
            } else {
                updateRealAddress(location);
            }
        }
    }

    private void updateRealAddress(Location location) {
        mBestLocation = location;
        updateAddress(location.getLatitude() + ", " + location.getLongitude());
        new GetAddress().execute(location);
    }

    private class GetAddress extends AsyncTask<Location, Void, Address> {
        @Override
        protected void onPostExecute(Address address) {
            if (address == null) {
                Toast.makeText(MainActivity.this, "Could not determine address", Toast.LENGTH_SHORT).show();
            } else {
                mRealAddress.setText(address.getAddressLine(0));
            }
        }

        @Override
        protected Address doInBackground(Location... locations) {
            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(locations[0].getLatitude(),
                        locations[0].getLongitude(), 1);
                if (addresses != null && addresses.size() >= 1) {
                    return addresses.get(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void updateAddress(String address) {
        mAddress.setText(address);
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
