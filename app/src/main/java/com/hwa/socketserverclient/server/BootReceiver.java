package com.hwa.socketserverclient.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Intent bdService = new Intent();
            bdService.setClass(context, AHNetWorkService.class);
//            context.startService(bdService);
        }

    }
}
