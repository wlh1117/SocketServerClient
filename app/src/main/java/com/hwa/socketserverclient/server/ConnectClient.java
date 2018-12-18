package com.hwa.socketserverclient.server;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hwa.socketserverclient.Constants;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class ConnectClient implements Runnable {
    private static final String TAG = Constants.TAG + ConnectClient.class.getSimpleName();
    private Socket mSocket;
    private Context mContext;
    private boolean mWorking = true;
    private String mRemoteSocketAddress;
    private Handler mHandler;

    public ConnectClient(Context context, Socket Socket, Handler handler) {
        mSocket = Socket;
        mContext = context;
        mHandler = handler;
        mRemoteSocketAddress= mSocket.getRemoteSocketAddress().toString();
    }

    public void stopRuning() {
        mWorking = false;
    }

    @Override
    public void run() {
        Log.d(TAG, "ConnectSocket is Running!!!");
        BufferedReader br = null;
        InputStream inputStream = null;
        try {
            inputStream = mSocket.getInputStream();
            br = new BufferedReader(new InputStreamReader(inputStream));
        } catch (IOException e) {

        }
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (mWorking) {
            try {
                String readStr = null; ;
                while ((readStr = br.readLine()) != null) {
                    Log.d(TAG, "readStr: " + readStr);
//                    byteArrayOutputStream.write(readStr.getBytes(), 0, readStr.length());
                    Message msg = mHandler.obtainMessage(Constants.MSG_DEAL_NETWORK_DATA);
                    msg.obj = readStr;
                    mHandler.sendMessage(msg);
                }

            } catch (IOException e) {
                Log.e(TAG, "IOException error: " + e.getMessage());
            } catch (NullPointerException e) {
                Log.e(TAG, "NullPointerException error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Exception error: " + e.getMessage());
            }
        }
        Log.d(TAG, "ConnectSocket is exit");

        try {
            mSocket.close();
        } catch (Exception e) {

        }
    }

    public void sendMsg(byte[] msg) {
        if(mSocket == null) {
            Log.e(TAG, "Socket is null!");
            return;
        }
        if(msg == null) {
            Log.e(TAG, "send msg failed, msg is null!");
            return;
        }
        OutputStream outputStream = null;
        try {
            outputStream = mSocket.getOutputStream();
            outputStream.write(msg);
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
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
