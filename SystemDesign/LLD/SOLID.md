# SOLID Principles — Senior Engineer Reference

> This is not a tutorial. It is a reference for engineers who want to think precisely about why these principles matter, where they are routinely violated in real codebases, and how to talk about them under interview pressure.

---

## The Five Principles at a Glance

| # | Principle | One-line rule |
|---|---|---|
| S | Single Responsibility | A class should have one reason to change |
| O | Open/Closed | Open for extension, closed for modification |
| L | Liskov Substitution | Subtypes must be substitutable for their base types |
| I | Interface Segregation | No client should be forced to depend on methods it does not use |
| D | Dependency Inversion | Depend on abstractions, not concretions |

---

## S — Single Responsibility Principle

**One-line rule**: A class should have one, and only one, reason to change.

"Reason to change" is the key phrase. It means: one stakeholder, one actor, one concern. If the Finance team and the Operations team can both ask for changes to the same class, that class serves two masters and violates SRP.

### The Problem

```java
// BAD: OrderService has three reasons to change:
// 1. Business logic changes (how an order is validated/processed)
// 2. Persistence layer changes (how orders are stored)
// 3. Notification requirements change (who gets notified, via what channel)

public class OrderService {
    private Connection dbConnection;

    public void processOrder(Order order) {
        // Validation logic
        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order total must be positive");
        }

        // Business logic
        order.setStatus(OrderStatus.PROCESSING);
        order.setProcessedAt(Instant.now());
        applyLoyaltyDiscount(order);

        // Persistence — directly embedded SQL
        try {
            PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT INTO orders (id, user_id, status, total, processed_at) VALUES (?, ?, ?, ?, ?)"
            );
            stmt.setString(1, order.getId());
            stmt.setString(2, order.getUserId());
            stmt.setString(3, order.getStatus().name());
            stmt.setBigDecimal(4, order.getTotalAmount());
            stmt.setTimestamp(5, Timestamp.from(order.getProcessedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist order", e);
        }

        // Notification — SMTP directly inside business logic
        try {
            Session session = Session.getInstance(System.getProperties());
            MimeMessage message = new MimeMessage(session);
            message.setRecipient(Message.RecipientType.TO, 
                new InternetAddress(order.getUserEmail()));
            message.setSubject("Order Confirmed: " + order.getId());
            message.setText("Your order has been processed.");
            Transport.send(message);
        } catch (MessagingException e) {
            // Swallowed — order succeeded, email failed silently
        }
    }

    private void applyLoyaltyDiscount(Order order) {
        if (order.getUserLoyaltyPoints() > 1000) {
            order.applyDiscount(0.05);
        }
    }
}
```

Any of three different teams can require a change: switch from SQL to JPA, add a new notification channel, change discount rules. Each change risks breaking the others because they are all interleaved in one class.

### The Fix

```java
// GOOD: Three focused classes with single responsibilities

// 1. Pure business logic — changes when business rules change
public class OrderProcessor {
    private final DiscountPolicy discountPolicy;

    public OrderProcessor(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }

    public ProcessedOrder process(Order order) {
        validateOrder(order);
        discountPolicy.apply(order);
        order.setStatus(OrderStatus.PROCESSING);
        order.setProcessedAt(Instant.now());
        return new ProcessedOrder(order);
    }

    private void validateOrder(Order order) {
        if (order.getItems().isEmpty()) {
            throw new OrderValidationException("Order must have at least one item");
        }
        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderValidationException("Order total must be positive");
        }
    }
}

// 2. Persistence concern — changes when storage technology changes
public interface OrderRepository {
    void save(ProcessedOrder order);
    Optional<Order> findById(String orderId);
}

public class JpaOrderRepository implements OrderRepository {
    private final EntityManager em;

    public JpaOrderRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public void save(ProcessedOrder order) {
        em.persist(order.toEntity());
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(em.find(OrderEntity.class, orderId))
                       .map(OrderEntity::toDomain);
    }
}

// 3. Notification concern — changes when communication requirements change
public interface OrderNotificationService {
    void notifyOrderProcessed(ProcessedOrder order);
}

public class EmailOrderNotificationService implements OrderNotificationService {
    private final EmailClient emailClient;

    public EmailOrderNotificationService(EmailClient emailClient) {
        this.emailClient = emailClient;
    }

    @Override
    public void notifyOrderProcessed(ProcessedOrder order) {
        emailClient.send(Email.builder()
            .to(order.getUserEmail())
            .subject("Order Confirmed: " + order.getId())
            .body(buildConfirmationBody(order))
            .build());
    }

    private String buildConfirmationBody(ProcessedOrder order) {
        return String.format("Your order #%s for $%.2f has been confirmed.",
            order.getId(), order.getTotalAmount());
    }
}

// 4. Orchestration — thin coordinator with no logic of its own
public class OrderApplicationService {
    private final OrderProcessor processor;
    private final OrderRepository repository;
    private final OrderNotificationService notificationService;

    public OrderApplicationService(OrderProcessor processor,
                                   OrderRepository repository,
                                   OrderNotificationService notificationService) {
        this.processor = processor;
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public void placeOrder(Order order) {
        ProcessedOrder processed = processor.process(order);
        repository.save(processed);
        notificationService.notifyOrderProcessed(processed);
    }
}
```

