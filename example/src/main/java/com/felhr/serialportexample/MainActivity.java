package com.felhr.serialportexample;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
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
    LineGraphSeries<DataPoint> _series0;
    LineGraphSeries<DataPoint> _series1;
    LineGraphSeries<DataPoint> _series2;
    LineGraphSeries<DataPoint> _series3;
    GraphView graph;
    private int numsamples = 70; // <256 please
    private int eventn = 0;
    private int downsample = 3;

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

        int radius = 6;
        int thickness = 4;

        graph = (GraphView) findViewById(R.id.graph);
        _series0 = new LineGraphSeries<DataPoint>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        _series0.setTitle("Chan 0");
        _series0.setColor(Color.RED);
        _series0.setDrawDataPoints(true);
        _series0.setDataPointsRadius(radius);
        _series0.setThickness(thickness);
        graph.addSeries(_series0);
        _series1 = new LineGraphSeries<DataPoint>(new DataPoint[] {
                new DataPoint(0, 2),
                new DataPoint(1, 6),
                new DataPoint(2, 4),
                new DataPoint(3, 0),
                new DataPoint(4, 0)
        });
        _series1.setTitle("Chan 1");
        _series1.setColor(Color.GREEN);
        _series1.setDrawDataPoints(true);
        _series1.setDataPointsRadius(radius);
        _series1.setThickness(thickness);
        graph.addSeries(_series1);
        _series2 = new LineGraphSeries<DataPoint>(new DataPoint[] {
                new DataPoint(0, 9),
                new DataPoint(1, 2),
                new DataPoint(2, 3),
                new DataPoint(3, -1),
                new DataPoint(4, -2)
        });
        _series2.setTitle("Chan 2");
        _series2.setColor(Color.BLUE);
        _series2.setDrawDataPoints(true);
        _series2.setDataPointsRadius(radius);
        _series2.setThickness(thickness);
        graph.addSeries(_series2);
        _series3 = new LineGraphSeries<DataPoint>(new DataPoint[] {
                new DataPoint(0, -3),
                new DataPoint(1, -5),
                new DataPoint(2, -3),
                new DataPoint(3, -2),
                new DataPoint(4, -1)
        });
        _series3.setTitle("Chan 3");
        _series3.setColor(Color.MAGENTA);
        _series3.setDrawDataPoints(true);
        _series3.setDataPointsRadius(radius);
        _series3.setThickness(thickness);
        graph.addSeries(_series3);

        mHandler = new MyHandler(this);

        display = (TextView) findViewById(R.id.textView1);
        editText = (EditText) findViewById(R.id.editText1);
        Button sendButton = (Button) findViewById(R.id.buttonSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = editText.getText().toString();
                if (!data.equals("")) {
                    if (data.equals("G") || data.equals("g")){
                        waitalittle();
                        send2usb(0); send2usb(20); // board ID 0
                        send2usb(30); send2usb(142); // get board ID
                        waitalittle();
                        send2usb(135); send2usb(0); send2usb(100); // serialdelaytimerwait of 100

                        waitalittle(); send2usb(139); // auto-rearm trigger
                        send2usb(100);//final arming

                        //send2usb(122); send2usb(1); send2usb(0); // 256 samples per channel
                        send2usb(122); send2usb(0); send2usb(numsamples); // samples per channel

                        send2usb(123); send2usb(0); // send increment
                        send2usb(124); send2usb(downsample); // downsample 3
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
                        waitalittle();
                        send2usb(10); // get an event
                    }
                    else if (data.equals("(")) {
                        if (downsample<10) {
                            downsample += 1;
                            send2usb(124); send2usb(downsample);
                            int ds=downsample-3;
                            if (ds<1) ds=1;
                            if (ds>8) ds=8; // otherwise we timeout upon readout
                            send2usb(125);  send2usb(ds);
                        }
                    }
                    else if (data.equals(")")) {
                        if (downsample>0) {
                            downsample -= 1;
                            send2usb(124); send2usb(downsample);
                            int ds=downsample-3;
                            if (ds<1) ds=1;
                            if (ds>8) ds=8; // otherwise we timeout upon readout
                            send2usb(125);  send2usb(ds);
                        }
                    }
                    else if (usbService != null) { // if UsbService was correctly bound, send data
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
                    int histlen=bd.length/4;
                    double xoffset = 1.0;
                    int yoffset=0;
                    double yscale = 7.5;
                    DataPoint [] series0 = new DataPoint[histlen];
                    DataPoint [] series1 = new DataPoint[histlen];
                    DataPoint [] series2 = new DataPoint[histlen];
                    DataPoint [] series3 = new DataPoint[histlen];
                    int p=0;
                    for (byte b : bd) {
                        //formatter.format("%02x ", b); // for debugging
                        int bdp = bd[p];
                        //convert to unsigned, then subtract 128
                        if (bdp < 0) bdp += 256;
                        bdp -= 128;
                        double yval=(yoffset-bdp)*(yscale/256.); // got to flip it, since it's a negative feedback op amp
                        if (p<histlen) series0[p] = new DataPoint(p+xoffset, yval);
                        else if (p<2*histlen) series1[p-histlen] = new DataPoint(p-histlen+xoffset, yval);
                        else if (p<3*histlen) series2[p-2*histlen] = new DataPoint(p-2*histlen+xoffset, yval);
                        else if (p<4*histlen) series3[p-3*histlen] = new DataPoint(p-3*histlen+xoffset, yval);
                        else break;
                        p++;
                    }
                    if (mActivity.get().display.getLineCount()>3) mActivity.get().display.setText("");
                    mActivity.get().display.append(formatter.toString()+" - "+String.valueOf(eventn)+" - "+String.valueOf(histlen)+"\n");
                    if (p>numsamples-2) {
                        _series0.resetData(series0);
                        _series1.resetData(series1);
                        _series2.resetData(series2);
                        _series3.resetData(series3);
                        graph.getViewport().setMinX(xoffset);
                        graph.getViewport().setMaxX(numsamples-1+xoffset);
                        graph.getViewport().setXAxisBoundsManual(true);
                        graph.getViewport().setMinY(-yscale*1.1/2.);
                        graph.getViewport().setMaxY(yscale*1.1/2.);
                        graph.getViewport().setYAxisBoundsManual(true);

                        eventn++;//count the events
                        send2usb(10); // get another event
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