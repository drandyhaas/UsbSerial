package com.felhr.serialportexample;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.Formatter;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private TextView display;
    private EditText editText;
    private MyHandler mHandler;
    LineGraphSeries<DataPoint> series;
    GraphView graph;
    private int numsamples = 20; // <256 please

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    protected void waitalittle(){
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void send2usb(int x){
        if (x>127) x -= 256; // since it goes to bytes as twos compliment
        usbService.write( BigInteger.valueOf(x).toByteArray() );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        graph = (GraphView) findViewById(R.id.graph);
        series = new LineGraphSeries<DataPoint>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        graph.addSeries(series);

        mHandler = new MyHandler(this);

        display = (TextView) findViewById(R.id.textView1);
        editText = (EditText) findViewById(R.id.editText1);
        Button sendButton = (Button) findViewById(R.id.buttonSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = editText.getText().toString();
                if (!data.equals("")) {
                    if (data.equals("I")){
                        waitalittle();
                        send2usb(0); send2usb(20); // board ID 0
                        send2usb(30); send2usb(142); // get board ID
                        waitalittle();
                        send2usb(135); send2usb(2); send2usb(0); // serialdelaytimerwait of 512

                        waitalittle(); send2usb(139); // auto-rearm trigger
                        send2usb(100);//final arming

                        //send2usb(122); send2usb(1); send2usb(0); // 256 samples per channel
                        send2usb(122); send2usb(0); send2usb(numsamples); // samples per channel

                        send2usb(123); send2usb(0); // send increment
                        send2usb(124); send2usb(3); // downsample 3
                        send2usb(125); send2usb(1); // tickstowait 1

                        //100, 10 // get event (or just 10 if auto-rearming)

                        waitalittle(); send2usb(136); send2usb(2); send2usb(32); send2usb(0); send2usb(0); send2usb(255); send2usb(200);// io expanders on
                        waitalittle(); send2usb(136); send2usb(2); send2usb(32); send2usb(1); send2usb(0); send2usb(255); send2usb(200);// io expanders on
                        waitalittle(); send2usb(136); send2usb(2); send2usb(33); send2usb(0); send2usb(0); send2usb(255); send2usb(200);// io expanders on
                        waitalittle(); send2usb(136); send2usb(2); send2usb(33); send2usb(1); send2usb(0); send2usb(255); send2usb(200);// io expanders on
                        waitalittle(); send2usb(136); send2usb(2); send2usb(32); send2usb(18); send2usb(240); send2usb(255); send2usb(200);// init, and turn on ADCs!
                        waitalittle(); send2usb(136); send2usb(2); send2usb(32); send2usb(19); send2usb(15); send2usb(255); send2usb(200);// init, and turn on ADCs!
                        waitalittle(); send2usb(136); send2usb(2); send2usb(33); send2usb(18); send2usb(0); send2usb(255); send2usb(200);// init, and turn on ADCs!
                        waitalittle(); send2usb(136); send2usb(2); send2usb(33); send2usb(19); send2usb(0); send2usb(255); send2usb(200);// init, and turn on ADCs!

                        waitalittle(); send2usb(131);  send2usb(8); send2usb(0); // spi offset
                        waitalittle(); send2usb(131);  send2usb(6); send2usb(16); // spi offset binary output
                        //waitalittle(); send2usb(131);  send2usb(6); send2usb(80); // spi offset binary output - test pattern
                        waitalittle(); send2usb(131);  send2usb(1); send2usb(0 ); // spi not multiplexed output

                        waitalittle(); send2usb(136); send2usb(3); send2usb(96); send2usb(80); send2usb(136); send2usb(22); send2usb(0); // board 0 calib, chan 0
                        waitalittle(); send2usb(136); send2usb(3); send2usb(96); send2usb(82); send2usb(135); send2usb(248); send2usb(0); // board 0 calib, chan 1
                        waitalittle(); send2usb(136); send2usb(3); send2usb(96); send2usb(84); send2usb(136); send2usb(52); send2usb(0); // board 0 calib, chan 2
                        waitalittle(); send2usb(136); send2usb(3); send2usb(96); send2usb(86); send2usb(136); send2usb(52); send2usb(0); // board 0 calib, chan 3

                        waitalittle();
                        display.append("sent initialization commands \n");
                    }
                    else if (usbService != null) { // if UsbService was correctly binded, Send data
                        display.append(data+"\n");
                        send2usb(Integer.parseInt(data));
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    Formatter formatter = new Formatter();
                    byte [] bd = (byte[])msg.obj;
                    int histlen=Math.min(numsamples,bd.length);
                    DataPoint [] seriesd = new DataPoint[histlen-1];
                    int p=0;
                    for (byte b : bd) {
                        formatter.format("%02x ", b);
                        if (p>0) { // don't plot the first point - it's screwed up
                            int bdp = bd[p];
                            //convert to unsigned, then subtract 128
                            if (bdp < 0) bdp += 256;
                            bdp -= 128;
                            seriesd[p-1] = new DataPoint(p, bdp);
                        }
                        p++;
                        if (p>=histlen) break;
                    }
                    mActivity.get().display.append(formatter.toString()+" -\n");
                    series.resetData(seriesd);
                    if (p>numsamples-2) {
                        graph.getViewport().setMinX(1);
                        graph.getViewport().setMaxX(numsamples);
                        graph.getViewport().setXAxisBoundsManual(true);
                    }

                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
}