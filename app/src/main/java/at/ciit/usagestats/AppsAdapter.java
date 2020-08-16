package at.ciit.usagestats;
/*
  Created by Javatraining www.ciit.at (Amjad) on 16,August,2020
*/

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

public class AppsAdapter extends ArrayAdapter<App> {
    public AppsAdapter(Context context, ArrayList<App> usageStatDTOArrayList) {
        super(context, 0, usageStatDTOArrayList);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Get the data item for this position
        App usageStats = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_app, parent, false);
        }

        // Lookup view for data population
        TextView app_name_tv = convertView.findViewById(R.id.app_name_tv);
        TextView usage_duration_tv =  convertView.findViewById(R.id.usage_duration_tv);
        TextView usage_perc_tv = convertView.findViewById(R.id.usage_perc_tv);
        ImageView icon_img =  convertView.findViewById(R.id.icon_img);
        ProgressBar progressBar = convertView.findViewById(R.id.progressBar);


        // Populate the data into the template view using the data object
        app_name_tv.setText(usageStats.appName);
        usage_duration_tv.setText(usageStats.usageDuration);
        usage_perc_tv.setText(usageStats.usagePercentage + "%");
        icon_img.setImageDrawable(usageStats.appIcon);
        progressBar.setProgress(usageStats.usagePercentage);

        // Return the completed view to render on screen
        return convertView;
    }
}