package com.example.imagia_app.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.imagia_app.R;

public class NotificationsFragment extends Fragment {
    private Button registerButton;
    private EditText editTextNom;
    private EditText editTextTfn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        registerButton = view.findViewById(R.id.registerButton);
        editTextNom = view.findViewById(R.id.editTextNom);
        editTextTfn = view.findViewById(R.id.editTextTfn);

        return view;
    }
}
