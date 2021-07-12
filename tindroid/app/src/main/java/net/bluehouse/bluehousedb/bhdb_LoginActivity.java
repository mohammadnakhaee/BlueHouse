package net.bluehouse.bluehousedb;
import android.content.Intent;
import android.os.AsyncTask;
import androidx.appcompat.app.AppCompatActivity;
import co.tinode.tindroid.R;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class bhdb_LoginActivity extends AppCompatActivity {

    String phonenumber = "";
    String pass1_access_token = "";
    EditText editTextPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bhdb_activity_login);

        Bundle extras = getIntent().getExtras();
        phonenumber = extras.getString("EXTRA_ID_phonenumber");
        pass1_access_token = extras.getString("EXTRA_ID_pass1_access_token");

        editTextPassword = (EditText) findViewById(R.id.editTextPassword);

        findViewById(R.id.buttonLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userLogin();
            }
        });

        /*findViewById(R.id.textViewRegister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //open register screen
                finish();
                startActivity(new Intent(getApplicationContext(), bhdb_MainActivity.class));
            }
        });*/
    }

    private void userLogin() {
        //first getting the values
        final String password = editTextPassword.getText().toString();

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Please enter your password");
            editTextPassword.requestFocus();
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

                    //if no error in response
                    if (!json.getBoolean("error")) {
                        Toast.makeText(getApplicationContext(), json.getString("message"), Toast.LENGTH_SHORT).show();

                        String user_id = json.getString("public_id");
                        //creating a new user object
                        bhdb_User user = new bhdb_User(user_id);
                        //storing the user in shared preferences
                        bhdb_SharedPrefManager.getInstance(getApplicationContext()).userLogin(user);

                        //starting the profile activity
                        finish();
                        startActivity(new Intent(getApplicationContext(), bhdb_ProfileActivity.class));
                    } else {
                        Toast.makeText(getApplicationContext(), "Invalid username or password", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected String doInBackground(Void... voids) {
                //creating request handler object
                bhdb_RequestHandler requestHandler = new bhdb_RequestHandler();

                HashMap<String, String> emptymap = new HashMap<>();
                String jwt_request = Jwts.builder().claim("type","secure-password1-data").claim("login-phone-number",phonenumber)
                        .claim("login-password1",password).claim("exp", System.currentTimeMillis() + 60000)
                        .signWith(SignatureAlgorithm.HS256, bhdb_URLs.API_KEY.getBytes())
                        .compact();
                HashMap<String, String> Header = new HashMap<>();
                Header.put("request_key", jwt_request);
                return requestHandler.sendPostRequest(bhdb_URLs.URL_lOGINPASS, emptymap, Header, pass1_access_token);
            }
        }

        UserLogin ul = new UserLogin();
        ul.execute();
    }
}