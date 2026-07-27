# Decorator Pattern

## Intent

Attach additional responsibilities to an object dynamically; decorators provide a flexible alternative to subclassing for extending functionality.

---

## Problem: Subclass Explosion

Suppose you have a `Notifier` class. You want to add email, SMS, Slack, and PagerDuty notifications — and any combination of them. With inheritance you need:

```
Notifier
  EmailNotifier
  SmsNotifier
  SlackNotifier
  EmailSmsNotifier
  EmailSlackNotifier
  SmsSlackNotifier
  EmailSmsSlackNotifier
  EmailSmsSlackPagerNotifier
  ... (2^N subclasses for N features)
```

Decorator solves this by wrapping objects at runtime rather than at compile time.

---

## Structure

```
         <<interface>>
          Component
         +operation()
              ^
              |
    +---------+---------+
    |                   |
ConcreteComponent    Decorator
+operation()        -component: Component
                    +operation()
                         ^
              +----------+----------+
              |                     |
   ConcreteDecoratorA     ConcreteDecoratorB
   -addedState             -addedBehavior
   +operation()            +operation()
```

- **Component** — interface or abstract class defining the contract
- **ConcreteComponent** — base object being decorated
- **Decorator** — abstract wrapper; holds a reference to a `Component`
- **ConcreteDecoratorA/B** — add specific behaviors before/after delegating to wrapped component

---

## Implementation

### Example 1: Coffee Beverage (Classic Intro)

```java
// Component
public interface Coffee {
    String getDescription();
    double getCost();
}

// ConcreteComponent
public class SimpleCoffee implements Coffee {
    @Override public String getDescription() { return "Simple coffee"; }
    @Override public double getCost()        { return 1.00; }
}

// Decorator base
public abstract class CoffeeDecorator implements Coffee {
    protected final Coffee coffee;
    public CoffeeDecorator(Coffee coffee) { this.coffee = coffee; }
    @Override public String getDescription() { return coffee.getDescription(); }
    @Override public double getCost()        { return coffee.getCost(); }
}

// ConcreteDecoratorA
public class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) { super(coffee); }
    @Override public String getDescription() { return coffee.getDescription() + ", milk"; }
    @Override public double getCost()        { return coffee.getCost() + 0.25; }
}

// ConcreteDecoratorB
public class VanillaDecorator extends CoffeeDecorator {
    public VanillaDecorator(Coffee coffee) { super(coffee); }
    @Override public String getDescription() { return coffee.getDescription() + ", vanilla"; }
    @Override public double getCost()        { return coffee.getCost() + 0.50; }
}

// Usage
Coffee order = new VanillaDecorator(new MilkDecorator(new SimpleCoffee()));
System.out.println(order.getDescription()); // Simple coffee, milk, vanilla
System.out.println(order.getCost());        // 1.75
```

---

### Example 2: HTTP Request Pipeline

This is the realistic production scenario. An `HttpClient` interface is decorated with authentication, structured logging, and automatic retry logic. Each concern is isolated in its own decorator.

