package qiblaarrow.blackfalcon.jan;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.GeomagneticField;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Full-screen black Qibla arrow with Refresh Location button. */
public class MainActivity extends Activity implements HeadingProvider.Listener {

    private static final int REQ_LOCATION = 1;

    private ArrowView arrowView;
    private HeadingProvider provider;
    private LocationManager locationManager;

    private boolean hasLoc;
    private double qiblaBearing;   // from true north
    private float declination;     // magnetic -> true north

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            saveLocation(location);
            locationManager.removeUpdates(this); // one fresh fix is enough
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        locationManager = getSystemService(LocationManager.class);

        // 1. Base ArrowView
        arrowView = new ArrowView(this);
        arrowView.setOnClickListener(v -> {
            if (!hasLoc) requestLocation();
        });

        // 2. Refresh Location Button (Pill Style)
        TextView refreshBtn = new TextView(this);
        refreshBtn.setText("↻ Refresh Location");
        refreshBtn.setTextColor(Color.parseColor("#2ECC71"));
        refreshBtn.setTextSize(14);
        refreshBtn.setTypeface(Typeface.DEFAULT_BOLD);
        refreshBtn.setPadding(dp(16), dp(10), dp(16), dp(10));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#1A0D1F14")); // Subtle dark background
        btnBg.setStroke(dp(1), Color.parseColor("#2ECC71")); // Green Border
        btnBg.setCornerRadius(dp(20));
        refreshBtn.setBackground(btnBg);
        refreshBtn.setClickable(true);

        refreshBtn.setOnClickListener(v -> {
            requestLocation();
            Toast.makeText(this, "Refreshing Location...", Toast.LENGTH_SHORT).show();
        });

        // 3. Layout params for Top-Right placement
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.gravity = Gravity.TOP | Gravity.END;
        btnParams.setMargins(0, dp(36), dp(16), 0); // Margin from top & right

        // 4. Combine in FrameLayout
        FrameLayout mainLayout = new FrameLayout(this);
        mainLayout.addView(arrowView);
        mainLayout.addView(refreshBtn, btnParams);

        setContentView(mainLayout);

        provider = new HeadingProvider(this, this, 0.15);
        loadSavedLocation();
        showState(0f, 0f);

        if (!HeadingProvider.isSupported(this)) {
            arrowView.setState(false, 0, 0, 0, "This device has no compass sensor");
        } else {
            requestLocation();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void requestLocation() {
        if (hasLocationPermission()) {
            refreshLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            refreshLocation();
            showState(0f, 0f);
        }
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void loadSavedLocation() {
        SharedPreferences sp = getSharedPreferences(Qibla.PREFS, MODE_PRIVATE);
        if (sp.getBoolean(Qibla.KEY_HAS_LOC, false)) {
            applyLocation(sp.getFloat(Qibla.KEY_LAT, 0f), sp.getFloat(Qibla.KEY_LON, 0f));
        }
    }

    private void applyLocation(double lat, double lon) {
        hasLoc = true;
        qiblaBearing = Qibla.bearing(lat, lon);
        declination = new GeomagneticField((float) lat, (float) lon, 0f,
                System.currentTimeMillis()).getDeclination();
    }

    @SuppressWarnings({"deprecation", "MissingPermission"})
    private void refreshLocation() {
        if (locationManager == null || !hasLocationPermission()) return;
        boolean fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        Location best = null;
        String[] providers = {LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER};
        for (String p : providers) {
            try {
                Location l = locationManager.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (Exception ignored) {
            }
        }
        if (best != null) saveLocation(best);

        // ask for one fresh fix as well
        try {
            String provider = null;
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                provider = LocationManager.NETWORK_PROVIDER;
            } else if (fine && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                provider = LocationManager.GPS_PROVIDER;
            }
            if (provider != null) {
                locationManager.removeUpdates(locationListener);
                locationManager.requestLocationUpdates(provider, 0L, 0f, locationListener);
            }
        } catch (Exception ignored) {
        }
    }

    private void saveLocation(Location l) {
        getSharedPreferences(Qibla.PREFS, MODE_PRIVATE).edit()
                .putBoolean(Qibla.KEY_HAS_LOC, true)
                .putFloat(Qibla.KEY_LAT, (float) l.getLatitude())
                .putFloat(Qibla.KEY_LON, (float) l.getLongitude())
                .apply();
        applyLocation(l.getLatitude(), l.getLongitude());
    }

    private void showState(float trueHeading, float rel) {
        if (hasLoc) {
            arrowView.setState(true, qiblaBearing, rel, trueHeading, "");
        } else {
            arrowView.setState(false, 0, 0, 0,
                    "Allow location to find the Qibla\n(tap the screen or refresh button to try again)");
        }
    }

    @Override
    public void onHeading(float magneticDegrees) {
        float trueHeading = (magneticDegrees + declination + 360f) % 360f;
        float rel = hasLoc ? Qibla.relative(qiblaBearing, trueHeading) : 0f;
        showState(trueHeading, rel);
    }

    @Override
    protected void onResume() {
        super.onResume();
        provider.start(SensorManager.SENSOR_DELAY_GAME);
        if (hasLocationPermission()) refreshLocation(); // keep your position up to date
    }

    @Override
    protected void onPause() {
        super.onPause();
        provider.stop();
        if (locationManager != null) locationManager.removeUpdates(locationListener);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
