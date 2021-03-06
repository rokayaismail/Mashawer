package com.fekratoday.mashawer.model.networkservices;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.fekratoday.mashawer.R;
import com.fekratoday.mashawer.model.database.TripDaoFirebase;
import com.fekratoday.mashawer.model.database.TripDaoSQL;
import com.fekratoday.mashawer.screens.loginscreen.LoginContract;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Objects;

public class LoginServices implements LoginServicesInterface {

    private static LoginServices instance = null;
    private static final String PRENS_NAME = "UserData";
    private static final String TAG = "LoginServices";
    private static final int RC_SIGN_IN = 1;
    private FirebaseAuth mAuth;
    private CallbackManager mCallbackManager;
    private LoginContract.View view;
    private LoginContract.Presenter loginPresenter;
    private SharedPreferences userData;
    private SharedPreferences.Editor editor;
    private FirebaseUser user;
    private String userId;
    private Context context;
    private TripDaoFirebase tripDaoFirebase;
    private TripDaoSQL tripDaoSQL;

    private LoginServices(LoginContract.View view, LoginContract.Presenter loginPresenter) {
        this.view = view;
        this.loginPresenter = loginPresenter;
        mAuth = FirebaseAuth.getInstance();
        context = (Context) view;
        userData = context.getSharedPreferences(PRENS_NAME, Context.MODE_PRIVATE);
        editor = userData.edit();
    }

    public static LoginServices getInstance(LoginContract.View view, LoginContract.Presenter loginPresenter) {
        if (instance == null) {
            instance = new LoginServices(view, loginPresenter);
        }
        return instance;
    }

    @Override
    public void createUserWithEmailAndPassword(String email, String password, String userName) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener((Activity) view, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        if (mAuth.getCurrentUser() != null) {
                            addUserNameToUser(mAuth.getCurrentUser(), userName);
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        loginPresenter.toast(Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
    }

    private void addUserNameToUser(FirebaseUser user, String userName) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated.");
                        loginPresenter.login(mAuth.getCurrentUser());
                    }
                });
    }

    @Override
    public void signInWithEmailAndPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener((Activity) view, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        loginPresenter.login(mAuth.getCurrentUser());
                        checkUserId();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        loginPresenter.toast(Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
    }

    @Override
    public void signInWithGoogle() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(((Activity)view).getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient((Activity) view, gso);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        loginPresenter.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        signInWithCredential(credential);
    }

    @Override
    public void signInWithFacebook(LoginButton btnFacebook) {
        mCallbackManager = CallbackManager.Factory.create();
        btnFacebook.setReadPermissions("email", "public_profile");
        btnFacebook.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
                loginPresenter.toast("Cancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(TAG, "facebook:onError", error);
                loginPresenter.toast("Error");
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        signInWithCredential(credential);
    }

    private void signInWithCredential(AuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener((Activity) view, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "signInWithCredential:success");
                loginPresenter.login(mAuth.getCurrentUser());
                checkUserId();
            } else {
                Log.w(TAG, "signInWithCredential:failure", task.getException());
                loginPresenter.toast(Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                loginPresenter.toast("Google sign in failed");
            }
        } else {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public FirebaseUser isLoggedIn() {
        return mAuth.getCurrentUser();
    }

    private void checkUserId() {
        tripDaoFirebase = new TripDaoFirebase();
        tripDaoSQL = new TripDaoSQL(context);
        user = mAuth.getCurrentUser();
        userId = user.getUid();
        /*if(userData.getString("userId", null).equals(userId)){
            if(tripDaoSQL.getAllTrips().size() > tripDaoFirebase.getAllTrips().size()){
                //remove firbase
            }
        }else {
            editor.putString("userId", userId);
            editor.commit();
            tripDaoSQL.deleteAllTrips();
            for(Trip trip: tripDaoFirebase.getAllTrips()){
                tripDaoSQL.insertTrip(trip);
            }

        }*/
    }

}
