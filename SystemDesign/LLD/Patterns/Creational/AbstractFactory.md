# Abstract Factory Pattern

## Intent

Provide an interface for creating families of related or dependent objects without
specifying their concrete classes.

---

## Problem: What Breaks When You Need a Family of Related Objects

Suppose you are building a UI toolkit that must run on both web and mobile. Web
components and mobile components are visually and behaviorally different, but they
fill the same roles: buttons, text fields, dropdowns, etc.

Without Abstract Factory:

```java
// Client code must decide the platform AND know every concrete class
public class LoginScreen {

    private Button submitButton;
    private TextField usernameField;

    public LoginScreen(String platform) {
        if (platform.equals("WEB")) {
            this.submitButton  = new WebButton("Submit");      // hard dependency
            this.usernameField = new WebTextField("Username");  // hard dependency
        } else if (platform.equals("MOBILE")) {
            this.submitButton  = new MobileButton("Submit");    // hard dependency
            this.usernameField = new MobileTextField("Username");// hard dependency
        }
        // Adding DESKTOP platform means editing this class and every other screen class
    }
}
```

**Concrete problems:**

1. **Mixed families.** Nothing prevents a caller from accidentally using a `WebButton`
   with a `MobileTextField`. The pairing constraint — all components in one screen
   must belong to the same platform family — is not enforced by the type system.

2. **OCP violation.** Adding a `DesktopUIFactory` requires modifying `LoginScreen`,
   `RegistrationScreen`, `DashboardScreen`, and every other screen that constructs
   components directly.

3. **Scattered platform-selection logic.** Every screen has its own `if (platform...)`
   block. Centralizing the platform decision is impossible without a factory.

4. **Tight coupling to concrete classes.** `LoginScreen` depends on six concrete
   implementation classes. Mocking in tests requires real constructors or extensive
   setup.

---

## Structure

```
+----------------------------------+
|      <<interface>>               |
|       UIFactory                  |
|   (AbstractFactory)              |
+----------------------------------+
| + createButton(label): Button    |
| + createTextField(label):        |
|        TextField                 |
+----------------------------------+
          ^                 ^
          |                 |
+-----------------+  +------------------+
|  WebUIFactory   |  | MobileUIFactory  |
| (ConcreteFactory|  | (ConcreteFactory |
|      1)         |  |       2)         |
+-----------------+  +------------------+
| + createButton  |  | + createButton   |
|   -> WebButton  |  |   -> MobileButton|
| + createTextField|  | + createTextField|
|   -> WebTextField|  |  ->MobileTextFld |
+-----------------+  +------------------+
     |      |              |       |
     |      |              |       |
     v      v              v       v
+--------+ +----------+ +----------+ +------------------+
|WebButton| |WebTextFld| |MobileBttn| | MobileTextField  |
+--------+ +----------+ +----------+ +------------------+
  (ConcreteProduct A1) (ConcreteProduct A2) ...

AbstractProduct A:            AbstractProduct B:
+------------------+          +------------------+
|   <<interface>>  |          |   <<interface>>  |
|     Button       |          |    TextField     |
+------------------+          +------------------+
| + render(): void |          | + render(): void |
| + onClick(h:     |          | + getValue():    |
|  Runnable): void |          |   String         |
+------------------+          | + setPlaceholder |
                               |  (text): void   |
                               +------------------+

Client:
+-------------------+
|    LoginScreen    |
+-------------------+
| - factory:        |
|   UIFactory       |
| - submitButton:   |
|   Button          |
| - usernameField:  |
|   TextField       |
+-------------------+
| + LoginScreen(    |
|   UIFactory f)    |
| + render(): void  |
+-------------------+
      |
      | depends only on UIFactory, Button, TextField interfaces
      | never on Web* or Mobile* concrete classes
```

---

## Implementation

### Step 1: Abstract Product interfaces

```java
public interface Button {
    void render();
    void onClick(Runnable handler);
    String getLabel();
}

public interface TextField {
    void render();
    String getValue();
    void setValue(String value);
    void setPlaceholder(String placeholder);
}
```

### Step 2: Concrete Products — Web family

