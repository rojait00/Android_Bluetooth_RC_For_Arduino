package de.rojait.control_forarduino.bluetoothjoystickforarduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static de.rojait.control_forarduino.bluetoothjoystickforarduino.R.id.Ve_Max_O;
import static de.rojait.control_forarduino.bluetoothjoystickforarduino.R.id.Ve_Max_U;
import static de.rojait.control_forarduino.bluetoothjoystickforarduino.R.id.Ve_Mi;
import static de.rojait.control_forarduino.bluetoothjoystickforarduino.R.id.action_settings;
import static de.rojait.control_forarduino.bluetoothjoystickforarduino.R.id.kreis_beweglich;
import static de.rojait.control_forarduino.bluetoothjoystickforarduino.R.id.kreis_fest;
import static de.rojait.control_forarduino.bluetoothjoystickforarduino.R.id.relativeLayout_main;
import static de.rojait.control_forarduino.bluetoothjoystickforarduino.R.id.seekBar;
import static de.rojait.control_forarduino.bluetoothjoystickforarduino.R.id.seekBar2;
import static de.rojait.control_forarduino.bluetoothjoystickforarduino.R.id.textView;
import static de.rojait.control_forarduino.bluetoothjoystickforarduino.R.layout.activity_control_window;


public class ControlWindow extends ActionBarActivity {

    public float x, y;
    ImageView beweglKreis,festerKreis;
    MenuItem settings;
    int servoHorizontal, servoVertikal;
    int screenheight, screenwidth;
    TextView auslenkungsanzeige;
    int seekbar2Prog = 200;


    //region BTGruscht

    String BT_Device_Name;
    TextView myLabel;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    boolean isConnected = false;

    int hl ;
    int hm ;
    int hr ;

    int vo ;
    int vm ;
    int vu ;
    private boolean js;
    private int servoVertikal_old =9999;
    private int servoHorizontal_old =9999;
    private int sleepKonst = 60;

    void startBT()
    {
        try {
            findBT();
            openBT();
        }
        catch (IOException ex)
        {
            showMessage("Connect Failed");
        }
    }

