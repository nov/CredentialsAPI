package jp.yauth.credentialsapi;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.support.design.widget.Snackbar;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.auth.api.credentials.IdToken;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    final int RC_CREDENTIALS_READ = 1;
    final int RC_HINT_READ = 2;
    final int RC_SIGN_IN = 3;
    GoogleApiClient mCredentialsClient;

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mCredentialsClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .enableAutoManage(this, this)
                .addApi(Auth.CREDENTIALS_API)
                .addApi(AppIndex.API).build();

        Button loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAuthentication();
            }
        });
    }

    private void startAuthentication() {
        Log.d(TAG, "startAuthentication");


        CredentialRequest mCredentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes(
                        IdentityProviders.GOOGLE,
                        IdentityProviders.FACEBOOK
//                        "https://gree.net"
                ).build();

        Auth.CredentialsApi.request(mCredentialsClient, mCredentialRequest).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(CredentialRequestResult credentialRequestResult) {
                        Log.d(TAG, "onResult");
                        if (credentialRequestResult.getStatus().isSuccess()) {
                            onCredentialRetrieved(credentialRequestResult.getCredential());
                        } else {
                            resolveResult(credentialRequestResult.getStatus());
                        }
                    }
                });
    }

    private void onCredentialRetrieved(Credential credential) {
        String accountType = credential.getAccountType();
        if (accountType == null) {
            // Sign the user in with information from the Credential.
            Log.d(TAG, String.format("onCredentialRetrieved:password (%s)", credential.getPassword()));
        } else {
            Log.d(TAG, String.format("Account Type: %s", accountType));
            Log.d(TAG, String.format("ID: %s", credential.getId()));
            if (!credential.getIdTokens().isEmpty()) {
                IdToken idToken = credential.getIdTokens().get(0);
                Log.d(TAG, String.format("ID Token: %s", idToken.getIdToken()));
            } else {
                Log.d(TAG, "ID Token: null");
            }
        }
    }

    private void onHintRetrieved(Credential credential) {
        Log.d(TAG, String.format("onHintRetrieved %s", credential.getId()));
        Log.d(TAG, String.format("Account Type: %s", credential.getAccountType()));
        if (!credential.getIdTokens().isEmpty()) {
            IdToken idToken = credential.getIdTokens().get(0);
            Log.d(TAG, String.format("ID Token: %s", idToken.getIdToken()));
        } else {
            Log.d(TAG, "ID Token: null");
        }
    }

    private void resolveResult(Status status) {
        if (status.hasResolution()) {
            Log.e(TAG, "STATUS: Failed with a resolution.");
            switch (status.getStatusCode()) {
                case CommonStatusCodes.RESOLUTION_REQUIRED:
                    try {
                        status.startResolutionForResult(this, RC_CREDENTIALS_READ);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(TAG, "STATUS: Failed to send resolution.", e);
                    }
                    break;
                case CommonStatusCodes.SIGN_IN_REQUIRED:
                    HintRequest hintRequest = new HintRequest.Builder()
                            .setEmailAddressIdentifierSupported(true)
                            .build();
                    PendingIntent intent = Auth.CredentialsApi.getHintPickerIntent(mCredentialsClient, hintRequest);
                    try {
                        startIntentSenderForResult(intent.getIntentSender(), RC_HINT_READ, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(TAG, "Could not start hint picker Intent", e);
                    }
                    break;
                default:
                    Log.e(TAG, "STATUS: Unsuccessful credential request.");
            }
        } else {
            Log.e(TAG, "STATUS: Failed with no resolution.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, String.format("onActivityResult %d", requestCode));
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_CREDENTIALS_READ:
                if (resultCode == RESULT_OK) {
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    onCredentialRetrieved(credential);
                } else {
                    Log.e(TAG, "Credential Read: NOT OK");
                }
                break;
            case RC_HINT_READ:
                if (resultCode == RESULT_OK) {
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    onHintRetrieved(credential);
                } else {
                    Log.e(TAG, "Credential Read: NOT OK");
                }
                break;
            case RC_SIGN_IN:
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (result.isSuccess()) {
                    GoogleSignInAccount acct = result.getSignInAccount();
                    // Get account information
                    String displayName = acct.getDisplayName();
                    String email = acct.getEmail();
                    Log.d(TAG, String.format("Logged in as %s (%s)", displayName, email));
                } else {
                    Log.e(TAG, "Google Signin Failed");
                }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mCredentialsClient.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://jp.yauth.credentialsapi/http/host/path")
        );
        AppIndex.AppIndexApi.start(mCredentialsClient, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://jp.yauth.credentialsapi/http/host/path")
        );
        AppIndex.AppIndexApi.end(mCredentialsClient, viewAction);
        mCredentialsClient.disconnect();
    }
}
