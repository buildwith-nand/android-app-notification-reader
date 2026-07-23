package com.app.appnotificationreader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private ImageView interceptedNotificationImageView;
    private ImageChangeBroadcastReceiver imageChangeBroadcastReceiver;
    private AlertDialog enableNotificationListenerAlertDialog;

    private final String channelId = "i.apps.notifications"; // Unique channel ID for notifications
    private final String description = "Test notification";  // Description for the notification channel
    private final int notificationId = 1234; // Unique identifier for the notification
    public static TextView tvSumarry;
    public static TextView tvAutoFillText;
    private View mFloatingView;

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 1000 * 60 * 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        @SuppressWarnings("deprecation")
        PowerManager gfgPowerDraw = (PowerManager)getSystemService(POWER_SERVICE);
        PowerManager.WakeLock gfgPowerLatch = gfgPowerDraw.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "GfGApp::AchieveWakeLock");
        gfgPowerLatch.acquire();




        startService(new Intent(this, NotificationListenerExampleService.class));

        mFloatingView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        tvSumarry = mFloatingView.findViewById(R.id.tvSummary);

        // Here we get a reference to the image we will modify when a notification is received
        interceptedNotificationImageView      = (ImageView) this.findViewById(R.id.intercepted_notification_logo);
        createNotificationChannel();

            // Request runtime permission for notifications on Android 13 and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            101
                    );
                    return;
                }
            }
            sendNotification(); // Trigger the notification



        // If the user did not turn the notification listener service on we prompt him to do so
        if(!isNotificationServiceEnabled()){
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
            enableNotificationListenerAlertDialog.show();
        }

//        if (!checkOverlayPermission())
//        {   Toast.makeText(MainActivity.this, "Please allow Display over app.", Toast.LENGTH_SHORT).show();
//            return;
//        }
        //startOverLayNew();
        // Finally we register a receiver to tell the MainActivity when a notification has been received
        imageChangeBroadcastReceiver = new ImageChangeBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.app.appnotificationreader");
        registerReceiver(imageChangeBroadcastReceiver,intentFilter);


        Log.d("onCreateMain","onCreateMain");
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, delay);
                Log.d("onCreateLoopApiMain","call api");
                OkHttpClient client = new OkHttpClient();

                String getUrl = "https://xxxxx.xxx/datahub/fetchuserplan?username=test&password=test&refNumber=";
                //Log.d("onNotificationPosted","getUrl "+getUrl);
                Request request = new Request.Builder()
                        .url(getUrl)
                        .build();
                Log.d("onCreateLoopApiMain", "client.newCall");

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        call.cancel();
                        Log.d("onCreateLoopApiMain", "call.cancel() " + e.toString());
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try {
                            final String myResponse = response.body().string();
                            Log.d("onCreateLoopApiMain", "Api Resp received");//okhttp allow once to fetch response

                        } catch (Exception e) {
                            Log.d("onCreateLoopApiMain", "Okhttp call failed " + e.toString());
                        }

                    }
                });
            }
        }, delay);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(imageChangeBroadcastReceiver);
    }

    private void changeInterceptedNotificationImage(int notificationCode){
        switch(notificationCode){
            case NotificationListenerExampleService.InterceptedNotificationCode.FACEBOOK_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.facebook_logo);
                break;
//            case NotificationListenerExampleService.InterceptedNotificationCode.INSTAGRAM_CODE:
//                interceptedNotificationImageView.setImageResource(R.drawable.instagram_logo);
//                break;
            case NotificationListenerExampleService.InterceptedNotificationCode.BHIM_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.instagram_logo);
               break;
            case NotificationListenerExampleService.InterceptedNotificationCode.WHATSAPP_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.whatsapp_logo);
                break;
            case NotificationListenerExampleService.InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.other_notification_logo);
                break;
        }
    }

    private boolean isNotificationServiceEnabled(){
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public class ImageChangeBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int receivedNotificationCode = intent.getIntExtra("Notification Code",-1);
            changeInterceptedNotificationImage(receivedNotificationCode);
        }
    }

    public void startOverLayNew() {
        Log.d("Floatingviewservice","startOverLayNew");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startService(new Intent(this, FloatingViewService.class));
            //finish();
        } else if (Settings.canDrawOverlays(this)) {
            startService(new Intent(this, FloatingViewService.class));
            //finish();
        } else {
            askPermission();
            Toast.makeText(this, "You need System Alert Window Permission to do this", Toast.LENGTH_SHORT).show();
        }
    }


    private AlertDialog buildNotificationServiceAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.notification_listener_service);
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation);
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return(alertDialogBuilder.create());
    }

    public boolean checkOverlayPermission(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // send user to the device settings
                Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(myIntent);
                return false;
            }
            return true;
        }
        return false;
    }

    private void askPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 2084);
    }

private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationChannel notificationChannel = new NotificationChannel(
                channelId,
                description,
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationChannel.enableLights(true); // Turn on notification light
        notificationChannel.setLightColor(Color.GREEN);
        notificationChannel.enableVibration(true); // Allow vibration for notifications

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}

/**
 * Build and send a notification with a custom layout and action.
 */
@SuppressLint("MissingPermission")
private void sendNotification() {

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle("Notification Reader")
            .setContentText("")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true);

    // Display the notification
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
    notificationManager.notify(notificationId, builder.build());
}
}