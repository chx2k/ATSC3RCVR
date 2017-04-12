package com.sony.tv.app.atsc3receiver1_0.app;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.sony.tv.app.atsc3receiver1_0.R;

import java.util.List;

import io.realm.Realm;

/**
 * Created by valokafor on 4/12/17.
 */

public class AdsListAdapter extends RecyclerView.Adapter<AdsListAdapter.ViewHolder>{
    private final List<Ad> adsList;
    private final Realm realm;

    public AdsListAdapter(List<Ad> adsList, Realm realm) {
        this.adsList = adsList;
        this.realm = realm;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.custom_row_layout_ads, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Ad selectedAd = adsList.get(position);

        holder.adNameTextView.setText(selectedAd.getTitle());
        holder.adUrlTextView.setText(selectedAd.getUri().toString());
        holder.adEnableCheckbox.setChecked(selectedAd.isEnabled());
        holder.adCounterTextView.setText(selectedAd.getDisplayCount());

    }

    @Override
    public int getItemCount() {
        if (adsList != null){
            return adsList.size();
        }else {
            return 0;
        }
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView adCounterTextView;
        public TextView adNameTextView;
        public TextView adUrlTextView;
        public CheckBox adEnableCheckbox;


        public ViewHolder(View itemView) {
            super(itemView);
            adCounterTextView = (TextView) itemView.findViewById(R.id.display_count_textview);
            adNameTextView = (TextView) itemView.findViewById(R.id.ad_name_textview);
            adUrlTextView = (TextView) itemView.findViewById(R.id.ad_url_textview);
            adEnableCheckbox = (CheckBox) itemView.findViewById(R.id.ad_enable_checkbox);

            adEnableCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    final Ad selectedAd = adsList.get(getAdapterPosition());
                    if (isChecked){
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                selectedAd.setEnabled(true);
                                notifyItemChanged(getAdapterPosition());
                            }
                        });
                    }else {
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                selectedAd.setEnabled(false);
                                notifyItemChanged(getAdapterPosition());
                            }
                        });

                    }
                }
            });
        }
    }


}
