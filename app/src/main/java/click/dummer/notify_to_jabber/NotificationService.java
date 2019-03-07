package click.dummer.notify_to_jabber;

import android.app.Notification;
import android.app.PendingIntent;
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
import android.telephony.SmsManager;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class NotificationService extends NotificationListenerService {
    //public static final String DATETIME_FORMAT = "dd.MM. HH:mm";
    public static final int DEFAULTPRIO = 4;
    public static final String ACTION_INCOMING_MSG = "click.dummer.notify_to_jabber.INCOMING_MSG";
    public static final String ACTION_NEW_FINGERPRINT = "click.dummer.notify_to_jabber.NEW_FINGERPRINT";
    public static final String SPECIAL_MUSIC = "com.google.android.music org.lineageos.eleven com.spotify.music deezer.android.app deezer.android.tv";
    private static String TAG = "NotificationService";
    private static SharedPreferences mPreferences;

    private SmsManager smsManager = SmsManager.getDefault();
    private ArrayList<String> parts;
    private String phone;
    private ArrayList<PendingIntent> sentPIs;
    private ArrayList<PendingIntent> deliveredPIs;

    private ArrayList<String> specialMusicPlayer;

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
        specialMusicPlayer = new ArrayList<>(Arrays.asList(SPECIAL_MUSIC.split(" ")));
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
        SimpleDateFormat formatOut;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Notification noti = sbn.getNotification();
        Bundle extras = noti.extras;
        String title = null;
        String pack = sbn.getPackageName();
        String msg = null;
        String msg1 = (String) noti.tickerText;
        Object obj = extras.get(Notification.EXTRA_TEXT);
        String msg2 = null;
        Drawable icon = null;

        try {
            if (title == null || title == "") {
                SpannableString sp = (SpannableString) extras.get("android.title");
                title = sp.toString();
            }
        } catch (Exception e) {
            title = extras.getString(Notification.EXTRA_TITLE);
        }

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
            Log.d(TAG, "title    "+ title);
            Log.d(TAG, "pack     " + pack);
            Log.d(TAG, "ticker   " + msg1);
            Log.d(TAG, "text     " + msg2);
            Log.d(TAG, "big.text " + msg3);
            if (sp != null) {
                msg4 = sp.toString();
            }
            Log.d(TAG, "android.text " + msg4);
        } catch (Exception e) {}

        msg = msg1; // ticker text is default

        if (msg4 != null && msg4.length()>0) { // android.text (for old androids)
            msg = msg4;
        }
        if (msg2 != null && msg2.length()>0) { // extra text
            msg = msg2;
        }
        if (msg3 != null && msg3.length()>0) { // favourit big text, if exists
            msg = msg3;
        }
        //if (msg != null && msg1 != null && msg.length() < msg1.length()) {
        //    msg = msg1; // reuse ticker text, because it gots more info
        //}

        try {
            ApplicationInfo appi = this.getPackageManager().getApplicationInfo(pack, 0);
            icon = getPackageManager().getApplicationIcon(appi);
            if (specialMusicPlayer.indexOf(pack) < 0) {
                pack = getPackageManager().getApplicationLabel(appi).toString();
            } else {
                pack = "Music";
            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        // catch not normal message -----------------------------
        if (!pack.equals("Music") && !sbn.isClearable()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (pack.equals("Music") && sbn.isGroup()) {
                Log.d(TAG, "is group");
                return;
            }
        }
        if (title == null) title = pack;

        title = title.trim();
        if (title.endsWith(":")) {
            title = title.substring(0, title.lastIndexOf(":"));
        }

        try {
            Log.d(TAG, "title: "+ title);
            Log.d(TAG, "msg: " + msg);
            Log.d(TAG, "app: " + pack);

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        if (msg == null) return;
        if ((title +msg).equals(lastPost) ) return;

        lastPost = title + msg;

        //sendNetBroadcast(title, msg, pack, formatOut.format(new Date()), icon);
        Intent i = new Intent(ACTION_INCOMING_MSG);
        i.putExtra("notification_event", msg);
        sendBroadcast(i);
        String time = "";
        switch (mPreferences.getInt("with_time", 0)) {
            case 1:
                formatOut = new SimpleDateFormat("dd.MM. HH:mm:ss", Locale.ENGLISH);
                time = formatOut.format(new Date());
                break;
            case 2:
                formatOut = new SimpleDateFormat("dd.MM. HH:mm", Locale.ENGLISH);
                time = formatOut.format(new Date());
                break;
            case 3:
                formatOut = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
                time = formatOut.format(new Date());
                break;
            case 4:
                time = Long.toString(new Date().getTime());
                break;
            default:
        }
        if (mPreferences.getBoolean("with_source", true) == false) pack = "";

        // ++++ Jabber
        new SendJabberTask().execute(title, msg, pack, time);

        // ++++ Gotify
        sendGotify(title, msg, pack);

        // ++++ SMS
        phone = mPreferences.getString("phone", "").trim();
        if (mPreferences.getBoolean("sms_forward", false) && phone.equals("") == false) {
            String SENT = "SMS_SENT";
            String DELIVERED = "SMS_DELIVERED";

            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);

            sentPIs = new ArrayList<>();
            deliveredPIs = new ArrayList<>();
            sentPIs.add(sentPI);
            deliveredPIs.add(deliveredPI);



            String message = "";
            if (!time.equals("")) time = time + "\n";

            if (pack.equals("")) {
                if (title.length() > 0 && msg.startsWith(title)) title = "";
                if (time.equals("")) {
                    if (title.equals("")) {
                        message = msg;
                    } else {
                        message = title + ": " + msg;
                    }
                } else {
                    if (title.equals("")) {
                        message = time + msg;
                    } else {
                        message = time + title + ": " + msg;
                    }
                }
            } else {
                if (title.length() > 0 && msg.startsWith(title)) title = "";
                if (time.equals("")) {
                    if (title.equals("")) {
                        message = "["+pack+"] " + msg;
                    } else {
                        message = "["+pack+"] " + title + ": " + msg;
                    }
                } else {
                    if (title.equals("")) {
                        message = "["+pack+"] " + time + msg;
                    } else {
                        message = "["+pack+"] " + time + title + ": " + msg;
                    }
                }
            }


            int limit = mPreferences.getInt("maxchars", 140);
            if (limit>0 && message.length()>limit) {
                message = message.substring(0, limit-1);
            }
            //smsManager.sendTextMessage(phone, null, message, null, null);
            parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(phone, null, parts, sentPIs, deliveredPIs);
        }

    }

    private void sendGotify(String... strings) {

        // Hack - should be done using an async task !!
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        String title = strings[0];
        String message = strings[1];
        String pack = strings[2];

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
                GotifyMessage gotifyMessage;



                if (pack.equals("")) {
                    if (title.length() > 0 && message.startsWith(title)) {
                        message = message.substring(0, title.length()).trim();
                    }
                    gotifyMessage = new GotifyMessage(DEFAULTPRIO, title, message);

                } else {
                    if (title.length() > 0 && message.startsWith(title)) {
                        message = message.substring(0, title.length()).trim();
                    }
                    if (title.length() > 0) {
                        gotifyMessage = new GotifyMessage(DEFAULTPRIO, pack, title + ": " + message);
                    } else {
                        gotifyMessage = new GotifyMessage(DEFAULTPRIO, pack, message);
                    }
                }


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





                    if (!time.equals("")) time = time + "\n";

                    if (pack.equals("")) {
                        if (title.length() > 0 && message.startsWith(title)) title = "";
                        if (time.equals("")) {
                            if (title.equals("")) {
                                chat.sendMessage(message);
                            } else {
                                chat.sendMessage(title + ": " + message);
                            }
                        } else {
                            if (title.equals("")) {
                                chat.sendMessage(time + message);
                            } else {
                                chat.sendMessage(time + title + ": " + message);
                            }
                        }
                    } else {
                        if (title.length() > 0 && message.startsWith(title)) title = "";
                        if (time.equals("")) {
                            if (title.equals("")) {
                                chat.sendMessage("["+pack+"] " + message);
                            } else {
                                chat.sendMessage("["+pack+"] " + title + ": " + message);
                            }
                        } else {
                            if (title.equals("")) {
                                chat.sendMessage("["+pack+"] " + time + message);
                            } else {
                                chat.sendMessage("["+pack+"] " + time + title + ": " + message);
                            }
                        }
                    }





                    //connection.disconnect();
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: " + e.getMessage());
            }
            return null;
        }
    }
}
