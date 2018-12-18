package click.dummer.notify_to_jabber;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.net.SSLCertificateSocketFactory;
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

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class NotificationService extends NotificationListenerService {
    public static final String DATETIME_FORMAT = "dd.MM. HH:mm";
    private String TAG = this.getClass().getSimpleName();
    private SharedPreferences mPreferences;

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

            // --------------------------------------------------------------------jabber ---------
            AbstractXMPPConnection connection = new XMPPTCPConnection(
                    fromJID.substring(0, fromJID.lastIndexOf("@")),
                    pass,
                    fromJID.substring(fromJID.lastIndexOf("@")+1)
            );

            if (!connection.isConnected()) {
                connection.connect().login();
            }

            if (connection.isConnected()) {
                ChatManager chatManager = ChatManager.getInstanceFor(connection);
                Chat chat = chatManager.createChat(toJID);
                chat.sendMessage("["+pack+"] " + time + "\n" + title + ": " + message);
                //connection.disconnect();
            }

            // --------------------------------------------------------------------gotify ---------

            String gotifyUrl = "";
            String appToken = "";

            if (mPreferences.contains("gotifyUrl")) {
                gotifyUrl = mPreferences.getString("gotifyUrl", gotifyUrl);
            }
            if (mPreferences.contains("appToken")) {
                appToken = mPreferences.getString("appToken", appToken);
            }


            // ---------------------------------ssl handling


            // START get cert with ignoring all ssl errors

            Certificate certificate = null;
            URL destinationURL = new URL(gotifyUrl);
            HttpURLConnection conn = (HttpURLConnection) destinationURL.openConnection();
            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                httpsConn.setSSLSocketFactory(SSLCertificateSocketFactory.getInsecure(0, null));
                httpsConn.setHostnameVerifier(new MyHostnameVerifier(destinationURL));
            }
            conn.connect();
            Certificate[] certs = ( (HttpsURLConnection) conn).getServerCertificates();
            Log.d(TAG,"nb = " + certs.length);
            for (Certificate cert : certs) {
                Log.d(TAG, cert.toString());
                if (cert instanceof X509Certificate) {
                    certificate = cert;
                    break;
                }
            }

            // END cert with ignoring all ssl errors: now we have to compare the signature as alternative way

            if (certificate == null) return;
            StringBuilder sb = new StringBuilder();
            for (byte b : ((X509Certificate) certificate).getSignature()) {
                sb.append(String.format("%02X:", b));
            }
            String sig = sb.toString();
            sig = sig.substring(0, sig.length()-1);
            Log.d(TAG, sig);


            // @todo store signature, compare it, add it to a config or main activity

            // @todo alternative way without all that stuff (not ignoring SSL fail)


            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", certificate);

            // Create a TrustManager that trusts the CAs in our KeyStore.
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
            trustManagerFactory.init(keyStore);

            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            X509TrustManager x509TrustManager = (X509TrustManager) trustManagers[0];


            // Create an SSLSocketFactory that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{x509TrustManager}, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            //create Okhttp client
            OkHttpClient client = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory,x509TrustManager)
                    .hostnameVerifier(new MyHostnameVerifier(destinationURL))
                    .build();




            if (!gotifyUrl.equals("") && !appToken.equals("")) {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(gotifyUrl)
                        .client(client) // ssl handling add on
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                GotifyMessageService gms = retrofit.create(GotifyMessageService.class);

                GotifyMessage gotifyMessage = new GotifyMessage(
                        4,
                        pack,
                        title + ": " + message
                );

                Call<GotifyMessage> call = gms.createMessage(appToken, gotifyMessage);
                call.execute();
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    private class MyHostnameVerifier implements HostnameVerifier {
        private URL shouldHost;

        public MyHostnameVerifier(URL shouldHost) {
            this.shouldHost = shouldHost;
        }

        @Override
        public boolean verify(String hostname, SSLSession sslSession) {
            return shouldHost.getHost().equals(hostname);
        }
    }
}
