package com.bond.ipc_ndk3;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bond.ipclib.IPClib;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class ForegroundService extends Service {
    private static final String TAG = "ForegroundService";


    private volatile boolean keepRun = false;
    private final KeepAliveThread keepAliveThread = new KeepAliveThread();



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void selfDestroyOnGuiThread() {

    }

    /** Called when the service is being created. */
    @Override
    public void onCreate() {
        Log.w(TAG, "Service:onCreate===> called");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.w(TAG, "Service:onStart===> called");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w(TAG, "Service:onStartCommand===> called");
        if (null!=intent) {
            Bundle b = intent.getExtras();
            if (null!=b) {
                keepRun = b.getBoolean("keepRun", false);
                Log.w(TAG, "keepRun ="+String.valueOf(keepRun));
                if (keepRun) {
                    //Log.w(TAG, "startIfNeed - must be started");
                    if (System.currentTimeMillis() - keepAliveThread.lastTimeAcive.get() > LConstants.MSEC_ANDROID_ANR) {
                        keepAliveThread.resume();
                    }
                } else {
                    keepAliveThread.pause();
                    selfDestroyOnGuiThread();
                }
            }
        }
        return START_STICKY;
    }//onStartCommand

    @Override
    public void onDestroy() {
        Log.v(TAG, "Service:onDestroy===> called");

    }

    private class KeepAliveThread implements Runnable {
        public final AtomicLong lastTimeAcive=new AtomicLong(0);
        public final ConditionVariable conditionVariable = new ConditionVariable();
        public Queue<Integer> msgQue= new ConcurrentLinkedQueue<Integer>();
        volatile Thread localThread = null;


        public synchronized void pause() {
            //Log.e(TAG, "KeepAliveThread Start pause() ");
            keepRun = false;
            if (null != localThread) {
                try {

                    if (localThread.isAlive()) {
                        conditionVariable.open();
                        localThread.interrupt();
                        localThread.join();
                    }
                } catch (InterruptedException e) {
                } catch (Exception e) {
                    Log.e(TAG, "KeepAliveThread pause() error:", e);
                }
                localThread = null;
            }
            // Log.w(TAG, "KeepAliveThread End pause() ");
        }


        public  void resume() {
            // Log.e(TAG,"KeepAliveThread resume() START");
            pause();
            keepRun = true;
            localThread = new Thread(this);
            localThread.start();
            //Log.w(TAG,"KeepAliveThread resume() END");
        }

        @Override
        public void run() {
            try {
                if (1!= IPClib.init("ndk0", "shm0", "ndk3", LConstants.MaxMsgSize)) {
                    keepRun = false;
                }

                if (1 != IPClib.start()) {
                    keepRun = false;
                }

                IntentFilter filter = new IntentFilter();
                filter.addAction(LConstants.IPC_BROADCAST1);
                //LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(bro, filter);
                registerReceiver(bro, filter);
                Log.w(TAG, "Service:while (keepRun)===> next");
                while (keepRun) {
                    conditionVariable.close();
                    long curTime = System.currentTimeMillis();
                    lastTimeAcive.set(curTime);

                    checkMail();

                    conditionVariable.block(LConstants.MSEC_KEEP_ALIVE);
                } //while
                //LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(bro);
                unregisterReceiver(bro);
                IPClib.stop();

            } catch (Exception e) {
               Log.e(TAG,"error:",e);
            }
        }

        private void checkMail(){
            if (msgQue.isEmpty()) { return;}
            Integer msg = msgQue.poll();
            String re = null;
            switch (msg) {
                case 1:
                    try {
                        byte[] arr = IPClib.getShared();
                        if (null!=arr){
                            re = new String(arr, "UTF-8");
                        }
                    } catch (Exception e) {
                        Log.e(TAG,"IPClib.onBtnGetShared( error:", e);
                    }
                    break;
                case 2:
                    re = IPClib.getSock();
                    break;
                case 3:
                    re = IPClib.getALoop();
                    break;
                default:
            }
            if (null!=re && !re.isEmpty()){
                Log.w(TAG, "checkMail()="+re);
                Intent i = new Intent(LConstants.IPC_BROADCAST2);
                i.putExtra("msg", "Received:"+re);
//                LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
//                if (null!=manager) {
                    try {
                        //manager.sendBroadcast(i);
                        sendBroadcast(i);
                    } catch (Exception e) {
                        Log.e(TAG,"checkMail() error:",e);
                    }
//                }
            }
        }
    } //KeepAliveThread



    private final BroadcastReceiver bro = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {

                Bundle b = null;
                if (null!=intent) b=intent.getExtras();
                if (null!=b) {
                    Integer i = b.getInt("Pressed", 0);
                    if (i>0) {
                        keepAliveThread.msgQue.add(i);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG,"error:",e);
            }

        }
    };



}
