package example.org.photoapp;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

/**
 * Allows the user to login usign their UserId and Password
 */
public class LoginActivity extends AppCompatActivity {

    private final static String PASSWORD = "a";
    private final static String LAST_USER_ID = "last_user_id";
    private final static String LAST_PASSWORD = "last_password";
    EditText userIDField;
    EditText passwordField;
    Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // If the device is running Lollipop or higher, set slide exit transition for when this Activity
        // is exited
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().setExitTransition(new Slide());
        }
        super.onCreate(savedInstanceState);

        // Inflate the layout and create the toolbar
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("PhotoApp");
        setSupportActionBar(toolbar);

        // Assign the EditTexts and Buttons to their corresponding views
        userIDField = (EditText) findViewById(R.id.user_id_field);
        passwordField = (EditText) findViewById(R.id.password_field);
        loginButton = (Button) findViewById(R.id.loginButton);

        // If the user has logged in successfully before, there should exist in the default shared user
        // preferences their userID and password. Retrieve them and assign them to local string variables.
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String userID = sharedPref.getString(LAST_USER_ID, "");
        String password = sharedPref.getString(LAST_PASSWORD, "");

        // Populate the EditTexts with the user's last successful login credentials (or empty strings
        // if credentials are not found)
        userIDField.setText(userID);
        passwordField.setText(password);

        Log.d("Login", " " + password);

        // When the login button is clicked, check that the password in the password EditText is correct.
        // If it is, start the MainActivity.
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userID = userIDField.getText().toString();
                String password = passwordField.getText().toString();

                if (validateCredentials(userID, password)) {

                    // Save the current credentials as the new last login credentials in the shared
                    // user preferences
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    sharedPref.edit().putString(LAST_USER_ID, userID).putString(LAST_PASSWORD, password).apply();
                    Intent mainScreenIntent = new Intent(LoginActivity.this, MainActivity.class);

                    // If the device is running Lollipop or better, transition to the MainActivity with an animation.
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startActivity(mainScreenIntent, ActivityOptions.makeSceneTransitionAnimation(LoginActivity.this).toBundle());
                    } else {
                        startActivity(mainScreenIntent);
                    }
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Check the validity of the UserID and password entered by the user
     * @param userID userID entered by the user
     * @param password password entered by the user
     * @return true if credentials are valid, false if invalid
     */
    private boolean validateCredentials(String userID, String password) {

        // If no userID has been entered, display a popup dialog to alert the user and return false
        if(userID.length() < 1) {
            generateDialog("User ID is required");
            return false;
        }

        // If no password has been entered, display a popup dialog to alert the user and return false
        if(password.length() < 1) {
            generateDialog("Password is required");
            return false;
        }

        // If the password is correct and a userID has been entered, return true
        if(password.equals(PASSWORD)) {
            return true;
        }

        Log.d("h", password + " " + PASSWORD);

        // If the password is incorrect, display a popup dialog to alert the user and return false
        generateDialog("Invalid Password");
        return false;
    }

    /**
     * Generate a popup dialog with a message and an OK button
     * @param dialogMessage String message to be displayed to the user
     */
    private void generateDialog(String dialogMessage) {
        // Create a new LoginDialogFragment and give it a bundle containing the Dialog message and show
        LoginDialogFragment loginDialogFragment = new LoginDialogFragment();
        Bundle dialogBundle = new Bundle();
        dialogBundle.putString("dialogMessage", dialogMessage);
        loginDialogFragment.setArguments(dialogBundle);
        loginDialogFragment.show(getSupportFragmentManager(), "loginDialog");
    }
}
