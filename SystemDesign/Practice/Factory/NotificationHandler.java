package SystemDesign.Practice.Factory;

import SystemDesign.Practice.Factory.models.INotification;

public class NotificationHandler {

    public static void main(String[] args) {
        Notification n1 = new Notification.Builder().body("Your OTP is xyz").type(NotificationType.EMAIL).build();
        Notification n2 = new Notification.Builder().body("New offer for you").build();

        INotification provider = NotificationFactory.createNotificationSender(n1);
        provider.sendNotification(n1);

        provider = NotificationFactory.createNotificationSender(n2);
        provider.sendNotification(n2);
    }
}