**Real-world analogy**: A restaurant kitchen has a chef who cooks, a cashier who handles payment, and a server who delivers food. The chef doesn't take payment. Each has one job to change.

**How it shows up in design patterns**: The Command pattern enforces SRP by encapsulating a single operation. The Facade pattern collects calls to multiple SRP-following components behind one interface.

**Interview question**: "We have a `UserService` class that handles user registration, password reset, login audit logging, and profile updates. What's wrong with this?" — Model answer: "It violates SRP. It has at least four reasons to change: registration flow, security policy, audit requirements, and profile schema. I would extract `AuthenticationService`, `UserRegistrationService`, `AuditService`, and `UserProfileService`. The key question is: who requests each change? If the security team, compliance team, and product team can all demand changes to the same class, it has too many responsibilities."

---

## O — Open/Closed Principle

**One-line rule**: Software entities should be open for extension but closed for modification.

"Closed for modification" means existing, tested, deployed code should not need to change when you add a new capability. "Open for extension" means you can add new behavior without touching that code. In practice this is achieved through abstraction: define stable interfaces that new implementations can satisfy.

### The Problem

```java
// BAD: Every time a new report format is needed, this class is modified.
// The class has been changed 6 times for "format additions" and each time
// it was a regression risk for the formats that already worked.

public class ReportExporter {

    public byte[] export(Report report, String format) {
        switch (format) {
            case "PDF":
                PdfDocument pdf = new PdfDocument();
                for (ReportSection section : report.getSections()) {
                    pdf.addPage(section.getTitle(), section.getContent());
                }
                pdf.setMetadata("Author", report.getAuthor());
                return pdf.toBytes();

            case "CSV":
                StringBuilder csv = new StringBuilder();
                csv.append(String.join(",", report.getColumnHeaders())).append("\n");
                for (String[] row : report.getRows()) {
                    csv.append(String.join(",", row)).append("\n");
                }
                return csv.toString().getBytes(StandardCharsets.UTF_8);

            case "XLSX":
                Workbook wb = new XSSFWorkbook();
                Sheet sheet = wb.createSheet(report.getTitle());
                // ... Excel construction logic
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                wb.write(out);
                return out.toByteArray();

            // Next sprint: "we need JSON export"
            // Next sprint: "we need HTML export for email"
            // Each addition modifies this class and risks breaking PDF/CSV
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
}
```

### The Fix

```java
// GOOD: New formats are added by implementing ReportFormatter.
// ReportExporter is never modified again.

public interface ReportFormatter {
    byte[] format(Report report);
    String supportedFormat();
}

public class PdfReportFormatter implements ReportFormatter {
    @Override
    public byte[] format(Report report) {
        PdfDocument pdf = new PdfDocument();
        for (ReportSection section : report.getSections()) {
            pdf.addPage(section.getTitle(), section.getContent());
        }
        pdf.setMetadata("Author", report.getAuthor());
        return pdf.toBytes();
    }

    @Override
    public String supportedFormat() { return "PDF"; }
}

public class CsvReportFormatter implements ReportFormatter {
    @Override
    public byte[] format(Report report) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", report.getColumnHeaders())).append("\n");
        for (String[] row : report.getRows()) {
            csv.append(escapeCsvRow(row)).append("\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String supportedFormat() { return "CSV"; }

    private String escapeCsvRow(String[] row) {
        return Arrays.stream(row)
            .map(cell -> cell.contains(",") ? "\"" + cell + "\"" : cell)
            .collect(Collectors.joining(","));
    }
}

// New requirement: JSON export. No existing code is touched.
public class JsonReportFormatter implements ReportFormatter {
    private final ObjectMapper mapper;

    public JsonReportFormatter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public byte[] format(Report report) {
        try {
            Map<String, Object> json = Map.of(
                "title", report.getTitle(),
                "headers", report.getColumnHeaders(),
                "rows", report.getRows()
            );
            return mapper.writeValueAsBytes(json);
        } catch (JsonProcessingException e) {
            throw new ReportExportException("JSON serialization failed", e);
        }
    }

    @Override
    public String supportedFormat() { return "JSON"; }
}

// ReportExporter is closed — it never changes regardless of how many formats are added
public class ReportExporter {
    private final Map<String, ReportFormatter> formatters;

    public ReportExporter(List<ReportFormatter> formatters) {
        this.formatters = formatters.stream()
            .collect(Collectors.toMap(ReportFormatter::supportedFormat, f -> f));
    }

    public byte[] export(Report report, String format) {
        ReportFormatter formatter = formatters.get(format);
        if (formatter == null) {
            throw new UnsupportedFormatException(
                "No formatter registered for: " + format + 
                ". Supported formats: " + formatters.keySet()
            );
        }
        return formatter.format(report);
    }
}
```

