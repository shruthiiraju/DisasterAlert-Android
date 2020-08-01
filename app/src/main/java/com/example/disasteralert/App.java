package com.example.disasteralert;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {
    public static final String CHANNEL_1_ID = "channel1";
    public static final String CHANNEL_2_ID = "channel2";
    public static final String CHANNEL_3_ID = "channel3";
    public static final String CHANNEL_4_ID = "channel4";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel1 = new NotificationChannel(
                    CHANNEL_1_ID,
                    "Low Priority Notifications",
                    NotificationManager.IMPORTANCE_MIN
            );
            channel1.setDescription("This has priority level 1");

            NotificationChannel channel2 = new NotificationChannel(
                    CHANNEL_2_ID,
                    "Medium Priority Notifications",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel2.setDescription("This has priority level 2");

            NotificationChannel channel3 = new NotificationChannel(
                    CHANNEL_3_ID,
                    "High Priority Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel3.setDescription("This has priority level 3");

            NotificationChannel channel4 = new NotificationChannel(
                    CHANNEL_4_ID,
                    "Very High Priority Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel4.setDescription("This has priority level 4");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel1);
            manager.createNotificationChannel(channel2);
            manager.createNotificationChannel(channel3);
            manager.createNotificationChannel(channel4);
        }
    }
}