```java
public class WebButton implements Button {

    private final String label;
    private Runnable clickHandler;

    public WebButton(String label) {
        this.label = label;
    }

    @Override
    public void render() {
        System.out.printf("<button class=\"web-btn\">%s</button>%n", label);
    }

    @Override
    public void onClick(Runnable handler) {
        this.clickHandler = handler;
    }

    @Override
    public String getLabel() { return label; }
}

public class WebTextField implements TextField {

    private final String name;
    private String value       = "";
    private String placeholder = "";

    public WebTextField(String name) {
        this.name = name;
    }

    @Override
    public void render() {
        System.out.printf("<input type=\"text\" name=\"%s\" placeholder=\"%s\" value=\"%s\"/>%n",
                name, placeholder, value);
    }

    @Override
    public String getValue()               { return value; }
    @Override
    public void setValue(String value)     { this.value = value; }
    @Override
    public void setPlaceholder(String p)   { this.placeholder = p; }
}
```

### Step 3: Concrete Products — Mobile family

```java
public class MobileButton implements Button {

    private final String label;
    private Runnable tapHandler;

    public MobileButton(String label) {
        this.label = label;
    }

    @Override
    public void render() {
        // Mobile renders a native touch-target component, not HTML
        System.out.printf("[NativeButton: label='%s' style=touchTarget ripple=true]%n", label);
    }

    @Override
    public void onClick(Runnable handler) {
        this.tapHandler = handler; // "click" maps to tap gesture on mobile
    }

    @Override
    public String getLabel() { return label; }
}

public class MobileTextField implements TextField {

    private final String fieldName;
    private String value       = "";
    private String placeholder = "";

    public MobileTextField(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public void render() {
        System.out.printf("[NativeTextField: name='%s' hint='%s' value='%s' keyboardType=default]%n",
                fieldName, placeholder, value);
    }

    @Override
    public String getValue()             { return value; }
    @Override
    public void setValue(String value)   { this.value = value; }
    @Override
    public void setPlaceholder(String p) { this.placeholder = p; }
}
```

### Step 4: Abstract Factory interface

```java
// The Abstract Factory declares creation methods for each product in the family.
// It does NOT return concrete types — only interfaces.
public interface UIFactory {
    Button    createButton(String label);
    TextField createTextField(String name);
}
```

### Step 5: Concrete Factories — one per platform family

```java
// WebUIFactory produces exclusively Web-family components.
// It is impossible to accidentally mix a WebButton with a MobileTextField
// when the factory is the only creation point.
public class WebUIFactory implements UIFactory {

    @Override
    public Button createButton(String label) {
        return new WebButton(label);
    }

    @Override
    public TextField createTextField(String name) {
        return new WebTextField(name);
    }
}

// MobileUIFactory produces exclusively Mobile-family components.
public class MobileUIFactory implements UIFactory {

    @Override
    public Button createButton(String label) {
        return new MobileButton(label);
    }

    @Override
    public TextField createTextField(String name) {
        return new MobileTextField(name);
    }
}
```

### Step 6: Client code — fully decoupled from concrete classes

```java
// LoginScreen depends ONLY on UIFactory, Button, TextField.
// It has zero imports of WebButton, MobileButton, WebTextField, MobileTextField.
// The same LoginScreen class renders correctly on both platforms.
public class LoginScreen {

    private final Button    submitButton;
    private final Button    cancelButton;
    private final TextField usernameField;
    private final TextField passwordField;

    // Factory is injected — LoginScreen never selects the platform
    public LoginScreen(UIFactory factory) {
        this.submitButton  = factory.createButton("Sign In");
        this.cancelButton  = factory.createButton("Cancel");
        this.usernameField = factory.createTextField("username");
        this.passwordField = factory.createTextField("password");

        usernameField.setPlaceholder("Enter your email");
        passwordField.setPlaceholder("Enter your password");
    }

    public void render() {
        System.out.println("--- Login Screen ---");
        usernameField.render();
        passwordField.render();
        submitButton.render();
        cancelButton.render();
    }

    public void attachHandlers(Runnable onSubmit, Runnable onCancel) {
        submitButton.onClick(onSubmit);
        cancelButton.onClick(onCancel);
    }
}

// The same pattern applies to every screen — none of them know about platforms
public class RegistrationScreen {

    private final TextField firstNameField;
    private final TextField lastNameField;
    private final TextField emailField;
    private final Button    registerButton;

    public RegistrationScreen(UIFactory factory) {
        this.firstNameField = factory.createTextField("firstName");
        this.lastNameField  = factory.createTextField("lastName");
        this.emailField     = factory.createTextField("email");
        this.registerButton = factory.createButton("Create Account");
    }

    public void render() {
        System.out.println("--- Registration Screen ---");
        firstNameField.render();
        lastNameField.render();
        emailField.render();
        registerButton.render();
    }
}
```

