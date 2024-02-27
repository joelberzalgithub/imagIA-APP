package com.example.imagia_app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String[] CAMERA_PERMISSION = new String[] { android.Manifest.permission.CAMERA };
    private static final int CAMERA_REQUEST_CODE = 10;
    /*
    private long lastTime = 0;
    private float lastX, lastY, lastZ;
    private static final int SHAKE_THRESHOLD = 600;
    private static final int TIME_THRESHOLD = 100;
    */
    private boolean isDoubleTapped = false;
    private GestureDetector gestureDetector;
    private AlertDialog dialog;
    private EditText editTextTfn;
    private EditText editTextNom;
    private EditText editTextEmail;
    private static final String urlNodeJsRegister = "https://ams24.ieti.site/api/user/register";

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

        loginDialog();
        /*
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
        */
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

    @SuppressLint("SetTextI18n")
    public void loginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Registre d'usuari");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(this);
        textView.setText("Insereix el teu número de telèfon, el teu nickname i el teu correu electrònic per poder registrar-te.");
        textView.setPadding(65, 65, 65, 55);
        layout.addView(textView);

        // Creem un EditText per inserir el número de telèfon de l'usuari
        editTextTfn = new EditText(this);
        editTextTfn.setHint("Número de telèfon");
        editTextTfn.setInputType(InputType.TYPE_CLASS_PHONE);
        LinearLayout.LayoutParams paramsTfn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsTfn.setMargins(65, 0, 65, 0);
        editTextTfn.setLayoutParams(paramsTfn);
        layout.addView(editTextTfn);

        // Creem un EditText per inserir el nickname de l'usuari
        editTextNom = new EditText(this);
        editTextNom.setHint("Nick Name");
        LinearLayout.LayoutParams paramsNom = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsNom.setMargins(65, 0, 65, 0);
        editTextNom.setLayoutParams(paramsNom);
        layout.addView(editTextNom);

        // Creem un EditText per inserir el correu electrònic de l'usuari
        editTextEmail = new EditText(this);
        editTextEmail.setHint("Correu electrònic");
        editTextEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        LinearLayout.LayoutParams paramsEmail = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsEmail.setMargins(65, 0, 65, 0);
        editTextEmail.setLayoutParams(paramsEmail);
        layout.addView(editTextEmail);

        builder.setView(layout);
        builder.setCancelable(false);

        // Creem un botó de registre que per defecte estarà deshabilitat
        builder.setPositiveButton("Registrar", null);
        dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setEnabled(false);

            // Afegim TextChangeListeners als camps dels EditTexts
            editTextTfn.addTextChangedListener(textWatcher);
            editTextNom.addTextChangedListener(textWatcher);
            editTextEmail.addTextChangedListener(textWatcher);

            View.OnClickListener positiveClickListener = v -> {
                // Obtenim les dades de l'usuari
                String email = editTextEmail.getText().toString();
                String nom = editTextNom.getText().toString();
                String tfn = editTextTfn.getText().toString();

                // Li assignem les dades corresponents al JSON de l'usuari
                String content = "{\r\n    \"name\": \"VALUE_1\",\r\n    \"email\": \"VALUE_2\",\r\n    \"phone\": \"VALUE_3\"\r\n}";
                content = content.replace("VALUE_1", nom)
                        .replace("VALUE_2", email)
                        .replace("VALUE_3", tfn);
                Log.i("Usuari", content);

                // Cridem a l'API
                callToNodeJS(content);

                // Tanquem l'interfície del Dialog
                dialogInterface.dismiss();

                // Obrim el Dialog de confirmació del registre d'usuari
                loadingDialog();
            };
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(positiveClickListener);
        });
        dialog.show();
    }

    private void loadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = LayoutInflater.from(this).inflate(R.layout.loading_dialog, null);
        builder.setView(view);
        builder.setCancelable(false);

        builder.create().show();
    }

    @SuppressLint("SetTextI18n")
    public void smsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmació del registre d'usuari");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(this);
        textView.setText("Valida el teu número de telèfon inserint en el següent camp de text l'SMS que t'acabem d'enviar:");
        textView.setPadding(65, 65, 65, 55);
        layout.addView(textView);

        EditText editTextSMS = new EditText(this);
        editTextSMS.setHint("SMS");
        LinearLayout.LayoutParams paramsSMS = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsSMS.setMargins(65, 0, 65, 0);
        editTextSMS.setLayoutParams(paramsSMS);
        layout.addView(editTextSMS);

        builder.setView(layout);
        builder.setCancelable(false);

        builder.setPositiveButton("Ok", (dialog, whichButton) -> {
            dialog.dismiss();
        });
        builder.create().show();
    }

    // Habilitem o deshabilitem el botó de registre fent servir un TextWatcher
    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            // Comprovem que tots els EditTexts tinguin algun valor
            boolean enableButton = editTextTfn.getText().length() > 0 &&
                    editTextNom.getText().length() > 0 &&
                    editTextEmail.getText().length() > 0;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enableButton);
        }
    };

    private void callToNodeJS(String content) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();

            MediaType mediaType = MediaType.parse("application/json");

            RequestBody body = RequestBody.create(content, mediaType);

            Request request = new Request.Builder()
                    .url(urlNodeJsRegister)
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try {
                // Executem la petició
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
