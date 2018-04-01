package plant.rc_car;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;

    private UUID myUUID;
    private final String UUID_STRING_WELL_KNOWN_SPP =
            "00001101-0000-1000-8000-00805F9B34FB";

    Button forward, backward, left, right;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        forward = (Button)findViewById(R.id.button);
        backward = (Button)findViewById(R.id.button2);
        left = (Button)findViewById(R.id.button3);
        right = (Button)findViewById(R.id.button4);


        forward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    myThreadConnected.write("F");
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    myThreadConnected.write("b");
                    return true;
                }
                return false;
            }
        });

        backward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    myThreadConnected.write("B");
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    myThreadConnected.write("b");
                    return true;
                }
                return false;
            }
        });

        left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    myThreadConnected.write("L");
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    myThreadConnected.write("e");
                    return true;
                }
                return false;
            }
        });

        right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    myThreadConnected.write("R");
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    myThreadConnected.write("e");
                    return true;
                }
                return false;
            }
        });

        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);


        Intent i = getIntent();
        String address = i.getStringExtra("address");

        myThreadConnectBTdevice = new ThreadConnectBTdevice(address);
        myThreadConnectBTdevice.start();
    }

    //Called in ThreadConnectBTdevice once connect successed
    //to start ThreadConnected
    private void startThreadConnected(BluetoothSocket socket){

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }

    /*
    ThreadConnectBTdevice:
    Background Thread to handle BlueTooth connecting
    */
    private class ThreadConnectBTdevice extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;

        private ThreadConnectBTdevice(String s) {
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            bluetoothDevice = ba.getRemoteDevice(s);

            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(myUUID);
                //textStatus.setText("bluetoothSocket: \n" + bluetoothSocket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                final String eMessage = e.getMessage();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        //forward.setText("something went wrong: \n" + eMessage);
                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if(success){
                //connect successful
                final String msgconnected = "connect successful:\n"
                        + "BluetoothDevice: " + bluetoothDevice.getAddress();


                runOnUiThread(new Runnable(){

                    @Override
                    public void run() {
                    }});

                startThreadConnected(bluetoothSocket);
            }else{

            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "Bluetooth Socket Closed",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    /*
    ThreadConnected:
    Background Thread to handle Bluetooth data communication
    after connected
     */
    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = connectedInputStream.read(buffer);
                    String strReceived = new String(buffer, 0, bytes);
                    final String msgReceived = String.valueOf(bytes) +
                            " bytes received:\n"
                            + strReceived;

                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            //textStatus.setText(msgReceived);
                        }});

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            //textStatus.setText(msgConnectionLost);
                        }});
                }
            }
        }

        public void write(String data) {
            try {
                connectedBluetoothSocket.getOutputStream().write(data.toString().getBytes());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
