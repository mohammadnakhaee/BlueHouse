package net.bluehouse.bluehousedb;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

//using a singleton pattern
public class bhdb_SharedPrefManager {

    //the constants
    private static final String SHARED_PREF_NAME = "simplifiedcodingsharedpref";
    private static final String KEY_PUBLICID = "keyuserpublicid";

    private static bhdb_SharedPrefManager mInstance;
    private static Context mCtx;

    private bhdb_SharedPrefManager(Context context) {
        mCtx = context;
    }

    public static synchronized bhdb_SharedPrefManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new bhdb_SharedPrefManager(context);
        }
        return mInstance;
    }

    //method to let the user login
    //this method will store the user data in shared preferences
    public void userLogin(bhdb_User user) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_PUBLICID, user.getPublic_id());
        editor.apply();
    }

    //this method will checker whether user is already logged in or not
    public boolean isLoggedIn() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_PUBLICID, null) != null;
    }

    //this method will give the logged in user
    public bhdb_User getUser() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return new bhdb_User(
                sharedPreferences.getString(KEY_PUBLICID, null)
        );
    }

    //this method will logout the user
    public void logout() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        //mCtx.startActivity(new Intent(mCtx, bhdb_LoginActivity.class));
    }
}
