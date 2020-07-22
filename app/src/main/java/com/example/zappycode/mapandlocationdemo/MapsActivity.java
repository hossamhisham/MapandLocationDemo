package com.example.zappycode.mapandlocationdemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.api.Result;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.io.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Writer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    LocationManager locationManager;
    LocationListener locationListener;
    String json;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mMap.clear();
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
                Log.i("location", userLocation.toString());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));

                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                String address = "Could not find address";
                try {

                    List<Address> listAddresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                    if (listAddresses != null && listAddresses.size() > 0 ) {

                        Log.i("PlaceInfo", listAddresses.get(0).toString());

                        address = "Address: \n";

                        if (listAddresses.get(0).getSubThoroughfare() != null) {

                            address += listAddresses.get(0).getSubThoroughfare() + " ";

                        }

                        if (listAddresses.get(0).getThoroughfare() != null) {

                            address += listAddresses.get(0).getThoroughfare() + "\n";

                        }

                        if (listAddresses.get(0).getLocality() != null) {

                            address += listAddresses.get(0).getLocality() + "\n";

                        }

                        if (listAddresses.get(0).getPostalCode() != null) {

                            address += listAddresses.get(0).getPostalCode() + "\n";

                        }

                        if (listAddresses.get(0).getCountryName() != null) {

                            address += listAddresses.get(0).getCountryName() + "\n";
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Gson gson = new Gson();
                UserLocation locationUpdate = new UserLocation(address, latitude, longitude);
                json =gson.toJson(locationUpdate);
                
                new SendDataToServer().execute(String.valueOf(json));


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
        };

        if (Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                mMap.clear();
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                LatLng userLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));

            }
        }


    }

    private String readStream(InputStream is) {
        try {
          ByteArrayOutputStream bo = new ByteArrayOutputStream();
          int i = is.read();
          while(i != -1) {
            bo.write(i);
            i = is.read();
          }
          return bo.toString();
        } catch (IOException e) {
          return "";
        }
    }

    class SendDataToServer extends AsyncTask <String,Void,Void>{
        @Override
        public Void doInBackground(String... json) {
            URL postURL = null;
            HttpURLConnection urlConnection = null;
            try {
                postURL = new URL("https://whispering-chamber-23002.herokuapp.com/create");

                urlConnection = (HttpURLConnection) postURL.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");

                // Writes json characters not bytes
                OutputStream os = urlConnection.getOutputStream() ;
                Writer writer = new BufferedWriter(new OutputStreamWriter(os));
                writer.write(json[0]);
                writer.flush();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                // Should output success
                String response = readStream(in);
            }catch (IOException e){
                e.printStackTrace();

            } finally {
                if ( urlConnection != null)
                urlConnection.disconnect();

                return null;
            }
        }
        
    }
//    class GetDataFromServer extends AsyncTask <String,Void,Void>{
//        @Override
//        private void doInBackground() {
//            URL url = new URL("https://whispering-chamber-23002.herokuapp.com/search/users");
//            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
//            try {
//                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
//                String JsonResponse = readStream(in);
//            } finally {
//                urlConnection.disconnect();
//            }
//
//        }
//    }
}

