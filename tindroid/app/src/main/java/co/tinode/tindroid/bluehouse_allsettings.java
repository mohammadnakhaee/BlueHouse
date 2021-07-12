package co.tinode.tindroid;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

public class bluehouse_allsettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluehouse_allsettings);

        setContentView(R.layout.activity_contacts);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        final ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowCustomEnabled(true);
            bar.setHomeButtonEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setHomeAsUpIndicator(R.drawable.back_toolbar);
            bar.setTitle("Settings");
        }


        Fragment fragment = new ChatsFragment();
        /*getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentFragment, fragment, FRAGMENT_CHATLIST)
                .setPrimaryNavigationFragment(fragment)
                .commit();*/

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(getApplicationContext(), bluehouse_MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}