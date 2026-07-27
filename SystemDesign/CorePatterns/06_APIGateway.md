# API Gateway

## 1. What It Is

An API Gateway is the single entry point for all client requests to a system's backend services, handling cross-cutting concerns — authentication, routing, rate limiting, SSL termination, request transformation, and circuit breaking — so individual services don't have to implement them. It exists because microservices architectures create N×M coupling between M clients and N services; the gateway collapses this to M→1→N, centralizing concerns that would otherwise be duplicated across every service.

---

## 2. The Problem It Solves

**Without an API Gateway:**

- Every service independently implements auth, rate limiting, SSL, logging.
- Mobile clients make 15 separate API calls to render one screen (N+1 across microservices).
- A JavaScript web app and a mobile app need different data shapes — neither gets what it needs efficiently.
- A downstream service failure propagates and takes down unrelated services (cascading failure).
- SSL certificates must be managed on every service instance.

**With an API Gateway:**
- Auth, rate limiting, SSL termination handled once at the gateway.
- Gateway aggregates multiple backend calls into one client response.
- Different clients (mobile/web/3rd-party) get tailored responses via BFF.
- Circuit breakers at the gateway prevent cascading failures.

---

## 3. API Gateway Responsibilities

```
                          Clients
              ┌────────────┼───────────┐
           Mobile         Web        3rd Party
              │            │            │
              └────────────┼────────────┘
                           │
                    ┌──────▼───────┐
                    │  API Gateway │
                    │──────────────│
                    │ SSL Termination    (TLS offloading)
                    │ Authentication     (JWT, OAuth2, API keys)
                    │ Authorization      (scope/role check)
                    │ Rate Limiting      (token bucket per client)
                    │ Request Routing    (path → service mapping)
                    │ Request Transform  (header rewrite, body map)
                    │ Response Transform (field filtering, aggregation)
                    │ Circuit Breaking   (fail fast on downstream errors)
                    │ Load Balancing     (across service instances)
                    │ Observability      (logging, tracing, metrics)
                    └──────┬───────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
    User Service    Order Service    Product Service
```

---

## 4. BFF — Backend for Frontend

The BFF pattern creates a dedicated API gateway per client type. Each BFF is optimized for one consumer's needs.

```
                    ┌─────────────┐
                    │  Mobile BFF │  ← optimized for low bandwidth,
                    │  /api/m/v1  │    small payloads, offline-first
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
Mobile App  ───────►│             │
                    │  Shared     │  ← Auth, rate limiting shared
Web App     ───────►│  Backend    │
                    │  Services   │
3rd Party   ───────►│             │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   Web BFF   │  ← optimized for rich data,
                    │  /api/w/v1  │    GraphQL, SSR
                    └─────────────┘
                    ┌─────────────┐
                    │ Partner BFF │  ← stable versioned REST API,
                    │  /api/p/v1  │    no internal details exposed
                    └─────────────┘
```

**Why BFF over a single generic gateway:**

| Problem | Generic Gateway | BFF |
|---------|----------------|-----|
| Mobile needs compressed, minimal payloads | Returns full response, client filters | BFF returns only needed fields |
| Web needs GraphQL | REST doesn't map cleanly | Web BFF implements GraphQL → REST fan-out |
| Partner API must be versioned | All clients share one version | Partner BFF is independently versioned |
| Mobile needs offline sync endpoint | Not in generic gateway | Mobile BFF adds sync endpoints |

**BFF ownership:** The frontend team owns their BFF. This removes the bottleneck of a central platform team.

---

## 5. Service Mesh vs API Gateway

A common confusion. They operate at different layers:

```
┌────────────────────────────────────────────────────────┐
│                       North-South Traffic               │
│             (client → cluster boundary)                 │
│                    API Gateway                          │
│   (auth, rate limit, routing, SSL, external clients)    │
└────────────────────────┬───────────────────────────────┘
                         │ enters the cluster
┌────────────────────────▼───────────────────────────────┐
│                       East-West Traffic                 │
│             (service → service, inside cluster)         │
│                    Service Mesh                         │
│   (mutual TLS, service discovery, retries, circuit      │
│    breaking, observability between microservices)        │
│                 e.g., Istio, Linkerd                    │
└────────────────────────────────────────────────────────┘
```

| Concern | API Gateway | Service Mesh |
|---------|-------------|--------------|
| External client auth | Yes | No |
| Rate limiting (external) | Yes | No |
| SSL termination (ingress) | Yes | Mutual TLS (internal) |
| Service-to-service encryption | No | Yes |
| Service discovery | Partial | Yes (sidecar proxy) |
| Circuit breaking (external) | Yes | Yes (internal) |
| Traffic policies (A/B, canary) | Limited | Yes |

They are complementary, not alternatives. Many production systems run both: API Gateway for ingress + Istio for internal mesh.

---

## 6. Circuit Breaker Pattern

The circuit breaker prevents a failing downstream service from causing cascading failures across the system by failing fast after a threshold of errors.

### State Machine