```java
import java.util.Map;
import java.util.HashMap;

// ─── Domain objects ───────────────────────────────────────────────

public class HttpRequest {
    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;

    public HttpRequest(String method, String url, String body) {
        this.method  = method;
        this.url     = url;
        this.headers = new HashMap<>();
        this.body    = body;
    }

    public String getMethod()  { return method; }
    public String getUrl()     { return url; }
    public String getBody()    { return body; }
    public Map<String, String> getHeaders() { return headers; }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    @Override
    public String toString() {
        return method + " " + url + " headers=" + headers;
    }
}

public class HttpResponse {
    private final int    statusCode;
    private final String body;

    public HttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body       = body;
    }

    public int    getStatusCode() { return statusCode; }
    public String getBody()       { return body; }
    public boolean isSuccess()    { return statusCode >= 200 && statusCode < 300; }

    @Override
    public String toString() {
        return "HttpResponse{status=" + statusCode + ", body='" + body + "'}";
    }
}

// ─── Component: the interface all decorators and real client share ─

public interface HttpClient {
    HttpResponse execute(HttpRequest request);
}

// ─── ConcreteComponent: real network call ────────────────────────

public class RealHttpClient implements HttpClient {
    @Override
    public HttpResponse execute(HttpRequest request) {
        // In production this would open a socket / use java.net.http.HttpClient
        System.out.println("[RealHttpClient] Sending: " + request);
        // Simulate a 200 OK for demonstration
        return new HttpResponse(200, "{\"status\":\"ok\"}");
    }
}

// ─── Abstract Decorator ──────────────────────────────────────────

public abstract class HttpClientDecorator implements HttpClient {
    protected final HttpClient delegate;

    public HttpClientDecorator(HttpClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
        return delegate.execute(request);
    }
}

// ─── ConcreteDecoratorA: Authentication ──────────────────────────

public class AuthDecorator extends HttpClientDecorator {
    private final String token;

    public AuthDecorator(HttpClient delegate, String token) {
        super(delegate);
        this.token = token;
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
        request.addHeader("Authorization", "Bearer " + token);
        System.out.println("[AuthDecorator] Injected Authorization header");
        return delegate.execute(request);
    }
}

// ─── ConcreteDecoratorB: Structured Logging ──────────────────────

public class LoggingDecorator extends HttpClientDecorator {
    public LoggingDecorator(HttpClient delegate) {
        super(delegate);
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
        long start = System.currentTimeMillis();
        System.out.printf("[LoggingDecorator] --> %s %s%n",
            request.getMethod(), request.getUrl());

        HttpResponse response = delegate.execute(request);

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("[LoggingDecorator] <-- %d (%d ms)%n",
            response.getStatusCode(), elapsed);
        return response;
    }
}

// ─── ConcreteDecoratorC: Retry ───────────────────────────────────

public class RetryDecorator extends HttpClientDecorator {
    private final int maxAttempts;

    public RetryDecorator(HttpClient delegate, int maxAttempts) {
        super(delegate);
        this.maxAttempts = maxAttempts;
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
        int attempt = 0;
        while (true) {
            attempt++;
            HttpResponse response = delegate.execute(request);
            if (response.isSuccess() || attempt >= maxAttempts) {
                return response;
            }
            System.out.printf("[RetryDecorator] Attempt %d failed (status %d), retrying...%n",
                attempt, response.getStatusCode());
        }
    }
}

// ─── Client: composing the pipeline ──────────────────────────────

public class PipelineDemo {
    public static void main(String[] args) {
        // Build the pipeline from inside out:
        // RetryDecorator -> LoggingDecorator -> AuthDecorator -> RealHttpClient
        HttpClient client = new RetryDecorator(
                                new LoggingDecorator(
                                    new AuthDecorator(
                                        new RealHttpClient(),
                                        "eyJhbGciOiJSUzI1NiJ9.abc"
                                    )
                                ),
                                3
                            );

        HttpRequest request = new HttpRequest("GET",
            "https://api.example.com/orders/42", null);

        HttpResponse response = client.execute(request);
        System.out.println("Final response: " + response);
    }
}
```

**Call flow:**
```
RetryDecorator.execute()
  └─> LoggingDecorator.execute()   (records start time, logs)
        └─> AuthDecorator.execute()  (injects Authorization header)
              └─> RealHttpClient.execute()  (actual network call)
```

Each decorator is independently testable. Swap `RetryDecorator` for an `ExponentialBackoffDecorator` without touching anything else.

---

## When to Use

- You need to add behaviors to individual objects without affecting other objects of the same class.
- Extension by subclassing is impractical because of combinatorial growth.
- Responsibilities need to be added and removed at runtime.
- You have cross-cutting concerns (logging, auth, metrics, caching) that should remain separate from core logic.

