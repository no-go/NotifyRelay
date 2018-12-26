package click.dummer.notify_to_jabber;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import okhttp3.OkHttpClient;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationService extends NotificationListenerService {
    public static final String DATETIME_FORMAT = "dd.MM. HH:mm";
    public static final String ACTION_INCOMING_MSG = "click.dummer.notify_to_jabber.INCOMING_MSG";
    public static final String ACTION_NEW_FINGERPRINT = "click.dummer.notify_to_jabber.NEW_FINGERPRINT";
    private static String TAG = "NotificationService";
    private static SharedPreferences mPreferences;

    private String lastPost = "";

    public static class GotifyMessage {
        public final int priority;
        public final String title;
        public final String message;

        public GotifyMessage(int priority, String title, String message) {
            this.priority = priority;
            this.title = title;
            this.message = message;
        }
    }

    public interface GotifyMessageService {
        @POST("/message")
        Call<GotifyMessage> createMessage(
                @Query("token") String apptoken,
                @Body GotifyMessage gotifyMessage
        );
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onNotificationRemoved(sbn);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        SimpleDateFormat formatOut = new SimpleDateFormat(DATETIME_FORMAT, Locale.ENGLISH);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Notification noti = sbn.getNotification();
        Bundle extras = noti.extras;
        String title = extras.getString(Notification.EXTRA_TITLE);
        String pack = sbn.getPackageName();
        String msg = (String) noti.tickerText;
        Object obj = extras.get(Notification.EXTRA_TEXT);
        String msg2 = null;
        Drawable icon = null;

        if (obj != null) {
            msg2 = obj.toString();
        }
        String msg3 = null;
        String msg4 = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            msg3 = extras.getString(Notification.EXTRA_BIG_TEXT);
        }

        try {
            SpannableString sp = (SpannableString) extras.get("android.text");
            Log.d(TAG, "title "+title);
            Log.d(TAG, "pack " + pack);
            Log.d(TAG, "ticker " +msg);
            Log.d(TAG, "text "+msg2);
            Log.d(TAG, "big.text "+msg3);
            if (sp != null) {
                msg4 = sp.toString();
            }
            Log.d(TAG, "android.text "+msg4);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        if (msg4 != null && msg4.length()>0) msg = msg4;
        if (msg2 != null && msg2.length()>0) msg = msg2;
        if (msg3 != null && msg3.length()>0) msg = msg3;

        String name="NULL";
        try {
            ApplicationInfo appi = this.getPackageManager().getApplicationInfo(pack, 0);
            icon = getPackageManager().getApplicationIcon(appi);
            pack = getPackageManager().getApplicationLabel(appi).toString();

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        // catch not normal message .-----------------------------
        if (!sbn.isClearable()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (sbn.isGroup()) {
                Log.d(TAG, "is group");
                //return;
            }
        }
        if (msg == null) return;
        if (msg.equals(lastPost) ) return;

        lastPost  = msg;

        //sendNetBroadcast(title, msg, pack, formatOut.format(new Date()), icon);
        Intent i = new Intent(ACTION_INCOMING_MSG);
        i.putExtra("notification_event", msg);
        sendBroadcast(i);
        new SendJabberTask().execute(title, msg, pack, formatOut.format(new Date()));
        sendGotify(title, msg, pack, formatOut.format(new Date()));
    }

    private void sendGotify(String... strings) {

        // Hack - should be done using an async task !!
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        String title = strings[0];
        String message = strings[1];
        String pack = strings[2];
        String time = strings[3];

        String gotifyUrl = "";
        String appToken = "";
        String sslFingerprint = "";

        if (mPreferences.contains("gotifyUrl")) {
            gotifyUrl = mPreferences.getString("gotifyUrl", gotifyUrl);
        }
        if (mPreferences.contains("appToken")) {
            appToken = mPreferences.getString("appToken", appToken);
        }
        if (mPreferences.contains("sslFingerprint")) {
            sslFingerprint = mPreferences.getString("sslFingerprint", sslFingerprint);
        }

        if (!gotifyUrl.equals("") && !appToken.equals("")) {
            Retrofit retrofit = null;
            try {
                URL destinationURL = new URL(gotifyUrl);
                SslHelper.Basics basics = SslHelper.CertCheck(destinationURL);

                if (basics.isHttps) {
                    if (basics.domain == null) {
                        Log.w(TAG, "gotify server cert: domain verify failed");
                        return;
                    }
                    if (basics.certificate == null) {
                        Log.w(TAG, "gotify server cert: no X509Certificate found");
                        // not a SSL certificate (and not self signed)
                        retrofit = new Retrofit.Builder()
                                .baseUrl(gotifyUrl)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();
                    } else {

                        if (sslFingerprint.equals("")) {
                            Log.w(TAG, "gotify server cert: add fingerprint");
                            Intent i = new Intent(ACTION_NEW_FINGERPRINT);
                            i.putExtra("new", basics.fingerprint);
                            getBaseContext().sendBroadcast(i);
                        } else {
                            if (!sslFingerprint.equals(basics.fingerprint)) {
                                Log.w(TAG, "gotify server cert: fingerprint changed");
                                Intent i = new Intent(ACTION_NEW_FINGERPRINT);
                                i.putExtra("old", sslFingerprint);
                                i.putExtra("new", basics.fingerprint);
                                getBaseContext().sendBroadcast(i);
                                return;
                            }
                        }

                        OkHttpClient client = SslHelper.createOkClient(destinationURL, basics.certificate);
                        if (client == null) {
                            Log.w(TAG, "gotify server: building OK-Client with SSL cert failed");
                            return;
                        }

                        retrofit = new Retrofit.Builder()
                                .baseUrl(gotifyUrl)
                                .client(client) // ssl handling add on
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();
                    }

                } else {
                    retrofit = new Retrofit.Builder()
                            .baseUrl(gotifyUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
                GotifyMessageService gms = retrofit.create(GotifyMessageService.class);

                GotifyMessage gotifyMessage = new GotifyMessage(
                        4,
                        pack,
                        title + ": " + message
                );

                Call<GotifyMessage> call = gms.createMessage(appToken, gotifyMessage);
                call.execute();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    private static class SendJabberTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String title = strings[0];
            String message = strings[1];
            String pack = strings[2];
            String time = strings[3];

            String fromJID = "";
            String toJID = "";
            String pass = "";
            if (mPreferences.contains("fromJID")) {
                fromJID = mPreferences.getString("fromJID", fromJID);
            }
            if (mPreferences.contains("toJID")) {
                toJID = mPreferences.getString("toJID", toJID);
            }
            if (mPreferences.contains("pass")) {
                pass = mPreferences.getString("pass", pass);
            }
            if (fromJID.equals("") || toJID.equals("") || pass.equals("")) return null;

            AbstractXMPPConnection connection = new XMPPTCPConnection(
                    fromJID.substring(0, fromJID.lastIndexOf("@")),
                    pass,
                    fromJID.substring(fromJID.lastIndexOf("@")+1)
            );
            try {
                if (!connection.isConnected()) {
                    connection.connect().login();
                }

                if (connection.isConnected()) {
                    ChatManager chatManager = ChatManager.getInstanceFor(connection);
                    Chat chat = chatManager.createChat(toJID);
                    chat.sendMessage("["+pack+"] " + time + "\n" + title + ": " + message);
                    //connection.disconnect();
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: " + e.getMessage());
            }
            return null;
        }
    }
}
