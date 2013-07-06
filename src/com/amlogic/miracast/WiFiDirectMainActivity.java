/**
 * @Package com.amlogic.miracast
 * @Description Copyright (c) Inspur Group Co., Ltd. Unpublished Inspur Group
 *              Co., Ltd. Proprietary & Confidential This source code and the
 *              algorithms implemented therein constitute confidential
 *              information and may comprise trade secrets of Inspur or its
 *              associates, and any use thereof is subject to the terms and
 *              conditions of the Non-Disclosure Agreement pursuant to which
 *              this source code was originally received.
 */
package com.amlogic.miracast;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.provider.Settings;
import android.graphics.drawable.AnimationDrawable;

import java.io.InputStream;
import org.apache.http.util.EncodingUtils;

/**
 * @ClassName WiFiDirectMainActivity
 * @Description TODO
 * @Date 2013-6-20
 * @Email
 * @Author
 * @Version V1.0
 */
public class WiFiDirectMainActivity extends Activity implements
        ChannelListener, PeerListListener {
    public static final String       TAG                    = "amlWifiDirect";
    public static final boolean      DEBUG                  = false;
    public static final String       DNSMASQ_IP_ADDR_ACTION = "android.net.dnsmasq.IP_ADDR";
    public static final String       DNSMASQ_MAC_EXTRA      = "MAC_EXTRA";
    public static final String       DNSMASQ_IP_EXTRA       = "IP_EXTRA";
    public static final String       DNSMASQ_PORT_EXTRA     = "PORT_EXTRA";
    private static final String      MIRACAST_PREF          = "miracast_prefences";
    private static final String      IP_ADDR                = "ip_addr";
    private final String             FB0_BLANK              = "/sys/class/graphics/fb0/blank";
    public static final String ENCODING = "UTF-8";
    private static final String VERSION_FILE = "version";
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
    private static final int         MAX_DELAY_MS           = 3000;
    private final IntentFilter       intentFilter           = new IntentFilter();
    private Channel                  channel;
    private BroadcastReceiver        mReceiver              = null;
    private PowerManager.WakeLock    mWakeLock;
    private ImageView                mConnectStatus;
    private TextView                 mConnectWarn;
    private TextView                 mConnectDesc;
    private Button                 mClick2Settings;
    private boolean                  retryChannel           = false;
    private WifiP2pDevice            mDevice                = null;
    private ArrayList<WifiP2pDevice> peers                  = new ArrayList<WifiP2pDevice>();

    @Override
    public void onContentChanged() {
        super.onContentChanged();
    }

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        /* enable backlight */
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        setContentView(R.layout.connect_layout);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG);
        mWakeLock.acquire();
        registerReceiver(mReceiver, intentFilter);
        if (DEBUG)
            Log.d(TAG, "onResume()");
        mConnectStatus = (ImageView) findViewById(R.id.show_connect);
        mConnectDesc = (TextView) findViewById(R.id.show_connect_desc);
        mConnectWarn = (TextView) findViewById(R.id.show_desc_more);
        mClick2Settings = (Button) findViewById(R.id.settings_btn);
        mClick2Settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WiFiDirectMainActivity.this.startActivity(new Intent(
                        Settings.ACTION_WIRELESS_SETTINGS));
            }
        });
        if(!isNetAvailiable()) {
            mConnectWarn.setText(WiFiDirectMainActivity.this.getResources()
                        .getString(R.string.p2p_off_warning));
            mConnectWarn.setVisibility(View.VISIBLE);
            mClick2Settings.setVisibility(View.VISIBLE);
        }
        TextView tv = (TextView) findViewById(R.id.device_dec);
        if (mDevice != null)
            tv.setText(mDevice.deviceName);
        resetData();
    }

    public void setDevice(WifiP2pDevice device) {
        mDevice = device;
        TextView tv = (TextView) findViewById(R.id.device_dec);
        if (mDevice != null)
            tv.setText(mDevice.deviceName);
        if (DEBUG)
            Log.d(TAG, "mDevice.status" + mDevice.status);
    }

    public void startSearch() {
        if (DEBUG)
            Log.d(TAG, "startSearch wifiP2pEnabled:" + isWifiP2pEnabled);
        if (!isWifiP2pEnabled) {
            if (manager != null && channel != null) {
                mConnectWarn.setVisibility(View.VISIBLE);
                mConnectWarn.setText(WiFiDirectMainActivity.this.getResources()
                        .getString(R.string.p2p_off_warning));
                mClick2Settings.setVisibility(View.VISIBLE);
            }
            return;
        }
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WiFiDirectMainActivity.this,
                        WiFiDirectMainActivity.this.getResources().getString(R.string.discover_init), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(WiFiDirectMainActivity.this,
                        WiFiDirectMainActivity.this.getResources().getString(R.string.discover_fail) + reasonCode, Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
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
    public void onChannelDisconnected() {
        if (manager != null && !retryChannel) {
            Toast.makeText(this, WiFiDirectMainActivity.this.getResources().getString(R.string.channel_try),
                    Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(
                    this,
                    WiFiDirectMainActivity.this.getResources().getString(R.string.channel_close),
                    Toast.LENGTH_LONG).show();
            retryChannel = false;
        }
    }
    
    public void resetData() {
        mConnectStatus.setBackgroundResource(R.drawable.wifi_connect);
        mConnectDesc.setText(getString(R.string.connect_ready));
        peers.clear();
    }

    public void setConnect() {
        mConnectDesc.setText(getString(R.string.connected_info));
        mConnectStatus.setBackgroundResource(R.drawable.wifi_yes);
    }

    public void setIsWifiP2pEnabled(boolean enable) {
        this.isWifiP2pEnabled = enable;
        if (enable) {
            mConnectDesc.setText(getString(R.string.connect_ready));
            mConnectWarn.setVisibility(View.INVISIBLE);
            mClick2Settings.setVisibility(View.GONE);
        } else {
            mConnectDesc.setText(getString(R.string.connect_not_ready));
            mConnectWarn.setText(WiFiDirectMainActivity.this.getResources()
                        .getString(R.string.p2p_off_warning));
            mConnectWarn.setVisibility(View.VISIBLE);
            mClick2Settings.setVisibility(View.VISIBLE);
        }
    }

    public void startMiracast(String ip, String port) {
        mPort = port;
        mIP = ip;
        setConnect();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                Intent intent = new Intent(WiFiDirectMainActivity.this,
                        SinkActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(SinkActivity.KEY_PORT, mPort);
                bundle.putString(SinkActivity.KEY_IP, mIP);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        }, MAX_DELAY_MS);
        if (DEBUG)
            Log.d(TAG, "start miracast delay " + MAX_DELAY_MS + " ms");
    }

    public void stopMiracast(boolean stop) {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intentFilter.addAction(DNSMASQ_IP_ADDR_ACTION);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
    }

    @Override  
    public void onWindowFocusChanged(boolean hasFocus) {  
        super.onWindowFocusChanged(hasFocus);
        mConnectStatus.setBackgroundResource(R.drawable.wifi_connect);  
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
    public void onPeersAvailable(WifiP2pDeviceList devicelist) {
        peers.clear();
        peers.addAll(devicelist.getDeviceList());
        freshView();
    }

    /**
     * @Description TODO
     */
    private void freshView() {
        for (int i = 0; i < peers.size(); i++) {
            if (peers.get(i).status == WifiP2pDevice.CONNECTED) {
                mConnectDesc.setText(getString(R.string.connecting_desc)
                        + peers.get(i).deviceName);
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        getMenuInflater().inflate( R.menu.action_items, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        int itemId = item.getItemId();
        switch ( itemId ) {
            case R.id.about_version:
                AlertDialog.Builder builder = new Builder(WiFiDirectMainActivity.this);
                if (getResources().getConfiguration().locale.getCountry().equals("CN")) {
                    builder.setMessage(getFromAssets(VERSION_FILE + "_cn"));
                }else {
                    builder.setMessage(getFromAssets(VERSION_FILE));
                }
                builder.setTitle(R.string.about_version);
                builder.setPositiveButton(R.string.close_dlg, new DialogInterface.OnClickListener() { 
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
                });
                builder.create().show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected( item );
    }

    public String getFromAssets(String fileName){
        String result = "";
        try {
            InputStream in = getResources().getAssets().open(fileName);
            int lenght = in.available();
            byte[]  buffer = new byte[lenght];
            in.read(buffer);
            result = EncodingUtils.getString(buffer, ENCODING);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private boolean isNetAvailiable() {
        ConnectivityManager connectivity = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info == null || !info.isAvailable()) {
                return false;
            }else {
                return true;
            }
        }
        return false;
    }
}