```
ASCII: Circuit Breaker State Machine

              [Success rate drops        ]
              [below threshold           ]
CLOSED ───────────────────────────────► OPEN
  │                                       │
  │  All requests pass through            │  All requests fail immediately
  │  Track error rate                     │  (no call to downstream)
  │                                       │  Wait for reset timeout
  │                                       │  (e.g., 30 seconds)
  │                                       │
  │   ◄────────────────────────────────── │
  │    [After timeout: allow ONE probe    ]
  │                                       │
  │          HALF-OPEN ◄──────────────────┘
  │              │
  │    Allow limited traffic (e.g., 10% of requests)
  │    or just one probe request
  │              │
  │     ┌────────┴────────┐
  │     │                 │
  │  PROBE              PROBE
  │  SUCCEEDS           FAILS
  │     │                 │
  └─────┘                 └───────────────► OPEN (reset timeout)
[Back to CLOSED]               [Back to waiting]
```

### Closed → Open Transition (Error Threshold)

```
Window-based: track errors in a sliding window of N requests
  - If error rate > 50% in last 20 requests → trip to OPEN
  - Or: if error count > 5 in last 10 seconds → trip to OPEN

Count-based (simpler):
  consecutive_failures = 0
  on success: consecutive_failures = 0
  on failure: consecutive_failures++
  if consecutive_failures >= threshold: OPEN
```

### Hystrix / Resilience4j Implementation

**Resilience4j (modern Java):**
```java
CircuitBreakerConfig config = CircuitBreakerConfig.custom()
    .failureRateThreshold(50)           // Open when 50% failures
    .waitDurationInOpenState(Duration.ofSeconds(30))  // Stay open 30s
    .ringBufferSizeInClosedState(20)    // Evaluate last 20 calls
    .ringBufferSizeInHalfOpenState(5)   // Allow 5 test calls in HALF-OPEN
    .slowCallRateThreshold(80)          // Also trip on slow calls
    .slowCallDurationThreshold(Duration.ofSeconds(2))
    .build();

CircuitBreaker cb = CircuitBreaker.of("paymentService", config);

// Decorate the call
Supplier<PaymentResponse> decoratedCall = CircuitBreaker
    .decorateSupplier(cb, () -> paymentService.charge(request));

// Execute with fallback
PaymentResponse response = Try.ofSupplier(decoratedCall)
    .recover(CallNotPermittedException.class, ex -> cachedFallbackResponse())
    .recover(Exception.class, ex -> defaultResponse())
    .get();
```

**Key metrics to expose:**
- `circuit_breaker_state` (CLOSED=0, OPEN=1, HALF_OPEN=2)
- `circuit_breaker_failure_rate`
- `circuit_breaker_calls_total` by outcome (success, failure, not_permitted)

---

## 7. Bulkhead Pattern

Isolate resource pools so one slow consumer does not starve others.

```
Without Bulkhead:
  Shared thread pool: 100 threads
  Payment Service slow → uses 100 threads waiting
  → Order Service, User Service: no threads available → failure

With Bulkhead:
  ┌───────────────────┬───────────────────┬──────────────────┐
  │  Payment Pool     │  Order Pool       │  User Pool       │
  │  20 threads       │  50 threads       │  30 threads      │
  └───────────────────┴───────────────────┴──────────────────┘
  
  Payment Service slow → saturates Payment Pool (20 threads)
  → Order and User services: unaffected (separate pools)
```

Resilience4j Bulkhead:
```java
BulkheadConfig config = BulkheadConfig.custom()
    .maxConcurrentCalls(20)
    .maxWaitDuration(Duration.ofMillis(100))
    .build();
```

---

## 8. Timeout and Retry Cascades — Why They Cause Outages

This is one of the most important failure modes in microservices.

```
Request timeout cascade:

Client          API Gateway      Service A        Service B
  │                │                │                │
  │  request       │                │                │
  │──────────────► │                │                │
  │  timeout=30s   │  request       │                │
  │                │──────────────► │                │
  │                │  timeout=30s   │  request       │
  │                │                │──────────────► │
  │                │                │  timeout=30s   │
  │                │                │  [Service B is slow, 29s response]
  │                │                │◄─────────────── │
  │                │◄──────────────── [29s elapsed]   │
  │◄────────────── [29s elapsed, close to client timeout]

Now add retries: each layer retries 3x
  Client:     1 request × 3 retries = 3 attempts
  API GW:     3 attempts × 3 retries = 9 downstream calls
  Service A:  9 calls × 3 retries = 27 calls to Service B
  
Service B receives 27x the original request volume.
Service B was slow due to load → now it's overwhelmed.
```

**Retry storm prevention:**
1. **Total timeout budget:** Set a deadline at the edge and propagate it. Each hop subtracts its overhead. If budget exhausted, fail immediately.
2. **Exponential backoff with jitter:** `delay = min(cap, base * 2^attempt) + random_jitter`. Jitter prevents synchronized retries.
3. **Circuit breaker at each layer:** Once error rate crosses threshold, fail fast without retrying.
4. **Do not retry on retries:** If request already has a `X-Request-Retry` header, do not add more retries.

