package com.smartvision.JxClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.*;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements Handler.Callback, TabHost.OnTabChangeListener, AdapterView.OnItemClickListener {
    private final static String MOBILE_CLIENT = "lifc.mobile_client";
    private final static String APK = "MobileClient";
    private final static int PORT = 7000;
    private final static int GOT_SPOTS = 0;
    private final static int GOT_SPOT = 1;
    private final static int ONLINE = 0;
    private final static int ALL = 1;
    private final static int ID = 0;
    private final static int NAME = 1;
    private final static int SERVER = 1;
    private final static int[] LIST_STYLE = {android.R.layout.simple_list_item_single_choice, android.R.layout.simple_list_item_1};
    private final static String[] URI = {"/mobile_list_online/", "/mobile_list_all/"};
    private final static String[] FROM = {"name"};
    private final static int[] TO = {android.R.id.text1};

    private ListView[] listView = new ListView[2];
    private SimpleAdapter listAdpater[] = new SimpleAdapter[2];
    private List<Map<String, String>>[] spots = new List[2];
    private String userId;
    private Handler handler;
    private ProgressDialog progressDialog;
    private TabHost tabHost;
    private boolean[] inited = {false, false};
    private DefaultHttpClient httpClient;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isMobileClientExists()) {
            installApk();
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.spot_select);

        tabHost = (TabHost) this.findViewById(R.id.tabHost);
        tabHost.setup();
        tabHost.setOnTabChangedListener(this);

        TabHost.TabSpec showOnline = tabHost.newTabSpec("online");
        showOnline.setContent(R.id.onlineSpots);
        showOnline.setIndicator(getString(R.string.showOnline));
        tabHost.addTab(showOnline);

        TabHost.TabSpec showAll = tabHost.newTabSpec("all");
        showAll.setContent(R.id.allSpots);
        showAll.setIndicator(getString(R.string.showAll));
        tabHost.addTab(showAll);

        listView[ONLINE] = (ListView) findViewById(R.id.onlineSpots);
        listView[ALL] = (ListView) findViewById(R.id.allSpots);

        listView[ONLINE].setOnItemClickListener(this);
        //listView[ALL].setOnItemClickListener(this);

        handler = new Handler(this);
        ImageView refresh = (ImageView) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSpots();
            }
        });
    }

    private boolean isMobileClientExists() {
        try {
            getPackageManager().getPackageInfo(MOBILE_CLIENT, PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    private void installApk() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //String apkPath = "/data/data/" + getPackageName() + "/files";
        String apkPath = "/sdcard";
        File file = new File(apkPath, APK + ".apk");
        try {
            InputStream is = getAssets().open(APK + ".jpg");

            file.createNewFile();
            //FileOutputStream os = openFileOutput(file.getName(), Context.MODE_WORLD_WRITEABLE);
            FileOutputStream os = new FileOutputStream(file,true);
            byte[] bytes = new byte[1024];
            int count;
            while ((count = is.read(bytes)) > 0) {
                os.write(bytes, 0, count);
            }

            os.close();
            is.close();

            String permission = "666";

            try {
                String command = "chmod " + permission + " " + apkPath + "/" + APK + ".apk";
                Runtime runtime = Runtime.getRuntime();
                runtime.exec(command);
            } catch (IOException e) {
                Log.e(SignInActivity.TAG,"exception",e);
            }

        } catch (Exception e) {
            Log.e(SignInActivity.TAG,"exception",e);
        }
        intent.setDataAndType(Uri.fromFile(file),"application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }

    private void sign_in() {
        Intent intent = new Intent("SignIn");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        if (data == null) {
            finish();
        } else {
            Bundle bundle = data.getExtras();
            userId = bundle.getString("userId");
            getSpots();
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId == null) {
            sign_in();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle(R.string.confirmExit)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    private void showProgressDialog(String title) {
        if (progressDialog == null)
            progressDialog = new ProgressDialog(MainActivity.this);
        //progressDialog.setTitle(title);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(title);
        progressDialog.show();
    }

    private void getSpots() {
        showProgressDialog(getString(R.string.loading_spots));
        if (httpClient == null) httpClient = new DefaultHttpClient();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = SignInActivity.BASE_URL + "/spots" + URI[tabHost.getCurrentTab()] + userId;
                HttpGet httpGet = new HttpGet(url);
                String _spots = "";
                try {
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    if (HttpStatus.SC_OK == httpResponse.getStatusLine().getStatusCode()) {
                        _spots = EntityUtils.toString(httpResponse.getEntity(), HTTP.UTF_8);
                        //Log.e("SPOTS",_spots);
                    }
                } catch (Exception e) {
                    Log.e(SignInActivity.TAG, "exception", e);
                }
                Message message = new Message();
                message.what = GOT_SPOTS;
                message.obj = _spots;
                handler.sendMessage(message);
            }
        }).start();
    }

    private void getSpot(final String id) {
        showProgressDialog(getString(R.string.loading_spot));

        if (httpClient == null) httpClient = new DefaultHttpClient();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = SignInActivity.BASE_URL + "/spots/mobile_get/" + id;
                HttpGet httpGet = new HttpGet(url);
                String _spot = "";
                try {
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    if (HttpStatus.SC_OK == httpResponse.getStatusLine().getStatusCode()) {
                        _spot = EntityUtils.toString(httpResponse.getEntity(), HTTP.UTF_8);
                    }
                } catch (Exception e) {
                    Log.e(SignInActivity.TAG, "exception", e);
                }
                Message message = new Message();
                message.what = GOT_SPOT;
                message.obj = _spot;
                handler.sendMessage(message);
            }
        }).start();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case GOT_SPOTS:
                String spotString = msg.obj.toString();
                int tabIndex = tabHost.getCurrentTab();
                String[] _spots = spotString.split("`");

                spots[tabIndex] = new ArrayList<Map<String, String>>();
                for (String _spot : _spots) {
                    Map<String, String> item = new HashMap<String, String>();
                    String[] s = _spot.split("~");
                    item.put("id", s[ID]);
                    item.put("name", s[NAME]);
                    spots[tabIndex].add(item);
                }
                inited[tabIndex] = true;
                listAdpater[tabIndex] = new SimpleAdapter(this, spots[tabIndex], LIST_STYLE[tabIndex], FROM, TO);
                listView[tabIndex].setAdapter(listAdpater[tabIndex]);
                progressDialog.dismiss();
                break;
            case GOT_SPOT:
                progressDialog.dismiss();
                if (!isMobileClientExists()) {
                    installApk();
                } else {
                    String[] spot = msg.obj.toString().split(":");
                    Intent intent = new Intent(MOBILE_CLIENT + ".CONNECT");
                    intent.putExtra("ip", spot[SERVER]);
                    intent.putExtra("port", PORT);
                    intent.putExtra("stream_id", Integer.parseInt(spot[ID]));
                    startActivity(intent);
                }
                break;
        }
        return false;
    }

    @Override
    public void onTabChanged(String tabId) {
        if (userId == null) return;
        if (!inited[tabHost.getCurrentTab()]) {
            getSpots();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        getSpot(spots[tabHost.getCurrentTab()].get(position).get("id"));
    }
}