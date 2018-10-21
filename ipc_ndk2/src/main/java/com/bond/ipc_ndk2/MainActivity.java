package com.bond.ipc_ndk2;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.bond.ipclib.IPClib;

public class MainActivity extends AppCompatActivity {
    com.bond.ipclib.GUI mainWindow;
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

        mainWindow = (com.bond.ipclib.GUI) findViewById(R.id.mainWindow);

        if (1==IPClib.init("ndk2", "shm0", "ndk1", MaxMsgSize)) {
            if (1 == IPClib.start()) {
                mainWindow.setMaxMsgSize(MaxMsgSize);
                mainWindow.setMessage("IPClib started");
            } else {
                mainWindow.setMessage("IPClib start ERROR");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainWindow.updateState();
    }

}
