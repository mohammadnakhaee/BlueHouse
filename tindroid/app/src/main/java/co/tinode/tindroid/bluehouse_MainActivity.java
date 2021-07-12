package co.tinode.tindroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
//import androidx.viewpager.widget.ActionBarActivity;
import android.content.Intent;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Bundle;
import android.os.Handler;
import co.tinode.tindroid.bluehouse_ViewPagerAdapter;  //import the ViewPagerAdapter.java file package here
import co.tinode.tindroid.db.BaseDb;
import me.relex.circleindicator.CircleIndicator;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import net.bluehouse.bluehousedb.bhdb_LoginActivity;
import net.bluehouse.bluehousedb.bhdb_MainActivity;
import net.bluehouse.bluehousedb.bhdb_SharedPrefManager;
import net.bluehouse.bluehousedb.bhdb_User;
import net.bluehouse.bluehousedb.bluehouse_SettingsActivity;

public class bluehouse_MainActivity extends AppCompatActivity {

    ClickableViewPager viewpager;
    PagerAdapter adapter;
    int[] img;

    private static int currentPage = 0;
    private static int NUM_PAGES = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluehouse__main);

        if (!bhdb_SharedPrefManager.getInstance(this).isLoggedIn()) {
            finish();
            startActivity(new Intent(this, bhdb_MainActivity.class));
        }
        else
        {
            bhdb_User user = bhdb_SharedPrefManager.getInstance(this).getUser();
            String Public_id = user.getPublic_id();
            Boolean isOffline = false;
            if (isOffline)
            {

            }
            else
            {

            }
        }





        currentPage=((TindroidApp) getApplication()).LastPage();
        img = new int[]{R.drawable.bluehouse_mainpage_homegroups, //R.drawable.bluehouse_mainpage_payments,// R.drawable.bluehouse_mainpage_chat, R.drawable.bluehouse_mainpage_savemoney,
                R.drawable.bluehouse_mainpage_settings};      //select the image from res/drawable  folder

        viewpager = (ClickableViewPager) findViewById(R.id.pager);

        viewpager.setOnViewPagerClickListener(new ClickableViewPager.OnClickListener() {
            @Override
            public void onViewPagerClick(ViewPager viewPager) {
                OpenChatActivity();
            }
        });

        adapter = new bluehouse_ViewPagerAdapter(bluehouse_MainActivity.this, img);
        viewpager.setAdapter(adapter);
        CircleIndicator indicator = (CircleIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(viewpager);

        viewpager.setCurrentItem(currentPage);
        viewpager.addOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                ((TindroidApp) getApplication()).setLastPage(position);
                currentPage = position;
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                /*Toast.makeText(getApplicationContext(), "context changed", Toast.LENGTH_SHORT).show();

                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    int pageCount = img.length;
                    if (currentPage == 0) {
                        viewpager.setCurrentItem(pageCount - 1, false);
                    } else if (currentPage == pageCount - 1) {
                        viewpager.setCurrentItem(0, false);
                    }
                }*/
            }
        });

        /*
        final Handler handler = new Handler();
        final Runnable update = new Runnable() {
            @Override
            public void run() {
                if (currentPage == NUM_PAGES) {
                    currentPage = 0;
                }
                viewpager.setCurrentItem(currentPage++, true);
            }
        };
        Timer swipeTimer = new Timer();
        swipeTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                handler.post(update);
            }
        }, 1000, 1000);*/
    }



    public void OpenChatActivity()
    {
        if (currentPage == 0) {
            boolean isUserReady = ((TindroidApp) this.getApplication()).getIsUserReady();
            final Intent launch = new Intent(this, isUserReady ?
                    ChatsActivity.class : LoginActivity.class);
            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launch);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
        else if (currentPage == 1)
        {
            final Intent launch = new Intent(this, bluehouse_SettingsActivity.class);
            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launch);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();

        }
        else if (currentPage == 2)
        {
            //bhdb_SharedPrefManager.getInstance(getApplicationContext()).logout();
        }
        else if (currentPage == 3)
        {
            /*final Intent launch = new Intent(this, PaymentsActivity.class);
            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launch);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();*/
        }
    }

}