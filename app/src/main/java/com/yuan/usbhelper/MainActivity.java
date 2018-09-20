package com.yuan.usbhelper;

import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.yuan.usbhelper.lib.USBHelper;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnFindAll, btnOpenOne;
    private TextView tvContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnFindAll = findViewById(R.id.btn_findAll);
        btnFindAll.setOnClickListener(this);
        btnOpenOne = findViewById(R.id.btn_openOne);
        btnOpenOne.setOnClickListener(this);
        tvContent = findViewById(R.id.tv_content);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_findAll:
                List<UsbDevice> list = USBHelper.getInstance(this).getUsbDevices();
                String str = "";
                for (UsbDevice device : list) {
                    str = str + device.getDeviceName() + "\n";
                }
                if (TextUtils.isEmpty(str)) str = "当前设备没有接入USB设备";
                tvContent.setText(str);
                break;
            case R.id.btn_openOne:
                List<UsbDevice> list2 = USBHelper.getInstance(this).getUsbDevices();
                for (UsbDevice device : list2) {
                    int statue = USBHelper.getInstance(this).connection(device.getVendorId(), device.getProductId());
                    tvContent.setText("连接状态：" + statue);
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        USBHelper.getInstance(this).close();
    }
}
