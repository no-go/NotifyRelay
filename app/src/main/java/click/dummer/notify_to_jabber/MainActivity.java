package click.dummer.notify_to_jabber;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private TextView txtView;
    private EditText fromJIDedit;
    private EditText toJIDedit;
    private EditText passEdit;

    private EditText gotifyEdit;
    private EditText gotifyAppToken;
    private EditText gotifyFingerprint;

    private ScrollView scrollView;

    private NotificationReceiver nReceiver;
    private SharedPreferences mPreferences;

    private Menu optionsmenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtView = (TextView) findViewById(R.id.textView);
        nReceiver = new NotificationReceiver();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        fromJIDedit = (EditText) findViewById(R.id.fromJIDedit);
        toJIDedit = (EditText) findViewById(R.id.toJIDedit);
        passEdit = (EditText) findViewById(R.id.passEdit);
        gotifyEdit = (EditText) findViewById(R.id.gotifyEdit);
        gotifyAppToken = (EditText) findViewById(R.id.gotifyAppToken);
        gotifyFingerprint = (EditText) findViewById(R.id.gotifyFingerprint);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        scrollView = (ScrollView) findViewById(R.id.primaryContent);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_UP);
            }
        });
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
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    startActivity(intent);
                }
            });
            alertDialogBuilder.setMessage(getString(R.string.sorry, getString(R.string.app_name)));
            alertDialogBuilder.show();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationService.ACTION_NEW_FINGERPRINT);
        filter.addAction(NotificationService.ACTION_INCOMING_MSG);
        registerReceiver(nReceiver, filter);

        String fromJID = "";
        String toJID = "";
        String pass = "";

        String gotifyUrl = "";
        String appToken = "";
        String sslFingerprint = "";

        if (mPreferences.contains("gotifyUrl")) {
            gotifyUrl = mPreferences.getString("gotifyUrl", gotifyUrl);
        }
        if (mPreferences.contains("appToken")) {
            appToken = mPreferences.getString("appToken", appToken);
        }

        if (mPreferences.contains("fromJID")) {
            fromJID = mPreferences.getString("fromJID", fromJID);
        }
        if (mPreferences.contains("toJID")) {
            toJID = mPreferences.getString("toJID", toJID);
        }
        if (mPreferences.contains("pass")) {
            pass = mPreferences.getString("pass", pass);
        }
        if (mPreferences.contains("sslFingerprint")) {
            sslFingerprint = mPreferences.getString("sslFingerprint", sslFingerprint);
        }
        fromJIDedit.setText(fromJID);
        toJIDedit.setText(toJID);
        passEdit.setText(pass);

        gotifyEdit.setText(gotifyUrl);
        gotifyAppToken.setText(appToken);
        gotifyFingerprint.setText(sslFingerprint);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(nReceiver);

        String fromJID = fromJIDedit.getText().toString();
        String toJID = toJIDedit.getText().toString();
        String pass = passEdit.getText().toString();
        String gotifyUrl = gotifyEdit.getText().toString();
        String appToken = gotifyAppToken.getText().toString();
        String sslFingerprint = gotifyFingerprint.getText().toString().trim();

        mPreferences.edit().putString("fromJID", fromJID).apply();
        mPreferences.edit().putString("toJID", toJID).apply();
        mPreferences.edit().putString("pass", pass).apply();
        mPreferences.edit().putString("gotifyUrl", gotifyUrl).apply();
        mPreferences.edit().putString("appToken", appToken).apply();
        mPreferences.edit().putString("sslFingerprint", sslFingerprint).apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);
        optionsmenu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        optionsmenu.findItem(R.id.action_sourcename).setChecked(mPreferences.getBoolean("with_source", true));

        int timetype = mPreferences.getInt("with_time", 0);
        switch (timetype) {
            case 1:
                optionsmenu.findItem(R.id.action_time).setChecked(true);
                optionsmenu.findItem(R.id.action_time1).setChecked(true);
                optionsmenu.setGroupEnabled(R.id.timeoptions, true);
                break;
            case 2:
                optionsmenu.findItem(R.id.action_time).setChecked(true);
                optionsmenu.findItem(R.id.action_time2).setChecked(true);
                optionsmenu.setGroupEnabled(R.id.timeoptions, true);
                break;
            case 3:
                optionsmenu.findItem(R.id.action_time).setChecked(true);
                optionsmenu.findItem(R.id.action_time3).setChecked(true);
                optionsmenu.setGroupEnabled(R.id.timeoptions, true);
                break;
            case 4:
                optionsmenu.findItem(R.id.action_time).setChecked(true);
                optionsmenu.findItem(R.id.action_time4).setChecked(true);
                optionsmenu.setGroupEnabled(R.id.timeoptions, true);
                break;
            default:
                optionsmenu.findItem(R.id.action_time).setChecked(false);
                optionsmenu.setGroupEnabled(R.id.timeoptions, false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sms) {
            Intent intentsmspref = new Intent(MainActivity.this, PreferencesActivity.class);
            startActivity(intentsmspref);
            return true;
        } else if (id == R.id.action_sourcename) {
            if (item.isChecked()) {
                item.setChecked(false);
                mPreferences.edit().putBoolean("with_source", false).apply();
            } else {
                item.setChecked(true);
                mPreferences.edit().putBoolean("with_source", true).apply();
            }
        } else if (id == R.id.action_time) {
            if (item.isChecked()) {
                item.setChecked(false);
                optionsmenu.setGroupEnabled(R.id.timeoptions, false);
                mPreferences.edit().putInt("with_time", 0).apply();
            } else {
                item.setChecked(true);
                optionsmenu.setGroupEnabled(R.id.timeoptions, true);
                int timetype = 0;
                if (optionsmenu.findItem(R.id.action_time1).isChecked()) timetype=1;
                if (optionsmenu.findItem(R.id.action_time2).isChecked()) timetype=2;
                if (optionsmenu.findItem(R.id.action_time3).isChecked()) timetype=3;
                if (optionsmenu.findItem(R.id.action_time4).isChecked()) timetype=4;
                mPreferences.edit().putInt("with_time", timetype).apply();
            }
        } else if (id == R.id.action_time1) {
            if (item.isChecked() == false) {
                item.setChecked(true);
                mPreferences.edit().putInt("with_time", 1).apply();
            }
        } else if (id == R.id.action_time2) {
            if (item.isChecked() == false) {
                item.setChecked(true);
                mPreferences.edit().putInt("with_time", 2).apply();
            }
        } else if (id == R.id.action_time3) {
            if (item.isChecked() == false) {
                item.setChecked(true);
                mPreferences.edit().putInt("with_time", 3).apply();
            }
        } else if (id == R.id.action_time4) {
            if (item.isChecked() == false) {
                item.setChecked(true);
                mPreferences.edit().putInt("with_time", 4).apply();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void buttonClicked(View v) {
        String fromJID = fromJIDedit.getText().toString();
        String toJID = toJIDedit.getText().toString();
        String pass = passEdit.getText().toString();
        String gotifyUrl = gotifyEdit.getText().toString();
        String appToken = gotifyAppToken.getText().toString();
        String sslFingerprint = gotifyFingerprint.getText().toString().trim();
        mPreferences.edit().putString("fromJID", fromJID).apply();
        mPreferences.edit().putString("toJID", toJID).apply();
        mPreferences.edit().putString("pass", pass).apply();
        mPreferences.edit().putString("gotifyUrl", gotifyUrl).apply();
        mPreferences.edit().putString("appToken", appToken).apply();
        mPreferences.edit().putString("sslFingerprint", sslFingerprint).apply();

        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setContentTitle("Test Title");
        ncomp.setContentText("I am a test message.");
        ncomp.setTicker("I am a test ticker");
        ncomp.setSmallIcon(R.mipmap.ic_launcher);
        ncomp.setAutoCancel(true);
        nManager.notify((int)System.currentTimeMillis(), ncomp.build());
    }

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(NotificationService.ACTION_INCOMING_MSG)) {
                String temp = intent.getStringExtra("notification_event") + ": " + txtView.getText();
                txtView.setText(temp);

            } else if (intent.getAction().equals(NotificationService.ACTION_NEW_FINGERPRINT)) {
                String sslFingerprint = intent.getStringExtra("new");
                if (gotifyFingerprint.getText().toString().trim().equals("")) {
                    gotifyFingerprint.setText(sslFingerprint);
                    mPreferences.edit().putString("sslFingerprint", sslFingerprint).apply();
                    txtView.setText(R.string.fingerprint_stored);
                } else {
                    txtView.setText(getString(R.string.fingerprint_may_bad, sslFingerprint));
                }
            }
        }
    }
}

