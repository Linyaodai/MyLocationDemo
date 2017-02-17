package com.example.mylocationdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity
        implements GoogleMap.OnMyLocationButtonClickListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleMap.OnMarkerDragListener,
        GoogleApiClient.OnConnectionFailedListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private Location mLastLocation;
    private AddressResultReceiver mResultReceiver;
    /**
     * 用来判断用户在连接上Google Play services之前是否有请求地址的操作
     */
    private boolean mAddressRequested;
    /**
     * 地图上锚点
     */
    private Marker perth ;
    private LatLng lastLatLng,perthLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //接收FetchAddressIntentService返回的结果
        mResultReceiver = new AddressResultReceiver(new Handler());
        //创建GoogleAPIClient实例
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        //取得SupportMapFragment,并在地图准备好后调用onMapReady
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMarkerDragListener(this);
        enableMyLocation();

    }

    /**
     * 检查是否已经连接到 Google Play services
     */
    private void checkIsGooglePlayConn() {
        Log.i("MapsActivity", "checkIsGooglePlayConn-->" +mGoogleApiClient.isConnected());
        if (mGoogleApiClient.isConnected() && mLastLocation != null) {
            startIntentService(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
        }
        mAddressRequested = true;
    }

    /**
     * 如果取得了权限,显示地图定位层
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }


    /**
     * '我的位置'按钮点击时的调用
     * @return
     */
    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        if (mLastLocation != null) {
            Log.i("MapsActivity", "Latitude-->" + String.valueOf(mLastLocation.getLatitude()));
            Log.i("MapsActivity", "Longitude-->" + String.valueOf(mLastLocation.getLongitude()));
        }
        if (lastLatLng != null)
            perth.setPosition(lastLatLng);
        checkIsGooglePlayConn();
        return false;
    }


    /**
     * 启动地址搜索Service
     */
    protected void startIntentService(LatLng latLng) {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(FetchAddressIntentService.RECEIVER, mResultReceiver);
        intent.putExtra(FetchAddressIntentService.LATLNG_DATA_EXTRA, latLng);
        startService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            Toast.makeText(getApplicationContext(),"Permission to access the location is missing.",Toast.LENGTH_LONG).show();
            return;
        }
        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("MapsActivity", "--onConnected--" );
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(),"Permission to access the location is missing.",Toast.LENGTH_LONG).show();
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            lastLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            displayPerth(true,lastLatLng);
            initCamera(lastLatLng);
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, "No geocoder available",Toast.LENGTH_LONG).show();
                return;
            }
            if (mAddressRequested) {
                startIntentService(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
            }
        }
    }

    /**
     * 添加标记
     */
    private void displayPerth(boolean isDraggable,LatLng latLng) {
        if (perth==null){
            perth = mMap.addMarker(new MarkerOptions().position(latLng).title("Your Position"));
            perth.setDraggable(isDraggable); //设置可移动
        }

    }


    /**
     * 将地图视角切换到定位的位置
     */
    private void initCamera(final LatLng sydney) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney,14));
                    }
                });
            }
        }).start();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
    }

    @Override
    public void onMarkerDrag(Marker marker) {
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        perthLatLng = marker.getPosition() ;
        startIntentService(perthLatLng);
    }


    class AddressResultReceiver extends ResultReceiver {
        private String mAddressOutput;

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            mAddressOutput = resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY);
            if (resultCode == FetchAddressIntentService.SUCCESS_RESULT) {
                Log.i("MapsActivity", "mAddressOutput-->" + mAddressOutput);
                new AlertDialog.Builder(MapsActivity.this)
                        .setTitle("Position")
                        .setMessage(mAddressOutput)
                        .create()
                        .show();
            }

        }
    }

}