### Step 7: Application entry point — the only place that knows the platform

```java
public class Application {

    public static void main(String[] args) {
        // The platform decision is made ONCE — at the composition root.
        // All downstream code (LoginScreen, RegistrationScreen) is oblivious.
        String platform = System.getProperty("ui.platform", "WEB");

        UIFactory factory = resolveFactory(platform);

        LoginScreen       login    = new LoginScreen(factory);
        RegistrationScreen register = new RegistrationScreen(factory);

        login.render();
        System.out.println();
        register.render();
    }

    private static UIFactory resolveFactory(String platform) {
        return switch (platform.toUpperCase()) {
            case "WEB"    -> new WebUIFactory();
            case "MOBILE" -> new MobileUIFactory();
            // Adding DESKTOP: create DesktopButton, DesktopTextField, DesktopUIFactory.
            // This method gets one new case. LoginScreen and RegistrationScreen: no changes.
            default -> throw new IllegalArgumentException("Unknown platform: " + platform);
        };
    }
}
```

---

## When to Use

- You need to ensure that components from different families are never mixed —
  e.g., web components with mobile components, or Oracle SQL syntax with PostgreSQL
  syntax. The factory enforces the constraint at the type level.
- The system must support multiple product families (platforms, themes, environments)
  and switching between them must happen at a single configuration point, not scattered
  throughout the codebase.
- You want to program against product interfaces, never concrete classes, across all
  client code. The Abstract Factory is the boundary between the "what" (interfaces)
  and the "how" (implementations).
- New product families will be added over time. Adding a new `ConcreteFactory`
  (e.g., `DesktopUIFactory`) requires no changes to any existing client screen.

## When NOT to Use

- **You have only one product family.** If there is no variation in families (only
  one platform), Abstract Factory is pure overhead. Use a simple Factory Method or
  a constructor.
- **Products do not need to be consistent across families.** If mixing a WebButton
  with a MobileTextField is acceptable, you do not need the family constraint that
  Abstract Factory enforces.
- **Adding new product types is frequent.** Abstract Factory is easy to extend by
  adding new families (new `ConcreteFactory`), but adding a new product *type* (e.g.,
  a `Dropdown` alongside `Button` and `TextField`) requires modifying the
  `UIFactory` interface and all existing `ConcreteFactory` classes. This is the
  pattern's primary rigidity.

---

## Variants

### Factory Registry with Abstract Factory
Instead of a hard-coded `switch` in the application entry point, register factories
by key. Useful when factories are provided by plugins or loaded from configuration:

```java
public final class UIFactoryRegistry {

    private static final Map<String, Supplier<UIFactory>> registry = new HashMap<>();

    public static void register(String platform, Supplier<UIFactory> supplier) {
        registry.put(platform.toUpperCase(), supplier);
    }

    public static UIFactory get(String platform) {
        Supplier<UIFactory> s = registry.get(platform.toUpperCase());
        if (s == null) throw new IllegalArgumentException("No factory for: " + platform);
        return s.get();
    }

    static {
        register("WEB",    WebUIFactory::new);
        register("MOBILE", MobileUIFactory::new);
    }
}
// Usage: UIFactory factory = UIFactoryRegistry.get(platform);
```

### Abstract Factory with Reflection
For highly dynamic plugin systems, `ConcreteFactory` classes can be discovered and
instantiated via reflection without compile-time imports. Typically used in OSGi
environments or SPI (Service Provider Interface) architectures where factory
implementations are loaded from the classpath at runtime via `ServiceLoader`.

