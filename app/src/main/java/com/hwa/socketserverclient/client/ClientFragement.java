package com.hwa.socketserverclient.client;


import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hwa.socketserverclient.Constants;
import com.hwa.socketserverclient.R;
import com.haige.multimode.mmm.adhoc.AdHocManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;


/**
 * Created by wenlihong on 18-3-9.
 */

public class ClientFragement extends Fragment {
    private static final String TAG = Constants.TAG + ClientFragement.class.getSimpleName();
    private static final String NETSTATUS_ACTION = "com.haige.multimode.mmm.adhocstatus";
    private static final int EVENT_UPDATE_VERSION = 0;
    private static final int EVENT_UPDATE_RECEIVE_DATA = 1;
    private Context mContext;
    EditText targetIp_et;
    EditText targetPort_et;
    EditText localIp_et;
    EditText localPort_et;
    EditText send_et;
    Button start_btn;
    Button stop_btn;
    Button send_btn;
    TextView version_tv;
    TextView receiveData_tv;
    boolean running = false;
    private Socket socket = null;
    private ProgressDialog pd;
    private boolean isAdhocOn;
    private AdHocManager mAdHocManager;
    StringBuffer buffer = new StringBuffer();
    StringBuffer receiveDataBuffer = new StringBuffer();
    private byte[] paraVersionCommand = new byte[]{ 2, 7, 0, 0 };
    private String mVersion = "2018-12-14-17-29\n";
    private ConnectThread connectThread;
    private ReadThread readThread;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case EVENT_UPDATE_VERSION:
                    Log.d(TAG, "dismiss pd!!!");
                    pd.dismiss();
                    break;
                case EVENT_UPDATE_RECEIVE_DATA:
                    receiveData_tv.setText(receiveDataBuffer.toString());
                    break;
            }
        }
    };

    private BroadcastReceiver mNetWorkStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "NetWorkStatusReceiver:onReceive");
            Log.d(TAG, "Action:" + intent.getAction());
            int status = intent.getIntExtra("adhoc.status.type", 0);
            Log.d(TAG, "status" + status);

            Log.d(TAG, "StatusType:0x" + Integer.toHexString(status));
            if(status == 0x0307) {
                final byte[] byteArrayExtra = intent.getByteArrayExtra("adhoc.status.buffer");
                String paraVerion = Bytes2HexString(byteArrayExtra);
                if(paraVerion == null || paraVerion.length() != 14) {
                    return;
                }
                final String version = String.valueOf(paraVerion.substring(0, 4)) + "-" + paraVerion.substring(4, 6) + "-"
                        + paraVerion.substring(6, 8) + "-" + paraVerion.substring(8, 10) + "-" +
                        paraVerion.substring(10, 12) + "-" + paraVerion.substring(12, 14);
                buffer.append("version: " + version + "\r\n");
                version_tv.setText(buffer.toString());
                mVersion = version;
            }

        }
    };

    public String Bytes2HexString(final byte[] array) {
        String string = "";
        for (int i = 0; i < array.length; ++i) {
            String s = Integer.toHexString(0xFF & array[i]);
            if (s.length() == 1) {
                s = String.valueOf('0') + s;
            }
            string = String.valueOf(string) + s.toUpperCase();
        }
        return string;
    }


    private void processThread() {
        Log.d(TAG, "processThread");

        pd = ProgressDialog.show(mContext, "Get Version", "get version", false, true);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int n = 0;
                while (!isAdhocOn) {
                    final int openAdHocDevice = mAdHocManager.openAdHocDevice();
                    Log.d(TAG, "AdhocState: " + openAdHocDevice);

                    ++n;
                    if (openAdHocDevice == 0) {
                        buffer.append("success!\r\n");
                        break;
                    }
                    if(n > 10) {
                        break;
                    }
                    try {
                        Thread.sleep(2000L);
                    }
                    catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }

                handler.sendEmptyMessage(EVENT_UPDATE_VERSION);
            }
        });
        thread.start();
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(NETSTATUS_ACTION);
        mContext.registerReceiver(mNetWorkStatusReceiver, filter);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View inflaterView = inflater.inflate(R.layout.activity_client, container, false);
        initView(inflaterView);
        return inflaterView;
    }

    private void initView(View parent) {
        targetIp_et = parent.findViewById(R.id.target_ip_et);
        targetPort_et = parent.findViewById(R.id.target_port_et);
        localIp_et = parent.findViewById(R.id.local_ip_et);
        localPort_et = parent.findViewById(R.id.local_port_et);
        send_et = parent.findViewById(R.id.send_et);
        version_tv = parent.findViewById(R.id.version);
        receiveData_tv = parent.findViewById(R.id.receive_data);
        Log.d(TAG, "initView");

        start_btn = parent.findViewById(R.id.start);
        stop_btn = parent.findViewById(R.id.stop);
        send_btn = parent.findViewById(R.id.send);
        mAdHocManager = AdHocManager.instance(mContext);
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String targetIp = targetIp_et.getText().toString();
                final String targetPort = targetPort_et.getText().toString();
                final String localIp = localIp_et.getText().toString();
                final String localPort = localPort_et.getText().toString();
                if(connectThread != null) {
                    connectThread.stopRuning();
                }
                try {
                    connectThread = new ConnectThread(InetAddress.getByName(targetIp), Integer.parseInt(targetPort),
                            InetAddress.getByName(localIp), Integer.parseInt(localPort));
                    connectThread.start();
                    Log.d(TAG, "start_btn create connectThread" + connectThread);
                } catch (Exception e) {
                    Log.e(TAG, "Create connectThread error: " + e.getMessage());
                    showMsg("Create connectThread error");
                }
            }
        });

        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "stop_btn connectThread" + connectThread);

                showMsg("stop_btn ");
                if(connectThread != null) {
                    connectThread.stopRuning();
                }

                try {
                    if(socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (Exception e) {

                }
                socket = null;
            }
        });

        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OutputStream stream = null;
                try {
                    final String sendText = send_et.getText().toString();
                    showMsg("content: " + sendText + ", socket: " + socket);

                    if(socket != null /*&& !socket.isClosed() && socket.isConnected()*/) {
                        stream = socket.getOutputStream();
                        stream.write((mVersion + sendText + "\n").getBytes("UTF-8"));
                        showMsg("write content: " + sendText);
                        stream.flush();
                    }
                } catch (Exception e) {

                } finally {
                    if(stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            Log.e(TAG, "close outputStream error! " + e.toString());
                        }
                    }
                }


            }
        });

        Button open_btn = parent.findViewById(R.id.open_device);
        open_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processThread();
            }
        });

        Button query_btn = parent.findViewById(R.id.query);
        query_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mAdHocManager != null) {
                    mAdHocManager.setParameters(paraVersionCommand);
                }
            }
        });
    }
    private void showMsg(final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
            }
        });
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
        showMsg("onDetach!! ");
        mContext.unregisterReceiver(mNetWorkStatusReceiver);

        if(connectThread != null) {
            connectThread.stopRuning();
        }

        if(readThread != null) {
            readThread.stopRuning();;
        }

        try {
            if(socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {

        }
        socket = null;
    }

    class ReadThread extends Thread {
        boolean stop = false;

        public ReadThread() {
            super();
        }

        public void stopRuning() {
            stop = true;
        }

        @Override
        public void run() {
            while (!stop) {
                try {
                    if(socket != null) {
                        InputStream inputStream = socket.getInputStream();
                        byte[] data = new byte[1024];
                        int length = 0;
                        while ((length = inputStream.read(data)) != -1) {
                            receiveDataBuffer.append(new String(data));
                        }
                        //BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
//                        String line = "";
//                        while ((line = reader.readLine()) != null) {
//                            receiveDataBuffer.append(line);
//                        }
                        handler.sendEmptyMessage(EVENT_UPDATE_RECEIVE_DATA);
                    }
                } catch (Exception e) {
                }
            }
        }
    }


    class ConnectThread extends Thread {
        private boolean stop = false;
        int count = 1;
        InetAddress targetAddress;
        int targetPort;
        InetAddress localAddress;
        int localPort;
        public ConnectThread(InetAddress targetAddress, int targetPort, InetAddress localAddress, int localPort) {
            super();
            this.targetAddress = targetAddress;
            this.targetPort = targetPort;
            this.localAddress = localAddress;
            this.localPort = localPort;
        }

        public void stopRuning() {
            stop = true;
        }

        @Override
        public void run() {
            try {
                if(socket != null && !socket.isClosed()) {
                    socket.close();
                    showMsg("socket.close()!! ");

                }
            } catch (Exception e) {

            }
            socket = null;

            while (socket == null && !stop) {
                try {
                    Log.d(TAG, "targetAddress: " + targetAddress + ", targetPort: " + targetPort + ", stop: " + stop);
//                    socket = new Socket(this.address, this.port/*, InetAddress.getByName("192.168.42.132"), 0*/);
//                    socket = new Socket(this.address, this.port, InetAddress.getByName("192.168.42.132"), 0);
                    socket = new Socket(targetAddress, targetPort, localAddress, localPort);
                    Log.d(TAG, "create socket success: " + socket);
                    showMsg("create socket success: " + socket);
                } catch (Exception e) {
                    Log.d(TAG, "Create Socket err: " + e.getMessage());
                }
                count++;
            }
            if(readThread != null) {
                readThread.stopRuning();;
            }
            if(socket != null) {
                readThread= new ReadThread();
                readThread.start();
            }

            OutputStream stream = null;
            try {
                if(socket != null && !socket.isClosed() && socket.isConnected()) {
                    final String sendText = send_et.getText().toString();
                    stream = socket.getOutputStream();
                    stream.write((mVersion + sendText + "\n").getBytes());
                    showMsg("write content!! ");

                    stream.flush();
                }
            } catch (Exception e) {

            } finally {
                try {
                    stream.close();
                } catch (Exception e){

                }
            }
            showMsg("exit ConnectThread success!! ");
        }
    }
}
