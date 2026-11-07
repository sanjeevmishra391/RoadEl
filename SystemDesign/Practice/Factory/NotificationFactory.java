package SystemDesign.Practice.Factory;

import SystemDesign.Practice.Factory.models.EmailNotification;
import SystemDesign.Practice.Factory.models.INotification;
import SystemDesign.Practice.Factory.models.PushNotification;
import SystemDesign.Practice.Factory.models.SMSNotification;

public class NotificationFactory {
    public static INotification createNotificationSender(Notification notification) {
        switch(notification.getType()) {
            case NotificationType.EMAIL:
                return new EmailNotification();
            case NotificationType.SMS:
                return new SMSNotification();
            case NotificationType.PUSH:
                return new PushNotification();
            default:
                throw new UnsupportedOperationException("Notification type not supported");
        }
    }
}
