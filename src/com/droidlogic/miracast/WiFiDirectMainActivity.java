/**
 * @Package com.droidlogic.miracast
 * @Description Copyright (c) Inspur Group Co., Ltd. Unpublished Inspur Group
 *              Co., Ltd. Proprietary & Confidential This source code and the
 *              algorithms implemented therein constitute confidential
 *              information and may comprise trade secrets of Inspur or its
 *              associates, and any use thereof is subject to the terms and
 *              conditions of the Non-Disclosure Agreement pursuant to which
 *              this source code was originally received.
 */
package com.droidlogic.miracast;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Message;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.provider.Settings;
import android.graphics.drawable.AnimationDrawable;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

/**
 * @ClassName WiFiDirectMainActivity
 * @Description TODO
 * @Date 2013-6-20
 * @Email
 * @Author
 * @Version V1.0
 */
public class WiFiDirectMainActivity extends Activity implements
    ChannelListener, PeerListListener, ConnectionInfoListener, GroupInfoListener
{
    public static final String       TAG                    = "amlWifiDirect";
    public static final boolean      DEBUG                  = false;
    public static final String       HRESOLUTION_DISPLAY     = "display_resolution_hd";
    public static final String       WIFI_P2P_IP_ADDR_CHANGED_ACTION = "com.droidlogic.miracast.IP_ADDR_CHANGED";
    public static final String       WIFI_P2P_PEER_IP_EXTRA       = "IP_EXTRA";
    public static final String       WIFI_P2P_PEER_MAC_EXTRA       = "MAC_EXTRA";
    private static final String      MIRACAST_PREF          = "miracast_prefences";
    private static final String      IP_ADDR                = "ip_addr";
    private final String             FB0_BLANK              = "/sys/class/graphics/fb0/blank";
    public static final String ENCODING = "UTF-8";
    private static final String VERSION_FILE = "version";

    public static final String  ACTION_FIX_RTSP_FAIL 	= "com.droidlogic.miracast.RTSP_FAIL";
    public static final String  ACTION_REMOVE_GROUP 	= "com.droidlogic.miracast.REMOVE_GROUP";

    // private final String CLOSE_GRAPHIC_LAYER =
    // "echo 1 > /sys/class/graphics/fb0/blank";
    // private final String OPEN_GRAPHIC_LAYER =
    // "echo 0 > /sys/class/graphics/fb0/blank";
    private final String             WIFI_DISPLAY_CMD       = "wfd -c";
    private WifiP2pManager           manager;
    private boolean                  isWifiP2pEnabled       = false;
    private String                   mPort;
    private String                   mIP;
    private Handler                  mHandler               = new Handler();
    private static final int         MAX_DELAY_MS           = 500;
    private static final int DIALOG_RENAME = 3;
    private final IntentFilter       intentFilter           = new IntentFilter();
    private Channel                  channel;
    private BroadcastReceiver        mReceiver              = null;
    private BroadcastReceiver        mReceiver2              = null;
    private PowerManager.WakeLock    mWakeLock;
    private ImageView                mConnectStatus;
    private TextView                 mConnectWarn;
    private TextView                 mConnectDesc;
    private TextView                 mPeerList;
    private Button                 mClick2Settings;
    private boolean                  retryChannel           = false;
    private WifiP2pDevice            mDevice                = null;
    private ArrayList<WifiP2pDevice> peers                  = new ArrayList<WifiP2pDevice>();
    private ProgressDialog progressDialog = null;
    private OnClickListener mRenameListener;
    private EditText mDeviceNameText;
    private TextView mDeviceNameShow;
    private TextView mDeviceTitle;
    private String mSavedDeviceName;
    private int mNetId = -1;
    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;
    private MenuItem mDisplayResolution;

    private File mFolder = new File("/data/misc/dhcp");
    private FileObserver mAddrObserver = new FileObserver(mFolder.getPath(), FileObserver.MODIFY | FileObserver.CREATE)
    {
        public void onEvent(int event, String path) {
            Log.d(TAG, "WFD : File changed : path=" + path + " event=" + event);
            if (null == path)
            {
                return;
            }

            if (path.equals(new String("dnsmasq.leases")))
            {
                File ipFile = new File(mFolder, path);
                String fullName = ipFile.getPath();
                parseDnsmasqAddr(fullName);
            }
        }
    };

    private boolean parseDnsmasqAddr(String fileName)
    {
        File file = new File(fileName);
        BufferedReader reader = null;
        String info = new String();
        try {
            reader = new BufferedReader(new FileReader(file));
            info = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
            {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (info == null)
                {
                    Log.d (TAG, "parseDnsmasqAddr info is NULL");
                    return false;
                }
                else
                {
                    StringTokenizer strToke = new StringTokenizer(info," ");
                    if (strToke.hasMoreElements())
                    {
                        strToke.nextToken();
                        if (strToke.hasMoreElements())
                        {
                            String mac = strToke.nextToken();
                            if (strToke.hasMoreElements())
                            {
                                String ip = strToke.nextToken();
                                Log.d (TAG, "Sending WIFI_P2P_IP_ADDR_CHANGED_ACTION broadcast : ip=" + ip + " mac=" + mac);
                                Intent intent = new Intent(WIFI_P2P_IP_ADDR_CHANGED_ACTION);
                                intent.putExtra(WIFI_P2P_PEER_IP_EXTRA, ip);
                                intent.putExtra(WIFI_P2P_PEER_MAC_EXTRA, mac);
                                sendBroadcastAsUser(intent, UserHandle.ALL);
                                return true;
                            }
                        }
                    }
                    return false;
                }
            }
            else
                return false;
        }
    }

    private final Runnable startSearchRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            startSearch();
        }
    };

    public void startSearchTimer()
    {
        if (DEBUG)
        {
            Log.d (TAG, " startSearchTimer 6s");
        }
        mHandler.postDelayed (startSearchRunnable, 6000);
    }

    public void cancelSearchTimer()
    {
        if (DEBUG)
        {
            Log.d (TAG, " cancelSearchTimer");
        }
        mHandler.removeCallbacks (startSearchRunnable);
    }

    @Override
    public void onContentChanged()
    {
        super.onContentChanged();
    }

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume()
    {
        super.onResume();
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "removeGroup Success");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "removeGroup Failure");
            }
        });

        File ipFile = new File(mFolder, "dnsmasq.leases");
        if (ipFile.exists ()) {
            Log.d(TAG, "delete" + ipFile.toString());
            ipFile.delete();
        }
        /* enable backlight */
        mReceiver = new WiFiDirectBroadcastReceiver (manager, channel, this);
        PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock (PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                    | PowerManager.ON_AFTER_RELEASE, TAG);
        mWakeLock.acquire();
        registerReceiver (mReceiver, intentFilter);
        mAddrObserver.startWatching();
        if (DEBUG)
        {
            Log.d (TAG, "onResume()");
        }
        mConnectStatus = (ImageView) findViewById (R.id.show_connect);
        mConnectDesc = (TextView) findViewById (R.id.show_connect_desc);
        mConnectWarn = (TextView) findViewById (R.id.show_desc_more);
        mClick2Settings = (Button) findViewById (R.id.settings_btn);
        mConnectDesc.setFocusable (true);
        mConnectDesc.requestFocus();
        mPeerList = (TextView) findViewById (R.id.peer_devices);

        mClick2Settings.setOnClickListener (new View.OnClickListener()
        {
            @Override
            public void onClick (View v)
            {
                WiFiDirectMainActivity.this.startActivity (new Intent (
                            Settings.ACTION_WIFI_SETTINGS) );
            }
        });
        if (!isNetAvailiable() )
        {
            mConnectWarn.setText (WiFiDirectMainActivity.this.getResources()
                                  .getString (R.string.p2p_off_warning) );
            mConnectWarn.setVisibility (View.VISIBLE);
            mClick2Settings.setVisibility (View.VISIBLE);
            mConnectDesc.setFocusable (false);
        }
        mDeviceNameShow = (TextView) findViewById (R.id.device_dec);
        mDeviceTitle = (TextView) findViewById (R.id.device_title);
        if (mDevice != null)
        {
            mSavedDeviceName = mDevice.deviceName;
            mDeviceNameShow.setText (mSavedDeviceName);
        }
        else
        {
            mDeviceTitle.setVisibility (View.INVISIBLE);
        }
        resetData();
    }

    public void setDevice (WifiP2pDevice device)
    {
        mDevice = device;
        if (mDevice != null)
        {
            if (mDeviceTitle != null)
            {
                mDeviceTitle.setVisibility (View.VISIBLE);
            }
            mSavedDeviceName = mDevice.deviceName;
            if (mDeviceNameShow != null)
            {
                mDeviceNameShow.setText (mSavedDeviceName);
            }
        }
        if (WifiP2pDevice.CONNECTED == mDevice.status)
        {
            cancelSearchTimer();
        }
        if (DEBUG)
        {
            Log.d (TAG, "localDevice name:" + mDevice.deviceName + ", status:" + mDevice.status + " (0-CONNECTED,3-AVAILABLE)");
        }
    }

    public void startSearch()
    {
        if (DEBUG)
        {
            Log.d (TAG, "startSearch wifiP2pEnabled:" + isWifiP2pEnabled);
        }
        if (!isWifiP2pEnabled)
        {
            if (manager != null && channel != null)
            {
                mConnectWarn.setVisibility (View.VISIBLE);
                mConnectWarn.setText (WiFiDirectMainActivity.this.getResources()
                                      .getString (R.string.p2p_off_warning) );
                mClick2Settings.setVisibility (View.VISIBLE);
                mConnectDesc.setFocusable (false);
            }
            return;
        }
        manager.discoverPeers (channel, new WifiP2pManager.ActionListener()
        {
            @Override
            public void onSuccess()
            {
                Log.d(TAG, "discoverPeers init success");
            }

            @Override
            public void onFailure (int reasonCode)
            {
                Log.d(TAG, "discoverPeers init failure, reasonCode:" + reasonCode);
            }
        });
    }
    public void onQuery (MenuItem item)
    {
        if (mDisplayResolution == null)
        {
            return;
        }
        Resources res = WiFiDirectMainActivity.this.getResources();
        switch (item.getItemId() )
        {
        case R.id.setting_sd:
            mDisplayResolution.setTitle (res.getString (R.string.setting_definition)
                                         + " : " + res.getString (R.string.setting_definition_sd) );
            mEditor.putBoolean (HRESOLUTION_DISPLAY, false);
            mEditor.commit();
            break;
        case R.id.setting_hd:
            mDisplayResolution.setTitle (res.getString (R.string.setting_definition)
                                         + " : " + res.getString (R.string.setting_definition_hd) );
            mEditor.putBoolean (HRESOLUTION_DISPLAY, true);
            mEditor.commit();
            break;
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mAddrObserver.stopWatching();
        unregisterReceiver (mReceiver);
        mWakeLock.release();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.net.wifi.p2p.WifiP2pManager.ChannelListener#onChannelDisconnected
     * ()
     */
    @Override
    public void onChannelDisconnected()
    {
        if (manager != null && !retryChannel)
        {
            Toast.makeText (this, WiFiDirectMainActivity.this.getResources().getString (R.string.channel_try),
                            Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize (this, getMainLooper(), this);
        }
        else
        {
            Toast.makeText (
                this,
                WiFiDirectMainActivity.this.getResources().getString (R.string.channel_close),
                Toast.LENGTH_LONG).show();
            //retryChannel = false;
        }
    }

    public void resetData()
    {
        mConnectStatus.setBackgroundResource (R.drawable.wifi_connect);
        String sFinal1 = String.format (getString (R.string.connect_ready), getString (R.string.device_name) );
        mConnectDesc.setText (sFinal1);
        peers.clear();

        String list = WiFiDirectMainActivity.this.getResources().getString(R.string.peer_list);
        mPeerList.setText(list);
    }

    public void setConnect()
    {
        mConnectDesc.setText (getString (R.string.connected_info) );
        mConnectStatus.setBackgroundResource (R.drawable.wifi_yes);
    }

    public void setIsWifiP2pEnabled (boolean enable)
    {
        this.isWifiP2pEnabled = enable;
        String sFinal1 = String.format(getString(R.string.connect_ready),getString(R.string.device_name));
        mConnectDesc.setText(sFinal1);
        if (enable)
        {
            mConnectWarn.setVisibility(View.INVISIBLE);
            mClick2Settings.setVisibility(View.GONE);
            mConnectDesc.setFocusable(false);
        }
        else
        {
            mConnectWarn.setText(WiFiDirectMainActivity.this.getResources()
                        .getString(R.string.p2p_off_warning));
            mConnectWarn.setVisibility(View.VISIBLE);
            mClick2Settings.setVisibility (View.VISIBLE);
            mConnectDesc.setFocusable (true);
        }
    }

    public void startMiracast (String ip, String port)
    {
        mPort = port;
        mIP = ip;
        setConnect();
        Log.d (TAG, "start miracast delay " + MAX_DELAY_MS + " ms");
        mHandler.postDelayed (new Runnable()
        {
            public void run()
            {
                Intent intent = new Intent (WiFiDirectMainActivity.this,
                                            SinkActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString (SinkActivity.KEY_PORT, mPort);
                bundle.putString (SinkActivity.KEY_IP, mIP);
                bundle.putBoolean (HRESOLUTION_DISPLAY, mPref.getBoolean (HRESOLUTION_DISPLAY, true) );
                intent.putExtras (bundle);
                startActivity (intent);
            }
        }, MAX_DELAY_MS);
    }

    public void stopMiracast (boolean stop) {
        if ((manager == null) || !stop) {
            return;
        }

        manager.stopPeerDiscovery (channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d (TAG, "stopMiracast Success");
            }

            @Override
            public void onFailure (int reasonCode) {
                Log.d (TAG, "stopMiracast Failure");
            }
        });
    }

    private void fixRtspFail()
    {
        if (manager != null /*&& mNetId != -1*/)
        {
            manager.removeGroup (channel, null);
            if (mNetId != -1)
                manager.deletePersistentGroup (channel, mNetId, null);

            new AlertDialog.Builder (this)
            .setTitle (R.string.rtsp_fail)
            .setMessage (R.string.rtsp_suggestion)
            .setIconAttribute (android.R.attr.alertDialogIcon)
            .setPositiveButton (android.R.string.ok,
                                new DialogInterface.OnClickListener()
            {
                public void onClick (DialogInterface dialog, int whichButton)
                {
                }
            })
            .show();
        }
    }

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.connect_layout);

        // add necessary intent values to be matched.
        intentFilter.addAction (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intentFilter.addAction (WIFI_P2P_IP_ADDR_CHANGED_ACTION);
        manager = (WifiP2pManager) getSystemService (Context.WIFI_P2P_SERVICE);
        channel = manager.initialize (this, getMainLooper(), null);
        mRenameListener = new OnClickListener()
        {
            @Override
            public void onClick (DialogInterface dialog, int which)
            {
                if (which == DialogInterface.BUTTON_POSITIVE)
                {
                    if (manager != null)
                    {
                        manager.setDeviceName (channel,
                                               mDeviceNameText.getText().toString(),
                                               new WifiP2pManager.ActionListener()
                        {
                            public void onSuccess()
                            {
                                mSavedDeviceName = mDeviceNameText.getText().toString();
                                mDeviceNameShow.setText (mSavedDeviceName);
                                if (DEBUG)
                                {
                                    Log.d (TAG, " device rename success");
                                }
                            }
                            public void onFailure (int reason)
                            {
                                Toast.makeText (WiFiDirectMainActivity.this,
                                                R.string.wifi_p2p_failed_rename_message,
                                                Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        };
        mReceiver2 = new BroadcastReceiver()
        {
            @Override
            public void onReceive (Context context, Intent intent)
            {
                String action = intent.getAction();
                if (action.equals (ACTION_FIX_RTSP_FAIL) )
                {
                    Log.d (TAG, "ACTION_FIX_RTSP_FAIL : mNetId=" + mNetId);
                    fixRtspFail();
                }
                else if (action.equals (ACTION_REMOVE_GROUP) )
                {
                    Log.d (TAG, "ACTION_REMOVE_GROUP");
                    manager.removeGroup (channel, null);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction (ACTION_FIX_RTSP_FAIL);
        filter.addAction (ACTION_REMOVE_GROUP);
        registerReceiver (mReceiver2, filter);
        mPref = PreferenceManager.getDefaultSharedPreferences (this);
        mEditor = mPref.edit();
        changeRole(true);
    }

    @Override
    protected void onDestroy()
    {
        changeRole(false);
        unregisterReceiver (mReceiver2);
        stopMiracast (true);
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged (boolean hasFocus)
    {
        super.onWindowFocusChanged (hasFocus);
        mConnectStatus.setBackgroundResource (R.drawable.wifi_connect);
        AnimationDrawable anim = (AnimationDrawable) mConnectStatus.getBackground();
        anim.start();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.net.wifi.p2p.WifiP2pManager.PeerListListener#onPeersAvailable
     * (android.net.wifi.p2p.WifiP2pDeviceList)
     */
    @Override
    public void onPeersAvailable (WifiP2pDeviceList devicelist)
    {
        String list = WiFiDirectMainActivity.this.getResources().getString (R.string.peer_list);
        if (progressDialog != null && progressDialog.isShowing() )
        {
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll (devicelist.getDeviceList() );
        freshView();
        for (int i = 0; i < peers.size(); i++)
        {
            list += peers.get (i).deviceName + " ";
            if (DEBUG)
            {
                Log.d (TAG, "onPeersAvailable peerDevice:" + peers.get (i).deviceName + ", status:" + peers.get (i).status + " (0-CONNECTED,3-AVAILABLE)");
            }
        }
        mPeerList.setText (list);
    }

    public void onGroupInfoAvailable (WifiP2pGroup group)
    {
        if (group != null)
        {
            Log.d (TAG, "onGroupInfoAvailable true : " + group);
            mNetId = group.getNetworkId();
        }
        else
        {
            Log.d (TAG, "onGroupInfoAvailable false");
            mNetId = -1;
        }
    }

    /**
     * @Description TODO
     */
    private void freshView()
    {
        for (int i = 0; i < peers.size(); i++)
        {
            if (peers.get (i).status == WifiP2pDevice.CONNECTED)
            {
                mConnectDesc.setText (getString (R.string.connecting_desc)
                                      + peers.get (i).deviceName);
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu ( Menu menu )
    {
        getMenuInflater().inflate ( R.menu.action_items, menu );
        mDisplayResolution = menu.findItem (R.id.setting_definition);
        if (mPref != null)
        {
            boolean high = mPref.getBoolean (HRESOLUTION_DISPLAY, true);
            Resources res = WiFiDirectMainActivity.this.getResources();
            if (high)
            {
                mDisplayResolution.setTitle (res.getString (R.string.setting_definition)
                                             + " : " + res.getString (R.string.setting_definition_hd) );
            }
            else
            {
                mDisplayResolution.setTitle (res.getString (R.string.setting_definition)
                                             + " : " + res.getString (R.string.setting_definition_sd) );
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected ( MenuItem item )
    {
        int itemId = item.getItemId();
        switch ( itemId )
        {
        case R.id.about_version:
            AlertDialog.Builder builder = new Builder (WiFiDirectMainActivity.this);
            if (getResources().getConfiguration().locale.getCountry().equals ("CN") )
            {
                builder.setMessage (getFromAssets (VERSION_FILE + "_cn") );
            }
            else
            {
                builder.setMessage (getFromAssets (VERSION_FILE) );
            }
            builder.setTitle (R.string.about_version);
            builder.setPositiveButton (R.string.close_dlg, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick (DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            builder.create().show();
            break;
        case R.id.atn_direct_discover:
            mPeerList.setText (R.string.peer_list);
            startSearch();
            return true;
        case R.id.setting_name:
            showDialog (DIALOG_RENAME);
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected ( item );
    }

    public String getFromAssets (String fileName)
    {
        String result = "";
        InputStream in = null;
        try {
            in = getResources().getAssets().open(fileName);
            int lenght = in.available();
            byte[]  buffer = new byte[lenght];
            in.read(buffer);
            result = new String(buffer, ENCODING);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    private boolean isNetAvailiable()
    {
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info == null || !info.isAvailable() )
            {
                return false;
            } else if (ConnectivityManager.TYPE_WIFI != info.getType()) {
                Log.d(TAG, "ActiveNetwork TYPE is not WIFI");
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    public void onConnectionInfoAvailable (WifiP2pInfo info)
    {
        if (DEBUG)
        {
            Log.d (TAG, "onConnectionInfoAvailable info:" + info);
        }
    }

    @Override
    public Dialog onCreateDialog (int id)
    {
        if (id == DIALOG_RENAME)
        {
            mDeviceNameText = new EditText (this);
            if (mSavedDeviceName != null)
            {
                mDeviceNameText.setText (mSavedDeviceName);
                mDeviceNameText.setSelection (mSavedDeviceName.length() );
            }
            else if (mDevice != null && !TextUtils.isEmpty (mDevice.deviceName) )
            {
                mDeviceNameText.setText (mDevice.deviceName);
                mDeviceNameText.setSelection (0, mDevice.deviceName.length() );
            }
            mSavedDeviceName = null;
            AlertDialog dialog = new AlertDialog.Builder (this)
            .setTitle (R.string.change_name)
            .setView (mDeviceNameText)
            .setPositiveButton (WiFiDirectMainActivity.this.getResources().getString (R.string.dlg_ok), mRenameListener)
            .setNegativeButton (WiFiDirectMainActivity.this.getResources().getString (R.string.dlg_cancel), null)
            .create();
            return dialog;
        }
        return null;
    }

    private void changeRole (boolean isSink)
    {
        WifiP2pWfdInfo wfdInfo = new WifiP2pWfdInfo();

        if (isSink) {
            wfdInfo.setWfdEnabled(true);
            wfdInfo.setDeviceType(WifiP2pWfdInfo.PRIMARY_SINK);
            wfdInfo.setSessionAvailable(true);
            wfdInfo.setControlPort(7236);
            wfdInfo.setMaxThroughput(50);
        } else {
            wfdInfo.setWfdEnabled(false);
        }

        manager.setWFDInfo(channel, wfdInfo, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Successfully set WFD info.");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to set WFD info with reason " + reason + ".");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                WiFiDirectMainActivity.this.changeRole(true);
            }
        });
    }

    public void discoveryStop()
    {
        if (progressDialog != null && progressDialog.isShowing() )
        {
            progressDialog.dismiss();
        }
    }
}
