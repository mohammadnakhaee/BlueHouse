package co.tinode.tindroid;

import android.app.Activity;
import android.content.DialogInterface;//new
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;//new
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.bluehouse.bluehousedb.bhdb_LoginActivity;
import net.bluehouse.bluehousedb.bhdb_RequestHandler;
import net.bluehouse.bluehousedb.bhdb_SharedPrefManager;
import net.bluehouse.bluehousedb.bhdb_URLs;
import net.bluehouse.bluehousedb.bhdb_User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import co.tinode.tindroid.account.Utils;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.Tinode;
import co.tinode.tinodesdk.model.AuthScheme;
import co.tinode.tinodesdk.model.ServerMessage;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * A placeholder fragment containing a simple view.
 */
public class LoginFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "LoginFragment";
    String phonenumber = "";
    String validation_access_token = "";
    String public_id = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) {
            return null;
        }

        phonenumber = ((TindroidApp) activity.getApplication()).Phonenumber();
        validation_access_token = ((TindroidApp) activity.getApplication()).ValidationToken();
        ((TindroidApp) activity.getApplication()).setValidationToken("");


        final ActionBar bar = activity.getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(false);
            //bar.setTitle(R.string.app_name);
            bar.setTitle(null);
            bar.setDisplayShowCustomEnabled(true);
            bar.setHomeButtonEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setHomeAsUpIndicator(R.drawable.logo_toolbar);

            bar.hide();
        }

        View fragment = inflater.inflate(R.layout.fragment_login, container, false);

        /*
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String login = pref.getString(LoginActivity.PREFS_LAST_LOGIN, null);

        if (!TextUtils.isEmpty(login)) {
            TextView loginView = fragment.findViewById(R.id.editLogin);
            if (loginView != null) {
                loginView.setText(login);
            }
        }*/

        fragment.findViewById(R.id.signIn).setOnClickListener(this);
        fragment.findViewById(R.id.forgotPassword).setOnClickListener(this);

        if (validation_access_token.equals(""))
        {
            UiUtils.doLogout(activity);
            bhdb_SharedPrefManager.getInstance(activity.getApplicationContext()).logout();

            final Intent launch = new Intent(activity.getApplicationContext(), bluehouse_MainActivity.class);
            startActivity(launch);
            activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            activity.finish();
        }

        return fragment;
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_login, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstance) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        TextView textViewPhonenumber = (TextView) activity.findViewById(R.id.textViewPhonenumber);
        textViewPhonenumber.setText(phonenumber);
    }

    /**
     * Either [Signin] or [Forgot password] pressed.
     * @param v ignored
     */
    public void onClick(View v) {
        userLogin();
    }

    private void userLogin() {
        final LoginActivity parent = (LoginActivity) getActivity();
        if (parent == null) {
            return;
        }

        final String verificationcode = ((EditText) parent.findViewById(R.id.editTextCode)).getText().toString().trim();
        if (verificationcode.isEmpty()) {
            ((EditText) parent.findViewById(R.id.editTextCode)).setError("Verification code is needed.");
            return;
        }

        //if everything is fine

        class UserLogin extends AsyncTask<Void, Void, String> {

            //ProgressBar progressBar;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                //progressBar = (ProgressBar) parent.findViewById(R.id.progressBar);
                //progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                //progressBar.setVisibility(View.GONE);


                try {
                    //converting response to json object
                    JSONObject json = new JSONObject(s);

                    if (json.getBoolean("error"))
                    {
                        Toast.makeText(parent.getApplicationContext(), json.getString("message"), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (json.getString("query") == "pass1") {
                        String pass1_access_token = json.getString("pass1-access-token");
                        Toast.makeText(parent.getApplicationContext(), json.getString("message"), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(parent.getApplicationContext(), bhdb_LoginActivity.class);
                        intent.putExtra("EXTRA_ID_phonenumber", phonenumber);
                        intent.putExtra("EXTRA_ID_pass1_access_token", pass1_access_token);
                        parent.finish();
                        startActivity(intent);
                    }

                    String local_public_id = json.getString("public_id");

                    public_id = local_public_id;
                    ((TindroidApp) parent.getApplication()).setPublicID(public_id);

                    userlogin();

                    //storing the user in shared preferences
                    //bhdb_SharedPrefManager.getInstance(getApplicationContext()).userLogin(user);

                    //activity.finish();
                    //startActivity(new Intent(activity.getApplicationContext(), bluehouse_MainActivity.class));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected String doInBackground(Void... voids) {
                //creating request handler object
                bhdb_RequestHandler requestHandler = new bhdb_RequestHandler();

                HashMap<String, String> emptymap = new HashMap<>();
                String jwt_request = Jwts.builder().claim("type","secure-verification-data").claim("registration-phone-number",phonenumber)
                        .claim("registration-verifcode",verificationcode).claim("exp", System.currentTimeMillis() + 60000)
                        .signWith(SignatureAlgorithm.HS256, bhdb_URLs.API_KEY.getBytes())
                        .compact();
                HashMap<String, String> Header = new HashMap<>();
                Header.put("request_key", jwt_request);
                return requestHandler.sendPostRequest(bhdb_URLs.URL_VERIFYIMG, emptymap, Header, validation_access_token);
            }
        }

        UserLogin ul = new UserLogin();
        //ul.execute();
        ul.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }




    public void userlogin() {
        final LoginActivity parent = (LoginActivity) getActivity();
        if (parent == null) {
            return;
        }

        /*if (v.getId() == R.id.forgotPassword) {
            parent.showFragment(LoginActivity.FRAGMENT_RESET);
            return;
        }*/
        final Button signIn = parent.findViewById(R.id.signIn);
        signIn.setEnabled(false);

        if (public_id.length() == 0) return;
        final String login = "user" + phonenumber.substring(1);
        final String fullName = phonenumber;
        final String password = public_id;





        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(parent);
        final String hostName = sharedPref.getString(Utils.PREFS_HOST_NAME, TindroidApp.getDefaultHostName(parent));
        boolean tls = sharedPref.getBoolean(Utils.PREFS_USE_TLS, TindroidApp.getDefaultTLS());
        //final String hostName = "10.0.2.2:16060";
        //boolean tls = false;
        final Tinode tinode = Cache.getTinode();


        /*AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(getActivity());
        dlgAlert.setMessage(hostName);
        dlgAlert.setTitle("App Title");
        dlgAlert.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //dismiss the dialog
                    }
                });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();*/

        // This is called on the websocket thread.
        tinode.connect(hostName, tls, false)
                .thenApply(
                        new PromisedReply.SuccessListener<ServerMessage>() {
                            @Override
                            public PromisedReply<ServerMessage> onSuccess(ServerMessage ignored) {
                                return tinode.loginBasic(
                                        login,
                                        password);
                            }
                        })
                .thenApply(
                        new PromisedReply.SuccessListener<ServerMessage>() {
                            @Override
                            public PromisedReply<ServerMessage> onSuccess(final ServerMessage msg) {
                                sharedPref.edit().putString(LoginActivity.PREFS_LAST_LOGIN, login).apply();

                                UiUtils.updateAndroidAccount(parent, tinode.getMyId(),
                                        AuthScheme.basicInstance(login, password).toString(),
                                        tinode.getAuthToken(), tinode.getAuthTokenExpiration());

                                // msg could be null if earlier login has succeeded.
                                if (msg != null && msg.ctrl.code >= 300 &&
                                        msg.ctrl.text.contains("validate credentials")) {
                                    parent.runOnUiThread(new Runnable() {
                                        public void run() {
                                            signIn.setEnabled(true);
                                            parent.showFragment(LoginActivity.FRAGMENT_CREDENTIALS);
                                        }
                                    });
                                } else {
                                    tinode.setAutoLoginToken(tinode.getAuthToken());

                                    //creating a new user object
                                    bhdb_User user = new bhdb_User(public_id);
                                    //storing the user in shared preferences
                                    bhdb_SharedPrefManager.getInstance(parent.getApplicationContext()).userLogin(user);

                                    // Force immediate sync, otherwise Contacts tab may be unusable.
                                    UiUtils.onLoginSuccess(parent, signIn, tinode.getMyId());


                                }
                                return null;
                            }
                        })
                .thenCatch(
                        new PromisedReply.FailureListener<ServerMessage>() {
                            @Override
                            public PromisedReply<ServerMessage> onFailure(Exception err) {
                                Log.i(TAG, "Login failed", err);
                                parent.reportError(err, signIn, 0, R.string.error_login_failed);
                                return null;
                            }
                        });
    }
}