    long zuletztgesendet;
    void startSending()
    {
        try {

            if((System.currentTimeMillis() > zuletztgesendet + sleepKonst)&& (servoVertikal!= servoVertikal_old || servoHorizontal != servoHorizontal_old)){
                sendData();
                zuletztgesendet = System.currentTimeMillis();
            }
        }
        catch (IOException ex) {
            showMessage("SEND FAILED");
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        try {
            closeBT();
        }
        catch (IOException ex) { }
    }

    void findBT() {
        try {


            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                myLabel.setText("No bluetooth adapter available");
            }

            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals(BT_Device_Name)) {
                        mmDevice = device;
                        break;
                    }
                }
            }
            myLabel.setText("Bluetooth Device Found");
        }
        catch (Exception e)
        {}
    }

    void openBT() throws IOException {
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard //SerialPortService ID
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
            beginListenForData();
            myLabel.setText("Bluetooth Opened");
            isConnected = true;
        }
        catch (Exception e){}
    }

    void beginListenForData() {
        try {
            final Handler handler = new Handler();
            final byte delimiter = 10; //This is the ASCII code for a newline character

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];
            workerThread = new Thread(new Runnable() {
                public void run() {
                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                        try {
                            int bytesAvailable = mmInputStream.available();
                            if (bytesAvailable > 0) {
                                byte[] packetBytes = new byte[bytesAvailable];
                                mmInputStream.read(packetBytes);
                                for (int i = 0; i < bytesAvailable; i++) {
                                    byte b = packetBytes[i];
                                    if (b == delimiter) {
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        handler.post(new Runnable() {
                                            public void run() {
                                                if (data == "r")
                                                     myLabel.setText("received!");
                                                else
                                                    myLabel.setText(data);
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            stopWorker = true;
                        }
                    }
                }
            });

            workerThread.start();
        }
        catch (Exception e)
        {}
    }

    void sendData() throws IOException {
        try {
            String msg;
            if (servoVertikal!= servoVertikal_old) {
                msg = "x" + servoVertikal;
                mmOutputStream.write(msg.getBytes());
                servoVertikal_old = servoVertikal;
            }
            if (servoHorizontal!= servoHorizontal_old) {
                msg = "y" + servoHorizontal;
                mmOutputStream.write(msg.getBytes());
                servoHorizontal_old = servoHorizontal;
            }
            myLabel.setText("Data Sent");
        }
        catch(Exception e)
        {}
    }

    void closeBT() throws IOException {
        try {


            stopWorker = true;
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
            myLabel.setText("Bluetooth Closed");
            isConnected = false;
        }
        catch (Exception e)
        {}
    }

    private void showMessage(String theMsg) {
        Log.i("Fehler:", theMsg);
        //Toast msg = Toast.makeText(getBaseContext(), theMsg,Toast.LENGTH_SHORT);
        //msg.show();
    }
    //endregion




    @Override
    protected void onResume()
    {
        loadProbs();
        try {
           startBT();
        }
        catch (Exception e)
        {}
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_control_window);

        //region Bt

        myLabel = (TextView)findViewById(de.rojait.control_forarduino.bluetoothjoystickforarduino.R.id.label);
        myLabel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction()==MotionEvent.ACTION_UP)
                {
                    if (isConnected)
                    {
                        try {
                            closeBT();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        startBT();
                    }
                }
                return true;
            }
        });
        //endregion

        //region probs
        loadProbs();
        //endregion

        //region GUI
        settings = (MenuItem) findViewById(action_settings);
        auslenkungsanzeige = (TextView) findViewById(textView);

        Display display = getWindowManager().getDefaultDisplay();
        screenwidth = display.getWidth();
        screenheight = display.getHeight();
        //endregion

        //region bewegl_Kreis_Portrait
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
        {


            beweglKreis = (ImageView) findViewById(kreis_beweglich);
            festerKreis = (ImageView) findViewById(kreis_fest);


            final View touchView = findViewById(relativeLayout_main);
            touchView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP && js) {
                        servoHorizontal = hm;
                        servoVertikal = vm;
                        setServoauslenkung();
                        SystemClock.sleep(sleepKonst);
                        setServoauslenkung();

                        x = festerKreis.getX();
                        y = festerKreis.getY();

                        beweglKreis.setX(x);
                        beweglKreis.setY(y);
                    } else {
                        x = event.getX();
                        y = event.getY();

                        calculateServoauslenkung();
                        setServoauslenkung();

                        beweglKreis.setX(x - 40);
                        beweglKreis.setY(y - 40);
                    }
                    return true;
                }
            });

        }
        //endregion

        //region seekBar_Angle_Land

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            //region Seekbar
            final SeekBar sk = (SeekBar)findViewById(seekBar);
            final SeekBar sk2 = (SeekBar)findViewById(seekBar2);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sk.getLayoutParams();
            params.width = screenheight-180;
            sk.setLayoutParams(params);


            sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    servoVertikal=progress+vu;
                    setServoauslenkung();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    servoVertikal=sk.getProgress()+vu;
                    setServoauslenkung();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if(js) {
                        sk.setMax(0);
                        sk.setMax(vo-vu);
                        sk.setProgress(vm-vu);
                        setServoauslenkung();
                        SystemClock.sleep(sleepKonst);
                        setServoauslenkung();
                    }
                }
            });
            sk.setMax(0);
            sk.setMax(vo-vu);
            sk.setProgress(vm-vu);



            //region

            CheckBox cb = (CheckBox)findViewById(R.id.checkBox);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    sk2.setVisibility(isChecked?View.VISIBLE:View.INVISIBLE);
                }
            });

            //region move_seekbar1

            sk2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    SharedPreferences pos;
                    String fileName = "Settings.st";

                    pos = getSharedPreferences(fileName, 0);
                    SharedPreferences.Editor editor = pos.edit();
                    sk.setX((float)progress-400);
                    editor.putInt("SeekBar_POS",  progress);
                    editor.commit();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            sk2.setMax(0);
            sk2.setMax(screenwidth);
            sk2.setProgress(seekbar2Prog);

            //endregion

            //endregion

            //endregion

            //region Angle
            final SensorListener sl = new SensorListener() {
                public void onSensorChanged(int sensor, float[] values) {
                    float pitch = values[2];
                    pitch+=1.63;//Kalibrierung
                    pitch+=45;
                    //pitch %= 180;
                    servoHorizontal = (int)((pitch/90)*(float)(hr-hl));

                    
                    servoHorizontal = servoHorizontal> hr? hr:servoHorizontal;
                    servoHorizontal = servoHorizontal< hl? hl:servoHorizontal;

                    servoHorizontal = hr-servoHorizontal;
                    setServoauslenkung();
                }

                public void onAccuracyChanged(int sensor, int accuracy) {
                }
            };

            // Locate the SensorManager using Activity.getSystemService
            SensorManager sm;
            sm = (SensorManager) getSystemService(SENSOR_SERVICE);

            // Register your SensorListener
            sm.registerListener(sl, SensorManager.SENSOR_ORIENTATION, SensorManager.SENSOR_DELAY_NORMAL);
            //endregion

        }
        //endregion


        startBT();

    }

    private void loadProbs() {
        SharedPreferences pos;
        String fileName = "Settings.st";
        pos = getSharedPreferences(fileName, 0);

        try {
            js = Boolean.parseBoolean(pos.getString("JumpToZero", "true"));
        }
        catch (Exception e)
        {}
        BT_Device_Name = pos.getString("BT_ID","HC-06");

        hl = Integer.parseInt(pos.getString("Hz_Max_L", getString(de.rojait.control_forarduino.bluetoothjoystickforarduino.R.string.Horizontal_Links_Max)));
        hm = Integer.parseInt(pos.getString("Hz_M", getString(de.rojait.control_forarduino.bluetoothjoystickforarduino.R.string.Horizontal_Mitte)));
        hr = Integer.parseInt(pos.getString("Hz_Max_R", getString(de.rojait.control_forarduino.bluetoothjoystickforarduino.R.string.Horizontal_Max_Rechts)));
        seekbar2Prog= pos.getInt("SeekBar_POS",screenwidth/2);
        vo = Integer.parseInt(pos.getString("Ve_Max_O", getString(de.rojait.control_forarduino.bluetoothjoystickforarduino.R.string.Vertikal_Max_Oben)));
        vm = Integer.parseInt(pos.getString("Ve_M", getString(de.rojait.control_forarduino.bluetoothjoystickforarduino.R.string.Vertikal_Mitte)));
        vu = Integer.parseInt(pos.getString("Ve_Max_U", getString(de.rojait.control_forarduino.bluetoothjoystickforarduino.R.string.Vertikal_Max_Unten)));

    }

    private void calculateServoauslenkung()
    {
        double tempVertikal,tempHorizontal;
        tempHorizontal = x/(screenwidth*0.9);
        tempVertikal = ((screenheight/2)+(screenwidth/2)-202-y)/(screenwidth*0.9);
        /*
        Log.i("h0-1: ",tempHorizontal+"");
        Log.i("v0-1: ",tempVertikal+"");
        */
        try {
            /* Zu langsam!
            servoHorizontal = tempHorizontal > 0.5 ? (int) (tempHorizontal * (hr - hl)/2 * (((double)(hr - hl)) / (hr - hm))) : (int) (tempHorizontal * (hr - hl)/2 * (((double)(hr - hl)) / (hm - hl)));
            servoHorizontal += 4; //kalibrierung

            double tempVoh = tempVertikal * (vo - vu) * (((double)(vo - vm)) / (vo - vu));
            double tempVuh = tempVertikal * (vo - vu) * (((double)(vm - vu)) / (vo - vu));
            servoVertikal = tempVertikal > 0.5 ? (int) (tempVoh)*2 : (int) (tempVuh)*2;
            servoVertikal -=2;//kalibrierung
            */


            servoHorizontal = (int)(tempHorizontal*(hr-hl));
            servoHorizontal = servoHorizontal> hr? hr:servoHorizontal;
            servoHorizontal = servoHorizontal< hl? hl:servoHorizontal;

            servoVertikal = (int)(tempVertikal*(vo-vu));
            servoVertikal = servoVertikal> vo? vo:servoVertikal;
            servoVertikal = servoVertikal< vu? vu:servoVertikal;
        }
        catch (Exception e)
        {}

    }

    private void setServoauslenkung() {
        auslenkungsanzeige.setText("X = " + servoHorizontal + "°\nY = "+ servoVertikal + "°");
        startSending();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(de.rojait.control_forarduino.bluetoothjoystickforarduino.R.menu.menu_control_window, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == action_settings) {
            Intent i = new Intent("de.rojait.control_forarduino.bluetoothjoystickforarduino.SettingsActivity");
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
