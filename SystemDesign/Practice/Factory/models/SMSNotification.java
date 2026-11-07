package SystemDesign.Practice.Factory.models;

import SystemDesign.Practice.Factory.Notification;

public class SMSNotification implements INotification {
    public void sendNotification(Notification notification) {
        // connect to sms provider and send notification;
        System.out.println("Sending sms ...");
        System.out.println(notification.getBody());
    }
}