## When NOT to Use

- When the component interface is large — each decorator must re-implement every method, which becomes noisy.
- When behavior ordering between decorators is complex and undocumented — use a Chain of Responsibility or Pipeline pattern with explicit ordering instead.
- When you only need a single, stable extension — straightforward subclassing is clearer.
- When the wrapped object's identity matters (decorators break `instanceof` checks and reference equality).

---

## Variants

### Static Decoration
Decoration decided at compile time by wrapping in constructors (as shown above). Order is explicit and readable.

### Dynamic Decoration
Wrap/unwrap at runtime by maintaining a mutable reference:

```java
public class DynamicPipeline {
    private HttpClient client;

    public DynamicPipeline(HttpClient base) { this.client = base; }

    public void addLogging() { this.client = new LoggingDecorator(this.client); }
    public void addAuth(String token) { this.client = new AuthDecorator(this.client, token); }

    public HttpResponse execute(HttpRequest req) { return client.execute(req); }
}
```

### Functional Decoration with Lambdas
When the interface is a single abstract method, decorators are just `Function` compositions:

```java
import java.util.function.Function;

Function<HttpRequest, HttpResponse> base =
    req -> new HttpResponse(200, "{}");

Function<HttpRequest, HttpResponse> withLogging = req -> {
    System.out.println("Sending: " + req.getUrl());
    HttpResponse res = base.apply(req);
    System.out.println("Received: " + res.getStatusCode());
    return res;
};

// Compose: withLogging -> withAuth -> base
Function<HttpRequest, HttpResponse> withAuth = req -> {
    req.addHeader("Authorization", "Bearer token");
    return withLogging.apply(req);
};
```

This works cleanly for single-method interfaces but cannot handle multi-method contracts.

---

## Real-World Examples

| Context | Component | Concrete Decorators |
|---|---|---|
| `java.io` | `InputStream` | `BufferedInputStream`, `DataInputStream`, `GZIPInputStream` |
| `java.io` | `OutputStream` | `BufferedOutputStream`, `GZIPOutputStream` |
| `java.io` | `Reader` | `BufferedReader`, `LineNumberReader` |
| Spring Security | `HttpServletRequest` | `SecurityContextHolderAwareRequestWrapper` |
| Spring | `DataSource` | `TransactionAwareDataSourceProxy` |

`new GZIPInputStream(new BufferedInputStream(new FileInputStream("data.gz")))` is textbook Decorator — the same pattern you just implemented.

---

## Interview Questions

**Q: What is the difference between Decorator and Proxy?**

Both wrap an object and share the same interface. The key distinction is *intent*:
- **Proxy** controls *access* to the subject — it manages the object's lifecycle (virtual proxy), enforces permissions (protection proxy), or hides location (remote proxy). The proxy often creates or manages the real subject itself.
- **Decorator** adds *behavior* — it enhances what the subject does. The decorated object is supplied externally by the client, not managed by the decorator.

**Q: Decorator vs Inheritance — when does each win?**

| Dimension | Inheritance | Decorator |
|---|---|---|
| Composition | Compile-time, static | Runtime, dynamic |
| Combinations | O(2^N) subclasses | O(N) decorators |
| Open/Closed | Requires modifying hierarchy | Add new decorator class only |
| Transparency | `instanceof` works | Breaks `instanceof` |

Use inheritance when the IS-A relationship is semantically sound and extensions are few. Use Decorator when you have orthogonal, combinable behaviors.

**Q: Does Decorator violate LSP?**

No. A decorator IS-A `Component` and can be substituted anywhere a `Component` is expected. The behavior changes, but the contract (interface) is respected — which is exactly what LSP requires.

**Q: How would you unit test a decorator?**

Inject a mock of the inner `HttpClient` (using Mockito or a hand-rolled stub). Verify that the decorator calls the delegate with the correct arguments and returns the delegate's response untampered. Test each decorator in total isolation.
