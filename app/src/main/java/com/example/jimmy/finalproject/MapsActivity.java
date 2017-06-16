package com.example.jimmy.finalproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.ActionCodeResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private LocationManager locMgr;
    float zoom;
    public static String bestProv,jsonString;
    public static Double Lat, Lng;
    public static LatLng mark;

    @Override
    public void onLocationChanged(Location location) {
        Lat = location.getLatitude();
        Lng = location.getLongitude();
        String x = "緯" + Double.toString(location.getLatitude());
        String y = "經" + Double.toString(location.getLongitude());
        LatLng Point = new LatLng(location.getLatitude(), location.getLongitude());
        zoom = 17;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(Point, zoom));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        //Toast.makeText(this,x + "\n" + y,Toast.LENGTH_LONG).show();

        Thread thread = new Thread() {
            public void run() {
                try {
                    String url = String.format("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="+Lat+","+Lng+"&radius=200&types=food&language=zh-TW&key=AIzaSyAfF30nT2O6YQbFIfo-MC-CsMn6ZyXfAsY");
                    jsonString = getJSON(url, 10000);
                    //addMark(jsonString);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        try {
            thread.join();
            try {
                addMark(jsonString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        locMgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        bestProv = locMgr.getBestProvider(criteria,true);

        if(locMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)||locMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locMgr.requestLocationUpdates(bestProv,1000,1,this);
            }
        }else{
            Toast.makeText(this,"請開啟定位服務",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            locMgr.removeUpdates(this);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras){
        Criteria criteria = new Criteria();
        bestProv = locMgr.getBestProvider(criteria,true);
    }

    @Override
    public void onProviderEnabled(String provider){
    }

    @Override
    public void onProviderDisabled(String provider){
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //mMap.addMarker(new MarkerOptions().position(mark).title(json3));
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
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        requestPermission();
        mMap.setOnMarkerClickListener(gmapListener);
    }

    private GoogleMap.OnMarkerClickListener gmapListener = new GoogleMap.OnMarkerClickListener(){
        @Override
        public boolean onMarkerClick(Marker marker){
            marker.showInfoWindow();
            Intent intent = new Intent();
            intent.setClass(MapsActivity.this, ResultActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("name", marker.getTitle().toString());
            bundle.putInt("count", 0);
            intent.putExtras(bundle);
            startActivity(intent);
            //Toast.makeText(getApplication(),marker.getTitle(),Toast.LENGTH_LONG).show();
            return true;
        }
    };

    private void requestPermission(){
        if(Build.VERSION.SDK_INT >= 23){
            int hasPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if(hasPermission != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                return;
            }
        }
        setMyLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        if(requestCode == 1){
            if(grantResults[0] ==PackageManager.PERMISSION_GRANTED){
                setMyLocation();
            }else{
                Toast.makeText(this,"未取得授權",Toast.LENGTH_SHORT).show();
                finish();
            }
        }else{
            super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        }
    }

    private void setMyLocation() throws SecurityException{
        mMap.setMyLocationEnabled(true);
    }

    public String getJSON(String url, int timeout) {
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    return sb.toString();
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }

    public void addMark(String jsonString) throws JSONException {
        for(int i=0;i<10;i++){
            JSONArray jarr = new JSONArray(new JSONObject(jsonString).getString("results"));
            Double store_lat = Double.valueOf(new JSONObject(jarr.getJSONObject(i).toString()).getJSONObject("geometry").getJSONObject("location").getString("lat"));
            Double store_lng = Double.valueOf(new JSONObject(jarr.getJSONObject(i).toString()).getJSONObject("geometry").getJSONObject("location").getString("lng"));
            String store_name = new JSONArray(new JSONObject(jsonString).getString("results")).getJSONObject(i).getString("name");
            mark = new LatLng(store_lat,store_lng);
            System.out.println(store_name);
            mMap.addMarker(new MarkerOptions().position(mark).title(store_name));
        }
    }
}

