package com.example.imagia_app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import com.example.imagia_app.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.imagia_app.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String[] CAMERA_PERMISSION = new String[] { android.Manifest.permission.CAMERA };
    private static final int CAMERA_REQUEST_CODE = 10;
    private long lastTime = 0;
    private float lastX, lastY, lastZ;
    private static final int SHAKE_THRESHOLD = 600;
    private static final int TIME_THRESHOLD = 100;
    private boolean isDoubleTapped = false;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.example.imagia_app.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);

        // Pasem cada ID del menú com un conjunt d'IDs perquè cada menú ha de considerar-se una destinació de nivell superior
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_ullada, R.id.navigation_historial, R.id.navigation_compte)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        if (!hasCameraPermission()) {
            requestPermission();
        }
        SensorEventListener sensorLnr = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    long currentTime = System.currentTimeMillis();
                    if ((currentTime - lastTime) > TIME_THRESHOLD) {
                        long timeDiff = currentTime - lastTime;
                        lastTime = currentTime;

                        float x = sensorEvent.values[0];
                        float y = sensorEvent.values[1];
                        float z = sensorEvent.values[2];

                        float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / timeDiff * 10000;

                        if (speed > SHAKE_THRESHOLD) {
                            isDoubleTapped = !isDoubleTapped;
                            Toast.makeText(MainActivity.this, "Has fet un Double Tapp!", Toast.LENGTH_SHORT).show();
                        }
                        lastX = x;
                        lastY = y;
                        lastZ = z;
                    }
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                // Es pot ignorar aquesta CB de moment
            }
        };

        // Seleccionem el tipus de sensor
        SensorManager sensorMgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Registrem el Listener per capturar els events del sensor
        if (sensor != null) {
            sensorMgr.registerListener(sensorLnr, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Creem una nova instància pel Gesture Detector
        gestureDetector = new GestureDetector(this, new GestureListener());
    }
    // Retornem un booleà depenent si l'usuari ha donat permissos de càmara o no
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    // Demanem permís per accedir a la càmara del dispositiu i poder fer l'anàlisi de la imatge
    private void requestPermission() {
        ActivityCompat.requestPermissions(this, CAMERA_PERMISSION, CAMERA_REQUEST_CODE);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Passa l'event a la instància del Gesture Detector
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            HomeFragment fragment = new HomeFragment();
            Bundle bundle = new Bundle();
            bundle.putBoolean("isDoubleTapped", true);
            fragment.setArguments(bundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
            return true;
        }
    }
}