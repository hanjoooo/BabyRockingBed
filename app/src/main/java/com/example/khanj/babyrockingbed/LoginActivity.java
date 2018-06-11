package com.example.khanj.babyrockingbed;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.Arrays;



public class LoginActivity extends BaseActivity {
    private CallbackManager callbackManager;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mConditionRef = mRootRef.child("users");
    DatabaseReference mchildRef;
    DatabaseReference mchild1Ref;

    Button login;


    EditText edId;
    EditText edpassword;

    TextView regist;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        setContentView(R.layout.activity_login);
        callbackManager = CallbackManager.Factory.create();
        mAuth = FirebaseAuth.getInstance();
        login = (Button) findViewById(R.id.Login);
        edId = (EditText) findViewById(R.id.ID);
        edpassword = (EditText) findViewById(R.id.Password);
        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        regist = (TextView)findViewById(R.id.txregist);
        regist.setPaintFlags(regist.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

        if (!hasPermissions(PERMISSIONS)) { //퍼미션 허가를 했었는지 여부를 확인
            requestNecessaryPermissions(PERMISSIONS);//퍼미션 허가안되어 있다면 사용자에게 요청
        } else {
            //이미 사용자에게 퍼미션 허가를 받음.
        }

        loginButton.setReadPermissions(Arrays.asList("public_profile", "email"));
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                GraphRequest graphRequest = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.v("result",object.toString());
                    }
                });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender,birthday");
                graphRequest.setParameters(parameters);
                graphRequest.executeAsync();

                handleFacebookAccessToken(loginResult.getAccessToken());




            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {
                Log.e("LoginErr",error.toString());
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn(edId.getText().toString(),edpassword.getText().toString());
            }
        });
        regist.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                if(event.getAction()==MotionEvent.ACTION_DOWN){
                    if(regist.getClass()==v.getClass()){
                        regist.setTextColor(Color.RED);
                    }
                }
                if(event.getAction()==MotionEvent.ACTION_UP){
                    if(regist.getClass()==v.getClass()){
                        regist.setTextColor(Color.BLACK);
                        mAuth.signOut();
                        Intent intent = new Intent(getApplicationContext(), RegistActivity.class);
                        startActivity(intent);
                    }
                }
                return true;
            }
        });

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    mchildRef = mConditionRef.child(user.getUid());
                    mchild1Ref = mchildRef.child("아기상태");
                    mchild1Ref.setValue("0");
                    mchild1Ref = mchildRef.child("흔들침대");
                    mchild1Ref.setValue("0");
                    finish();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
                else {
                    // User is signed out
                }
                // ...


            }
        };
    }
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.

        mAuth.addAuthStateListener(mAuthListener);
    }
    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information


                        } else {
                            // If sign in fails, display a message to the user.

                        }

                        // ...
                    }
                });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
    private void signIn(String email, String password) {
        if (!validateForm()) {
            return;
        }
        showProgressDialog();
        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {


                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "인증 실패",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else{

                        }
                        hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
        // [END sign_in_with_email]
    }
    private boolean validateForm() {
        boolean valid = true;

        String email = edId.getText().toString();
        if (TextUtils.isEmpty(email)) {
            edId.setError("Required.");
            valid = false;
        } else {
            edId.setError(null);
        }

        String password = edpassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            edpassword.setError("Required.");
            valid = false;
        } else {
            edpassword.setError(null);
        }

        return valid;
    }



    static final int PERMISSION_REQUEST_CODE = 1;
    String[] PERMISSIONS  = {"android.permission.ACCESS_COARSE_LOCATION"};
    @SuppressLint("WrongConstant")
    private boolean hasPermissions(String[] permissions) {
        int ret = 0;
        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions){
            ret = checkCallingOrSelfPermission(perms);
            if (!(ret == PackageManager.PERMISSION_GRANTED)){
                //퍼미션 허가 안된 경우
                return false;
            }
        }

        //모든 퍼미션이 허가된 경우
        return true;
    }

    private void requestNecessaryPermissions(String[] permissions) {
        //마시멜로( API 23 )이상에서 런타임 퍼미션(Runtime Permission) 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        switch(permsRequestCode){

            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean Accepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        if (!Accepted  )
                        {
                            showDialogforPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                            return;
                        }else
                        {
                            //이미 사용자에게 퍼미션 허가를 받음.
                        }
                    }
                }
                break;
        }
    }

    private void showDialogforPermission(String msg) {

        final AlertDialog.Builder myDialog = new AlertDialog.Builder( LoginActivity.this);
        myDialog.setTitle("알림");
        myDialog.setMessage(msg);
        myDialog.setCancelable(false);
        myDialog.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
                }

            }
        });
        myDialog.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        myDialog.show();
    }

}


