package com.sony.tv.app.atsc3receiver1_0.app;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.sony.tv.app.atsc3receiver1_0.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class NewAddDialogFragment extends DialogFragment {


    public NewAddDialogFragment() {
        // Required empty public constructor
    }



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder adDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();

        View convertView = layoutInflater.inflate(R.layout.fragment_new_add_dialog, null);
        adDialog.setView(convertView);
        RadioGroup radioGroup = (RadioGroup)convertView.findViewById(R.id.radio_group_payment_type);

        View titleView = layoutInflater.inflate(R.layout.dialog_title, null);
        TextView titleText = (TextView)titleView.findViewById(R.id.text_view_dialog_title);
        titleText.setText(R.string.enter_new_add);
        adDialog.setCustomTitle(titleView);


        adDialog.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        });

        adDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });


        return adDialog.create();
    }


}
