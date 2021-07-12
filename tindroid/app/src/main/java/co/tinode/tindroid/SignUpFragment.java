package co.tinode.tindroid;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import co.tinode.tindroid.account.Utils;
import co.tinode.tindroid.media.VxCard;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.ServerResponseException;
import co.tinode.tinodesdk.Tinode;
import co.tinode.tinodesdk.model.AuthScheme;
import co.tinode.tinodesdk.model.Credential;
import co.tinode.tinodesdk.model.MetaSetDesc;
import co.tinode.tinodesdk.model.ServerMessage;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment for managing registration of a new account.
 */
public class SignUpFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "SignUpFragment";
    String phonenumber = "";
    String validation_access_token = "";
    AppCompatActivity activity;
    String public_id = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(false);

        activity = (AppCompatActivity) getActivity();
        if (activity == null) {
            return null;
        }

        phonenumber = ((TindroidApp) activity.getApplication()).Phonenumber();
        validation_access_token = ((TindroidApp) activity.getApplication()).ValidationToken();
        ((TindroidApp) activity.getApplication()).setValidationToken("");


        ActionBar bar = activity.getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(R.string.sign_up);

            bar.hide();
        }

        View fragment = inflater.inflate(R.layout.fragment_signup, container, false);

        fragment.findViewById(R.id.signUp).setOnClickListener(this);

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
    public void onViewCreated(@NonNull View view, Bundle savedInstance) {

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        /*
        // Get avatar from the gallery
        // TODO(gene): add support for taking a picture
        view.findViewById(R.id.uploadAvatar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.requestAvatar(SignUpFragment.this);
            }
        });
         */

        TextView textViewPhonenumber = (TextView) activity.findViewById(R.id.textViewPhonenumber);
        textViewPhonenumber.setText(phonenumber);
    }

    /**
     * Create new account with various methods
     *
     * @param v button pressed
     */
    @Override
    public void onClick(View v) {
        final LoginActivity parent = (LoginActivity) getActivity();
        if (parent == null) {
            return;
        }

        userLogin();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (requestCode == UiUtils.ACTIVITY_RESULT_SELECT_PICTURE && resultCode == RESULT_OK) {
            UiUtils.acceptAvatar(activity, (ImageView) activity.findViewById(R.id.imageAvatar), data);
        }
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
                        Toast.makeText(activity.getApplicationContext(), json.getString("message"), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (json.getString("query") == "pass1") {
                        String pass1_access_token = json.getString("pass1-access-token");
                        Toast.makeText(activity.getApplicationContext(), json.getString("message"), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(activity.getApplicationContext(), bhdb_LoginActivity.class);
                        intent.putExtra("EXTRA_ID_phonenumber", phonenumber);
                        intent.putExtra("EXTRA_ID_pass1_access_token", pass1_access_token);
                        activity.finish();
                        startActivity(intent);
                    }

                    String local_public_id = json.getString("public_id");

                    public_id = local_public_id;
                    ((TindroidApp) parent.getApplication()).setPublicID(public_id);

                    CreateTinodeUser();

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


    public void CreateTinodeUser() {
        final LoginActivity parent = (LoginActivity) getActivity();
        if (parent == null) {
            return;
        }

        if (public_id.length() == 0) return;
        final String login = "user" + phonenumber.substring(1);
        final String fullName = phonenumber;
        final String password = public_id;


        final Button signUp = parent.findViewById(R.id.signUp);
        signUp.setEnabled(false);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(parent);
        String hostName = sharedPref.getString(Utils.PREFS_HOST_NAME, TindroidApp.getDefaultHostName(parent));
        boolean tls = sharedPref.getBoolean(Utils.PREFS_USE_TLS, TindroidApp.getDefaultTLS());

        final ImageView avatar = parent.findViewById(R.id.imageAvatar);
        final Tinode tinode = Cache.getTinode();
        // This is called on the websocket thread.
        tinode.connect(hostName, tls, false)
                .thenApply(
                        new PromisedReply.SuccessListener<ServerMessage>() {
                            @Override
                            public PromisedReply<ServerMessage> onSuccess(ServerMessage ignored_msg) {
                                // Try to create a new account.
                                Bitmap bmp = null;
                                try {
                                    bmp = ((BitmapDrawable) avatar.getDrawable()).getBitmap();
                                } catch (ClassCastException ignored) {
                                    // If image is not loaded, the drawable is a vector.
                                    // Ignore it.
                                }
                                VxCard vcard = new VxCard(fullName, bmp);

                                /*return tinode.createAccountBasic(
                                        login, password, true, null,
                                        new MetaSetDesc<VxCard,String>(vcard, null),
                                        Credential.append(null, new Credential("email", email)));*/

                                String[] tags = {"tel:" + phonenumber};
                                return tinode.createAccountBasic(
                                        login, password, true, tags,
                                        new MetaSetDesc<VxCard,String>(vcard, null));
                            }
                        })
                .thenApply(
                        new PromisedReply.SuccessListener<ServerMessage>() {
                            @Override
                            public PromisedReply<ServerMessage> onSuccess(final ServerMessage msg) {
                                UiUtils.updateAndroidAccount(parent, tinode.getMyId(),
                                        AuthScheme.basicInstance(login, password).toString(),
                                        tinode.getAuthToken(), tinode.getAuthTokenExpiration());

                                // Flip back to login screen on success;
                                parent.runOnUiThread(new Runnable() {
                                    public void run() {
                                        if (msg.ctrl.code >= 300 && msg.ctrl.text.contains("validate credentials")) {
                                            signUp.setEnabled(true);
                                            parent.showFragment(LoginActivity.FRAGMENT_CREDENTIALS);
                                        } else {
                                            // We are requesting immediate login with the new account.
                                            // If the action succeeded, assume we have logged in.
                                            tinode.setAutoLoginToken(tinode.getAuthToken());

                                            //creating a new user object
                                            bhdb_User user = new bhdb_User(public_id);
                                            //storing the user in shared preferences
                                            bhdb_SharedPrefManager.getInstance(activity.getApplicationContext()).userLogin(user);


                                            UiUtils.onLoginSuccess(parent, signUp, tinode.getMyId());




                                        }
                                    }
                                });
                                return null;
                            }
                        })
                .thenCatch(
                        new PromisedReply.FailureListener<ServerMessage>() {
                            @Override
                            public PromisedReply<ServerMessage> onFailure(Exception err) {
                                if (parent.isFinishing() || parent.isDestroyed()) {
                                    return null;
                                }
                                final String cause = ((ServerResponseException)err).getReason();
                                if (cause != null) {
                                    parent.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            signUp.setEnabled(true);
                                            ((EditText) parent.findViewById(R.id.editTextCode)).setError(getText(R.string.login_rejected));
                                            /*
                                            switch (cause) {
                                                case "auth":
                                                    // Invalid login
                                                    ((EditText) parent.findViewById(R.id.newLogin)).setError(getText(R.string.login_rejected));
                                                    break;
                                                case "email":
                                                    // Duplicate email:
                                                    ((EditText) parent.findViewById(R.id.email)).setError(getText(R.string.email_rejected));
                                                    break;
                                            }
                                             */
                                        }
                                    });
                                }
                                parent.reportError(err, signUp, 0, R.string.error_new_account_failed);
                                return null;
                            }
                        });
    }
}