```
Safe retry budget:
  Client timeout: 10s
  API Gateway timeout: 8s (leaves 2s for network)
  Service A timeout: 6s (leaves 2s for API GW overhead)
  Service B timeout: 4s (leaves 2s for Service A overhead)
  
  Retries: only at client layer, max 2 retries, with circuit breaker
```

---

## 9. Trade-offs

### API Gateway Pros
- Single point for cross-cutting concerns (no duplication across services).
- Simplifies clients (one endpoint, one auth flow).
- Enables gradual migration (route /v1 to legacy, /v2 to new service).

### API Gateway Cons
- **Single point of failure:** If the gateway goes down, all clients are offline. Mitigate: multiple gateway instances behind a load balancer.
- **Latency overhead:** Every request adds one extra network hop + gateway processing (~1–5ms).
- **Bottleneck:** All traffic flows through one component. Must be horizontally scalable.
- **Coupling:** Teams depend on the gateway for deployment (if not using BFF model).

### Circuit Breaker Trade-offs
- **False opens:** A brief spike in errors (e.g., a deploy) can open the circuit and cause unnecessary failures. Tune the window size and threshold carefully.
- **Open circuit fallback quality:** Fallback must be meaningful (cached response, degraded but functional), not just an empty 503.
- **Half-open probing:** Too aggressive probing of a recovering service can re-overwhelm it.

---

## 10. Where It Appears in Real Systems

| System | Usage |
|--------|-------|
| **Netflix Zuul/Zuul 2** | Gateway with circuit breaking (Hystrix) for all API traffic |
| **Netflix Hystrix** | Circuit breaker (now in maintenance; replaced by Resilience4j) |
| **Amazon API Gateway** | Managed gateway; WAF, auth, throttling, Lambda integration |
| **Kong** | Open-source gateway; plugins for auth, rate limiting, logging |
| **AWS ALB** | Layer 7 routing; target groups per path/host |
| **Nginx / Envoy** | Reverse proxy with circuit breaking, health checks |
| **Istio** | Service mesh; circuit breaking for east-west traffic |
| **GraphQL Federation (Apollo Router)** | BFF-style unified API over microservices |
| **Cloudflare Workers** | Edge-based API gateway with custom logic at CDN layer |

---

## 11. Numbers to Know

| Metric | Value |
|--------|-------|
| API Gateway latency overhead | 1–5ms per request |
| Netflix API Gateway throughput | Hundreds of thousands req/s |
| Hystrix default circuit open threshold | 50% error rate in 10s rolling window |
| Resilience4j default timeout | Configurable (no default) — must set explicitly |
| Circuit breaker OPEN → HALF-OPEN wait | 30–60 seconds (typical) |
| Exponential backoff base | 100ms, cap at 30s, jitter ±50% |
| Max safe retry multiplier per layer | 2–3x (not 3 retries at every layer) |
| SSL termination overhead | ~1ms per handshake (TLS 1.3 is faster) |
| Connection pool size (gateway to service) | 100–500 connections per upstream |

---

## 12. Interview Tips

### What You'll Be Asked

**"Design the API layer for a microservices system serving mobile and web clients."**
- Introduce the API Gateway first, explain cross-cutting concerns.
- Propose BFF for mobile vs web — justify with different data needs.
- Add circuit breakers between gateway and downstream services.
- Discuss fallback behavior: what happens when a downstream is down?

**"How do you prevent a slow downstream service from taking down your entire system?"**
- Circuit breaker + bulkhead.
- Timeout at every layer with a shared budget.
- Fallback to cache / degraded response.
- Avoid retry cascades — retry only at the outermost layer with backoff + jitter.

**"What's the difference between a service mesh and an API gateway?"**
- API Gateway: ingress, external clients, north-south.
- Service Mesh: internal service-to-service, east-west.
- Both can do circuit breaking but at different scopes.

### Common Follow-ups
- "How do you handle gateway downtime?" → Multiple gateway instances behind a cloud load balancer (ALB, GCLB). Gateway should be stateless.
- "Where do you store rate limiting state across gateway instances?" → Redis (shared state). In-memory only works for single-instance gateways.
- "How do you do canary deployments with an API gateway?" → Weighted routing: 95% to stable, 5% to canary. Gradually increase based on error rates.
- "What happens to in-flight requests when you deploy a new gateway version?" → Graceful shutdown: stop accepting new connections, drain existing connections (SIGTERM handler with 30s drain period).

### Mistakes to Avoid
- Putting business logic in the gateway — it should only handle cross-cutting concerns.
- Not having a fallback for open circuit breakers — a 503 with no fallback is often worse than a degraded response.
- Retrying at every layer (client + gateway + service) without coordinating — causes retry storms.
- Not making the gateway stateless — stateful gateways cannot be horizontally scaled.
- Forgetting that circuit breakers need monitoring — an open circuit that nobody notices is an invisible outage.
- Confusing BFF with "we have one gateway with different paths" — a true BFF is a separate deployment per client type with independent ownership.
