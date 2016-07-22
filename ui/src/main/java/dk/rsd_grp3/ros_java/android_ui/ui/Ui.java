package dk.rsd_grp3.ros_java.android_ui.ui;

//import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;

import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.RosActivity;
import org.ros.android.view.RosImageView;
import org.ros.node.NodeMainExecutor;
import org.ros.node.NodeConfiguration;

import java.net.URI;
import java.util.ResourceBundle;

public class Ui extends RosActivity implements SensorEventListener
{

    public enum ControlMode {
        JOYSTICK, SENSOR, DISABLED
    }

    private int numOfWifiLevels = 10;

    private RelativeLayout layout_joystick;
    private JoyStickClass js;
    private Handler checkWifiSignalHandler;

    private PublishButtons Buttontalker;
    private SubscriberMessages MessageListener;
    private PublisherJoyStick JoyStikPub;
    private RosImageView<sensor_msgs.CompressedImage> rosImageView;
    private String consoleText = "";
    private Vibrator myVib;
    private Switch enableJoystickSwitch;
    private Switch enableAccSwitch;
    private ProgressBar WifiSignalStreagth;
    private ProgressBar ProgressBarTurnLeft;
    private ProgressBar ProgressBarTurnRight;
    private ProgressBar ProgressBarTurnMid;
    private ProgressBar ProgressBarSpeedForward;
    private ProgressBar ProgressBarSpeedMid;
    private ProgressBar ProgressBarSpeedBackward;
    //sensor data
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private CompoundButton.OnCheckedChangeListener switchListener;
    private ControlMode ControlState;
    private float[] rMatrix = new float[9];
    private float[] result = new float[3];

