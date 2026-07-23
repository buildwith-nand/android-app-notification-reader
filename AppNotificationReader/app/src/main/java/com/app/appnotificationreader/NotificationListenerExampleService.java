package com.app.appnotificationreader;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.os.Bundle;
import android.os.Handler;

import android.content.pm.PackageManager;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.os.Process;

import androidx.core.app.NotificationCompat;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NotificationListenerExampleService extends NotificationListenerService {

    /*
        These are the package names of the apps. for which we want to
        listen the notifications
     */

    String respMessage = "";

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 1000 * 60 * 10;
    int delayRecconect = 1000 * 60 * 60 * 1;
    int countNotificationSms = 0;
    int countSuccessApiCall = 0;
    int countFailedApiCall=0;
    private static final String TAG = "NotificationListener";
    int trxnAmount=0;
    private static final class ApplicationPackageNames {
        public static final String FACEBOOK_PACK_NAME = "com.facebook.katana";
        public static final String FACEBOOK_MESSENGER_PACK_NAME = "com.facebook.orca";
        public static final String WHATSAPP_PACK_NAME = "com.whatsapp";
        public static final String INSTAGRAM_PACK_NAME = "com.instagram.android";
        public static final String BHIM_SBI_PACK_NAME = "com.sbi.upi";
        public static final String SMS_PACK_NAME = "com.google.android.apps.messaging";
    }

    /*
        These are the return codes we use in the method which intercepts
        the notifications, to decide whether we should do something or not
     */
    public static final class InterceptedNotificationCode {
        public static final int FACEBOOK_CODE = 1;
        public static final int WHATSAPP_CODE = 2;
        //public static final int INSTAGRAM_CODE = 3;
        public static final int BHIM_CODE = 3;
        public static final int SMS_CODE = 4;
        public static final int OTHER_NOTIFICATIONS_CODE = 5; // We ignore all notification with code == 4
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("NotificationListener","onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.i("NotificationListener","onListenerConnected");
        //tryReconnectService();

        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, delayRecconect);
                Log.d("NotificationListener","call tryReconnectService");
                tryReconnectService();
            }
        }, delayRecconect);

    }

    public void tryReconnectService() {
        toggleNotificationListenerService();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ComponentName componentName =
                    new ComponentName(getApplicationContext(), NotificationListenerExampleService.class);

            //It say to Notification Manager RE-BIND your service to listen notifications again inmediatelly!
            requestRebind(componentName);
        }
    }

    private void toggleNotificationListenerService1() {
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, NotificationListenerExampleService.class),PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(new ComponentName(this, NotificationListenerExampleService.class),PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("onCreate","onCreate");
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, delay);
                Log.d("onCreateLoopApi","call api");
                OkHttpClient client = new OkHttpClient();

                String getUrl = "https://xxx.xx/datahub/fetchuserplan?username=test&password=test&refNumber=";
                //Log.d("onNotificationPosted","getUrl "+getUrl);
                Request request = new Request.Builder()
                        .url(getUrl)
                        .build();
                Log.d("onCreateLoopApi", "client.newCall");

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        call.cancel();
                        Log.d("onCreateLoopApi", "call.cancel() " + e.toString());
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try {
                            final String myResponse = response.body().string();
                            Log.d("onCreateLoopApi", "Api Resp received");//okhttp allow once to fetch response

                        } catch (Exception e) {
                            Log.d("onCreateLoopApi", "Okhttp call failed " + e.toString());
                            }

                    }
                });
            }
        }, delay);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn){
        int notificationCode = matchNotificationCode(sbn);
        Log.d("onNotificationPosted:Pkg",sbn.getPackageName());

        //if(notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE){
        if(notificationCode == InterceptedNotificationCode.BHIM_CODE
                ||notificationCode == InterceptedNotificationCode.SMS_CODE
                ||sbn.getPackageName().contains("com.android.mms")
        ){
            Intent intent = new  Intent("com.app.appnotificationreader");
            Log.d("onNotificationPosted:Pkg",sbn.getPackageName());

            String title = null;
            String text = null;
            //String textLogfile=null;

            try {
                title = sbn.getNotification().extras.get("android.title").toString();
                Log.d("onNotificationPosted:title",title);

            } catch (Exception ignored) {}
            try {
                text = sbn.getNotification().extras.get("android.text").toString();
                //textLogfile = text;
                Log.d("onNotificationPosted:text",text);
                //FloatingViewService.tvAutoFillText.setText(text);
                //MainActivity.tvSumarry.setText(text);
            } catch (Exception ignored) {
                text = "Empty";
            }
            if (text.contains("You have received a UPI Payment with Rs.") || text.contains("credited by Rs.") ||text.contains("credited to") || text.contains("Money Received - INR"))
            {
                countNotificationSms++;
                try {

                    if (text.contains("Money Received"))
                    {
                        Pattern p = Pattern.compile("- INR ?\\d*");
                        Matcher m = p.matcher(text);
                        while (m.find()) {
                            System.out.println(m.group());
                            Log.d("onNotificationPosted:Amount", "" + m.group());
                            String tmpAmount = m.group();
                            trxnAmount = Integer.parseInt(tmpAmount.replace("- INR", "").trim().toString());
                            break;
                        }
                    }
                    else {
                        Pattern p = Pattern.compile("Rs. ?\\d*");
                        Matcher m = p.matcher(text);
                        while (m.find()) {
                            System.out.println(m.group());
                            Log.d("onNotificationPosted:Amount", "" + m.group());
                            String tmpAmount = m.group();
                            trxnAmount = Integer.parseInt(tmpAmount.replace("Rs.", "").trim().toString());
                            break;
                        }
                    }
                }
                catch (Exception ex)
                {
                    Log.d("onNotificationPosted:Amount","Error getting amount"+ex.toString());
                    trxnAmount=0;

                }
                Log.d("onNotificationPosted:amount","Transaction amount:"+trxnAmount);

                ///check internet connection start
                if (isConnected()) {
                    Log.d("onNotificationPosted","Internet connected");
                }
                else
                {
                    ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
                    File directory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                    String filename = dateFormat.format(new Date());
                    File txtFile = new File(directory, "NetworkErrorLog_"+ filename +".txt");
                    Log.d("onNotificationPosted", "check network write log file");

                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(txtFile,true);
                        OutputStreamWriter osw = new OutputStreamWriter(fos);

                        SimpleDateFormat dateFormatTS = new SimpleDateFormat("yyyyMMdd_HHmmss");
                        String logTS = dateFormatTS.format(new Date());

                        osw.write("Data:"+logTS+":No internet\n\n");
                        osw.flush();
                        osw.close();
                        fos.close();
                        Log.d("onNotificationPosted", "Check Network:write logfile Finished:");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Log.d("onNotificationPosted", "Check network:write logfile error:" + ex.toString());

                    }
                }
                ///check internet connect end

                /// api start
                OkHttpClient client = new OkHttpClient();

                String getUrl = "https://xxx.xxxx/datahub/createupireference?text="+text+"&amount="+trxnAmount;
                //Log.d("onNotificationPosted","getUrl "+getUrl);
                Request request = new Request.Builder()
                        .url(getUrl)
                        .build();
                Log.d("onNotificationPosted", "client.newCall");

                String finalTextLogfile = text;
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        call.cancel();
                        Log.d("onNotificationPosted", "call.cancel() " + e.toString());
                        countFailedApiCall++;

                        ///write error details start

                        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
                        File directory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                        String filename = dateFormat.format(new Date());

                        Log.d("onNotificationPosted", "call.cancel() write log location"+directory);

                        File txtFile = new File(directory, "ErrorLog_"+ filename +".txt");
                        Log.d("onNotificationPosted", "call.cancel() write log file");

                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(txtFile,true);
                            OutputStreamWriter osw = new OutputStreamWriter(fos);

                            SimpleDateFormat dateFormatTS = new SimpleDateFormat("yyyyMMdd_HHmmss");
                            String logTS = dateFormatTS.format(new Date());

                            osw.write("Data:"+logTS+":"+finalTextLogfile+":ERROR:"+e.toString()+"\n\n");
                            osw.flush();
                            osw.close();
                            fos.close();
                            Log.d("onNotificationPosted", "call.cancel():write logfile Finished:");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Log.d("onNotificationPosted", "call.cancel():write logfile error:" + ex.toString());

                        }

                        NotificationManager mNotificationManager =   (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(contextWrapper.getApplicationContext(), "i.apps.notifications")
                                .setSmallIcon(R.drawable.notification_icon)
                                .setContentText("New sms")
                                .setAutoCancel(true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setOngoing(true);

                        builder.setContentText(countNotificationSms+" Payments received "+countSuccessApiCall+" success "+countFailedApiCall+" Failed");
                        mNotificationManager.notify(1,builder.build());

                        /// write error details end
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try {
                            final String myResponse = response.body().string();
                            Log.d("onNotificationPosted", "Api Resp received");//okhttp allow once to fetch response

                            JSONObject json = new JSONObject(myResponse);

                            //Log.d("onNotificationPosted","resp:"+json.get("success")+"Full Res"+json);
                            try {
                                respMessage = json.get("message").toString();
                            } catch (Exception ex) {
                                respMessage = "No resp Message";
                            }

                            if (json.get("success").toString().equals("1")) {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {

                                        //do stuff like remove view etc
                                        //FloatingViewService.tvAutoFillText.setText("Logged text msg in db");
                                    }
                                });
                                cancelNotification(sbn.getKey());
                                countSuccessApiCall++;
                            } else {
                                countFailedApiCall++;
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //do stuff like remove view etc
                                        //FloatingViewService.tvAutoFillText.setText("Failed to log text msg in db");
                                        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
                                        File directory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                                        String filename = dateFormat.format(new Date());

                                        Log.d("onNotificationPosted", "call.cancel() write log location"+directory);

                                        File txtFile = new File(directory, "ErrorLog_"+ filename +".txt");
                                        Log.d("onNotificationPosted", "call.cancel() write log file");

                                        FileOutputStream fos = null;
                                        try {
                                            fos = new FileOutputStream(txtFile,true);
                                            OutputStreamWriter osw = new OutputStreamWriter(fos);

                                            SimpleDateFormat dateFormatTS = new SimpleDateFormat("yyyyMMdd_HHmmss");
                                            String logTS = dateFormatTS.format(new Date());

                                            osw.write("Data:"+logTS+":"+finalTextLogfile+":ERROR:No success API call\n\n");
                                            osw.flush();
                                            osw.close();
                                            fos.close();
                                            Log.d("onNotificationPosted", "call.cancel():write logfile Finished:");
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            Log.d("onNotificationPosted", "call.cancel():write logfile error:" + ex.toString());

                                        }
                                    }
                                });
                            }

                        } catch (Exception e) {
                            Log.d("onNotificationPosted", "Okhttp call failed " + e.toString());
                            ///FloatingViewService.tvAutoFillText.setText("Error occurred while calling api to log msg text");
                            countFailedApiCall++;

                            ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
                            File directory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                            String filename = dateFormat.format(new Date());

                            Log.d("onNotificationPosted", "call.cancel() write log location"+directory);

                            File txtFile = new File(directory, "ErrorLog_"+ filename +".txt");
                            Log.d("onNotificationPosted", "call.cancel() write log file");

                            FileOutputStream fos = null;
                            try {
                                fos = new FileOutputStream(txtFile,true);
                                OutputStreamWriter osw = new OutputStreamWriter(fos);

                                SimpleDateFormat dateFormatTS = new SimpleDateFormat("yyyyMMdd_HHmmss");
                                String logTS = dateFormatTS.format(new Date());

                                osw.write("Data:"+logTS+":"+finalTextLogfile+":ERROR:Failed Parse resp body:"+e.toString()+"\n\n");
                                osw.flush();
                                osw.close();
                                fos.close();
                                Log.d("onNotificationPosted", "call.cancel():write logfile Finished:");
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                Log.d("onNotificationPosted", "call.cancel():write logfile error:" + ex.toString());

                            }
                        }

                        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());

                        NotificationManager mNotificationManager =   (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(contextWrapper.getApplicationContext(), "i.apps.notifications")
                                .setSmallIcon(R.drawable.notification_icon)
                                .setContentText("New sms")
                                .setAutoCancel(true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setOngoing(true);
                        builder.setContentText(countNotificationSms+" Payments received "+countSuccessApiCall+" success "+countFailedApiCall+" Failed");
                        mNotificationManager.notify(1,builder.build());

                    }
                });
                /// api end

