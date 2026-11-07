package SystemDesign.Practice.Factory.models;

import SystemDesign.Practice.Factory.Notification;

public class EmailNotification implements INotification {
    public void sendNotification(Notification notification) {
        // connect to email provider and send email;
        System.out.println("Sending email...");
        System.out.println(notification.getBody());
    }
}
