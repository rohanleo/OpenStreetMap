package com.example.map_map;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import static java.security.AccessController.getContext;

public class MapActivity extends AppCompatActivity implements MapEventsReceiver {

    private MapView mMap;

    private MapEventsOverlay mapEventsOverlay;
    private KmlDocument kmlDocument;
    private List<GeoPoint> geoPoints;
    private Polygon polygon;
    private Button saveButton, resetButton;


    private MapController mapController;
    LocationManager locationManager;
    Marker marker = null;

    ArrayList<OverlayItem> overlayItemArray;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_map);
        initMap();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, myLocationListener);
        mMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(myLocationListener);
        mMap.onPause();
    }

    private LocationListener myLocationListener = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location)
        {
            updateLoc(location);
        }

        @Override
        public void onProviderDisabled(String provider)
        {
        }

        @Override
        public void onProviderEnabled(String provider)
        {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initMap() {
        mMap = findViewById(R.id.map);
        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mMap.setMultiTouchControls(true);
        mMap.setClickable(true);

        mapController = (MapController) mMap.getController();
        mapController.setZoom(18.0f);
        overlayItemArray = new ArrayList<>();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastLocation != null) {
            updateLoc(lastLocation);
        }
        //Add Scale Bar
        ScaleBarOverlay myScaleBarOverlay = new ScaleBarOverlay(mMap);
        myScaleBarOverlay.setAlignRight(true);
        mMap.getOverlays().add(myScaleBarOverlay);

        //Add Compass
        CompassOverlay mCompassOverlay = new CompassOverlay(this, mMap);
        mCompassOverlay.enableCompass();
        mMap.getOverlays().add(mCompassOverlay);

        mapEventsOverlay = new MapEventsOverlay(this, this);
        mMap.getOverlays().add(0, mapEventsOverlay);

        //Rotation Gesture
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(this, mMap);
        mRotationGestureOverlay.setEnabled(true);
        mMap.setMultiTouchControls(true);
        mMap.getOverlays().add(mRotationGestureOverlay);

        //Add polygon
        geoPoints = new ArrayList<>();
        polygon = new Polygon();
        polygon.setFillColor(Color.argb(75, 255,0,0));
    }



    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        geoPoints.add(p);
        polygon.setPoints(geoPoints);
        Marker mMarker = new Marker(mMap);
        mMarker.setPosition(p);
        //marker.setIcon(getResources().getDrawable(R.mipmap.marker_red_round));
        mMarker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM);
        mMarker.setTitle(p.getLatitude() + "," + p.getLongitude());
        mMap.getOverlays().add(mMarker);
        mMap.getOverlayManager().add(polygon);
        mMap.invalidate();
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    private void updateLoc(Location loc)
    {
        GeoPoint locGeoPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        if(marker==null){
            mapController.setCenter(locGeoPoint);
            mapController.animateTo(locGeoPoint);
            marker = new Marker(mMap);
            marker.setPosition(locGeoPoint);
        }
        else{
            marker.setPosition(locGeoPoint);
        }
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_my_location));
        marker.setTitle("My Location" + "\n" + locGeoPoint.getLatitude() + "," + locGeoPoint.getLongitude());
        mMap.getOverlays().add(marker);

        setOverlayLoc(loc);

        mMap.invalidate();
    }
    private void setOverlayLoc(Location overlayloc)
    {
        GeoPoint overlocGeoPoint = new GeoPoint(overlayloc);
        //---
        overlayItemArray.clear();

        OverlayItem newMyLocationItem = new OverlayItem("My Location", "My Location", overlocGeoPoint);
        overlayItemArray.add(newMyLocationItem);
        //---
    }
}
