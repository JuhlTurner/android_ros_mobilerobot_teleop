package dk.rsd_grp3.ros_java.android_ui.ui;

import android.util.Log;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import sensor_msgs.Joy;

/**
 * Created by turner on 9/17/15.
 */
public class PublisherJoyStick extends AbstractNodeMain{

    private String topic_name;
    private float x;
    private float y;

    public PublisherJoyStick() {
        topic_name = "android/joyStick";
    }
    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android/joyStick");
    }

    public void updateAxis(float _x, float _y){
        x = _x;
        y = _y;
        Log.d("Control X-axis:",Float.toString(_x));
        Log.d("Control Y-axis",Float.toString(_y));
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        final Publisher<sensor_msgs.Joy> publisher =
                connectedNode.newPublisher(topic_name, sensor_msgs.Joy._TYPE);
        // This CancellableLoop will be canceled automatically when the node shuts
        // down.
        connectedNode.executeCancellableLoop(new CancellableLoop() {


            @Override
            protected void setup() {
                x = (float)0.0;
                y = (float)0.0;
            }

            @Override
            protected void loop() throws InterruptedException {
                sensor_msgs.Joy input = publisher.newMessage();
                float[] input_data = {x,y};
                input.setAxes(input_data);
                //butnum.setData(butNumber);

                publisher.publish(input);

                Thread.sleep(50);
            }
        });
    }

}