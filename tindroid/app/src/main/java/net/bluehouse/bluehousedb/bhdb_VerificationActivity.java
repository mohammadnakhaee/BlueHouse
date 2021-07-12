package net.bluehouse.bluehousedb;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import co.tinode.tindroid.Cache;
import co.tinode.tindroid.LoginActivity;
import co.tinode.tindroid.R;
import co.tinode.tindroid.TindroidApp;
import co.tinode.tindroid.UiUtils;
import co.tinode.tindroid.account.Utils;
import co.tinode.tindroid.bluehouse_MainActivity;
import co.tinode.tindroid.media.VxCard;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.ServerResponseException;
import co.tinode.tinodesdk.Tinode;
import co.tinode.tinodesdk.model.AuthScheme;
import co.tinode.tinodesdk.model.MetaSetDesc;
import co.tinode.tinodesdk.model.ServerMessage;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class bhdb_VerificationActivity extends AppCompatActivity {

    String phonenumber = "";
    String validation_access_token = "";
    EditText editTextCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bhdb_activity_verification);

        Bundle extras = getIntent().getExtras();
        phonenumber = extras.getString("EXTRA_ID_phonenumber");
        validation_access_token = extras.getString("EXTRA_ID_validation_access_token");

        TextView textViewPhonenumber = (TextView) findViewById(R.id.textViewPhonenumber);
        textViewPhonenumber.setText(phonenumber);

        editTextCode = (EditText) findViewById(R.id.editTextCode);

        //if user presses on login
        //calling the method login
        findViewById(R.id.buttonLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userLogin();
            }
        });

        //if user presses on not registered
        findViewById(R.id.textViewRegister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //open register screen
                finish();
                startActivity(new Intent(getApplicationContext(), bhdb_MainActivity.class));
            }
        });

    }

    private void userLogin() {
        //first getting the values
        final String verificationcode = editTextCode.getText().toString();

        if (TextUtils.isEmpty(verificationcode)) {
            editTextCode.setError("Please enter your verification code");
            editTextCode.requestFocus();
            return;
        }

        //if everything is fine

        class UserLogin extends AsyncTask<Void, Void, String> {

            ProgressBar progressBar;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressBar = (ProgressBar) findViewById(R.id.progressBar);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                progressBar.setVisibility(View.GONE);


                try {
                    //converting response to json object
                    JSONObject json = new JSONObject(s);

                    if (json.getBoolean("error"))
                    {
                        Toast.makeText(getApplicationContext(), json.getString("message"), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String query = json.getString("query");
                    if (query.equals("pass1")) {
                        String pass1_access_token = json.getString("pass1-access-token");
                        Toast.makeText(getApplicationContext(), json.getString("message"), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), bhdb_LoginActivity.class);
                        intent.putExtra("EXTRA_ID_phonenumber", phonenumber);
                        intent.putExtra("EXTRA_ID_pass1_access_token", pass1_access_token);
                        finish();
                        startActivity(intent);
                    }

                    String public_id = json.getString("public_id");
                    //creating a new user object
                    bhdb_User user = new bhdb_User(public_id);
                    //storing the user in shared preferences
                    bhdb_SharedPrefManager.getInstance(getApplicationContext()).userLogin(user);

                    //storing the user in shared preferences
                    //bhdb_SharedPrefManager.getInstance(getApplicationContext()).userLogin(user);
                    finish();

                    startActivity(new Intent(getApplicationContext(), bluehouse_MainActivity.class));

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
        ul.execute();
    }

}