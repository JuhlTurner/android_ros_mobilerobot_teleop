package dk.rsd_grp3.ros_java.android_ui.ui;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

/**
 * Created by StigT on 01-08-2016.
 * Class for connecting to robot using wifi
 */
public class WifiConnector {

    private String ssid;
    private String key;
    private WifiManager wifiManager;

    /**
     * Class for connectiong to wifi/robot
     * @param context Application context
     * @param _ssid network ssid
     * @param _key network password
     */
    public WifiConnector(Context context, String _ssid, String _key)
    {
        ssid = _ssid;
        key = _key;

        wifiManager = (WifiManager)context.getSystemService(context.WIFI_SERVICE);
    }

    /**
     * Set Wifi SSID and passwork
     * @param _ssid SSID
     * @param _key Password
     */
    public void setSsidAndKey(String _ssid, String _key){
        ssid = _ssid;
        key = _key;
    }

    /**
     * Set wifi/robot SSID
     * @param _ssid SSID
     */
    public void setSsid(String _ssid){
        ssid = _ssid;
    }

    /**
     * Set wifi/robot password
     * @param _key Password
     */
    public void setKey(String _key){
        key = _key;
    }

    /**
     * Connect or reconnect to wifi/robot
     */
    public void reconnect(){
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", key);

        //remember id
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }

    /**
     * Disconnect from wifi/robot
     */
    public void disconnect(){

        wifiManager.reconnect();
    }
}