**Real-world analogy**: A power strip is closed for modification (you don't rewire it) but open for extension (you plug in whatever device you want).

**How it shows up in design patterns**: Strategy pattern is OCP made explicit — new strategies extend behavior without modifying the context. Decorator pattern adds behavior to existing objects without modifying them.

**Interview question**: "How would you add a new discount type — say, a VIP discount — to an existing `OrderService` that already handles promotional and loyalty discounts?" — Model answer: "I'd check whether discounts are modeled as a strategy. If `applyDiscount(Order order)` exists as an interface, adding `VipDiscountPolicy` is a pure extension. If discount logic is embedded in a switch-case inside `OrderService`, that's an OCP violation. The fix is to extract a `DiscountPolicy` interface and register policies at configuration time — new discount types never touch `OrderService`."

---

## L — Liskov Substitution Principle

**One-line rule**: If S is a subtype of T, then objects of type T may be replaced with objects of type S without altering any correct behavior of the program.

In practical terms: a subclass must fulfill the contract of its parent class, not just its syntax. If you need to add preconditions, weaken postconditions, or throw exceptions the parent doesn't throw, you have an LSP violation. The canonical test: "Can I swap the parent for the child everywhere the parent is used and have the program remain correct?"

### The Problem

```java
// BAD: ReadOnlyUserRepository "is a" UserRepository only syntactically.
// Callers of UserRepository have a contract that includes write operations.
// ReadOnlyUserRepository breaks that contract at runtime.

public class UserRepository {
    protected Database db;

    public User findById(String id) {
        return db.query("SELECT * FROM users WHERE id = ?", id);
    }

    public void save(User user) {
        db.execute("INSERT OR REPLACE INTO users ...", user);
    }

    public void delete(String id) {
        db.execute("DELETE FROM users WHERE id = ?", id);
    }
}

// This compiles. It does not substitute correctly.
public class ReadOnlyUserRepository extends UserRepository {
    @Override
    public void save(User user) {
        throw new UnsupportedOperationException("This repository is read-only");
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException("This repository is read-only");
    }
}

// The caller is broken at runtime despite the code compiling
public class UserSyncService {
    private final UserRepository repo;  // type says it can write

    public void syncFromExternalSystem(List<User> users) {
        for (User user : users) {
            repo.save(user);  // explodes if repo is ReadOnlyUserRepository
        }
    }
}
```

### The Fix

```java
// GOOD: The hierarchy reflects the actual contract, not the coincidental
// overlap in method names. Read and write capabilities are separate.

public interface UserReadRepository {
    Optional<User> findById(String id);
    List<User> findByStatus(UserStatus status);
    boolean existsByEmail(String email);
}

public interface UserWriteRepository {
    void save(User user);
    void delete(String id);
    void saveAll(List<User> users);
}

// Full repository — used by services that need both read and write
public interface UserRepository extends UserReadRepository, UserWriteRepository { }

// Read-only implementation — correct contract, no surprises
public class ReadOnlyUserRepository implements UserReadRepository {
    private final DataSource dataSource;

    @Override
    public Optional<User> findById(String id) {
        // read from replica database
        return queryReplica(id);
    }

    @Override
    public List<User> findByStatus(UserStatus status) {
        return queryReplicaByStatus(status);
    }

    @Override
    public boolean existsByEmail(String email) {
        return queryReplicaEmailExists(email);
    }
}

// Full implementation for write-capable services
public class JpaUserRepository implements UserRepository {
    private final EntityManager em;

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(em.find(UserEntity.class, id)).map(UserEntity::toDomain);
    }

    @Override
    public void save(User user) {
        em.merge(user.toEntity());
    }

    @Override
    public void delete(String id) {
        UserEntity entity = em.find(UserEntity.class, id);
        if (entity != null) em.remove(entity);
    }
    // ... other methods
}

// Sync service now expresses its actual dependency: it writes users
public class UserSyncService {
    private final UserWriteRepository repo;  // precise dependency

    public void syncFromExternalSystem(List<User> users) {
        repo.saveAll(users);
    }
}

// Reporting service expresses that it only reads
public class UserReportingService {
    private final UserReadRepository repo;  // clear contract, no accidental writes

    public UserReport generateStatusReport(UserStatus status) {
        List<User> users = repo.findByStatus(status);
        return UserReport.from(users);
    }
}
```

**Real-world analogy**: A `SavingsAccount` and `CheckingAccount` can both substitute for a `BankAccount` when performing a balance check. But if `SavingsAccount.withdraw()` throws an exception for amounts exceeding a daily limit that `BankAccount.withdraw()` doesn't declare, it violates LSP — callers of `BankAccount` don't expect that exception.

**How it shows up in design patterns**: Template Method pattern relies on LSP — subclasses fill in the "template holes" without breaking the algorithm. Strategy pattern relies on it — strategies are substitutable implementations of the same interface. Any violation of LSP in a Strategy makes the Strategy unsafe.

**Interview question**: "We have a `Square` that extends `Rectangle`. Is this OK?" — Model answer: "No — this is the canonical LSP violation. A `Rectangle` has the contract that setting width doesn't affect height and vice versa. A `Square` cannot satisfy this because its dimensions must be equal. If you substitute a `Square` where a `Rectangle` is expected and the caller does `rect.setWidth(5); rect.setHeight(10); assert rect.getArea() == 50;`, it will fail. The fix is to not model this as inheritance. They can both implement a `Shape` interface with `getArea()`, but `Square` should not extend `Rectangle`."

---

## I — Interface Segregation Principle

**One-line rule**: No client should be forced to depend on methods it does not use.

Fat interfaces create accidental coupling. When a class is forced to implement methods it doesn't need (or leaves them as empty stubs or `UnsupportedOperationException`), it's a signal that the interface is doing too much. ISP says: prefer many small, role-specific interfaces over one large general-purpose interface.

### The Problem

```java
// BAD: NotificationProvider is a single interface with six methods.
// EmailNotificationProvider has no concept of push tokens.
// SmsNotificationProvider has no concept of email addresses.
// Both are forced to implement methods that are semantically meaningless for them.

public interface NotificationProvider {
    void sendEmail(String recipient, String subject, String body);
    void sendEmailWithAttachment(String recipient, String subject, String body, byte[] attachment);
    void sendSms(String phoneNumber, String message);
    void sendSmsWithCallback(String phoneNumber, String message, String callbackUrl);
    void sendPushNotification(String deviceToken, String title, String message);
    void sendPushNotificationWithData(String deviceToken, String title, String message, Map<String, String> data);
}

// Forced to throw for every SMS/push method
public class EmailNotificationProvider implements NotificationProvider {
    private final SmtpClient smtpClient;

    @Override
    public void sendEmail(String recipient, String subject, String body) {
        smtpClient.send(recipient, subject, body);
    }

    @Override
    public void sendEmailWithAttachment(String recipient, String subject, String body, byte[] attachment) {
        smtpClient.sendWithAttachment(recipient, subject, body, attachment);
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        throw new UnsupportedOperationException("Email provider cannot send SMS");
    }

    @Override
    public void sendSmsWithCallback(String phoneNumber, String message, String callbackUrl) {
        throw new UnsupportedOperationException("Email provider cannot send SMS");
    }

    @Override
    public void sendPushNotification(String deviceToken, String title, String message) {
        throw new UnsupportedOperationException("Email provider cannot send push");
    }

    @Override
    public void sendPushNotificationWithData(String deviceToken, String title, String message, Map<String, String> data) {
        throw new UnsupportedOperationException("Email provider cannot send push");
    }
}
```

### The Fix

```java
// GOOD: Three focused interfaces.
// Implementations only implement what they can actually do.
// Callers declare exactly which capability they need.

public interface EmailNotifier {
    void sendEmail(String recipient, String subject, String body);
    void sendEmailWithAttachment(String recipient, String subject, String body, byte[] attachment);
}

public interface SmsNotifier {
    void sendSms(String phoneNumber, String message);
    void sendSmsWithCallback(String phoneNumber, String message, String callbackUrl);
}

public interface PushNotifier {
    void sendPushNotification(String deviceToken, String title, String message);
    void sendPushNotificationWithData(String deviceToken, String title, String message, Map<String, String> data);
}

// EmailNotificationProvider only implements what it can do
public class SesEmailNotificationProvider implements EmailNotifier {
    private final AmazonSimpleEmailService sesClient;

    @Override
    public void sendEmail(String recipient, String subject, String body) {
        sesClient.sendEmail(buildRequest(recipient, subject, body));
    }

    @Override
    public void sendEmailWithAttachment(String recipient, String subject, String body, byte[] attachment) {
        sesClient.sendRawEmail(buildRawRequest(recipient, subject, body, attachment));
    }
}

// Twilio provider only implements what it can do
public class TwilioSmsProvider implements SmsNotifier {
    private final TwilioClient twilioClient;

    @Override
    public void sendSms(String phoneNumber, String message) {
        twilioClient.messages().create(phoneNumber, TwilioFrom.DEFAULT, message);
    }

    @Override
    public void sendSmsWithCallback(String phoneNumber, String message, String callbackUrl) {
        twilioClient.messages().create(phoneNumber, TwilioFrom.DEFAULT, message, callbackUrl);
    }
}

// A multi-channel provider can implement all three — and this is valid
// because it genuinely supports all channels
public class MultiChannelNotificationProvider implements EmailNotifier, SmsNotifier, PushNotifier {
    // ... implements all six methods genuinely
}

// Callers declare their minimum required interface
public class OrderConfirmationService {
    private final EmailNotifier emailNotifier;  // doesn't know or care about SMS or push

    public OrderConfirmationService(EmailNotifier emailNotifier) {
        this.emailNotifier = emailNotifier;
    }

    public void confirmOrder(ProcessedOrder order) {
        emailNotifier.sendEmail(
            order.getUserEmail(),
            "Order Confirmed: " + order.getId(),
            buildConfirmationMessage(order)
        );
    }
}

public class DeliveryAlertService {
    private final SmsNotifier smsNotifier;
    private final PushNotifier pushNotifier;

    public void alertDelivery(Delivery delivery) {
        smsNotifier.sendSms(delivery.getCustomerPhone(), "Your package has arrived.");
        pushNotifier.sendPushNotification(
            delivery.getCustomerDeviceToken(),
            "Package Delivered",
            "Your order #" + delivery.getOrderId() + " was delivered."
        );
    }
}
```

**Real-world analogy**: A printer, scanner, and fax machine can all implement separate interfaces. A device that only prints should not be forced to implement a fax interface just because some other device in the family does fax.

**How it shows up in design patterns**: The Adapter pattern often solves ISP violations — it wraps a fat interface and exposes only the role-specific subset a client needs. The Facade pattern similarly narrows a broad API to a specific use-case surface.

**Interview question**: "You have a `PaymentProcessor` interface with `authorize`, `capture`, `refund`, `chargeCard`, `storeCard`, and `deleteStoredCard`. Is this well designed?" — Model answer: "Probably not. These methods serve different clients: authorization and capture are for the order placement flow; refund is for customer service; card storage operations are for a wallet feature. Each of these consumers should depend on a narrower interface: `PaymentAuthorizer`, `PaymentRefunder`, `CardVaultService`. This prevents a refund handler from accidentally having access to the `chargeCard` method, which is a security concern in addition to an ISP concern."

---

## D — Dependency Inversion Principle

**One-line rule**: High-level modules should not depend on low-level modules. Both should depend on abstractions.

DIP has two parts. First, high-level policy (business logic) should not be coupled to low-level mechanisms (file I/O, database, network). Second, abstractions should not depend on details — the interface `UserRepository` should not have SQL in its method signatures. When you follow DIP, you can swap entire infrastructure layers (database, email provider, payment gateway) without touching business logic.

### The Problem

```java
// BAD: AuthenticationService directly instantiates its dependencies.
// Switching from LDAP to OAuth, or from MySQL to Postgres,
// requires modifying AuthenticationService — a business logic class.

public class AuthenticationService {

    // Direct instantiation creates hard coupling to specific implementations
    private final MysqlUserStore userStore = new MysqlUserStore(
        "jdbc:mysql://prod-db:3306/users", "app_user", System.getenv("DB_PASSWORD")
    );

    private final LdapPasswordValidator ldapValidator = new LdapPasswordValidator(
        "ldap://corp-ldap:389", "cn=service-account,dc=corp,dc=com"
    );

    private final JavaMailSender mailSender = new JavaMailSenderImpl();

    public AuthResult authenticate(String username, String rawPassword) {
        // Business logic is entangled with infrastructure details
        UserRecord user = userStore.fetchByUsername(username);  // MysqlUserStore-specific type

        if (user == null) {
            return AuthResult.failure("User not found");
        }

        // Directly coupled to LDAP validation logic
        boolean valid = ldapValidator.validateAgainstDirectory(username, rawPassword);

        if (!valid) {
            userStore.incrementFailedAttempts(username);
            if (userStore.getFailedAttempts(username) >= 5) {
                userStore.lockAccount(username);
                // Directly building and sending email here
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(user.getEmail());
                msg.setSubject("Account Locked");
                msg.setText("Your account has been locked due to too many failed attempts.");
                mailSender.send(msg);
            }
            return AuthResult.failure("Invalid credentials");
        }

        userStore.resetFailedAttempts(username);
        return AuthResult.success(buildSession(user));
    }
}
```

### The Fix

```java
// GOOD: AuthenticationService depends on abstractions defined in the domain layer.
// Infrastructure implementations are injected from outside.
// The business logic never changes when the infrastructure changes.

// Abstractions defined in the domain/application layer (high-level module)
public interface UserCredentialRepository {
    Optional<UserCredential> findByUsername(String username);
    void recordFailedAttempt(String username);
    void resetFailedAttempts(String username);
    void lockAccount(String username);
    int getFailedAttemptCount(String username);
}

public interface PasswordVerifier {
    boolean verify(String username, String rawPassword, PasswordHash storedHash);
}

public interface AccountLockNotifier {
    void notifyAccountLocked(UserCredential user);
}

// The business logic class depends on interfaces — not implementations
public class AuthenticationService {
    private final UserCredentialRepository credentialRepo;
    private final PasswordVerifier passwordVerifier;
    private final AccountLockNotifier lockNotifier;
    private final int maxFailedAttempts;

    // Dependencies injected — not created internally
    public AuthenticationService(UserCredentialRepository credentialRepo,
                                  PasswordVerifier passwordVerifier,
                                  AccountLockNotifier lockNotifier,
                                  int maxFailedAttempts) {
        this.credentialRepo = credentialRepo;
        this.passwordVerifier = passwordVerifier;
        this.lockNotifier = lockNotifier;
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public AuthResult authenticate(String username, String rawPassword) {
        Optional<UserCredential> credentialOpt = credentialRepo.findByUsername(username);

        if (credentialOpt.isEmpty()) {
            return AuthResult.failure(AuthFailureReason.USER_NOT_FOUND);
        }

        UserCredential credential = credentialOpt.get();

        if (credential.isLocked()) {
            return AuthResult.failure(AuthFailureReason.ACCOUNT_LOCKED);
        }

        boolean valid = passwordVerifier.verify(username, rawPassword, credential.getPasswordHash());

        if (!valid) {
            credentialRepo.recordFailedAttempt(username);
            int failCount = credentialRepo.getFailedAttemptCount(username);
            if (failCount >= maxFailedAttempts) {
                credentialRepo.lockAccount(username);
                lockNotifier.notifyAccountLocked(credential);
            }
            return AuthResult.failure(AuthFailureReason.INVALID_CREDENTIALS);
        }

        credentialRepo.resetFailedAttempts(username);
        return AuthResult.success(Session.create(credential));
    }
}

// Low-level implementations in the infrastructure layer
// These depend on the abstractions defined above — never the other way around

public class JpaUserCredentialRepository implements UserCredentialRepository {
    private final UserCredentialJpaRepository jpaRepo;  // Spring Data JPA

    @Override
    public Optional<UserCredential> findByUsername(String username) {
        return jpaRepo.findByUsername(username).map(UserCredentialEntity::toDomain);
    }

    @Override
    public void recordFailedAttempt(String username) {
        jpaRepo.incrementFailedAttempts(username);
    }
    // ... other methods
}

public class BcryptPasswordVerifier implements PasswordVerifier {
    private final BCryptPasswordEncoder encoder;

    @Override
    public boolean verify(String username, String rawPassword, PasswordHash storedHash) {
        return encoder.matches(rawPassword, storedHash.getValue());
    }
}

// Swap to OAuth verification without touching AuthenticationService
public class OAuthPasswordVerifier implements PasswordVerifier {
    private final OAuthClient oauthClient;

    @Override
    public boolean verify(String username, String rawPassword, PasswordHash storedHash) {
        return oauthClient.validateCredentials(username, rawPassword).isSuccess();
    }
}

public class EmailAccountLockNotifier implements AccountLockNotifier {
    private final EmailNotifier emailNotifier;

    @Override
    public void notifyAccountLocked(UserCredential user) {
        emailNotifier.sendEmail(
            user.getEmail(),
            "Your account has been locked",
            "Too many failed login attempts. Contact support to unlock."
        );
    }
}
```

**Real-world analogy**: A lamp depends on the electrical socket abstraction (the 110V interface), not on a specific power plant. You can change how electricity is generated (coal, solar, nuclear) without rewiring the lamp.

**How it shows up in design patterns**: DIP is the foundation of almost every structural pattern. Factory and Abstract Factory manage which concrete implementation gets created while keeping callers abstracted. Strategy, Observer, and Command all depend on the principle that callers reference interfaces, not concrete types.

**Interview question**: "How do you test business logic that depends on a database?" — Model answer: "If the business logic class directly instantiates its database connection, it's untestable in isolation — this is a DIP violation. The fix is to express the dependency as an interface (`UserRepository`), inject it into the class via constructor injection, and in tests provide an in-memory implementation or a mock. This is why DIP and testability are directly correlated. A class that violates DIP is almost always hard to unit test without standing up infrastructure."

---

## SOLID in Practice — How They Interact

The principles reinforce each other. Following one often requires following another. A codebase that takes SOLID seriously doesn't consciously apply principles one at a time — they become a unified design sensibility.

### Mini Example: A Payment Processor that Violates Multiple Principles

Start here — this is what real legacy code looks like:

```java
// Violations: SRP (processes, persists, notifies, logs all in one class)
//              OCP (adding a payment provider requires modifying this class)
//              DIP (directly instantiates Stripe and MySQL)
//              ISP (implied — if this were an interface it would be one fat method)

public class PaymentProcessor {

    public void processPayment(String orderId, double amount, String currency, String paymentMethod) {

        System.out.println("Processing payment for order: " + orderId);

        if (paymentMethod.equals("STRIPE")) {
            Stripe.apiKey = "sk_live_...";
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long)(amount * 100))
                .setCurrency(currency)
                .build();
            try {
                PaymentIntent intent = PaymentIntent.create(params);
                System.out.println("Stripe payment intent created: " + intent.getId());
            } catch (StripeException e) {
                throw new RuntimeException("Stripe payment failed", e);
            }
        } else if (paymentMethod.equals("PAYPAL")) {
            PayPalEnvironment env = new PayPalEnvironment.Live("client-id", "client-secret");
            PayPalHttpClient client = new PayPalHttpClient(env);
            // ... PayPal-specific construction
        }
        // Adding BRAINTREE requires modifying this class — OCP violation

        // Directly writes to database — DIP violation, SRP violation
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://prod:3306/payments", "root", "password")) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO payment_records (order_id, amount, currency, method, status) VALUES (?,?,?,?,?)"
            );
            stmt.setString(1, orderId);
            stmt.setDouble(2, amount);
            stmt.setString(3, currency);
            stmt.setString(4, paymentMethod);
            stmt.setString(5, "COMPLETED");
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payment record", e);
        }

        // Directly sends email — SRP violation
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.company.com");
        Session session = Session.getInstance(props);
        try {
            MimeMessage message = new MimeMessage(session);
            message.setRecipient(Message.RecipientType.TO, new InternetAddress("finance@company.com"));
            message.setSubject("Payment Processed: " + orderId);
            Transport.send(message);
        } catch (MessagingException e) {
            // Silent failure
        }
    }
}
```

### Step 1 — Apply SRP: Separate the concerns

Extract: `PaymentGateway` (process payment), `PaymentRecordRepository` (persist), `PaymentEventNotifier` (notify). The `PaymentProcessor` becomes an orchestrator with no logic of its own.

### Step 2 — Apply OCP: Abstract the gateway

```java
public interface PaymentGateway {
    PaymentResult charge(PaymentRequest request);
    String getProviderName();
}
```

New payment providers (Braintree, Adyen, Square) implement `PaymentGateway`. `PaymentProcessor` never changes.

### Step 3 — Apply LSP: Ensure substitutability

Every `PaymentGateway` implementation must fulfill the same contract: if `charge()` returns a `PaymentResult`, it must always return a `PaymentResult` — not throw an undeclared provider-specific exception. Wrap provider-specific exceptions into a declared `PaymentException` hierarchy.

### Step 4 — Apply ISP: Split the gateway interface if needed

If some code only needs to verify a payment without charging (fraud detection), and other code only needs to initiate refunds, split:

```java
public interface PaymentCharger { PaymentResult charge(PaymentRequest request); }
public interface PaymentRefunder { RefundResult refund(String transactionId, Money amount); }
public interface PaymentStatusChecker { PaymentStatus check(String transactionId); }
```

Fraud detection depends on `PaymentStatusChecker`. Order service depends on `PaymentCharger`. Neither knows about `refund`.

### Step 5 — Apply DIP: Inject, don't instantiate

```java
// Final form — testable, extensible, correct
public class PaymentProcessor {
    private final PaymentGateway gateway;
    private final PaymentRecordRepository repository;
    private final PaymentEventNotifier notifier;

    public PaymentProcessor(PaymentGateway gateway,
                             PaymentRecordRepository repository,
                             PaymentEventNotifier notifier) {
        this.gateway = gateway;
        this.repository = repository;
        this.notifier = notifier;
    }

    public PaymentResult processPayment(PaymentRequest request) {
        PaymentResult result = gateway.charge(request);

        if (result.isSuccessful()) {
            repository.record(PaymentRecord.from(request, result));
            notifier.paymentCompleted(request.getOrderId(), result);
        } else {
            notifier.paymentFailed(request.getOrderId(), result.getFailureReason());
        }

        return result;
    }
}
```

This is what the code looks like after taking all five principles seriously. It is:
- Testable in complete isolation (inject mocks)
- Extensible to new gateways without any modification
- Correct under substitution (any `PaymentGateway` works)
- Minimal in each dependency's footprint
- Decoupled from infrastructure at the business logic layer

---

## DRY — Don't Repeat Yourself

Every piece of knowledge should have a single, authoritative representation in the system. DRY is not about eliminating duplicate lines of code — it is about eliminating duplicate knowledge. Two methods that happen to share a `for` loop are not necessarily a DRY violation. Two places that encode the rule "a premium user gets a 15% discount" absolutely are.

The violation that costs the most in production is scattered business rules. When "order is eligible for free shipping if total > $50" appears in the checkout service, the cart summary service, the order confirmation email, and the shipping cost API, changing the threshold to $75 requires four code changes and four deployments. The fix is one authoritative `ShippingEligibilityPolicy` that all four places call.

```java
// BAD: Same eligibility rule in three places
public class CartService {
    public boolean qualifiesForFreeShipping(Cart cart) {
        return cart.getTotal().compareTo(new BigDecimal("50.00")) >= 0;  // hardcoded
    }
}

public class CheckoutService {
    public ShippingOptions getShippingOptions(Order order) {
        boolean freeShipping = order.getSubtotal().compareTo(new BigDecimal("50.00")) >= 0;
        // ...
    }
}

// GOOD: One authoritative policy
public class FreeShippingPolicy {
    private static final BigDecimal THRESHOLD = new BigDecimal("50.00");

    public boolean isEligible(Money orderSubtotal) {
        return orderSubtotal.getAmount().compareTo(THRESHOLD) >= 0;
    }
}
// CartService, CheckoutService, and ConfirmationService all call FreeShippingPolicy
```

Caution: over-applying DRY creates accidental coupling. Two pieces of code that look the same today but change for different reasons should remain separate. The principle applies to knowledge, not syntax.

---

## KISS — Keep It Simple, Stupid

The simplest design that correctly solves the stated problem is the right design. Complexity is a liability, not an asset. Every layer of abstraction, every design pattern, every interface has a carrying cost: cognitive overhead, more files, more places to look when something breaks.

Senior engineers are recognized not by the complexity they introduce, but by the complexity they avoid. The temptation in interviews is to show off patterns. The discipline is to apply them only when the problem they solve is actually present.

```java
// BAD: AbstractStrategyFactoryBuilderVisitor for a one-time report
// (real smell from real codebases attempting to "be SOLID")
public interface ReportGenerationStrategyFactory {
    ReportGenerationStrategy createStrategy(ReportType type);
}
public class DefaultReportGenerationStrategyFactory implements ReportGenerationStrategyFactory {
    @Override
    public ReportGenerationStrategy createStrategy(ReportType type) {
        if (type == ReportType.MONTHLY_SUMMARY) return new MonthlySummaryStrategy();
        return new DefaultStrategy();
    }
}
// This exists to generate one report. There is one report type that ever gets used.

// GOOD: One method, one responsibility, no indirection
public class MonthlyRevenueReport {
    public ReportData generate(YearMonth month, List<Transaction> transactions) {
        BigDecimal total = transactions.stream()
            .filter(t -> YearMonth.from(t.getDate()).equals(month))
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReportData(month, total, transactions.size());
    }
}
```

The right time to introduce a Strategy pattern is when you have two strategies and expect more. Not when you have one strategy and are optimistically preparing for a second.

---

## YAGNI — You Aren't Gonna Need It

Do not build infrastructure for requirements that do not exist yet. Every speculative feature added today is code that must be maintained, tested, and reasoned about — even if it is never used. In a long-lived codebase, the code no one uses is often the most dangerous, because it drifts out of sync with reality while remaining in the dependency graph.

```java
// BAD: Payment service built for "future" multi-currency support
// that was never in the requirements and never shipped

public class PaymentService {
    private final Map<String, ExchangeRateProvider> exchangeRateProviders;
    private final CurrencyConverter converter;
    private final MultiCurrencyLedger ledger;  // not used
    private final CurrencyRiskAnalyzer riskAnalyzer;  // not used

    // Constructor requires 4 dependencies, 2 of which do nothing
    public PaymentService(Map<String, ExchangeRateProvider> providers,
                          CurrencyConverter converter,
                          MultiCurrencyLedger ledger,
                          CurrencyRiskAnalyzer riskAnalyzer) { ... }

    public PaymentResult charge(PaymentRequest request) {
        // Converts to USD even though all payments are already in USD
        Money normalized = converter.toBaseCurrency(request.getAmount());
        return chargeInBaseCurrency(normalized, request);
    }
}

// GOOD: Charge in the currency of the request.
// Add multi-currency support when there are actual multi-currency customers.
public class PaymentService {
    private final PaymentGateway gateway;

    public PaymentService(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public PaymentResult charge(PaymentRequest request) {
        return gateway.charge(request);
    }
}
```

YAGNI is hardest to follow under pressure to "future-proof" the design. The counterintuitive truth is that clean, focused code following SRP and OCP is easier to extend when the real requirement arrives than speculative infrastructure written against imagined requirements. Build for today; structure for tomorrow.
