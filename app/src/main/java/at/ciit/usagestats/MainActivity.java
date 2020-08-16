package at.ciit.usagestats;
/*
  Created by Javatraining www.ciit.at (Amjad) on 16,August,2020
*/

import androidx.appcompat.app.AppCompatActivity;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;

public class MainActivity extends AppCompatActivity {

    Button enableBtn, showBtn;
    TextView permissionDescriptionTv, usageTv;
    ListView appsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enableBtn = findViewById(R.id.enable_btn);
        showBtn =  findViewById(R.id.show_btn);
        permissionDescriptionTv =findViewById(R.id.permission_description_tv);
        usageTv =  findViewById(R.id.usage_tv);
        appsList =  findViewById(R.id.apps_list);

        this.loadStatistics();
    }


    // each time the application gets in foreground -> getGrantStatus and render the corresponding buttons
    @Override
    protected void onStart() {
        super.onStart();
        if (getGrantStatus()) {
            showHideWithPermission();
            showBtn.setOnClickListener(view -> {
                loadStatistics();
            });
        } else {
            showHideNoPermission();
            enableBtn.setOnClickListener(view -> {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            });
        }
    }


    /**
     * load the usage stats for last 24h
     */
    public void loadStatistics() {
        UsageStatsManager usm = (UsageStatsManager) this.getSystemService(USAGE_STATS_SERVICE);
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  System.currentTimeMillis() - 1000*3600*24,  System.currentTimeMillis());

        // Group the usageStats by application and sort them by total time in foreground
        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getTotalTimeInForeground(), usageStats);
            }
            showAppsUsage(mySortedMap);
        }
    }


    public void showAppsUsage(SortedMap<Long, UsageStats> mySortedMap) {
        ArrayList<App> appsList = new ArrayList<>();
        List<UsageStats> usageStatsList = mySortedMap.values().stream().filter(this::isAppInfoAvailable).collect(Collectors.toList());

        // get total time of apps usage to calculate the usagePercentage for each app
        long totalTime = usageStatsList.stream().map(UsageStats::getTotalTimeInForeground).mapToLong(Long::longValue).sum();

        //fill the appsList
        for (UsageStats usageStats : usageStatsList) {
            try {
                String packageName = usageStats.getPackageName();
                ApplicationInfo ai = getApplicationContext().getPackageManager().getApplicationInfo(packageName, 0);
                Drawable icon = getApplicationContext().getPackageManager().getApplicationIcon(ai);
                String appName = getApplicationContext().getPackageManager().getApplicationLabel(ai).toString();
                String usageDuration = getDurationBreakdown(usageStats.getTotalTimeInForeground());
                int usagePercentage = (int) (usageStats.getTotalTimeInForeground() * 100 / totalTime);

                App usageStatDTO = new App(icon, appName, usagePercentage, usageDuration);
                appsList.add(usageStatDTO);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }


        // reverse the list to get most usage first
        Collections.reverse(appsList);
        // build the adapter
        AppsAdapter adapter = new AppsAdapter(this, appsList);

        // attach the adapter to a ListView
        ListView listView = findViewById(R.id.apps_list);
        listView.setAdapter(adapter);

        showHideItemsWhenShowApps();
    }

    /**
     * check if PACKAGE_USAGE_STATS permission is aloowed for this application
     * @return true if permission granted
     */
    private boolean getGrantStatus() {
        AppOpsManager appOps = (AppOpsManager) getApplicationContext()
                .getSystemService(Context.APP_OPS_SERVICE);

        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getApplicationContext().getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            return (getApplicationContext().checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            return (mode == MODE_ALLOWED);
        }
    }

    /**
     * check if the application info is still existing in the device / otherwise it's not possible to show app detail
     * @return true if application info is available
     */
    private boolean isAppInfoAvailable(UsageStats usageStats) {
        try {
            getApplicationContext().getPackageManager().getApplicationInfo(usageStats.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * helper method to get string in format hh:mm:ss from miliseconds
     *
     * @param millis (application time in foreground)
     * @return string in format hh:mm:ss from miliseconds
     */
    private String getDurationBreakdown(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        return (hours + " h " +  minutes + " m " + seconds + " s");
    }


    /**
     * helper method used to show/hide items in the view when  PACKAGE_USAGE_STATS permission is not allowed
     */
    public void showHideNoPermission() {
        enableBtn.setVisibility(View.VISIBLE);
        permissionDescriptionTv.setVisibility(View.VISIBLE);
        showBtn.setVisibility(View.GONE);
        usageTv.setVisibility(View.GONE);
        appsList.setVisibility(View.GONE);

    }

    /**
     * helper method used to show/hide items in the view when  PACKAGE_USAGE_STATS permission allowed
     */
    public void showHideWithPermission() {
        enableBtn.setVisibility(View.GONE);
        permissionDescriptionTv.setVisibility(View.GONE);
        showBtn.setVisibility(View.VISIBLE);
        usageTv.setVisibility(View.GONE);
        appsList.setVisibility(View.GONE);
    }

    /**
     * helper method used to show/hide items in the view when showing the apps list
     */
    public void showHideItemsWhenShowApps() {
        enableBtn.setVisibility(View.GONE);
        permissionDescriptionTv.setVisibility(View.GONE);
        showBtn.setVisibility(View.GONE);
        usageTv.setVisibility(View.VISIBLE);
        appsList.setVisibility(View.VISIBLE);

    }
}