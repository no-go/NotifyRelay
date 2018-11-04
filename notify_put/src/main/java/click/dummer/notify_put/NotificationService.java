package click.dummer.notify_put;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NotificationService extends NotificationListenerService {
    public static final String SPLITTOKEN = " || ";
    public static final String DATETIME_FORMAT = "ssmmHH dd.MM.";
    public static final byte[] INIVECTOR = "3262737X857900719147446620464".getBytes(StandardCharsets.US_ASCII);
    private String TAG = this.getClass().getSimpleName();
    private SharedPreferences mPreferences;

    private String lastPost = "";
    private String lastTitle = "";

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
        Intent i = new Intent("click.dummer.notify_put.NOTIFICATION_LISTENER");
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Notification noti = sbn.getNotification();
        Bundle extras = noti.extras;
        String title = extras.getString(Notification.EXTRA_TITLE);
        String pack = sbn.getPackageName();
        String msg = (String) noti.tickerText;
        String msg2 = extras.getString(Notification.EXTRA_TEXT);
        String msg3 = null;
        String msg4 = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            msg3 = extras.getString(Notification.EXTRA_BIG_TEXT);
        }

        try {
            msg4 = extras.getCharSequence("android.text").toString();
            Log.d(TAG, "title "+title);
            Log.d(TAG, "pack " + pack);
            // Log.d(TAG, "pack " + getPackageName());
            Log.d(TAG, "ticker " +msg);
            Log.d(TAG, "text "+msg2);
            Log.d(TAG, "big.text "+msg3);
            Log.d(TAG, "android.text "+msg4);
        } catch (Exception e) {}

        if (msg4 != null && msg4.length()>0) msg = msg4;
        if (msg2 != null && msg2.length()>0) msg = msg2;
        if (msg3 != null && msg3.length()>0) msg = msg3;

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
        if (title.equals(lastTitle) ) msg = msg.replaceFirst(title, "");

        lastPost  = msg;
        lastTitle = title;

        sendNetBroadcast(
                (formatOut.format(new Date()) +
                        SPLITTOKEN +
                        pack + " " +
                        title + ": " +
                        msg
                ).trim()
        );
        i.putExtra("notification_event", msg);
        sendBroadcast(i);
    }

    private byte xor(byte a, byte b, byte c) {
        int one = (int)a;
        int two = (int)b;
        int thr = (int)c;
        int xor = one ^ two ^ thr;
        return (byte)(0xff & xor);
    }

    private String codeIt(String msg) {
        StringBuilder sb = new StringBuilder();
        InputStream ins = getResources().openRawResource(R.raw.phrase);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] pass = mPreferences.getString("pass", "123abc").trim().getBytes(StandardCharsets.UTF_8);

        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = ins.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            ins.close();
        } catch (IOException e) {}
        String[] phases = outputStream.toString().split("~\n");
        Log.v(TAG, String.valueOf(phases.length));
        ArrayList<String[]> phrases = new ArrayList<>();
        for (String phrasi : phases) {
            //Log.v(TAG, phrasi);
            phrases.add(phrasi.split("\n"));
        }

        byte[] data = msg.getBytes(StandardCharsets.US_ASCII);
        byte[] coded = new byte[data.length];
        for (int i=0; i<data.length; i++) {
            if (i<INIVECTOR.length) {
                coded[i] = xor(data[i], pass[i % pass.length], INIVECTOR[i]);
            } else {
                coded[i] = xor(data[i], pass[i % pass.length], data[i-INIVECTOR.length]);
            }
            sb.append(String.format("%02X", coded[i]));
        }

        String hexToText = sb.toString();
        Log.v(TAG, hexToText);
        sb = new StringBuilder();
        int ph;
        String dummy;
        char str;
        for (int i=0; i < hexToText.length(); i++) {
            ph = i%phrases.size();
            str = hexToText.charAt(i);
            if (str == '0') {
                dummy = phrases.get(ph)[0];
            } else if (str == '1') {
                dummy = phrases.get(ph)[1];
            } else if (str == '2') {
                dummy = phrases.get(ph)[2];
            } else if (str == '3') {
                dummy = phrases.get(ph)[3];
            } else if (str == '4') {
                dummy = phrases.get(ph)[4];
            } else if (str == '5') {
                dummy = phrases.get(ph)[5];
            } else if (str == '6') {
                dummy = phrases.get(ph)[6];
            } else if (str == '7') {
                dummy = phrases.get(ph)[7];
            } else if (str == '8') {
                dummy = phrases.get(ph)[8];
            } else if (str == '9') {
                dummy = phrases.get(ph)[9];
            } else if (str == 'A') {
                dummy = phrases.get(ph)[10];
            } else if (str == 'B') {
                dummy = phrases.get(ph)[11];
            } else if (str == 'C') {
                dummy = phrases.get(ph)[12];
            } else if (str == 'D') {
                dummy = phrases.get(ph)[13];
            } else if (str == 'E') {
                dummy = phrases.get(ph)[14];
            } else {
                dummy = phrases.get(ph)[15];
            }

            if (ph == 0) {
                char first = Character.toUpperCase(dummy.charAt(0));
                dummy = first + dummy.substring(1);
            }
            if (ph == phrases.size()-1) {
                dummy += ". ";
            } else {
                dummy += " ";
            }
            sb.append(dummy);
        }
        return sb.toString();
    }

    public void sendNetBroadcast(String messageStr) {

        // Hack - should be done using an async task !!
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Log.d(TAG, "uncoded: " + messageStr);
        messageStr = codeIt(messageStr);
        Log.d(TAG, "coded: " + messageStr);

        try {

            InetAddress address = InetAddress.getByName(
                    mPreferences.getString("hostname", "ugly.example.de").trim()
            );
            int port = Integer.parseInt(mPreferences.getString("port", "58000").trim());

            Socket s = new Socket(address, port);
            PrintWriter outp = new PrintWriter(s.getOutputStream(), true);
            outp.println(messageStr);
            s.close();

            /* UDP
            //Open a random port to send the package
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] sendData = messageStr.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(sendPacket);
            */

            Log.i(TAG, getClass().getName() + "packets sent to: " + address.getHostAddress());
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
        }
    }
}
