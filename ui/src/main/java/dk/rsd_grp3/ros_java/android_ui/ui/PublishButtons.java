package dk.rsd_grp3.ros_java.android_ui.ui;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
/**
 * Created by turner on 9/17/15.
 */
public class PublishButtons extends AbstractNodeMain{

    private String topic_name;
    private int butNumber;

    public PublishButtons() {
        topic_name = "android/key";
    }
    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android/key");
    }

    public void updateKey(int i){
        butNumber = i;
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        final Publisher<std_msgs.Int32> publisher =
                connectedNode.newPublisher(topic_name, std_msgs.Int32._TYPE);
        // This CancellableLoop will be canceled automatically when the node shuts
        // down.
        connectedNode.executeCancellableLoop(new CancellableLoop() {


            @Override
            protected void setup() {
                butNumber = 0;
            }

            @Override
            protected void loop() throws InterruptedException {
                std_msgs.Int32 butnum = publisher.newMessage();
                butnum.setData(butNumber);
                if (butNumber!=0){
                    publisher.publish(butnum);
                }
                Thread.sleep(50);
            }
        });
    }

}