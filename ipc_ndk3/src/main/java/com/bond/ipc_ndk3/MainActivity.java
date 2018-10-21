package com.bond.ipc_ndk3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import com.bond.ipclib.IPClib;

public class MainActivity extends AppCompatActivity {

    com.bond.ipc_ndk3.GUI mainWindow;
    static final int MaxMsgSize = 256;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        mainWindow = (com.bond.ipc_ndk3.GUI) findViewById(R.id.mainWindow);

        if (1==IPClib.init("ndk3", "shm0", "ndk0", MaxMsgSize)) {
            if (1 == IPClib.start()) {
                mainWindow.setMaxMsgSize(MaxMsgSize);
                mainWindow.setContext(this);
                mainWindow.setMessage("IPClib started");
            } else {
                mainWindow.setMessage("IPClib start ERROR");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //startService(new Intent(LConstants.IPC_SERVICE_ID));
        //startService(new Intent(this, ForegroundService.class));
        Intent i = new Intent(LConstants.IPC_SERVICE_ID);
        i.putExtra("keepRun", true);
        getApplicationContext().startService(i);
        mainWindow.updateState();
        IntentFilter filter = new IntentFilter();
        filter.addAction(LConstants.IPC_BROADCAST2);
        //LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(bro, filter);
        registerReceiver(bro, filter);
    }

    @Override
    protected void onStop() {
        Intent i = new Intent(this, ForegroundService.class);
        i.putExtra("keepRun", false);
        getApplicationContext().startService(i);
        mainWindow.setContext(null);
        //LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(bro);
        unregisterReceiver(bro);
        IPClib.stop();
        getApplicationContext().stopService(new Intent(LConstants.IPC_SERVICE_ID));
        super.onStop();
    }

    private final BroadcastReceiver bro = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {

                Bundle b = null;
                if (null!=intent) b=intent.getExtras();
                if (null!=b) {
                    String str = b.getString("msg", "");
                    mainWindow.setMessage(str);
                }

            } catch (Exception e) {
                Log.e("BroadcastReceiver","error:",e);
            }

        }
    };
}
