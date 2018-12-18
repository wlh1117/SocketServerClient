package com.hwa.socketserverclient.server;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.haige.multimode.mmm.adhoc.AdHocManager;
import com.haige.multimode.model.ManagerModel;
import com.hwa.socketserverclient.Constants;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AHNetWorkService extends Service {
    private static final String TAG = Constants.TAG + AHNetWorkService.class.getSimpleName();
    private static final int MIN_THREAD_COUNT = 3;
    private static int THREAD_COUNT = Runtime.getRuntime().availableProcessors() + 1;
    private ExecutorService executorService;
    private Context mContext;
    private AdHocManager adHocManager;
    private ServerSocket mServerSocket = null;
    private ServerReceiver mServerReceiver;
    private HashMap<String, ConnectClient> mConnectClients = new HashMap<>();
    private ConnectClient connectClient;
    private SenderHandler senderHandler;
    private UsbManager usbManager;
    String ip = "";
    int port = Constants.LOCALPORT;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };
    private SendContentThread sendContentThread;

    private BroadcastReceiver netWorkStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "NetWorkStatusReceiver:onReceive--------------------");
            Log.d(TAG, "Action:" + intent.getAction());
            Log.d(TAG, "StatusType:" + intent.getIntExtra("adhoc.status.type", 0));
            Log.d(TAG, "StatusType:0x" + Integer.toHexString(intent.getIntExtra("adhoc.status.type", 0)));
            final byte[] byteArrayExtra = intent.getByteArrayExtra("adhoc.status.buffer");
            Log.d(TAG, "byteArrayExtra:" + new String(byteArrayExtra));
            Log.d(TAG, "NetWorkStatusReceiver:onReceive end----------------- \r\n");

        }
    };

    private BroadcastReceiver netWorkStatusReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Action:" + intent.getAction());
            showToast(intent.getAction());
            UsbAccessory[] usbAccessories = usbManager.getAccessoryList();
            if(usbAccessories != null) {
                showToast("usbAccessories.size: " + usbAccessories.length);
            } else {
                showToast("usbAccessories.size: 00000000" );
            }

            HashMap<String, UsbDevice> lists= usbManager.getDeviceList();
            if(lists != null) {
                showToast("lists.size: " + lists.size());
            } else {
                showToast("lists.size: 00000000" );
            }
        }
    };

    private AdHocManager.DataRecvListener dataRecvListener = new AdHocManager.DataRecvListener() {
        @Override
        public void onDataReceived(String s, byte[] bytes) {
            Log.d(TAG, "onDataReceived!");
            showToast("onDataReceived: " + new String(bytes));
        }
    };


    private AdHocManager.NetWorkStatusListener netWorkStatusListener = new ManagerModel.NetWorkStatusListener() {
        @Override
        public void onNetWorkStatusReceived(int i, byte[] bytes) {

        }
    };

    private ManagerModel.PcmVoiceListener pcmVoiceListener = new ManagerModel.PcmVoiceListener() {
        @Override
        public void onPcmVoiceReceived(byte[] bytes, long l) {

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate!");
        mContext = getApplicationContext();
//        adHocManager = AdHocManager.instance(mContext);
        executorService = Executors.newFixedThreadPool(Math.max(MIN_THREAD_COUNT, THREAD_COUNT));

//        adHocManager.addDataRecvListener(dataRecvListener);
//        adHocManager.addNetWorkStatusListener(netWorkStatusListener);
//        adHocManager.addPcmVoiceListener(pcmVoiceListener);

        usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.haige.multimode.mmm.adhocstatus");
        registerReceiver(netWorkStatusReceiver, filter);

        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter2.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter2.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter2.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(netWorkStatusReceiver2, filter2);

        HandlerThread handlerThread = new HandlerThread("SenderHandler");
        handlerThread.start();

        Looper looper = handlerThread.getLooper();
        senderHandler = new SenderHandler(looper);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        ip = intent.getStringExtra("ip");
        port = intent.getIntExtra("port", Constants.LOCALPORT);
        Log.d(TAG, "onStart ip: " + ip + ", port: " + this.port);
        Thread thread = new Thread(new ServerRunable(ip, port));
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
        senderHandler.sendEmptyMessageDelayed(2, 4000);

    }

    private void showToast(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind!");
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind!");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy!");
        try {
            if(mServerSocket != null) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch (Exception e) {

        }
    }

    class ServerRunable implements Runnable {
        private Socket mSocket;

        public ServerRunable(String ip, int port) {

        }

        @Override
        public void run() {

            if(mServerSocket == null) {
                try {
                    if(TextUtils.isEmpty(ip)) {
                        mServerSocket = new ServerSocket(port, Constants.SERVER_SOCKET_BACKLOG);
                    } else {
                            mServerSocket = new ServerSocket(port, 100, Inet4Address.getByName(ip));
//                        mServerSocket = new ServerSocket();
                        mServerSocket.setReuseAddress(true);
                        showToast("setReuseAddress: " + true);

//                        mServerSocket.bind(new InetSocketAddress(ip,port));
                        showToast("mServerSocket: " + mServerSocket.toString());
                    }
                    showToast("mServerSocket: " + mServerSocket);
                    Log.d(TAG, "mServerSocket: " + mServerSocket);
                } catch (IOException e) {
                    Log.e(TAG, "Create AHServerSocket failed: " + e.getMessage());
                    showToast("Create AHServerSocket failed: " + e.getMessage());
                    try {
                        if(mServerSocket != null) {
                            mServerSocket.close();
                            mServerSocket = null;
                        }
                    } catch (IOException ex) {
                    }
                }
            }
            mServerReceiver = new ServerReceiver();
            executorService.execute(mServerReceiver);

        }
    }

    class ServerReceiver implements Runnable {
        private Socket mSocket;

        @Override
        public void run() {
            //when more connect accept, how to deal with it
            showToast("wait client connect!! ");
            Log.d(TAG, "wait client connect!! " );
            while (true) {
                try {
                    mSocket = mServerSocket.accept();
                    showToast("one client connect mSocket " + mSocket);
                    Log.d(TAG, "one client connect!!! " );

                    if(mSocket != null) {
                        Log.d(TAG, "getRemoteSocketAddress: " + mSocket.getRemoteSocketAddress().toString());
                        Log.d(TAG, "getInetAddress: " + mSocket.getInetAddress() + ", port: " + mSocket.getPort());
                        Log.d(TAG, "getLocalAddress: " + mSocket.getLocalAddress() + ", LocalPort: " + mSocket.getLocalPort());
                        Log.d(TAG, "mServerSocket.getInetAddress: " + mServerSocket.getInetAddress());
                        Log.d(TAG, "mServerSocket.getInetAddress: " + mServerSocket.getLocalSocketAddress().toString());
                        if(connectClient != null) {
                            connectClient.stopRuning();
                        }
                        if(sendContentThread != null) {
                            sendContentThread.stopRunning();
                        }
                        connectClient = new ConnectClient(mContext, mSocket, senderHandler);
                        executorService.execute(connectClient);
                        sendContentThread = new SendContentThread(mSocket);
                        executorService.execute(sendContentThread);
                        //save all connected sockets
                        if(mConnectClients.size() == 0) {
//                            connectClient = new ConnectClient(mContext, mSocket, senderHandler);
//                            mConnectClients.put(connectClient.toString(), connectClient);

                        } else {
                            //reject new socket
                        }
                    }
                } catch (Exception e) {
//                    Log.e(TAG, "Connection socket error: " + e.getMessage());
                } finally {
                }
            }
        }
    }

    class SendContentThread implements Runnable {
        private Socket socket;
        private int count = 1;
        private boolean stopRuning = false;
        public  SendContentThread(Socket socket) {
            this.socket = socket;
        }

        public void stopRunning() {
            stopRuning = true;
        }

        @Override
        public void run() {
            while (socket != null) {
                OutputStream outputStream = null;
                BufferedWriter bw = null;
                try {
                    outputStream = socket.getOutputStream();
                    bw = new BufferedWriter(new OutputStreamWriter(outputStream));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                }
                while (!stopRuning) {
                    try {
                        SystemClock.sleep(5000);
                        bw.write("Response: " + count + "\r\n");
                        Log.d(TAG, "send Response: " + count + "\r\n");
                        count++;
//                        bw.newLine();
                        bw.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                    }
                }

                try {
                    if(outputStream != null) {
                        outputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class SenderHandler extends Handler implements Runnable {
        Intent intent;
        public SenderHandler(Looper looper) {
            super(looper);
            intent = new Intent("com.hwa.ACTION_DATA_CHANGED");
        }

        @Override
        public void run() {
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                //this msg send from udpServerThread
                case Constants.MSG_DEAL_NETWORK_DATA:
                    try {
                        String data = (String)msg.obj;
                        Log.d(TAG, "get socket content: " + data);
                        intent.putExtra("data", data);
                        sendBroadcast(intent);
                    } catch (Exception e) {

                    }
                    break;
                case 2:
//                    byte[] paraVersionCommand = new byte[] { 2, 7, 0, 0 };
//                    adHocManager.openAdHocDevice();
//                    SystemClock.sleep(30000);
//                    adHocManager.setParameters(paraVersionCommand);
                    break;
            }
        }

    }

}
