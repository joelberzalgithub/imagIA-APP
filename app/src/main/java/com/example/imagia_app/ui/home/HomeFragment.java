package com.example.imagia_app.ui.home;

import android.content.ContextWrapper;
import android.os.Bundle;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class HomeFragment extends Fragment {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;

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

        return view;
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
                /*
                // Obtenim una llista de bytes de l'arxiu de la imatge
                byte[] imageBytes = new byte[(int) imageFile.length()];
                Log.i("INFO", Arrays.toString(imageBytes));

                // Llegim l'arxiu de la imatge
                try {
                    FileInputStream fis = new FileInputStream(imageFile);
                    fis.read(imageBytes);
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.i("INFO", Arrays.toString(imageBytes));
                */
                // Notifiquem a l'usuari que la imatge s'ha desat amb èxit
                Toast.makeText(requireContext(), "La imatge s'ha desat amb èxit!", Toast.LENGTH_SHORT).show();
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
}