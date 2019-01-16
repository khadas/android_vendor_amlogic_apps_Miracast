package com.droidlogic.miracast;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.os.HwBinder;
import java.util.NoSuchElementException;

import java.util.ArrayList;

import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import vendor.amlogic.hardware.miracastserver.V1_0.IMiracastServer;
import vendor.amlogic.hardware.miracastserver.V1_0.IMiracastServerCallback;
import vendor.amlogic.hardware.miracastserver.V1_0.ConnectType;
import vendor.amlogic.hardware.miracastserver.V1_0.Result;


public class MiracastControlManager {
    private static final String TAG = "MiracastControlManager";

    private static MiracastControlManager mSingleton = null;
    private ArrayList<WatchHandler> mHandlers = new ArrayList<>();
    // Notification object used to listen to the start of the rpcserver daemon.
    private final ServiceNotification mServiceNotification = new ServiceNotification();
    private static final int MIRACASTSERVER_DEATH_COOKIE = 1000;
    private IMiracastServer mProxy = null;
    private HALCallback mHALCallback;
    // Mutex for all mutable shared state.
    private final Object mLock = new Object();
    /** Event type: Data was read from a file */
    public static final int ACCESS = 0x00000001;
    /** Event type: Data was written to a file */
    public static final int MODIFY = 0x00000002;
    /** Event type: Metadata (permissions, owner, timestamp) was changed explicitly */
    public static final int ATTRIB = 0x00000004;
    /** Event type: Someone had a file or directory open for writing, and closed it */
    public static final int CLOSE_WRITE = 0x00000008;
    /** Event type: Someone had a file or directory open read-only, and closed it */
    public static final int CLOSE_NOWRITE = 0x00000010;
    /** Event type: A file or directory was opened */
    public static final int OPEN = 0x00000020;
    /** Event type: A file or subdirectory was moved from the monitored directory */
    public static final int MOVED_FROM = 0x00000040;
    /** Event type: A file or subdirectory was moved to the monitored directory */
    public static final int MOVED_TO = 0x00000080;
    /** Event type: A new file or subdirectory was created under the monitored directory */
    public static final int CREATE = 0x00000100;
    /** Event type: A file was deleted from the monitored directory */
    public static final int DELETE = 0x00000200;
    /** Event type: The monitored file or directory was deleted; monitoring effectively stops */
    public static final int DELETE_SELF = 0x00000400;
    /** Event type: The monitored file or directory was moved; monitoring continues */
    public static final int MOVE_SELF = 0x00000800;

    final class ServiceNotification extends IServiceNotification.Stub {
        @Override
        public void onRegistration(String fqName, String name, boolean preexisting) {
            Log.i(TAG, "rpcserver HIDL service started " + fqName + " " + name);
            connectToProxy();
        }
    }

    private void connectToProxy() {
        synchronized (mLock) {
            if (mProxy != null) {
                return;
            }

            try {
                mProxy = IMiracastServer.getService();
                mProxy.linkToDeath(new DeathRecipient(), MIRACASTSERVER_DEATH_COOKIE);
                mProxy.setCallback(mHALCallback, ConnectType.TYPE_EXTEND);
            } catch (NoSuchElementException e) {
                Log.e(TAG, "connectToProxy: MiracastServer HIDL service not found."
                        + " Did the service fail to start?", e);
            } catch (RemoteException e) {
                Log.e(TAG, "connectToProxy: MiracastServer HIDL service not responding", e);
            }
        }

        Log.i(TAG, "connect to MiracastServer HIDL service success");
    }

    private static class HALCallback extends IMiracastServerCallback.Stub {
        MiracastControlManager MiracastClient;
        HALCallback(MiracastControlManager mccm) {
            MiracastClient = mccm;
    }

    public void notifyCallback(ArrayList<String> AddrList) {
            Log.i(TAG, "notifyCallback resource AddrList size = " + AddrList.size());
            for (WatchHandler handler : MiracastClient.mHandlers) {
                handler.onEvent(AddrList);
            }

        }
    }

    final class DeathRecipient implements HwBinder.DeathRecipient {
        DeathRecipient() {
        }

        @Override
        public void serviceDied(long cookie) {
            if (MIRACASTSERVER_DEATH_COOKIE == cookie) {
                Log.e(TAG, "dtvkitserver HIDL service died cookie: " + cookie);
                synchronized (mLock) {
                    mProxy = null;
                }
            }
        }
    }
    interface WatchHandler {
        void onEvent(ArrayList<String> AddrList);
    }

    protected MiracastControlManager() {
        // Singleton
        mHALCallback = new HALCallback(this);
        connectIfUnconnected();
    }

    public static MiracastControlManager getInstance() {
        if (mSingleton == null) {
            mSingleton = new MiracastControlManager();
        }
        return mSingleton;
    }

    public void registerSignalHandler(WatchHandler handler) {
        if (!mHandlers.contains(handler)) {
            mHandlers.add(handler);
        }
    }

    public void unregisterSignalHandler(WatchHandler handler) {
        if (mHandlers.contains(handler)) {
            mHandlers.remove(handler);
        }
    }

    private void connectIfUnconnected() {
        try {
            boolean ret = IServiceManager.getService()
                .registerForNotifications("vendor.amlogic.hardware.miracastserver@1.0::IMiracastServer", "", mServiceNotification);
            if (!ret) {
                Log.e(TAG, "Failed to register service start notification");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register service start notification", e);
        }
            connectToProxy();
    }

    public int StartWatching(String path, int mask) {
        synchronized (mLock) {
            try {
                return mProxy.startWatching(path, mask);
            } catch (RemoteException e) {
                Log.e(TAG, "StartWatching:" + e);
            }
        }
        return -1;
    }

    public void StopWatching() {
        synchronized (mLock) {
            try {
                 mProxy.stopWatching();
            } catch (RemoteException e) {
                Log.e(TAG, "StopWatching:" + e);
            }
        }
    }
}
