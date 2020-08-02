package com.example.disasteralert;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import static com.example.disasteralert.App.CHANNEL_1_ID;
import static com.example.disasteralert.App.CHANNEL_2_ID;
import static com.example.disasteralert.App.CHANNEL_3_ID;
import static com.example.disasteralert.App.CHANNEL_4_ID;


public class DisasterFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final int SAFE_BUTTON_REQUEST_CODE = 1234;
    private static final int NOT_SAFE_BUTTON_REQUEST_CODE = 12345;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private NotificationManagerCompat notificationManager;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with GCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            Map<String, String> map = remoteMessage.getData();

            String priority = map.get("priority");

            notificationManager = NotificationManagerCompat.from(this);

            if (map.containsKey("isSevere") && map.get("isSevere").equals("True")) {
                buildSevereNotification(CHANNEL_3_ID, map);
            } else {
                switch (priority) {
                    case "1":
                        buildNotification(CHANNEL_1_ID, map);
                        break;
                    case "2":
                        buildNotification(CHANNEL_2_ID, map);
                        break;
                    case "3":
                        buildNotification(CHANNEL_3_ID, map);
                        break;
                    default:
                        buildNotification(CHANNEL_4_ID, map);
                        break;
                }
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]


    private void buildNotification(String CHANNEL_ID, Map<String, String> map) {
        int notification_id = Integer.parseInt(map.get("notification_id").substring(map.get("notification_id").length() - 9));

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID);

        notification
                .setSmallIcon(R.drawable.ic_public_24px)
                .setContentTitle(map.get("title"))
                .setContentText(map.get("body"));

        notificationManager.notify(notification_id, notification.build());
    }

    private void buildSevereNotification(String CHANNEL_ID, Map<String, String> map) {
        int notification_id = Integer.parseInt(map.get("notification_id").substring(map.get("notification_id").length() - 9));

        RemoteViews expandedNotificationView = new RemoteViews(getPackageName(), R.layout.layout_severe_notification_expanded);
        RemoteViews collapsedNotificationView = new RemoteViews(getPackageName(), R.layout.layout_severe_notification_collapsed);

        collapsedNotificationView.setTextViewText(R.id.text_notification_report_title, map.get("title"));
        collapsedNotificationView.setTextViewText(R.id.text_notification_report_description, map.get("body"));

        expandedNotificationView.setTextViewText(R.id.text_notification_report_title, map.get("title"));
        expandedNotificationView.setTextViewText(R.id.text_notification_report_description, map.get("body"));

        Intent safeButtonClickIntent = new Intent(this, LovedOnesActivity.class);
        safeButtonClickIntent.putExtra("isSafe", true);
        safeButtonClickIntent.putExtra("notificationId", notification_id);
        PendingIntent safeButtonClickPendingIntent = PendingIntent.getActivity(this, SAFE_BUTTON_REQUEST_CODE, safeButtonClickIntent, 0);
        Intent notSafeButtonClickIntent = new Intent(this, LovedOnesActivity.class);
        notSafeButtonClickIntent.putExtra("isSafe", false);
        notSafeButtonClickIntent.putExtra("notificationId", notification_id);
        PendingIntent notSafeButtonClickPendingIntent = PendingIntent.getActivity(this, NOT_SAFE_BUTTON_REQUEST_CODE, notSafeButtonClickIntent, 0);

        expandedNotificationView.setOnClickPendingIntent(R.id.button_notification_safe, safeButtonClickPendingIntent);
        expandedNotificationView.setOnClickPendingIntent(R.id.button_notification_not_safe, notSafeButtonClickPendingIntent);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_public_24px)
                .setContentTitle(map.get("title"))
                .setContentText(map.get("body"))
                .setCustomBigContentView(expandedNotificationView)
                .build();

        notificationManager.notify(notification_id, notification);
    }

    // [START on_new_token]

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token);
    }
    // [END on_new_token]


    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            db.collection("users")
                    .document(mAuth.getCurrentUser().getUid())
                    .update("token", token)

                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "onFailure: ", e);
                        }
                    });
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification_important_24px)
                        .setContentTitle(getString(R.string.fcm_message))
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
