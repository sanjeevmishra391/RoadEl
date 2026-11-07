package SystemDesign.Practice.Factory;

import java.time.Instant;

public class Notification {

    private final NotificationType type;
    private final NotificationPriority priority;
    private final String body;
    private final Instant createdAt;

    private Notification(Builder builder) {
        this.type = builder.type;
        this.priority = builder.priority;
        this.body = builder.body;
        this.createdAt = builder.createdAt;
    }

    public static class Builder {
        private NotificationType type = NotificationType.PUSH;
        private NotificationPriority priority = NotificationPriority.DEFAULT;
        private Instant createdAt = Instant.now();
        private String body;

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder priority(NotificationPriority priority) {
            this.priority = priority;
            return this;
        }

        public Notification build() {
            return new Notification(this);
        }
    }

    public NotificationPriority getPriority() { 
        return this.priority; 
    } 
    
    public Instant getCreatedDate() { 
        return this.createdAt; 
    }
    
    public String getBody() { 
        return this.body; 
    }
    
    public NotificationType getType() { 
        return this.type; 
    }
}