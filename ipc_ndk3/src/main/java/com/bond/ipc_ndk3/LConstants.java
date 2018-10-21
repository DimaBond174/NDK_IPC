package com.bond.ipc_ndk3;

public class LConstants {
    public final static String IPC_BROADCAST1   = "com.bond.IPC.fromGUI";
    public final static String IPC_BROADCAST2  = "com.bond.IPC.fromService";
    public final static String IPC_SERVICE_ID  = "com.bond.ipc_ndk3.serviceProcess";

    public static final long MSEC_KEEP_ALIVE = 100;
    public static final long MSEC_ANDROID_ANR = 5000;
    public static final int MaxMsgSize = 256;
}
