package dk.rsd_grp3.ros_java.android_ui.ui;

/**
 * Created by turner on 9/17/15.
 */


import android.os.Handler;
import android.os.Message;

import org.apache.commons.logging.Log;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

public class SubscriberMessages extends AbstractNodeMain {

    public Handler handler;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android/listener");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        final Log log = connectedNode.getLog();
        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber("/UIchatter", std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                log.info("I heard: \"" + message.getData() + "\"");
                java.lang.String str = message.getData();
                Message msg = handler.obtainMessage();
                msg.obj = str;
                handler.sendMessage(msg);
            }
        });
    }

}
