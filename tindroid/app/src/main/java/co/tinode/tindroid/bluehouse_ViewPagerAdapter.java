package co.tinode.tindroid;
import android.R.layout;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.text.method.ReplacementTransformationMethod;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

class bluehouse_ViewPagerAdapter extends PagerAdapter {
    Activity activity;
    int[] flag;
    LayoutInflater inflater;
    Context context;
    int position = 0;

    public bluehouse_ViewPagerAdapter(bluehouse_MainActivity mainActivity, int[] img) {
        this.activity = mainActivity;
        this.context = mainActivity;
        this.flag = img;
    }

    @Override
    public int getCount() {
        return flag.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemview = inflater.inflate(R.layout.bluehouse_item, container, false);

        ImageView img;
        img = (ImageView) itemview.findViewById(R.id.mainpage_item);
        /*DisplayMetrics dis = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dis);
        int height = dis.heightPixels;
        int width = dis.widthPixels;
        img.setMinimumHeight(height);
        img.setMinimumHeight(width);*/
        img.setImageResource(flag[position]);

        //add item.xml to viewpager
        ((ViewPager) container).addView(itemview);
        return itemview;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        // Remove viewpager_item.xml from ViewPager
        ((ViewPager) container).removeView((RelativeLayout) object);
    }

    /*
    @Override
    public float getPageWidth(int position) {
        if (position == 0)
            return .40f;
        else if (position == 1)
            return .60f;
        else if (position == 2)
            return .60f;
        else
            return .40f;
    }*/
}
