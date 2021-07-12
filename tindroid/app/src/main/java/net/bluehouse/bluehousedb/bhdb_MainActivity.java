package net.bluehouse.bluehousedb;
import co.tinode.tindroid.LoginActivity;
import co.tinode.tindroid.R;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import co.tinode.tindroid.TindroidApp;
import co.tinode.tindroid.UiUtils;
import co.tinode.tindroid.bluehouse_MainActivity;
import io.jsonwebtoken.*;

public class bhdb_MainActivity extends AppCompatActivity {
    EditText editTextPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bhdb_activity_main);

        UiUtils.doLogout(this);
        bhdb_SharedPrefManager.getInstance(this.getApplicationContext()).logout();

        ((TindroidApp) getApplication()).setLastPage(0);

        //if the user is already logged in we will directly start the profile activity
        //if (bhdb_SharedPrefManager.getInstance(this).isLoggedIn()) {
            //finish();
            //startActivity(new Intent(this, bhdb_ProfileActivity.class));
            //return;
        //}

        editTextPhoneNumber = (EditText) findViewById(R.id.editTextPhoneNumber);

        findViewById(R.id.buttonRegister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if user pressed on button register
                //here we will register the user to server
                registerUser();
            }
        });

    }

    private void registerUser() {
        final String phonenumber = editTextPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(phonenumber)) {
            editTextPhoneNumber.setError("Please enter your phone number");
            editTextPhoneNumber.requestFocus();
            return;
        }

        /*if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Enter a valid email");
            editTextEmail.requestFocus();
            return;
        }*/

        //if it passes all the validations
        class RegisterUser extends AsyncTask<Void, Void, String> {

            private ProgressBar progressBar;

            @Override
            protected String doInBackground(Void... voids) {
                //creating request handler object
                bhdb_RequestHandler requestHandler = new bhdb_RequestHandler();

                HashMap<String, String> emptymap = new HashMap<>();

                String jwt_request = Jwts.builder().claim("type","secure-registration-request")
                        .claim("exp", System.currentTimeMillis() + 60000)
                        .signWith(SignatureAlgorithm.HS256, bhdb_URLs.API_KEY.getBytes())
                        .compact();
                HashMap<String, String> Header = new HashMap<>();
                Header.put("request_key", jwt_request);
                String registration_access_token_jsonstring = requestHandler.sendPostRequest(bhdb_URLs.URL_REGISTER_REQUEST, emptymap, Header);

                String registration_access_token = "";
                try {
                    JSONObject json = new JSONObject(registration_access_token_jsonstring);
                    //registration_access_token = registration_access_token_json.getJSONObject("LabelData").getString("slogan");
                    registration_access_token = json.getString("registration-access-token");
                } catch (JSONException e) {
                    //some exception handler code.
                }

                String jwt_request2 = Jwts.builder().claim("type","secure-registration-data").claim("registration-phone-number",phonenumber)
                        .claim("exp", System.currentTimeMillis() + 60000)
                        .signWith(SignatureAlgorithm.HS256, bhdb_URLs.API_KEY.getBytes())
                        .compact();
                HashMap<String, String> Header2 = new HashMap<>();
                Header2.put("request_key", jwt_request2);
                return requestHandler.sendPostRequest(bhdb_URLs.URL_REGISTER, emptymap, Header2, registration_access_token);

                //creating request parameters
                /*
                HashMap<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("email", email);
                params.put("password", password);
                params.put("gender", gender);

                //returning the response
                return requestHandler.sendPostRequest(bhdb_URLs.URL_REGISTER, params);*/
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                //displaying the progress bar while user registers on the server
                progressBar = (ProgressBar) findViewById(R.id.progressBar);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                //hiding the progressbar after completion
                progressBar.setVisibility(View.GONE);

                try {
                    //converting response to json object
                    JSONObject json = new JSONObject(s);

                    //if no error in response
                    if (!json.getBoolean("error")) {
                        String validation_access_token = json.getString("validation-access-token");

                        Boolean isSignUp = false;
                        String query = json.getString("query");
                        if (query.equals("signup")) isSignUp = true;

                        //storing the user in shared preferences
                        //bhdb_SharedPrefManager.getInstance(getApplicationContext()).userLogin(user);

                        //starting the profile activity

                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        ((TindroidApp) getApplication()).setSignUpNeeded(isSignUp);
                        ((TindroidApp) getApplication()).setPhonenumber(phonenumber);
                        ((TindroidApp) getApplication()).setValidationToken(validation_access_token);

                        //intent.putExtra("EXTRA_ID_isSignUp", isSignUp);
                        //intent.putExtra("EXTRA_ID_phonenumber", phonenumber);
                        //intent.putExtra("EXTRA_ID_validation_access_token", validation_access_token);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();

                    } else {
                        Toast.makeText(getApplicationContext(), "Unable to connect the server!", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        //executing the async task
        RegisterUser ru = new RegisterUser();
        //ru.execute();
        ru.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

}