# Builder Pattern

## Intent

Separate the construction of a complex object from its representation so that the
same construction process can create different representations.

---

## Problem: The Telescoping Constructor

Consider an `HttpRequest` class. A realistic HTTP request has many optional fields:
method, URL, headers, query parameters, body, timeout, retry policy, authentication,
and so on. Without Builder, the usual workaround is telescoping constructors:

```java
// Telescoping constructors — every combination needs its own overload
public class HttpRequest {

    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;
    private final int timeoutMs;
    private final int retryCount;
    private final String authToken;

    // 2-param constructor
    public HttpRequest(String method, String url) {
        this(method, url, new HashMap<>(), null, 5000, 0, null);
    }

    // 4-param constructor
    public HttpRequest(String method, String url, Map<String, String> headers, String body) {
        this(method, url, headers, body, 5000, 0, null);
    }

    // Full constructor — 7 parameters, all positional
    public HttpRequest(String method, String url, Map<String, String> headers,
                       String body, int timeoutMs, int retryCount, String authToken) {
        this.method    = method;
        this.url       = url;
        this.headers   = headers;
        this.body      = body;
        this.timeoutMs = timeoutMs;
        this.retryCount = retryCount;
        this.authToken = authToken;
    }
}

// Call site — what does this mean? Which null is body vs token?
HttpRequest req = new HttpRequest("POST", "https://api.example.com/orders",
                                  new HashMap<>(), "{\"qty\":1}", 3000, 2, null);
//                                                                          ^^^^
//                                                               is this body? retryCount? token?
```

**Pain points:**

1. **Readability:** positional parameters carry no names at call sites. `null, 0, null`
   is meaningless without an IDE tooltip.
2. **Combinatorial explosion:** 7 optional fields require up to 2^7 = 128 constructor
   overloads to cover every combination without forcing callers to pass nulls.
3. **Immutability vs. flexibility tradeoff:** JavaBeans setters solve readability but
   make the object mutable (setters can be called at any time, breaking invariants).
4. **Validation is deferred or scattered:** there is no single place to check that
   `method` and `url` are both present before construction completes.

---

## Structure

```
+------------------------+          +------------------------------------+
|       <<interface>>    |          |         HttpRequest.Builder        |
|       HttpRequest      |          +------------------------------------+
+------------------------+          | - method: String                   |
| + getMethod(): String  |          | - url: String                      |
| + getUrl(): String     |          | - headers: Map<String,String>      |
| + getHeaders(): Map    |          | - body: String                     |
| + getBody(): String    |          | - timeoutMs: int                   |
| + getTimeoutMs(): int  |          | - retryCount: int                  |
| + getRetryCount(): int |          | - authToken: String                |
| + getAuthToken(): String          +------------------------------------+
+------------------------+          | + Builder(method, url)             |
         ^                          | + header(k,v): Builder             |
         |                          | + body(body): Builder              |
         |  builds                  | + timeout(ms): Builder             |
         +-- HttpRequest.Builder -> | + retries(n): Builder              |
                                    | + auth(token): Builder             |
                                    | + build(): HttpRequest             |
                                    +------------------------------------+
                                              |
                                              | creates
                                              v
                                    +------------------------------------+
                                    |   HttpRequest (concrete)           |
                                    +------------------------------------+
                                    | - method: String (final)           |
                                    | - url: String (final)              |
                                    | - headers: Map (final, unmodif.)   |
                                    | - body: String (final)             |
                                    | - timeoutMs: int (final)           |
                                    | - retryCount: int (final)          |
                                    | - authToken: String (final)        |
                                    +------------------------------------+
                                    | + getMethod(): String              |
                                    | + getUrl(): String                 |
                                    | + ...                              |
                                    +------------------------------------+

Director (optional — encapsulates reusable build sequences):
+----------------------------+
|    HttpRequestDirector     |
+----------------------------+
| - builder: HttpRequest.Builder |
+----------------------------+
| + buildGetRequest(url): HttpRequest          |
| + buildAuthenticatedPost(url,body): HttpRequest |
+----------------------------+
```

---

## Implementation

### Step 1: The Problem Domain — telescoping constructor (repeated from above for context)

```java
import java.util.*;

// BEFORE Builder — the problem
public class BadHttpRequest {
    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;
    private final int timeoutMs;
    private final int retryCount;
    private final String authToken;

    // Caller must pass nulls for unused optional fields — brittle and unreadable
    public BadHttpRequest(String method, String url, Map<String, String> headers,
                          String body, int timeoutMs, int retryCount, String authToken) {
        if (method == null || url == null) throw new IllegalArgumentException("method and url are required");
        this.method     = method;
        this.url        = url;
        this.headers    = headers != null ? Collections.unmodifiableMap(headers) : Collections.emptyMap();
        this.body       = body;
        this.timeoutMs  = timeoutMs > 0 ? timeoutMs : 5000;
        this.retryCount = retryCount;
        this.authToken  = authToken;
    }
}
```

