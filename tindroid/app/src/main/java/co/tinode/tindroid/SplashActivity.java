package co.tinode.tindroid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import co.tinode.tindroid.db.BaseDb;

/**
 * Splash screen on startup
 */
public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_TIME_OUT = 1000;
    private static final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // No need to check for live connection here.
        // Send user to appropriate screen:
        // 1. If we have an account and no credential validation is needed, send to ChatsActivity.
        // 2. If we don't have an account or credential validation is required send to LoginActivity.

        //final Intent launch = new Intent(this, BaseDb.getInstance().isReady() ?
        //        ChatsActivity.class : LoginActivity.class);

        ((TindroidApp) this.getApplication()).setIsUserReady(BaseDb.getInstance().isReady());
        final Intent launch = new Intent(this, bluehouse_MainActivity.class);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(launch);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}
