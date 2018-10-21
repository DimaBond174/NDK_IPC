package com.bond.ipclib;

public class IPClib {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public static native String stringFromJNI();
    public static native int init(String appname, String shmname, String appname2, int strsize);
    public static native int start();
    public static native void stop();
    public static native int getThreadState();
    public static native void sendShared(byte[] data, int len);
    public static native byte[] getShared();
    public static native String getSock();
    public static native void sendSock(String str);
    public static native String getALoop();
    public static native void sendALoop(String str);
}