    public Ui() {
        // The RosActivity constructor configures the notification title and ticker
        // messages.
        super("Robot Control", "Robot Control", URI.create("http://192.168.2.3:11311"));
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        //Config joystick
        layout_joystick = (RelativeLayout)findViewById(R.id.layout_joystick);
        js = new JoyStickClass(getApplicationContext()
                , layout_joystick, R.drawable.image_button);
        js.setStickSize(200, 200);
        js.setLayoutSize(500, 500);
        js.setLayoutAlpha(150);
        js.setStickAlpha(100);
        js.setOffset(90);
        js.setMinimumDistance(50);

        layout_joystick.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                js.drawStick(arg1);
                JoyStikPub.updateAxis(js.getNomX(),js.getNomY());
                updateSpeedTurnUI(js.getNomX(),js.getNomY());
                return true;
            }
            });

        //init sensor stuff
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);

        ControlState = ControlMode.JOYSTICK;

        myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        final Button stopButton = (Button) findViewById(R.id.stopButton);

        rosImageView = (RosImageView<sensor_msgs.CompressedImage>)findViewById(R.id.ros_camera_view);
        rosImageView.setTopicName("/camera/image_raw/compressed");
        rosImageView.setMessageType("sensor_msgs/CompressedImage");
        rosImageView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        enableJoystickSwitch = (Switch) findViewById(R.id.joystick_switch);
        enableAccSwitch = (Switch) findViewById(R.id.acc_switch);

        switchListener = new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) { //If new control mode is enabled
                    switch (buttonView.getId()) {
                        case R.id.acc_switch:
                            switchControlMode(ControlMode.SENSOR);
                            enableJoystickSwitch.setChecked(false);
                            break;
                        case R.id.joystick_switch:
                            switchControlMode(ControlMode.JOYSTICK);
                            enableAccSwitch.setChecked(false);
                            break;
                    }
                } else {
                    switch (buttonView.getId()) {
                        case R.id.acc_switch:
                           if(ControlState == ControlMode.SENSOR)
                               switchControlMode(ControlMode.DISABLED);
                            break;
                        case R.id.joystick_switch:
                            if(ControlState == ControlMode.JOYSTICK)
                                switchControlMode(ControlMode.DISABLED);
                            break;
                    }
                }


            }
        };

        enableJoystickSwitch.setOnCheckedChangeListener(switchListener);
        enableAccSwitch.setOnCheckedChangeListener(switchListener);

        WifiSignalStreagth = (ProgressBar) findViewById(R.id.progressBarWifi);

        WifiSignalStreagth.setMax(numOfWifiLevels);
        WifiSignalStreagth.setProgress(getWifiStrength());

        stopButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {
                updateButton(4);
                return false;
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                releasedButton();
            }
        });

        ProgressBarTurnLeft = (ProgressBar)findViewById(R.id.progressBarTurnLeft);
        ProgressBarTurnRight = (ProgressBar)findViewById(R.id.progressBarTurnRight);
        ProgressBarTurnMid = (ProgressBar)findViewById(R.id.progressBarTurnMid);
        ProgressBarTurnMid.getProgressDrawable().setColorFilter(0xFF466360,android.graphics.PorterDuff.Mode.MULTIPLY);
        ProgressBarSpeedForward = (ProgressBar)findViewById(R.id.progressBarSpeedForward);
        ProgressBarSpeedForward.getProgressDrawable().setColorFilter(0xFF00AA00,android.graphics.PorterDuff.Mode.MULTIPLY);
        ProgressBarSpeedBackward = (ProgressBar)findViewById(R.id.progressBarSpeedBackward);
        ProgressBarSpeedBackward.getProgressDrawable().setColorFilter(0xFFAA0000,android.graphics.PorterDuff.Mode.MULTIPLY);
        ProgressBarSpeedMid = (ProgressBar)findViewById(R.id.progressBarSpeedMid);
        ProgressBarSpeedMid.getProgressDrawable().setColorFilter(0xFF466360,android.graphics.PorterDuff.Mode.MULTIPLY);

        //Style, styling from code, (easyest way to keep button style)
        stopButton.getBackground().setColorFilter(0xFF661111, PorterDuff.Mode.MULTIPLY);

        checkWifiSignalHandler = new Handler();
        checkWifiSignalHandler.post(runnableCode);
    }

    private void convertToDegrees(float[] vector){
        for (int i = 0; i < vector.length; i++){
            vector[i] = Math.round(Math.toDegrees(vector[i]));
        }
    }

    public void onSensorChanged(SensorEvent event) {
        // we received a sensor event. it is a good practice to check
        // that we received the proper event
        float x = 0, xNom = 0;
        if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMatrix, event.values);
            SensorManager.getOrientation(rMatrix, result);
            convertToDegrees(result);
            x = -result[1];

            if(-10.0 < x && x < 10.0)
                xNom = (float)0.0;
            else {
                if(x < 0.0)
                    x = x + (float)10;
                else
                    x = x - (float)10;

                xNom = x/(float)35;
                if (xNom > 1.0)
                    xNom = (float)1.0;
                else if (xNom < -1.0)
                    xNom = (float)-1.0;
            }



            JoyStikPub.updateAxis(xNom,0);
            updateSpeedTurnUI(xNom,0);
        }
    }

    public void updateSpeedTurnUI(float x, float y) {
        if( x == 0.0) {
            ProgressBarTurnLeft.setProgress(0);
            ProgressBarTurnRight.setProgress(0);
        } else if( x < 0.0) {
            ProgressBarTurnLeft.setProgress((int)(-x*(float)100));
            ProgressBarTurnRight.setProgress(0);
        } else if (x > 0.0) {
            ProgressBarTurnRight.setProgress((int)(x*(float)100));
            ProgressBarTurnLeft.setProgress(0);
        }

        if( y == 0.0) {
            ProgressBarSpeedForward.setProgress(0);
            ProgressBarSpeedBackward.setProgress(0);
        } else if( y < 0.0) {
            ProgressBarSpeedBackward.setProgress((int)(-y*(float)100));
            ProgressBarSpeedForward.setProgress(0);
        } else if (y > 0.0) {
            ProgressBarSpeedForward.setProgress((int)(y*(float)100));
            ProgressBarSpeedBackward.setProgress(0);
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void switchControlMode(ControlMode mode)
    {
        ControlState = mode;
        switch ( ControlState){
            case JOYSTICK:
                mSensorManager.unregisterListener(this);
                JoyStikPub.updateAxis(0,0);
                updateSpeedTurnUI(0,0);
                layout_joystick.setVisibility(View.VISIBLE);
                break;

            case SENSOR:
                layout_joystick.setVisibility(View.INVISIBLE);
                JoyStikPub.updateAxis(0,0);
                updateSpeedTurnUI(0,0);
                mSensorManager.registerListener(this, mSensor,30000);
                break;

            case DISABLED:
                layout_joystick.setVisibility(View.INVISIBLE);
                mSensorManager.unregisterListener(this);
                JoyStikPub.updateAxis(0,0);
                updateSpeedTurnUI(0,0);
                break;

            default:
                layout_joystick.setVisibility(View.INVISIBLE);
                mSensorManager.unregisterListener(this);
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        switchControlMode(ControlMode.DISABLED); //Disable control before pausing (Dont read sensor input when app is in background)
        enableJoystickSwitch.setChecked(false);
        enableJoystickSwitch.setChecked(false);
        JoyStikPub.updateAxis(0,0); // Stop the robot before pauseing
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
    }

    //Enable fullscreen mode with no softkeys
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }



    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            int wifiStrength = getWifiStrength();

            //Update signal bar
            WifiSignalStreagth.setProgress(wifiStrength);
            //Color signal bar
            if(wifiStrength < 3)
                WifiSignalStreagth.getProgressDrawable().setColorFilter(0xFFFF0000,android.graphics.PorterDuff.Mode.MULTIPLY);
            else if (wifiStrength < 5)
                WifiSignalStreagth.getProgressDrawable().setColorFilter(0xFFFFFF00,android.graphics.PorterDuff.Mode.MULTIPLY);
            else
                WifiSignalStreagth.getProgressDrawable().setColorFilter(0xFF00FF00,android.graphics.PorterDuff.Mode.MULTIPLY);
            // Repeat this the same runnable code block again another 0.5 seconds
            checkWifiSignalHandler.postDelayed(runnableCode, 500);
        }
    };

    public void releasedButton(){
        Buttontalker.updateKey(0);
        myVib.vibrate(50);
    }
    public void updateButton(int _Buttonnumber){
        Buttontalker.updateKey(_Buttonnumber);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        Buttontalker = new PublishButtons();
        JoyStikPub = new PublisherJoyStick();
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        nodeConfiguration.setMasterUri(getMasterUri());
        nodeMainExecutor.execute(Buttontalker, nodeConfiguration);
        nodeMainExecutor.execute(rosImageView,nodeConfiguration);
        nodeMainExecutor.execute(JoyStikPub,nodeConfiguration);
    }

    private int getWifiStrength()
    {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numOfWifiLevels);
    }


    @Override //save text in console before screen rotation
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

}