### Step 2: Builder Pattern — clean, fluent, immutable result

```java
import java.util.*;

public final class HttpRequest {

    // All fields are final — object is immutable once built
    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;
    private final int timeoutMs;
    private final int retryCount;
    private final String authToken;

    // Private constructor — only Builder can call it
    private HttpRequest(Builder builder) {
        this.method     = builder.method;
        this.url        = builder.url;
        this.headers    = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.body       = builder.body;
        this.timeoutMs  = builder.timeoutMs;
        this.retryCount = builder.retryCount;
        this.authToken  = builder.authToken;
    }

    // Getters only — no setters, object is immutable
    public String getMethod()              { return method; }
    public String getUrl()                 { return url; }
    public Map<String, String> getHeaders(){ return headers; }
    public String getBody()                { return body; }
    public int getTimeoutMs()              { return timeoutMs; }
    public int getRetryCount()             { return retryCount; }
    public String getAuthToken()           { return authToken; }

    @Override
    public String toString() {
        return String.format("HttpRequest{method='%s', url='%s', timeout=%dms, retries=%d, hasBody=%s}",
                method, url, timeoutMs, retryCount, body != null);
    }

    // -----------------------------------------------------------------------
    // Inner Static Builder — co-located with the product class
    // -----------------------------------------------------------------------
    public static final class Builder {

        // Required fields — passed in constructor so they cannot be omitted
        private final String method;
        private final String url;

        // Optional fields — initialized to sensible defaults
        private Map<String, String> headers = new HashMap<>();
        private String body       = null;
        private int    timeoutMs  = 5000;
        private int    retryCount = 0;
        private String authToken  = null;

        // Constructor enforces required parameters
        public Builder(String method, String url) {
            if (method == null || method.isBlank()) throw new IllegalArgumentException("method is required");
            if (url == null || url.isBlank())       throw new IllegalArgumentException("url is required");
            this.method = method.toUpperCase();
            this.url    = url;
        }

        // Each setter returns 'this' — enables fluent method chaining
        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder timeout(int timeoutMs) {
            if (timeoutMs <= 0) throw new IllegalArgumentException("timeout must be positive");
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder retries(int retryCount) {
            if (retryCount < 0) throw new IllegalArgumentException("retryCount cannot be negative");
            this.retryCount = retryCount;
            return this;
        }

        public Builder auth(String token) {
            this.authToken = token;
            return this;
        }

        // build() is the single place to enforce cross-field validation
        public HttpRequest build() {
            if (body != null && !List.of("POST", "PUT", "PATCH").contains(method)) {
                throw new IllegalStateException("Body is only valid for POST, PUT, PATCH — got " + method);
            }
            if (authToken == null && headers.containsKey("Authorization")) {
                // allow header-based auth even without explicit .auth() call
            }
            return new HttpRequest(this);
        }
    }
}
```

### Step 3: Director — encapsulates reusable build recipes

```java
// The Director knows HOW to build common variants; clients don't need to remember
// the recipe for a "standard authenticated POST" every time.
public class HttpRequestDirector {

    private final String baseUrl;
    private final String defaultAuthToken;

    public HttpRequestDirector(String baseUrl, String defaultAuthToken) {
        this.baseUrl = baseUrl;
        this.defaultAuthToken = defaultAuthToken;
    }

    public HttpRequest buildGetRequest(String path) {
        return new HttpRequest.Builder("GET", baseUrl + path)
                .timeout(3000)
                .auth(defaultAuthToken)
                .build();
    }

    public HttpRequest buildAuthenticatedPost(String path, String jsonBody) {
        return new HttpRequest.Builder("POST", baseUrl + path)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(jsonBody)
                .timeout(10000)
                .retries(3)
                .auth(defaultAuthToken)
                .build();
    }
}
```

### Step 4: Usage at call sites

```java
public class BuilderDemo {
    public static void main(String[] args) {

        // Direct fluent builder — readable, no nulls, no position guessing
        HttpRequest getRequest = new HttpRequest.Builder("GET", "https://api.example.com/users/42")
                .timeout(3000)
                .auth("Bearer eyJhbGci...")
                .build();

        HttpRequest postRequest = new HttpRequest.Builder("POST", "https://api.example.com/orders")
                .header("Content-Type", "application/json")
                .header("X-Request-Id", UUID.randomUUID().toString())
                .body("{\"productId\": \"SKU-123\", \"quantity\": 2}")
                .timeout(8000)
                .retries(2)
                .auth("Bearer eyJhbGci...")
                .build();

        // Using the Director for standard recipes
        HttpRequestDirector director = new HttpRequestDirector(
                "https://api.example.com", "Bearer eyJhbGci...");

        HttpRequest standardGet  = director.buildGetRequest("/inventory/SKU-456");
        HttpRequest standardPost = director.buildAuthenticatedPost("/shipments",
                "{\"orderId\": \"ORD-789\", \"carrier\": \"FEDEX\"}");

        System.out.println(getRequest);
        System.out.println(postRequest);
        System.out.println(standardGet);
        System.out.println(standardPost);
    }
}
```

