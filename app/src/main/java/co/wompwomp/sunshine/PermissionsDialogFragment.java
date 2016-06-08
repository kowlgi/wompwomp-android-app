package co.wompwomp.sunshine;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public class PermissionsDialogFragment extends DialogFragment {
    public PermissionsDialogFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static PermissionsDialogFragment newInstance() {
        PermissionsDialogFragment frag = new PermissionsDialogFragment();
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return  inflater.inflate(R.layout.dialog_permissions, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setCancelable(false); // Don't want the back button or clicking outside to close the dialog
        View nextButton = view.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};
                int permissionRequestCode = 200;
                requestPermissions(permissions, permissionRequestCode);
                getDialog().dismiss();
            }
        });
    }
}

