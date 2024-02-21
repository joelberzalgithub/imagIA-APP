package com.example.imagia_app.ui.notifications;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.imagia_app.R;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationsFragment extends Fragment {
    private EditText editTextEmail;
    private EditText editTextNom;
    private EditText editTextTfn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextNom = view.findViewById(R.id.editTextNom);
        editTextTfn = view.findViewById(R.id.editTextTfn);

        Button registerButton = view.findViewById(R.id.registerButton);
        registerButton.setOnClickListener(v -> {
            // Obtenim les dades de l'usuari
            String email = editTextEmail.getText().toString();
            String nom = editTextNom.getText().toString();
            String tfn = editTextTfn.getText().toString();

            // Comprovem que totes les dades tinguin algun valor
            if (email.isEmpty() || nom.isEmpty() || tfn.isEmpty()) {
                // Notifiquem a l'usuari que no s'ha pogut registrar
                Toast.makeText(requireContext(), "No t'has pogut registrar! Et falta inserir dades de l'usuari.", Toast.LENGTH_SHORT).show();
            } else {
                // Notifiquem a l'usuari que s'ha registrat correctament
                Toast.makeText(requireContext(), "T'has registrat amb èxit!", Toast.LENGTH_SHORT).show();

                // Li assignem les dades corresponents al JSON de l'usuari
                String content = "{\r\n    \"name\": \"VALUE_1\",\r\n    \"email\": \"VALUE_2\",\r\n    \"phone\": \"VALUE_3\"\r\n}";
                content = content.replace("VALUE_1", nom)
                        .replace("VALUE_2", email)
                        .replace("VALUE_3", tfn);
                Log.i("Usuari", content);

                // Cridem a l'API
                new NetworkTask().execute(content);
            }
        });

        return view;
    }

    @SuppressLint("StaticFieldLeak")
    private static class NetworkTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String content = params[0];
            callToNodeJS(content);
            return null;
        }

        private static void callToNodeJS(String content) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .build();

                MediaType mediaType = MediaType.parse("application/json");

                RequestBody body = RequestBody.create(content, mediaType);
                Log.i("Cos de la petició", body.toString());

                Request request = new Request.Builder()
                        .url("https://ams24.ieti.site/api/user/register")
                        .method("POST", body)
                        .addHeader("Content-Type", "application/json")
                        .build();
                Log.i("Petició", String.valueOf(request));

                try {
                    // Executem la petició
                    Response response = client.newCall(request).execute();
                    Log.i("Resposta", String.valueOf(response));
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
