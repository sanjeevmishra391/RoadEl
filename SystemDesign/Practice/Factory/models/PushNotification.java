package SystemDesign.Practice.Factory.models;

import SystemDesign.Practice.Factory.Notification;

public class PushNotification implements INotification {
    public void sendNotification(Notification notification) {
        // connect to push notification provider and send notification;
        System.out.println("Sending push notification...");
        System.out.println(notification.getBody());
    }
}