### Builder vs. Factory — key distinction

```java
// FACTORY: decides WHICH type to create based on input; caller does not configure the object
Notification n = NotificationFactory.create("EMAIL"); // factory picks the type

// BUILDER: caller configures a SINGLE complex type step-by-step
HttpRequest r = new HttpRequest.Builder("POST", url)
        .body(payload)
        .timeout(5000)
        .build(); // builder assembles a fully configured instance

// Rule of thumb:
//   Factory  -> "what to create"   (type selection)
//   Builder  -> "how to configure" (step-by-step assembly of one type)
```

---

## When to Use

- The object has many optional configuration parameters and positional constructors
  become unreadable (more than 3-4 parameters is a common threshold).
- You need an immutable object but also need flexible initialization — the Builder
  accumulates state; the product is sealed at `build()`.
- Cross-field validation is required before the object is considered valid. The
  `build()` method is the natural enforcement point.
- You need multiple representations of the same construction process (the Director
  captures reusable recipes without duplicating builder call sequences).

## When NOT to Use

- **Simple objects with 1-3 mandatory fields.** A constructor is clearer. Builder adds
  a class for no gain when there is nothing to configure optionally.
- **Mutable objects designed for incremental mutation.** Builder produces an immutable
  snapshot; if the object must change over its lifetime, use setters or a dedicated
  state machine instead.
- **Performance-critical paths where object allocation is measured.** The Builder itself
  is an extra allocation per construction. In tight loops creating millions of objects,
  this is measurable. (In practice, the JIT often elides it via escape analysis.)

---

## Variants

### Inner Static Builder (shown above)
The Builder is a static inner class of the product. This is the most common form in
Java. Co-location makes it easy to discover and keeps the API surface clean.

### Director Pattern (shown above)
A separate `Director` class encodes reusable build sequences. Clients call the Director
rather than the Builder directly. Use when the same complex build sequence is repeated
across the codebase and the recipe must be centrally maintained.

### Step Builder (enforced ordering via interfaces)
Uses a chain of interfaces where each method returns the next interface in sequence,
making certain orderings impossible to express (compile-time enforcement):

```java
// Step-builder skeleton — each interface returns the next required step
public interface MethodStep  { UrlStep   method(String m); }
public interface UrlStep     { BodyStep  url(String u); }
public interface BodyStep    { BuildStep body(String b); }
public interface BuildStep   { HttpRequest build(); }

// Usage is forced into method -> url -> body -> build order
HttpRequest r = HttpRequestStepBuilder.newBuilder()
        .method("POST")
        .url("https://api.example.com/data")
        .body("{}")
        .build();
```

---

## Real-World Examples

- **`java.lang.StringBuilder`** — the archetypal builder. Accumulates character
  sequences via `.append()` calls, produces an immutable `String` at `.toString()`.
- **`Lombok @Builder`** — annotation processor generates a full inner static builder
  at compile time, eliminating boilerplate for data classes.
- **Spring MockMvc request builders** — `MockMvcRequestBuilders.post("/api/data")
  .contentType(MediaType.APPLICATION_JSON).content(body).header("Auth", token)` is
  a textbook fluent builder in test infrastructure.
- **OkHttp `Request.Builder`** — production HTTP client library uses the exact pattern
  shown above: `new Request.Builder().url(url).addHeader(...).post(body).build()`.
- **`ProcessBuilder`** in the JDK — configures a process (command, environment,
  working directory) before starting it.

---

## Interview Questions

**Q1: When does Builder outperform a constructor with named parameters (as in Kotlin
or Python)?**

In Java, constructors are positional — there are no named parameters at the language
level. Builder compensates for this by making each step self-documenting (`.timeout(3000)`
vs. the third `int` parameter). In languages with named parameters (Kotlin, Python,
Swift), the justification for Builder shifts to: (a) enforcing immutability — the
product has no setters; (b) cross-field validation at build time; (c) reusable Director
recipes; (d) the need to pass an incomplete "builder-in-progress" across API boundaries
before the final `build()` call. Named parameters alone do not give you any of these.

**Q2: How does Builder differ from the Factory pattern?**

Factory decides which *type* to instantiate and returns a ready-to-use object —
the caller provides selection criteria, not configuration. Builder is responsible
for assembling a *single type* through sequential configuration steps — the caller
drives the configuration, then triggers instantiation via `build()`. Use Factory
when object type varies; use Builder when object configuration varies.

**Q3: Builder objects are often not thread-safe. Is that a problem?**

Intentionally not. A Builder instance is expected to be owned by a single thread
during construction. The *product* (`HttpRequest` in the example) is immutable and
therefore thread-safe to share across threads after `build()`. If a Builder is shared
across threads, the design is wrong — each thread should own its own Builder and
produce its own product.
