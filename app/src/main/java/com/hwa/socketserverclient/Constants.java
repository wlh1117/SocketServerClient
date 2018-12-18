package com.hwa.socketserverclient;

/**
 * Created by wenlihong on 18-2-1.
 */

public class Constants {
    public static final String TAG = "AdHoc.";

    public static final int LOCALPORT = 9988;
    public static final int SERVER_SOCKET_BACKLOG = 1;

    public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

    public static final int INT_TYPE_SIZE = 4;
    public static final int MODEM_ID_LEN = 12;
    public static final int PHONE_ID_LEN = 12;

    public static final int PHONE_NUMBER_BUF_LENGTH = 16;
    public static final int TIME_STAMP_BUF_LENGTH = 32;
    public static final int SMS_DATA_MAX_SIZE = 284;

    public static final int MSG_DEAL_NETWORK_DATA = 1;

}
