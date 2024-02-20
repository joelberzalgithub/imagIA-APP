package com.example.imagia_app.ui.notifications;

import android.os.Bundle;
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

public class NotificationsFragment extends Fragment {
    private EditText editTextEmail;
    private EditText editTextNom;
    private EditText editTextTfn;
    private EditText editTextCodi;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextNom = view.findViewById(R.id.editTextNom);
        editTextTfn = view.findViewById(R.id.editTextTfn);
        editTextCodi = view.findViewById(R.id.editTextCodi);

        Button registerButton = view.findViewById(R.id.registerButton);
        registerButton.setOnClickListener(v -> {
            if (editTextEmail.getText().toString().trim().isEmpty() ||
                    editTextNom.getText().toString().trim().isEmpty() ||
                    editTextTfn.getText().toString().trim().isEmpty() ||
                    editTextCodi.getText().toString().trim().isEmpty()) {
                Toast.makeText(requireContext(), "No t'has pogut registrar! Et falta inserir dades de l'usuari.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "T'has registrat amb èxit!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
