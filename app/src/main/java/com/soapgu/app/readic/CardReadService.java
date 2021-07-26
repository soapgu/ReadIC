package com.soapgu.app.readic;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.serialport.SerialPort;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

public class CardReadService extends Service {

    private static final String TAG = "CardReadService";
    public static final String GET_CARD_NUM = "GET_CARD_NUM";

    protected OutputStream mOutputStream;
    private InputStream mInputStream;
    ReadThread mReadThread;
    private SerialPort mSerialPort = null;

    private CardNumCallBack callBack;

    private static final String BEGIN = "7f0910000400";
    private boolean isPause;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    private final MyBinder myBinder = new MyBinder();

    /**
     * 内部类继承Binder
     */
    public class MyBinder extends Binder {
        /**
         * 声明方法返回值是MyService本身
         *
         * @return service
         */
        public CardReadService getService() {
            return CardReadService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        start();
    }

    public void setCallBack(CardNumCallBack callBack) {
        this.callBack = callBack;
    }

    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    byte[] buffer = new byte[64];
                    if (mInputStream == null) return;
                    size = mInputStream.read(buffer);
                    if (size > 0 && !isPause) {
                        onDataReceived(buffer, size);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }

        }
    }

    public void start() {
        try {
            mSerialPort = getSerialPort();
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            /* Create a receiving thread */
            mReadThread = new ReadThread();
            mReadThread.start();
        } catch (SecurityException e) {
            Log.e(TAG, "权限问题");
        } catch (IOException e) {
            Log.e(TAG, "io问题");
        } catch (InvalidParameterException e) {
            Log.e(TAG, "配置问题");
        }
    }


    public boolean isPause() {
        return isPause;
    }

    public void setPause(boolean pause) {
        isPause = pause;
    }

    /**
     * 更新
     * 后因发现ZETO在接读卡器时0,1线接反了，所以不需要反码，只需要做一次字节逆序。
     * <p>
     * 即HID读出来是：
     * AA BB CC DD
     * <p>
     * 通过YL02协议读出来是：
     * DD CC BB AA
     * <p>
     * 我们现在以HID为基准
     *
     * @param buffer 缓冲区
     * @param size 大小
     */
    private void onDataReceived(byte[] buffer, final int size) {
        StringBuilder stringBuilder = new StringBuilder();
        if (buffer == null || buffer.length == 0) {
            return;
        }
        for (byte b : buffer) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        String msg = stringBuilder.toString();
        Log.e(TAG, msg);
        if (msg.startsWith(BEGIN)) {
            String cardNum = msg.substring(12, 20);
            Log.v(TAG, cardNum);
            String right_msg = cardNum.substring(6, 8) + cardNum.substring(4, 6) + cardNum.substring(2, 4) + cardNum.substring(0, 2);
            Intent intent=new Intent(GET_CARD_NUM);
            intent.putExtra("cardNum", right_msg.toUpperCase());
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            if (callBack != null) {
                callBack.receiveCardNum(right_msg);
            }
        }
    }

    public SerialPort getSerialPort()
            throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort == null) {
            mSerialPort = new SerialPort(new File("/dev/ttyS3"), 9600);
        }
        return mSerialPort;
    }

    public void closeSerialPort() {
        mReadThread.interrupt();
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        closeSerialPort();
        this.callBack = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        closeSerialPort();
        this.callBack = null;
        super.onDestroy();
    }

    public interface CardNumCallBack {
        void receiveCardNum(String s);
    }

}
