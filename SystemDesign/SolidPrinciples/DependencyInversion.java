package SystemDesign.SolidPrinciples;

/*
Example Without Dependency Inversion:
```
class EmailService {
    void sendEmail(String message) {
        System.out.println("Sending email: " + message);
    }
}

class Notification {
    private EmailService emailService;

    public Notification() {
        this.emailService = new EmailService(); // Tightly coupled to EmailService
    }

    void notifyUser(String message) {
        emailService.sendEmail(message);
    }
}
```

Problems:
- If you want to add another notification type (e.g., SMS), you'll have to modify the Notification class.
- Hard to test, as the Notification class directly depends on EmailService.

Introduce an abstraction and make both the high-level Notification and low-level EmailService depend on it.

- You can switch to any MessageService (Email, SMS, etc.) without changing the Notification class.
- The code is easier to test by injecting mock implementations of MessageService.
- It adheres to DIP since both the Notification and EmailService depend on the MessageService interface.
 */
interface MessageService {
    void sendMessage(String message);
}

class EmailService implements MessageService {
    @Override
    public void sendMessage(String message) {
        System.out.println("Sending email: " + message);
    }
}

class SMSService implements MessageService {
    @Override
    public void sendMessage(String message) {
        System.out.println("Sending SMS: " + message);
    }
}

class Notification {
    private MessageService messageService;

    // Injecting the abstraction via constructor
    public Notification(MessageService messageService) {
        this.messageService = messageService;
    }

    void notifyUser(String message) {
        messageService.sendMessage(message);
    }
}


public class DependencyInversion {
    public static void main(String[] args) {
        MessageService emailService = new EmailService();
        Notification emailNotification = new Notification(emailService);
        emailNotification.notifyUser("Hello via Email!");

        MessageService smsService = new SMSService();
        Notification smsNotification = new Notification(smsService);
        smsNotification.notifyUser("Hello via SMS!");
    }
}
