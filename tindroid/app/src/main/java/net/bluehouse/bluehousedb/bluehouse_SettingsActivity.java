package net.bluehouse.bluehousedb;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import co.tinode.tindroid.LoginActivity;
import co.tinode.tindroid.R;
import co.tinode.tindroid.UiUtils;
import co.tinode.tindroid.bluehouse_MainActivity;

public class bluehouse_SettingsActivity extends AppCompatActivity {

    static AppCompatActivity activity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        final ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowCustomEnabled(true);
            bar.setHomeButtonEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setHomeAsUpIndicator(R.drawable.back_toolbar);
            bar.setTitle("Settings");
        }

        activity = (AppCompatActivity)this;

    }



    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.bluehouse_preferences, rootKey);

            Preference signout = (Preference) findPreference("Signoutfrombluehouse");
            signout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {

                    UiUtils.doLogout(activity);
                    bhdb_SharedPrefManager.getInstance(activity.getApplicationContext()).logout();

                    final Intent launch = new Intent(activity.getApplicationContext(), bluehouse_MainActivity.class);
                    startActivity(launch);
                    activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    activity.finish();
                    return true;
                }
            });
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(getApplicationContext(), bluehouse_MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}