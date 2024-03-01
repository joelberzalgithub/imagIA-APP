package com.example.imagia_app.ui.home;

import android.content.ContextWrapper;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.imagia_app.MainActivity;
import com.example.imagia_app.R;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

public class HomeFragment extends Fragment implements TextToSpeech.OnInitListener {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextToSpeech textToSpeech;
    private ImageCapture imageCapture;
    private static final String urlNodeJsImatge = "https://ams24.ieti.site/api/maria/image";
    private StringBuilder completeResponse;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        previewView = view.findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider);

                if (getArguments() != null) {
                    if (getArguments().getBoolean("isDoubleTapped", false)) {
                        saveAndCaptureImage();
                    }
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));

        textToSpeech = new TextToSpeech(requireContext(), this);
        return view;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(new Locale("es", "ES"));
        }
    }

    private void saveAndCaptureImage() {
        File outputFile = getOutputFile();
        if (outputFile != null) {
            captureImage(outputFile);
        } else {
            Log.e("Error", "Failed to save image. Output file is null.");
        }
    }

    private void captureImage(File outputFile) {
        // Configurem opcions per a la captura (p. ex., format, qualitat, etc.)
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(outputFile).build();
        // Capturem la imatge
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                // Obtenim la ruta de l'arxiu de la imatge
                String imagePath = outputFile.getAbsolutePath();
                Log.i("INFO", "Ruta de la imatge: " + imagePath);

                Log.i("INFO", outputFile.toString());

                // Notifiquem a l'usuari que la imatge s'ha desat amb èxit
                Toast.makeText(requireContext(), "La imatge s'ha desat amb èxit!", Toast.LENGTH_SHORT).show();
                List<File> imatges = new ArrayList<>();
                imatges.add(outputFile);

                // Cridem a l'API
                callToNodeJS(imatges);
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                // Notifiquem a l'usuari que hi ha hagut un error en capturar la imatge
                Toast.makeText(requireContext(), "Error en capturar la imatge", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Creem el fitxer on es desarà la imatge
    private File getOutputFile() {
        File directory = new ContextWrapper(requireContext()).getFilesDir();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";
        return new File(directory, fileName);
    }

    /*
    Vinculem l'ImageAnalyzer al proveïdor de la càmara creada en el mètode onCreate
    i està atent a possibles canvis en la rotació de la càmara
    */
    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageCapture.Builder builder = new ImageCapture.Builder();
        imageCapture = builder.build();

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), ImageProxy::close);

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);
    }

    private void callToNodeJS(List<File> imageFiles) {
        completeResponse = new StringBuilder();
        completeResponse.append("");
        String token = MainActivity.getApiKey(new File(MainActivity.filesDir, "api_token.json"));
        if (token == null) {
            Log.e("error", "no se pudo conseguir el token");
            return;
        }
        Log.i("info", "token para imagen");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .build();

            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

            for (File file : imageFiles) {
                if (file.exists()) {
                    multipartBuilder.addFormDataPart("file", file.getName(),
                            RequestBody.create(file, MediaType.parse("application/octet-stream")));
                } else {
                    Log.e("Error", "L'arxiu de la imatge no existeix, abortant POST");
                    return;
                }

            }

            multipartBuilder.addFormDataPart("prompt", "Describe esta imagen en castellano")
                    .addFormDataPart("token", token);

            RequestBody body = multipartBuilder.build();

            Request request = new Request.Builder()
                    .url(urlNodeJsImatge)
                    .method("POST", body)
                    .build();

            try {
                // Executem la petició
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                // Gestionem la resposta segons sigui necessari
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    BufferedSource source = responseBody.source();
                    Buffer buffer = new Buffer();
                    long bytesRead;

                    while ((bytesRead = source.read(buffer, 8192)) != -1) {
                        // Process the chunk of data here
                        String chunkString = buffer.readUtf8();
                        if (source.exhausted()) {
                            JSONObject jsonObject = new JSONObject(chunkString);
                            Log.i("Json", jsonObject.getString("message"));
                        } else {
                            completeResponse.append(chunkString);
                        }
                        Log.i("Chunk", chunkString);
                    }

                    // Tanquem el cos de la resposta quan haguem acabat de fer-lo servir
                    responseBody.close();
                    // Log.i("Missatge final", String.valueOf(completeResponse));
                }
            } catch (JSONException e) {
                Log.e("Error", "Timeout exception");
            } catch (SocketTimeoutException e) {
                Log.e("Error", "SocketTimeOut exception");
            } catch (Exception e) {
                Log.e("Error", "Something went wrong...");
            }

            if (!String.valueOf(completeResponse).equals("")) {
                textToSpeech.speak(completeResponse, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }
}
