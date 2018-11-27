package click.dummer.notify_to_jabber;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationService extends NotificationListenerService {
    public static final String DATETIME_FORMAT = "dd.MM. HH:mm";
    private String TAG = this.getClass().getSimpleName();
    private SharedPreferences mPreferences;

    private String lastPost = "";

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
    public void onNotificationPosted(StatusBarNotification sbn) {
        SimpleDateFormat formatOut = new SimpleDateFormat(DATETIME_FORMAT, Locale.ENGLISH);
        Intent i = new Intent("click.dummer.notify_to_jabber.NOTIFICATION_LISTENER");
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
            msg4 = sp.toString();
            Log.d(TAG, "title "+title);
            Log.d(TAG, "pack " + pack);
            Log.d(TAG, "ticker " +msg);
            Log.d(TAG, "text "+msg2);
            Log.d(TAG, "big.text "+msg3);
            Log.d(TAG, "android.text "+msg4);
        } catch (Exception e) {}

        if (msg4 != null && msg4.length()>0) msg = msg4;
        if (msg2 != null && msg2.length()>0) msg = msg2;
        if (msg3 != null && msg3.length()>0) msg = msg3;

        String name="NULL";
        try
        {
            ApplicationInfo appi = this.getPackageManager().getApplicationInfo(pack, 0);
            icon = getPackageManager().getApplicationIcon(appi);
            pack = getPackageManager().getApplicationLabel(appi).toString();

        } catch (PackageManager.NameNotFoundException e) { }

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

        sendNetBroadcast(title, msg, pack, formatOut.format(new Date()), icon);
        i.putExtra("notification_event", msg);
        sendBroadcast(i);
    }

    public void sendNetBroadcast(String title, String message, String pack, String time, Drawable icon) {

        // Hack - should be done using an async task !!
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
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

            // ------------------------------------------------------------------------------
            AbstractXMPPConnection connection = new XMPPTCPConnection(
                    fromJID.substring(0, fromJID.lastIndexOf("@")),
                    pass,
                    fromJID.substring(fromJID.lastIndexOf("@")+1)
            );
            if (!connection.isConnected()) {
                connection.connect().login();
            }

            ChatManager chatManager = ChatManager.getInstanceFor(connection);
            Chat chat = chatManager.createChat(toJID);
            chat.sendMessage("["+pack+"] " + time + "\n" + title + ": " + message);
            //connection.disconnect();
            // ------------------------------------------------------------------------------

        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }
}
