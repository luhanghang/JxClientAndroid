package com.smartvision.JxClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;

public class SignInActivity extends Activity implements View.OnClickListener, Handler.Callback {
    /**
     * Called when the activity is first created.
     */
    protected static final String TAG = "JXCLIENT";
    protected static String BASE_URL;

    private final static int SERVER_ERROR = 1;
    private final static int SIGN_ERROR = 2;
    private final static int SIGN_SUCCESS = 3;
    private final static int INVALID_SIGN_IN = 4;

    private SharedPreferences prefs;
    private MyEditText server;
    private MyEditText port;

    private MyEditText username;
    private MyEditText passwd;
    private CheckBox checkBox;
    private Button sign_in;

    private Handler handler;

    private DefaultHttpClient httpClient;
    private String userId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt("version", MainActivity.getVersionCode(this)).commit();
        server = (MyEditText) findViewById(R.id.server);
        port = (MyEditText) findViewById(R.id.port);
        username = (MyEditText) findViewById(R.id.username);
        passwd = (MyEditText) findViewById(R.id.passwd);
        checkBox = (CheckBox) findViewById(R.id.save_setting);

        handler = new Handler(this);
        sign_in = (Button) findViewById(R.id.sign_in);

        server.setText(prefs.getString("server", ""));
        port.setText(prefs.getString("port", ""));
        username.setText(prefs.getString("username", ""));
        passwd.setText(prefs.getString("passwd", ""));
        checkBox.setChecked(prefs.getBoolean("save", true));

        sign_in.setOnClickListener(this);
    }

    private void restoreSignInButton() {
        sign_in.setText(getString(R.string.sign_in));
        sign_in.setEnabled(true);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case SERVER_ERROR:
                message(getString(R.string.server_error));
                restoreSignInButton();
                break;
            case SIGN_ERROR:
                message(getString(R.string.sign_in_error));
                restoreSignInButton();
                break;
            case SIGN_SUCCESS:
                Intent intent = new Intent();
                intent.putExtra("userId", userId);
                setResult(RESULT_OK, intent);
                finish();
                break;
            case INVALID_SIGN_IN:
                message(getString(R.string.invalid_inf));
                restoreSignInButton();
                break;
        }
        return false;
    }

    private void message(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle(R.string.confirmExit)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(RESULT_OK,null);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    @Override
    public void onClick(View v) {
        if (port.getText().toString().trim().equals("")) port.setText("80");

        if (server.getText().toString().trim().equals("") || username.getText().toString().trim().equals("") || passwd.getText().toString().equals("")) {
            handler.sendEmptyMessage(INVALID_SIGN_IN);
            return;
        }

        saveSetting();

        sign_in.setText(getString(R.string.signing_in));
        sign_in.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                sign_in();
                Looper.loop();
            }
        }).start();
    }

    private void saveSetting() {
        prefs.edit().putString("server", checkBox.isChecked() ? server.getText().toString() : "").commit();
        prefs.edit().putString("port", checkBox.isChecked() ? port.getText().toString() : "80").commit();
        prefs.edit().putString("username", checkBox.isChecked() ? username.getText().toString() : "").commit();
        prefs.edit().putString("passwd", checkBox.isChecked() ? passwd.getText().toString() : "").commit();
        prefs.edit().putBoolean("save", checkBox.isChecked()).commit();
    }

    private void sign_in() {
        httpClient = new DefaultHttpClient();
        BASE_URL = "http://" + server.getText() + ":" + port.getText();
        String url = BASE_URL + "/users/sign_in_mobile";
        HttpPost httpPost = new HttpPost(url);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("account", username.getText().toString().trim()));
        params.add(new BasicNameValuePair("passwd", passwd.getText().toString().trim()));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (HttpStatus.SC_OK == httpResponse.getStatusLine().getStatusCode()) {
                String response = EntityUtils.toString(httpResponse.getEntity());
                if (response.indexOf("success:") == 0) {
                    userId = response.split(":")[1];
                    handler.sendEmptyMessage(SIGN_SUCCESS);
                } else {
                    handler.sendEmptyMessage(SIGN_ERROR);
                }
            } else {
                handler.sendEmptyMessage(SERVER_ERROR);
            }
        } catch (Exception e) {
            Log.e(TAG,"exception", e);
            handler.sendEmptyMessage(SERVER_ERROR);
        }
    }
}
