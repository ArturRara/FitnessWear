package com.example.fitnesswear;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.wear.widget.SwipeDismissFrameLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static android.graphics.Color.YELLOW;

public class MainActivity extends WearableActivity implements OnMapReadyCallback {

    private TextView mTextView;

    private static final String TAG = "WearableActivity";


    private static final long UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);
    private static final long FASTEST_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);

    private static final float MPH_IN_METERS_PER_SECOND = 2.23694f;

    private static final int SPEED_LIMIT_DEFAULT_MPH = 45;

    private static final long INDICATOR_DOT_FADE_AWAY_MS = 500L;

    // Request codes for changing speed limit and location permissions.
    private static final int REQUEST_PICK_SPEED_LIMIT = 0;

    // Id to identify Location permission request.
    private static final int REQUEST_GPS_PERMISSION = 1;

    // Shared Preferences for saving speed limit and location permission between app launches.
    private static final String PREFS_SPEED_LIMIT_KEY = "SpeedLimit";

    private Calendar mCalendar;

    private TextView mSpeedLimitTextView;
    private TextView mSpeedTextView;
    private ImageView mGpsPermissionImageView;
    private TextView mCurrentSpeedMphTextView;
    private TextView mGpsIssueTextView;
    private View mBlinkingGpsStatusDotView;

    private String mGpsPermissionNeededMessage;
    private String mAcquiringGpsMessage;

    private int mSpeedLimit;
    private float mSpeed;

    private boolean mGpsPermissionApproved;

    private boolean mWaitingForGpsSignal;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private GoogleMap mMap;
    private Handler mHandler = new Handler();

    private enum SpeedState {
        BELOW(R.color.green), CLOSE(R.color.yellow), ABOVE(R.color.red);
        private int mColor;
        SpeedState(int color) {
            mColor = color;
        }
        int getColor() {
            return mColor;
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate()");


        mTextView = (TextView) findViewById(R.id.text);


        setAmbientEnabled();


        mCalendar = Calendar.getInstance();

        mGpsPermissionApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        mGpsPermissionNeededMessage = getString(R.string.permission_rationale);
        mAcquiringGpsMessage = getString(R.string.acquiring_gps);


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSpeedLimit = sharedPreferences.getInt(PREFS_SPEED_LIMIT_KEY, SPEED_LIMIT_DEFAULT_MPH);

        mSpeed = 0;

        mWaitingForGpsSignal = true;


        /*
         * If this hardware doesn't support GPS, we warn the user. Note that when such device is
         * connected to a phone with GPS capabilities, the framework automatically routes the
         * location requests from the phone. However, if the phone becomes disconnected and the
         * wearable doesn't support GPS, no location is recorded until the phone is reconnected.
         */
        if (!hasGps()) {
            Log.w(TAG, "This hardware doesn't have GPS, so we warn user.");
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.gps_not_available))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            dialog.cancel();
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        }


        setupViews();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }


                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "onLocationChanged() : " + location);


                    if (mWaitingForGpsSignal) {
                        mWaitingForGpsSignal = false;
                        updateActivityViewsBasedOnLocationPermissions();
                    }

                    mSpeed = location.getSpeed() * MPH_IN_METERS_PER_SECOND;
                    updateSpeedInViews();
                    addLocationEntry(location.getLatitude(), location.getLongitude());
                }
            };
        };


        final SwipeDismissFrameLayout swipeDismissRootFrameLayout =
                (SwipeDismissFrameLayout) findViewById(R.id.swipe_dismiss_root_container);
        final FrameLayout mapFrameLayout = (FrameLayout) findViewById(R.id.map_container);

        swipeDismissRootFrameLayout.addCallback(new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                layout.setVisibility(View.GONE);
                finish();
            }
        });

        swipeDismissRootFrameLayout.setOnApplyWindowInsetsListener(
                new View.OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                        insets = swipeDismissRootFrameLayout.onApplyWindowInsets(insets);

                        RelativeLayout.LayoutParams params =
                                (RelativeLayout.LayoutParams) mapFrameLayout.getLayoutParams();

                        params.setMargins(
                                insets.getSystemWindowInsetLeft(),
                                insets.getSystemWindowInsetTop(),
                                insets.getSystemWindowInsetRight(),
                                insets.getSystemWindowInsetBottom());
                        mapFrameLayout.setLayoutParams(params);

                        return insets;
                    }
                });

        MapFragment mapFragment =
                (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(getApplicationContext(), R.string.intro_text, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        LatLng warsaw = new LatLng(52, 21);
        mMap.addMarker(new MarkerOptions().position(warsaw).title("Marker in warsaw"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(warsaw));
    }



    @Override
    protected void onResume() {
        super.onResume();
        requestLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);

    }

    private void setupViews() {
        mSpeedLimitTextView = findViewById(R.id.max_speed_text);
        mSpeedTextView = findViewById(R.id.current_speed_text);
        mCurrentSpeedMphTextView = findViewById(R.id.current_speed_mph);

        mGpsPermissionImageView = findViewById(R.id.gps_permission);
        mGpsIssueTextView = findViewById(R.id.gps_issue_text);
        mBlinkingGpsStatusDotView = findViewById(R.id.dot);

        updateActivityViewsBasedOnLocationPermissions();
    }

    public void onSpeedLimitClick(View view) {
        Intent speedIntent = new Intent(MainActivity.this,
                SpeedPickerActivity.class);
        startActivityForResult(speedIntent, REQUEST_PICK_SPEED_LIMIT);
    }

    public void onHeartbeatServiceClick(View view){
        Intent heartIntent = new Intent(MainActivity.this,HeartbeatService.class);
        startActivityForResult(heartIntent,2);

    }

    public void onGpsPermissionClick(View view) {

        if (!mGpsPermissionApproved) {

            Log.i(TAG, "Location permission has NOT been granted. Requesting permission.");

            // On 23+ (M+) devices, GPS permission not granted. Request permission.
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_GPS_PERMISSION);
        }
    }

    private void updateActivityViewsBasedOnLocationPermissions() {
        if (mGpsPermissionApproved && mWaitingForGpsSignal) {

            // We are getting a GPS signal w/ user permission.
            mGpsIssueTextView.setText(mAcquiringGpsMessage);
            mGpsIssueTextView.setVisibility(View.VISIBLE);
            mGpsPermissionImageView.setImageResource(R.drawable.ic_gps_saving_grey600_96dp);

            mSpeedTextView.setVisibility(View.GONE);
            mSpeedLimitTextView.setVisibility(View.GONE);
            mCurrentSpeedMphTextView.setVisibility(View.GONE);

        } else if (mGpsPermissionApproved) {

            mGpsIssueTextView.setVisibility(View.GONE);

            mSpeedTextView.setVisibility(View.VISIBLE);
            mSpeedLimitTextView.setVisibility(View.VISIBLE);
            mCurrentSpeedMphTextView.setVisibility(View.VISIBLE);
            mGpsPermissionImageView.setImageResource(R.drawable.ic_gps_saving_grey600_96dp);

        } else {

            // User needs to enable location for the app to work.
            mGpsIssueTextView.setVisibility(View.VISIBLE);
            mGpsIssueTextView.setText(mGpsPermissionNeededMessage);
            mGpsPermissionImageView.setImageResource(R.drawable.ic_gps_not_saving_grey600_96dp);

            mSpeedTextView.setVisibility(View.GONE);
            mSpeedLimitTextView.setVisibility(View.GONE);
            mCurrentSpeedMphTextView.setVisibility(View.GONE);
        }
    }

    private void updateSpeedInViews() {

        if (mGpsPermissionApproved) {

            mSpeedLimitTextView.setText(getString(R.string.speed_limit, mSpeedLimit));
            mSpeedTextView.setText(String.format(getString(R.string.speed_format), mSpeed));
            SpeedState state = SpeedState.ABOVE;
            if (mSpeed <= mSpeedLimit - 5) {
                state = SpeedState.BELOW;
            } else if (mSpeed <= mSpeedLimit) {
                state = SpeedState.CLOSE;
            }

            mSpeedTextView.setTextColor(ResourcesCompat.getColor(getResources(),state.getColor(),null));
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBlinkingGpsStatusDotView.setVisibility(View.VISIBLE);
                }
            });
            mBlinkingGpsStatusDotView.setVisibility(View.VISIBLE);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBlinkingGpsStatusDotView.setVisibility(View.INVISIBLE);
                }
            }, INDICATOR_DOT_FADE_AWAY_MS);
        }
    }


    private void requestLocation() {
        Log.d(TAG, "requestLocation()");

        /*
         * mGpsPermissionApproved covers 23+ (M+) style permissions. If that is already approved or
         * the device is pre-23, the app uses mSaveGpsLocation to save the user's location
         * preference.
         */
        if (mGpsPermissionApproved) {

            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(UPDATE_INTERVAL_MS)
                    .setFastestInterval(FASTEST_INTERVAL_MS);


            try {
                fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest, locationCallback, Looper.getMainLooper());

            } catch (SecurityException unlikely) {
                Log.e(TAG, "Lost location permissions. Couldn't remove updates. " + unlikely);
            }
        }
    }

    private void addLocationEntry(double latitude, double longitude) {
        if (!mGpsPermissionApproved) {
            return;
        }
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        LocationEntry entry = new LocationEntry(mCalendar, latitude, longitude);
        String path = "/location" + "/" + mCalendar.getTimeInMillis();
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
        putDataMapRequest.getDataMap().putDouble("lat", entry.latitude);
        putDataMapRequest.getDataMap().putDouble("lng", entry.longitude);
        putDataMapRequest.getDataMap()
                .putLong("time", entry.calendar.getTimeInMillis());
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        request.setUrgent();

        Task<DataItem> dataItemTask =
                Wearable.getDataClient(getApplicationContext()).putDataItem(request);

        dataItemTask.addOnSuccessListener(dataItem -> {
            Log.d(TAG, "Data successfully sent: " + dataItem.toString());
        });
        dataItemTask.addOnFailureListener(exception -> {
            Log.e(TAG, "AddPoint:onClick(): Failed to set the data, "
                    + "exception: " + exception);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_PICK_SPEED_LIMIT) {
            if (resultCode == RESULT_OK) {
                int newSpeedLimit =
                        data.getIntExtra(SpeedPickerActivity.EXTRA_NEW_SPEED_LIMIT, mSpeedLimit);

                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(MainActivity.PREFS_SPEED_LIMIT_KEY, newSpeedLimit);
                editor.apply();

                mSpeedLimit = newSpeedLimit;

                updateSpeedInViews();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult(): " + permissions);


        if (requestCode == REQUEST_GPS_PERMISSION) {
            Log.i(TAG, "Received response for GPS permission request.");

            if ((grantResults.length == 1)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.i(TAG, "GPS permission granted.");
                mGpsPermissionApproved = true;

                requestLocation();

            } else {
                Log.i(TAG, "GPS permission NOT granted.");
                mGpsPermissionApproved = false;
            }

            updateActivityViewsBasedOnLocationPermissions();
        }
    }

    private boolean hasGps() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

}