//                NotificationManager mNotificationManager =   (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "i.apps.notifications")
//                        .setSmallIcon(R.drawable.notification_icon)
//                        .setContentText("New sms")
//                        .setAutoCancel(true)
//                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                        .setOngoing(true);
//
//                builder.setContentText(countNotificationSms+" Payments processed "+countFailedApiCall+" Failed");
//                mNotificationManager.notify(1,builder.build());

            }

            intent.putExtra("Notification Code", notificationCode);
            sendBroadcast(intent);
        }
    }

    private void ensureCollectorRunning() {
        ComponentName collectorComponent = new ComponentName(this, /*NotificationListenerService Inheritance*/ NotificationListenerExampleService.class);
        Log.v(TAG, "ensureCollectorRunning collectorComponent: " + collectorComponent);
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean collectorRunning = false;
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices == null ) {
            Log.w(TAG, "ensureCollectorRunning() runningServices is NULL");
            return;
        }
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service.service.equals(collectorComponent)) {
                Log.w(TAG, "ensureCollectorRunning service - pid: " + service.pid + ", currentPID: " + Process.myPid() + ", clientPackage: " + service.clientPackage + ", clientCount: " + service.clientCount
                        + ", clientLabel: " + ((service.clientLabel == 0) ? "0" : "(" + getResources().getString(service.clientLabel) + ")"));
                if (service.pid == Process.myPid() /*&& service.clientCount > 0 && !TextUtils.isEmpty(service.clientPackage)*/) {
                    collectorRunning = true;
                }
            }
        }
        if (collectorRunning) {
            Log.d(TAG, "ensureCollectorRunning: collector is running");
            return;
        }
        Log.d(TAG, "ensureCollectorRunning: collector not running, reviving...");
        toggleNotificationListenerService();
    }

    private void toggleNotificationListenerService() {
        Log.d(TAG, "toggleNotificationListenerService() called");
        ComponentName thisComponent = new ComponentName(this, /*getClass()*/ NotificationListenerExampleService.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

    }

    private int matchNotificationCode(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        if(packageName.equals(ApplicationPackageNames.FACEBOOK_PACK_NAME)
                || packageName.equals(ApplicationPackageNames.FACEBOOK_MESSENGER_PACK_NAME)){
            return(InterceptedNotificationCode.FACEBOOK_CODE);
        }
//        else if(packageName.equals(ApplicationPackageNames.INSTAGRAM_PACK_NAME)){
//            return(InterceptedNotificationCode.INSTAGRAM_CODE);
//        }
        else if(packageName.equals(ApplicationPackageNames.BHIM_SBI_PACK_NAME)){
            return(InterceptedNotificationCode.BHIM_CODE);
        }
        else if(packageName.equals(ApplicationPackageNames.SMS_PACK_NAME)){
            return(InterceptedNotificationCode.SMS_CODE);
        }
        else if(packageName.equals(ApplicationPackageNames.WHATSAPP_PACK_NAME)){
            return(InterceptedNotificationCode.WHATSAPP_CODE);
        }
        else{
            return(InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE);
        }
    }

    public boolean isConnected() {
        boolean connected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo nInfo = cm.getActiveNetworkInfo();
            connected = nInfo != null && nInfo.isAvailable() && nInfo.isConnected();
            return connected;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            Log.d(TAG, "check connectivity error"+e.getMessage());
        }
        return connected;
    }
}
