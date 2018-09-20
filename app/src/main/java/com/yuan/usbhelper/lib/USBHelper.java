package com.yuan.usbhelper.lib;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by YuanYe on 18/6/21.
 * Description: 封装Usb接口通信的工具类
 * <p>
 * 使用USB设备：
 * 1.添加权限：
 * <uses-feature  android:name="android.hardware.usb.host" android:required="true">
 * </uses-feature>
 * 2.Manifest中添加以下<intent-filter>，获取USB操作的通知：
 * <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
 * 3.添加设备过滤信息，气筒usb_xml可以自由修改：
 * <meta-data  android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
 * android:resource="@xml/usb_xml"></meta-data>
 * 4.根据目标设备的vendorId和productId过滤USB设备,拿到UsbDevice操作对象
 * 5.获取设备通讯通道
 * 6.连接
 */
public class USBHelper {

    private static final String TAG = "USBDeviceUtil";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static USBHelper util;
    private static Context mContext;

    private UsbDevice usbDevice; //目标USB设备
    private UsbManager usbManager;
    /**
     * 块输出端点
     */
    private UsbEndpoint epBulkOut;
    private UsbEndpoint epBulkIn;
    /**
     * 控制端点
     */
    private UsbEndpoint epControl;
    /**
     * 中断端点
     */
    private UsbEndpoint epIntEndpointOut;
    private UsbEndpoint epIntEndpointIn;

    private PendingIntent intent; //意图
    private UsbDeviceConnection conn = null;

    private OnFindListener listener;

    private int statue = USBInterface.usb_ok;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                statue = USBInterface.usb_permission_ok;
            } else {
                statue = USBInterface.usb_permission_fail;
            }
        }
    };

    public static USBHelper getInstance(Context _context) {
        if (util == null) util = new USBHelper(_context);
        mContext = _context;
        return util;
    }

    private USBHelper(Context _context) {
        intent = PendingIntent.getBroadcast(_context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        _context.registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_PERMISSION));
    }

    public UsbDevice getUsbDevice() {
        return usbDevice;
    }

    public UsbManager getUsbManager() {
        return usbManager;
    }

    /**
     * 找到自定设备
     */
    public UsbDevice getUsbDevice(int vendorId, int productId) {
        //1)创建usbManager
        if (usbManager == null)
            usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        //2)获取到所有设备 选择出满足的设备
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            Log.i(TAG, "vendorID--" + device.getVendorId() + "ProductId--" + device.getProductId());
            if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                return device; // 获取USBDevice
            }
        }
        statue = USBInterface.usb_find_this_fail;
        return null;
    }

    /**
     * 查找本机所有的USB设备
     */
    public List<UsbDevice> getUsbDevices() {
        //1)创建usbManager
        if (usbManager == null)
            usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        //2)获取到所有设备 选择出满足的设备
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        //创建返回数据
        List<UsbDevice> lists = new ArrayList<>();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            Log.i(TAG, "vendorID--" + device.getVendorId() + "ProductId--" + device.getProductId());
            lists.add(device);
        }
        return lists;
    }


    /**
     * 根据指定的vendorId和productId连接USB设备
     *
     * @param vendorId  产商id
     * @param productId 产品id
     */
    public int connection(int vendorId, int productId) {
        usbDevice = getUsbDevice(vendorId, productId);
        //3)查找设备接口
        if (usbDevice == null) {
            Log.e(TAG, "未找到目标设备，请确保供应商ID" + vendorId + "和产品ID" + productId + "是否配置正确");
            return statue;
        }
        UsbInterface usbInterface = null;
        for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
            //一个设备上面一般只有一个接口，有两个端点，分别接受和发送数据
            usbInterface = usbDevice.getInterface(i);
            break;
        }
        //4)获取usb设备的通信通道endpoint
        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            UsbEndpoint ep = usbInterface.getEndpoint(i);
            switch (ep.getType()) {
                case UsbConstants.USB_ENDPOINT_XFER_BULK://USB端口传输
                    if (UsbConstants.USB_DIR_OUT == ep.getDirection()) {//输出
                        epBulkOut = ep;
                        Log.e(TAG, "获取发送数据的端点");
                    } else {
                        epBulkIn = ep;
                        Log.e(TAG, "获取接受数据的端点");
                    }
                    break;
                case UsbConstants.USB_ENDPOINT_XFER_CONTROL://控制
                    epControl = ep;
                    Log.e(TAG, "find the ControlEndPoint:" + "index:" + i + "," + epControl.getEndpointNumber());
                    break;
                case UsbConstants.USB_ENDPOINT_XFER_INT://中断
                    if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {//输出
                        epIntEndpointOut = ep;
                        Log.e(TAG, "find the InterruptEndpointOut:" + "index:" + i + "," + epIntEndpointOut.getEndpointNumber());
                    }
                    if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                        epIntEndpointIn = ep;
                        Log.e(TAG, "find the InterruptEndpointIn:" + "index:" + i + "," + epIntEndpointIn.getEndpointNumber());
                    }
                    break;
                default:
                    break;
            }
        }
        //5)打开conn连接通道
        if (usbManager.hasPermission(usbDevice)) {
            //有权限，那么打开
            conn = usbManager.openDevice(usbDevice);
        } else {
            usbManager.requestPermission(usbDevice, intent);
            if (usbManager.hasPermission(usbDevice)) { //权限获取成功
                conn = usbManager.openDevice(usbDevice);
            } else {
                Log.e(TAG, "没有权限");
                statue = USBInterface.usb_permission_fail;
            }
        }
        if (null == conn) {
            Log.e(TAG, "不能连接到设备");
            statue = USBInterface.usb_open_fail;
            return statue;
        }
        //打开设备
        if (conn.claimInterface(usbInterface, true)) {
            if (conn != null)// 到此你的android设备已经连上zigbee设备
                Log.i(TAG, "open设备成功！");
            final String mySerial = conn.getSerial();
            Log.i(TAG, "设备serial number：" + mySerial);
            statue = USBInterface.usb_ok;
        } else {
            Log.i(TAG, "无法打开连接通道。");
            statue = USBInterface.usb_passway_fail;
            conn.close();
        }
        return statue;
    }

    /**
     * 通过USB发送数据
     */
    public void sendData(byte[] buffer) {
        if (conn == null || epBulkOut == null) return;
        if (conn.bulkTransfer(epBulkOut, buffer, buffer.length, 0) >= 0) {
            //0 或者正数表示成功
            Log.i(TAG, "发送成功");
            statue = USBInterface.usb_permission_ok;
        } else {
            Log.i(TAG, "发送失败的");
            statue = USBInterface.usb_permission_fail;
        }
    }

    /**
     * 关闭USB连接
     */
    public void close() {
        if (conn != null) { //关闭USB设备
            conn.close();
            conn = null;
        }
        if (mContext != null && broadcastReceiver != null) {
            mContext.unregisterReceiver(broadcastReceiver);
        }
    }

    /**
     * 是否找到设备回调
     */
    public interface OnFindListener {
        void onFind(UsbDevice usbDevice, UsbManager usbManager);

        void onFail(String error);
    }
}

