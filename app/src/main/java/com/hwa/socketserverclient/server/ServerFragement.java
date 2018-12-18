package com.hwa.socketserverclient.server;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hwa.socketserverclient.Constants;
import com.hwa.socketserverclient.R;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by wenlihong on 18-3-9.
 */

public class ServerFragement extends Fragment {
    private static final String TAG = Constants.TAG + ServerFragement.class.getSimpleName();
    private Context mContext;
    EditText ip_et;
    EditText port_et;
    Button start_btn;
    Button stop_btn;
    TextView receiveData_tv;
    StringBuffer buffer = new StringBuffer();
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra("data");
            buffer.append(data + "\r\n");
            receiveData_tv.setText(buffer.toString());
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.hwa.ACTION_DATA_CHANGED");
        mContext.registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View inflaterView = inflater.inflate(R.layout.activity_server, container, false);
        initView(inflaterView);
        return inflaterView;
    }

    private void initView(View parent) {
        ip_et = parent.findViewById(R.id.ip_et);
        port_et = parent.findViewById(R.id.port_et);

        start_btn = parent.findViewById(R.id.start);
        stop_btn = parent.findViewById(R.id.stop);
        receiveData_tv = parent.findViewById(R.id.receive_data);
        buffer.append("Receive data: \r\n");
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ipAddress = ip_et.getText().toString();
                String port = port_et.getText().toString();
                Intent intent = new Intent();
                intent.setClass(mContext, AHNetWorkService.class);
                intent.putExtra("ip", ipAddress);
                try {
                    intent.putExtra("port", Integer.parseInt(port));
                } catch (Exception e) {

                }
                Log.d(TAG, "ipAddress: " + ipAddress + ", port: " + port);
                mContext.startService(intent);
            }
        });

        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "stopService: ");

                Intent intent = new Intent();
                intent.setClass(mContext, AHNetWorkService.class);
                mContext.stopService(intent);
            }
        });

        receiveData_tv.setText(buffer.toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView!!");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach!!");
        mContext.unregisterReceiver(broadcastReceiver);
    }
}
