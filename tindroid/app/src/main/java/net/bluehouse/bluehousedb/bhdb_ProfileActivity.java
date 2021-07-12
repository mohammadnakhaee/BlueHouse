package net.bluehouse.bluehousedb;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import co.tinode.tindroid.R;

public class bhdb_ProfileActivity extends AppCompatActivity {

    TextView textViewUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bhdb_activity_profile);

        //if the user is not logged in
        //starting the login activity
        if (!bhdb_SharedPrefManager.getInstance(this).isLoggedIn()) {
            finish();
            startActivity(new Intent(this, bhdb_LoginActivity.class));
        }

        textViewUsername = (TextView) findViewById(R.id.textViewUsername);

        bhdb_User user = bhdb_SharedPrefManager.getInstance(this).getUser();

        textViewUsername.setText(user.getPublic_id());

        //when the user presses logout button
        //calling the logout method
        findViewById(R.id.buttonLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                bhdb_SharedPrefManager.getInstance(getApplicationContext()).logout();
            }
        });
    }
}