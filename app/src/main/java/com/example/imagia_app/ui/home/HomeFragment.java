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

import com.example.imagia_app.R;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
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
    private static final String urlNodeJsImatge = "http://192.168.17.139:3000/api/maria/image";

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
                        captureImage();
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
            textToSpeech.setLanguage(Locale.US);
        }
    }

    private void captureImage() {
        // Configurem opcions per a la captura (p. ex., format, qualitat, etc.)
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(getOutputFile()).build();
        // Capturem la imatge
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                // Obtenim la ruta de l'arxiu de la imatge
                String imagePath = getOutputFile().getAbsolutePath();
                Log.i("INFO", "Ruta de la imatge: " + imagePath);

                // Convertim la ruta de l'arxiu de la imatge en un objecte tipus File
                File imageFile = new File(imagePath);
                Log.i("INFO", imageFile.toString());

                // Notifiquem a l'usuari que la imatge s'ha desat amb èxit
                Toast.makeText(requireContext(), "La imatge s'ha desat amb èxit!", Toast.LENGTH_SHORT).show();
                List<File> imatges = new ArrayList<>();
                imatges.add(imageFile);

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

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);
    }

    private void callToNodeJS(List<File> imageFiles) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            /*
            OkHttpClient client = new OkHttpClient.Builder()
                    .build();

            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

            for (File file : imageFiles) {
                multipartBuilder.addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse("application/octet-stream")));
            }

            multipartBuilder.addFormDataPart("prompt", "Please describe both images briefly")
                    .addFormDataPart("token", "123456789");

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
                    StringBuilder completeResponse = new StringBuilder();
                    while ((bytesRead = source.read(buffer, 8192)) != -1) {
                        // Process the chunk of data here
                        String chunkString = buffer.readUtf8();
                        if (source.exhausted()) {
                            JSONObject jsonObject = new JSONObject(chunkString);
                            Log.i("Json", jsonObject.getString("message"));
                        } else {
                            completeResponse.append(chunkString);
                        }
                        Log.i("chunk", chunkString);
                    }

                    // Tanquem el cos de la resposta quan haguem acabat de fer-lo servir
                    responseBody.close();
                    // Log.i("Missatge final", String.valueOf(completeResponse));
                }
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
            */
            String test = "In the image, a large cow is hanging upside down from a wooden beam. In the second image, there's an animal lying on the side of a wall and another one hanging off a piece of wood with its belly facing the camera. The third image features two cows, one in the foreground and another further back, both appearing to be standing near a wall and possibly interacting with each other.";
            textToSpeech.speak(test, TextToSpeech.QUEUE_FLUSH, null, null);
        });
    }
}