```java
// META-INF/services/com.example.UIFactory contains:
//   com.example.web.WebUIFactory
//   com.example.mobile.MobileUIFactory

ServiceLoader<UIFactory> loader = ServiceLoader.load(UIFactory.class);
Map<String, UIFactory> factories = new HashMap<>();
for (UIFactory f : loader) {
    // Convention: each factory declares its platform via a marker annotation or method
    factories.put(f.getPlatformName(), f);
}
```

---

## Real-World Examples

- **`javax.xml.parsers.DocumentBuilderFactory`** — the canonical Java example.
  `DocumentBuilderFactory.newInstance()` returns a platform- or classpath-appropriate
  factory. `factory.newDocumentBuilder()` returns a `DocumentBuilder` without the
  caller knowing whether it is Xerces, OpenJDK's built-in parser, or another
  implementation. The factory + builder pair together form an Abstract Factory.

- **`javax.xml.transform.TransformerFactory`** and
  **`javax.xml.validation.SchemaFactory`** — same pattern across the JAXP family:
  a factory selects the implementation family, and the factory's methods produce
  the consistent family of products.

- **Spring's `FactoryBean<T>` implementations** — Spring uses Abstract Factory
  internally for component creation. Infrastructure components like
  `LocalContainerEntityManagerFactoryBean` produce JPA `EntityManagerFactory`
  instances, abstracting over Hibernate, EclipseLink, and other JPA providers.
  Switching providers is a one-line configuration change.

- **JDBC `Connection` / `Statement` / `ResultSet`** — `DataSource.getConnection()`
  returns a connection whose `createStatement()`, `prepareStatement()`, and
  `prepareCall()` all produce statement objects from the same vendor family. A
  PostgreSQL `Connection` always produces PostgreSQL `Statement` objects.

- **SLF4J + Logback / Log4j2** — SLF4J's `LoggerFactory.getLogger()` is an Abstract
  Factory that produces `Logger` objects from whichever binding is on the classpath.
  Application code imports only `org.slf4j.*`; the concrete logging implementation
  is a deployment-time decision.

---

## Interview Questions

**Q1: How does Abstract Factory differ from Factory Method?**

Factory Method solves a single-product problem: one abstract method, overridden by
subclasses, each producing one type of product. It is about a single "virtual
constructor."

Abstract Factory solves a family-of-products problem: one factory interface declares
multiple creation methods, each producing a different product *from the same family*.
The constraint is that all products from one factory are compatible with each other.

Rule of thumb: if you have one product, use Factory Method. If you have multiple
products that must be used together and must come from the same family (web vs. mobile,
Oracle vs. PostgreSQL), use Abstract Factory.

**Q2: You need to add a new product type (e.g., Dropdown) to an existing Abstract
Factory. What is the impact?**

This is the pattern's primary cost. You must: (1) add `createDropdown()` to the
`UIFactory` interface, (2) implement `createDropdown()` in every existing `ConcreteFactory`
(`WebUIFactory`, `MobileUIFactory`). Every concrete factory is a modification site.
This is why Abstract Factory is easy to extend along the "add a new family" axis
(new `ConcreteFactory`) but expensive along the "add a new product type" axis.
Design the product family interface upfront with all the product types you anticipate.

**Q3: How would you test a class like LoginScreen that uses a UIFactory?**

Inject a test-double factory:

```java
// In a test — no web or mobile code involved
UIFactory mockFactory = new UIFactory() {
    @Override
    public Button createButton(String label) {
        return new SpyButton(label); // records calls for assertion
    }
    @Override
    public TextField createTextField(String name) {
        return new SpyTextField(name); // records calls for assertion
    }
};

LoginScreen screen = new LoginScreen(mockFactory);
screen.render();
// Assert that render() called usernameField.render(), passwordField.render(), etc.
```

Because `LoginScreen` depends only on the `UIFactory` interface, any implementation
can be substituted. This is the main testability benefit of the pattern.

**Q4: When does a registry-based Abstract Factory become preferable to a hard-coded
factory hierarchy?**

When the set of families is not known at compile time — e.g., third-party plugin
authors provide new UI toolkits that must integrate without recompiling the core
application. The registry approach combined with Java's `ServiceLoader` SPI mechanism
lets plugins register `UIFactory` implementations on the classpath. The core
application discovers them at startup with no compile-time coupling to plugin code.
