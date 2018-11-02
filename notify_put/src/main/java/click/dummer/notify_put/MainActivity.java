package click.dummer.notify_put;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private TextView txtView;
    private EditText hostName;
    private EditText portEdit;
    private EditText pass;

    private NotificationReceiver nReceiver;
    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtView = (TextView) findViewById(R.id.textView);
        nReceiver = new NotificationReceiver();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        hostName = (EditText) findViewById(R.id.hostEdit);
        portEdit = (EditText) findViewById(R.id.portEdit);
        pass = (EditText) findViewById(R.id.pass);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Set<String> packs = NotificationManagerCompat.getEnabledListenerPackages(getApplicationContext());
        boolean readNotiPermissions = packs.contains(getPackageName());
        if (readNotiPermissions == false) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            alertDialogBuilder.setMessage(getString(R.string.sorry, getString(R.string.app_name)));
            alertDialogBuilder.show();
        }

        IntentFilter filter = new IntentFilter("click.dummer.notify_put.NOTIFICATION_LISTENER");
        registerReceiver(nReceiver, filter);

        String host = "";
        String port = "";
        String passw = "";
        if (mPreferences.contains("hostname")) {
            host = mPreferences.getString("hostname", "ugly.example.de");
        }
        if (mPreferences.contains("pass")) {
            passw = mPreferences.getString("pass", passw);
        }
        if (mPreferences.contains("port")) {
            port = mPreferences.getString("port", port);
        }
        hostName.setText(host.equals("") ? "ugly.example.de" : host);
        portEdit.setText(port.equals("") ? "58000" : port);
        pass.setText(passw.equals("") ? "abc123" : passw);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(nReceiver);

        String host = hostName.getText().toString();
        String port = portEdit.getText().toString();
        String passw = pass.getText().toString();
        mPreferences.edit().putString("hostname", host).apply();
        mPreferences.edit().putString("port", port).apply();
        mPreferences.edit().putString("pass", passw).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void buttonClicked(View v) {
        String host = hostName.getText().toString();
        String port = portEdit.getText().toString();
        String passw = pass.getText().toString();
        mPreferences.edit().putString("hostname", host).apply();
        mPreferences.edit().putString("port", port).apply();
        mPreferences.edit().putString("pass", passw).apply();

        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setContentTitle("[myip]");
        ncomp.setContentText("I am the Text from " + getString(R.string.app_name));
        ncomp.setTicker("I am the Ticker");
        ncomp.setSmallIcon(R.mipmap.ic_launcher);
        ncomp.setAutoCancel(true);
        nManager.notify((int)System.currentTimeMillis(), ncomp.build());
    }

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String temp = intent.getStringExtra("notification_event") + ": " + txtView.getText();
            txtView.setText(temp);
        }
    }
}

