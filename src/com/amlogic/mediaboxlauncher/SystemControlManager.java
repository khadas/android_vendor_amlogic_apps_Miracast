package com.amlogic.mediaboxlauncher;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class SystemControlManager {   
    private static final String TAG             = "SysControlManager";
    
    public static final int REMOTE_EXCEPTION    = -0xffff;

    int GET_PROPERTY                            = IBinder.FIRST_CALL_TRANSACTION;
    int GET_PROPERTY_STRING                     = IBinder.FIRST_CALL_TRANSACTION + 1;
    int GET_PROPERTY_INT                        = IBinder.FIRST_CALL_TRANSACTION + 2;
    int GET_PROPERTY_LONG                       = IBinder.FIRST_CALL_TRANSACTION + 3;
    int GET_PROPERTY_BOOL                       = IBinder.FIRST_CALL_TRANSACTION + 4;
    int SET_PROPERTY                            = IBinder.FIRST_CALL_TRANSACTION + 5;
    int READ_SYSFS                              = IBinder.FIRST_CALL_TRANSACTION + 6;
    int WRITE_SYSFS                             = IBinder.FIRST_CALL_TRANSACTION + 7;
           
    int GET_BOOT_ENV                            = IBinder.FIRST_CALL_TRANSACTION + 8;
    int SET_BOOT_ENV                            = IBinder.FIRST_CALL_TRANSACTION + 9;
    
    private Context mContext;
    private IBinder mIBinder = null;
    public SystemControlManager(Context context){
        mContext = context;
        
        try {
            Object object = Class.forName("android.os.ServiceManager")
                    .getMethod("getService", new Class[] { String.class })
                    .invoke(null, new Object[] { "system_control" });
            mIBinder = (IBinder)object;
        }
        catch (Exception ex) {
            Log.e(TAG, "system control manager init fail:" + ex);
        }
    }
    
    public String getProperty(String prop){
        try {
            if (null != mIBinder) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.app.ISystemControlService");
                data.writeString(prop);
                mIBinder.transact(GET_PROPERTY, data, reply, 0);
                int result = reply.readInt();
                String value = reply.readString();
                reply.recycle();
                data.recycle();
                return value;
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "getProperty:" + ex);
        }
        
        return null;
    }
    
    public String getPropertyString(String prop, String def){
        try {
            if (null != mIBinder) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.app.ISystemControlService");
                data.writeString(prop);
                data.writeString(def);
                mIBinder.transact(GET_PROPERTY_STRING, data, reply, 0);
                int result = reply.readInt();
                String value = reply.readString();
                reply.recycle();
                data.recycle();
                return value;
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "getPropertyString:" + ex);
        }
        
        return null;
    }
    
    public int getPropertyInt(String prop, int def){
        try {
            if (null != mIBinder) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.app.ISystemControlService");
                data.writeString(prop);
                data.writeInt(def);
                mIBinder.transact(GET_PROPERTY_INT, data, reply, 0);
                int result = reply.readInt();
                reply.recycle();
                data.recycle();
                return result;
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "getPropertyInt:" + ex);
        }
        
        return 0;
    }
    
    public long getPropertyLong(String prop, long def){
        try {
            if (null != mIBinder) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.app.ISystemControlService");
                data.writeString(prop);
                data.writeLong(def);
                mIBinder.transact(GET_PROPERTY_LONG, data, reply, 0);
                long result = reply.readLong();
                reply.recycle();
                data.recycle();
                return result;
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "getPropertyLong:" + ex);
        }
        
        return 0;
    }
    
    public boolean getPropertyBoolean(String prop, boolean def){
        try {
            if (null != mIBinder) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.app.ISystemControlService");
                data.writeString(prop);
                data.writeInt(def?1:0);
                mIBinder.transact(GET_PROPERTY_BOOL, data, reply, 0);
                int result = reply.readInt();
                reply.recycle();
                data.recycle();
                return result!=0;
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "getPropertyBoolean:" + ex);
        }
        
        return false;
    }
    
    public void setProperty(String prop, String val){
        try {
            if (null != mIBinder) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.app.ISystemControlService");
                data.writeString(prop);
                data.writeString(val);
                mIBinder.transact(SET_PROPERTY, data, reply, 0);
                reply.recycle();
                data.recycle();
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "setProperty:" + ex);
        }
    }
    
    public String readSysFs(String path){
        try {
            if (null != mIBinder) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.app.ISystemControlService");
                data.writeString(path);
                mIBinder.transact(READ_SYSFS, data, reply, 0);
                String value = reply.readString();
                reply.recycle();
                data.recycle();
                return value;
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "readSysFs:" + ex);
        }
        
        return null;
    }
    
    public boolean writeSysFs(String path, String val){
        try {
            if (null != mIBinder) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.app.ISystemControlService");
                data.writeString(path);
                data.writeString(val);
                mIBinder.transact(WRITE_SYSFS, data, reply, 0);
                int result = reply.readInt();
                reply.recycle();
                data.recycle();
                return result!=0;
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "writeSysFs:" + ex);
        }
        
        return false;
    }
    
    public String getBootenv(String prop, String def){
        try {
            if (null != mIBinder) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.app.ISystemControlService");
                data.writeString(prop);
                mIBinder.transact(GET_BOOT_ENV, data, reply, 0);
                int result = reply.readInt();
                String value = reply.readString();
                reply.recycle();
                data.recycle();
                if(0 == result)
                    return def;//have some error
                else
                    return value;
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "getProperty:" + ex);
        }
        
        return null;
    }
    
    public void setBootenv(String prop, String val){
        try {
            if (null != mIBinder) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.app.ISystemControlService");
                data.writeString(prop);
                data.writeString(val);
                mIBinder.transact(SET_BOOT_ENV, data, reply, 0);
                reply.recycle();
                data.recycle();
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "setProperty:" + ex);
        }
    }
}
