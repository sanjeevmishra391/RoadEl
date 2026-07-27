# Load Balancing

## 1. What It Is

A load balancer distributes incoming network traffic across a pool of backend servers to maximize throughput, minimize latency, and avoid overwhelming any single server. It decouples the public entry point from the fleet of workers, enabling horizontal scaling and fault tolerance. Operating at different layers of the OSI model (L4 vs L7) determines what information the LB can inspect and what routing decisions it can make.

---

## 2. How It Works

### The Basic Flow

```
                         ┌─────────────────────────────────────────────┐
Client ──── Internet ──► │          LOAD BALANCER                      │
                         │                                              │
                         │  1. Accept TCP connection (L4)              │
                         │  2. TLS termination (L7 only)               │
                         │  3. Parse HTTP headers / URL (L7 only)      │
                         │  4. Select upstream via algorithm            │
                         │  5. Forward request; proxy response back     │
                         └──────────────┬──────────────────────────────┘
                                        │
               ┌────────────────────────┼────────────────────────┐
               ▼                        ▼                        ▼
         ┌──────────┐            ┌──────────┐            ┌──────────┐
         │ Server A │            │ Server B │            │ Server C │
         │  :8080   │            │  :8080   │            │  :8080   │
         └──────────┘            └──────────┘            └──────────┘
               ▲                        ▲                        ▲
               └────────────────────────┴────────────────────────┘
                              Health Check Loop
                       (LB probes /health every 5-10s)
```

### L7 Load Balancer Request Flow (Full Detail)

```
Client
  │
  │  TCP SYN / TLS ClientHello
  ▼
┌─────────────────────────────────────────────────────────┐
│  LOAD BALANCER (L7)                                     │
│                                                         │
│  ┌──────────────┐    ┌─────────────┐    ┌───────────┐  │
│  │ TLS          │    │ HTTP Parse  │    │ Routing   │  │
│  │ Termination  │───►│ Host header │───►│ Decision  │  │
│  │ (owns cert)  │    │ /path /URL  │    │ Algorithm │  │
│  └──────────────┘    └─────────────┘    └─────┬─────┘  │
│                                               │        │
│  ┌─────────────────────────────────────────── │──────┐ │
│  │  Upstream Pool                             │      │ │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐   │      │ │
│  │  │ srv-1   │  │ srv-2   │  │ srv-3   │◄──┘      │ │
│  │  │ conn:42 │  │ conn:18 │  │ conn:61 │          │ │
│  │  └────┬────┘  └────┬────┘  └────┬────┘          │ │
│  │       │            │            │                │ │
│  │  ┌────▼────────────▼────────────▼────────────┐   │ │
│  │  │        Health Check Monitor               │   │ │
│  │  │  HTTP GET /health every 5s                │   │ │
│  │  │  Mark DOWN if 3 consecutive failures      │   │ │
│  │  └───────────────────────────────────────────┘   │ │
│  └──────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
  │
  │  New TLS session to upstream (or keepalive reuse)
  │  X-Forwarded-For: <client-ip> injected here
  ▼
Backend Server
```

---

## 3. Deep Dive

### L4 vs L7 Load Balancing

| Dimension | L4 (Transport Layer) | L7 (Application Layer) |
|---|---|---|
| OSI Layer | Layer 4 — TCP/UDP | Layer 7 — HTTP/HTTPS/gRPC |
| Sees | IP, port, TCP flags | HTTP headers, cookies, URL, body |
| Routing basis | IP:port tuples | Host, path, header value, cookie |
| TLS | Pass-through (SNI only) | Terminates TLS (owns the cert) |
| CPU overhead | Low (NAT / packet forwarding) | ~2-3x higher (TLS handshake + HTTP parsing) |
| Use cases | TCP proxying, raw throughput | API routing, A/B testing, auth offload |
| Examples | AWS NLB, HAProxy TCP mode | NGINX, AWS ALB, Envoy, Traefik |
| Request affinity | IP-based only | Cookie, header, or URL-based |
| WebSocket | Transparent pass-through | Requires Upgrade header handling |

**Key insight for interviews:** L7 pays a latency cost (roughly 0.5–1ms extra for TLS termination + HTTP parsing) but unlocks content-aware routing, header injection (`X-Forwarded-For`, `X-Request-ID`), and the ability to route `/api` to one cluster and `/static` to another. L4 is preferred when raw throughput matters more than routing intelligence — e.g., forwarding TCP for a custom protocol, or game server UDP.

---

### Algorithms in Depth

#### Round-Robin
Requests cycle through servers in order: A → B → C → A → B → C …

- **Simplicity:** O(1) per request, no shared state needed.
- **Failure mode:** Ignores server load entirely. A server handling 10-second database queries gets the same traffic as one finishing in 10ms.
- **When to use:** Only when requests are roughly uniform in cost and servers are identical.

#### Weighted Round-Robin
Assign static weights: `{ A: 3, B: 2, C: 1 }`. A gets 3 out of every 6 requests.

- **Problem:** Weights are set at deploy time. A server that starts GC-pausing gets weight 3 forever.
- **When to use:** Heterogeneous hardware (one beefy server, one weak one). Never for auto-scaled fleets where all nodes are identical.

#### Least Connections
Route to the server with the fewest active connections.

- **Better for variable-cost requests** (mix of fast reads and slow writes).
- **Requires shared state** across LB workers — a single counter store (in-memory or atomic) that all threads update. This becomes a contention bottleneck at very high scale.
- **Variant: Weighted Least Connections** — `score = active_connections / weight`. Preferred when servers have different capacities.

#### IP Hash
`server_index = hash(client_ip) % server_count`

- Guarantees the same client always hits the same server — useful for session affinity without a session store.
- **Fatal flaw:** NAT. An office of 1000 users behind a single NAT IP all land on the same server. IPv6 prefix delegation causes similar imbalance.
- **Another failure mode:** When a server is added/removed, `% server_count` changes, invalidating all existing affinities.

#### Consistent Hashing

```
Ring (0 to 2^32 - 1):

         0
         │
    ┌────┴────┐
    │         │
  srv-A     srv-C
(+ virtual  (+ virtual
  nodes)     nodes)
    │         │
  srv-B ──────┘
(+ virtual
  nodes)

Request hash → walk clockwise → first server encountered
```

- Virtual nodes (150–200 per physical server) ensure even distribution even with a small fleet. Without them, 3 physical nodes divide the ring into 3 uneven arcs.
- **Adding a server:** Only the keys between the new node and its predecessor migrate. ~1/N of keys move.
- **Removing a server:** Same — only that server's keys rehash to its successor.
- **Used by:** Amazon DynamoDB, Apache Cassandra, Memcached client sharding.

#### Power of Two Choices (P2C)

```
1. Pick 2 servers at random from the pool
2. Query both for their current load (connection count)
3. Route to the less-loaded of the two

     Pool of 100 servers
          │
    ┌─────┴─────┐
    ▼           ▼
  srv-47      srv-83
 conn: 12    conn: 7
    │
    └──── Route here (lower load wins)
```

- **Why it beats least-connections at scale:** Least-connections requires a globally consistent counter. P2C samples only 2 servers — O(1) with negligible coordination overhead. The math (from the "power of two" paper) shows expected max load is `O(log log N)` instead of `O(log N / log log N)` for random.
- **Used by:** HAProxy (with `random` algorithm), Google Maglev, Envoy's `LEAST_REQUEST` with `choice_count: 2`.

---

### Health Checks

#### Active Health Checks
The LB probes each backend on a schedule:
- **TCP probe:** Just checks port is open. Fast, dumb. Doesn't verify the app is actually serving.
- **HTTP probe:** `GET /health HTTP/1.1` — expects 200. Can include logic (DB connectivity check, queue depth check).
- **Typical config:** Interval = 5–10s, timeout = 2s, healthy threshold = 2 consecutive successes, unhealthy threshold = 3 consecutive failures.
- **Aggressive config (low-latency systems):** Interval = 1s, unhealthy threshold = 2 failures → marks down in 2s. Trade-off: false positives from transient network glitches.

#### Passive Health Checks (Circuit Breaker Pattern)
The LB watches real traffic for errors:
- If error rate > threshold (e.g., 50% 5xx in last 10s), eject the backend.
- **Envoy's outlier detection:** Ejects a host after N consecutive 5xx responses. Re-admits after an exponentially increasing ejection period.
- **Advantage:** Catches slow servers (high latency, not just errors) and requires no extra probe traffic.
- **Used with active checks together** for defense in depth.

---

### Sticky Sessions (Session Affinity)

**Why they exist:** Stateful server-side sessions. The session object lives in the server's memory; only that server can serve the user.

**Mechanisms:**
1. **Cookie-based:** LB sets `SERVERID=srv-2` cookie. Routes subsequent requests by cookie value.
2. **IP hash:** As described above — fragile with NAT.
3. **Custom header:** `X-Backend-Server: srv-2`.

**Why sticky sessions are dangerous:**

```
Normal traffic:            With sticky sessions after srv-2 overloads:

srv-1: 33%                 srv-1: 15%  (new users only)
srv-2: 33%                 srv-2: 70%  (all sticky users + some new)  ← HOT
srv-3: 33%                 srv-3: 15%  (new users only)
```

- **Hot spots:** Popular users (or large enterprise accounts) hash to one server.
- **Stateful servers are hard to scale:** Adding srv-4 doesn't help existing sticky users.
- **Failure recovery:** If srv-2 dies, all its users lose session state anyway. Sticky sessions provided false safety.

**Better alternatives:**
1. **Distributed session store:** Redis/Memcached holds session data. Any server can serve any user. Scales horizontally.
2. **JWT (stateless sessions):** Session state is in the signed token. No server-side storage. Any server validates the token. Preferred for modern stateless APIs.
3. **Database-backed sessions:** Durability, but higher latency (~1ms per request for a DB read).

---

### Global vs Local Load Balancing

#### Local Load Balancing
A single LB (or HA pair) in one datacenter distributes to local backends. Everything discussed above.

#### Global Load Balancing (GSLB)

**DNS-Based GSLB:**
```
Client in US-East:
  DNS query: api.example.com
  Authoritative DNS (Route 53 / Cloudflare) detects geo
  Returns: 52.1.2.3 (US-East LB VIP)

Client in EU-West:
  DNS query: api.example.com
  Returns: 18.2.3.4 (EU-West LB VIP)
```

- **TTL trade-off:** 
  - Low TTL (30s): Stale routing lasts only 30s after failover. But 30s × millions of users = enormous DNS query volume. Also OS/browser caching ignores TTL floors.
  - High TTL (300s+): Less DNS load, but a dead datacenter keeps receiving traffic for 5 minutes.
  - **Recommended for GSLB failover:** 30–60s TTL. Accept the DNS query load; use anycast to minimize it.

**Anycast Routing (BGP):**
```
Multiple PoPs announce the same IP prefix (e.g., 104.16.0.0/12) via BGP:

Client → nearest BGP hop → routed to closest PoP automatically
                           (no DNS indirection)

Cloudflare / Fastly / Google use this for their CDN edge nodes
```

- **Advantage:** Automatic geo-routing with no DNS TTL penalty. Failover happens when BGP reconverges (~seconds to minutes).
- **Disadvantage:** BGP convergence is not instantaneous. Hijacking risk (BGP route leaks). Not all operators control their BGP.
- **Use case:** CDN edge PoPs, DDoS scrubbing (sink traffic at many PoPs simultaneously).

---

## 4. Trade-offs

### When to Use Each Algorithm

| Scenario | Recommended Algorithm | Why |
|---|---|---|
| Homogeneous fleet, uniform requests | Round-robin | Simple, no shared state |
| Heterogeneous hardware | Weighted round-robin | Match weight to server capacity |
| Variable-cost requests (mix of fast/slow) | Least connections | Routes around busy servers |
| Session affinity required, no NAT | IP hash | Deterministic, no state needed |
| Distributed cache sharding | Consistent hashing | Minimal key migration on scale events |
| High-scale service mesh, microservices | P2C (power of two choices) | Low coordination cost, good load distribution |

### L4 vs L7 Decision

| Use L4 When | Use L7 When |
|---|---|
| Raw TCP throughput is critical | You need content-based routing (path, header, host) |
| Non-HTTP protocol (SMTP, MQTT, custom TCP) | You want TLS termination at the LB |
| Minimal latency overhead required | You need auth offload, header injection, WAF |
| WebSocket pass-through | You want URL-based A/B routing |
| LB manages millions of connections | Request tracing / logging at HTTP level |

### Sticky Sessions vs Distributed Sessions

| | Sticky Sessions | Redis Session Store | JWT |
|---|---|---|---|
| Session loss on server failure | Yes | No | N/A (stateless) |
| Horizontal scale | Poor (hot spots) | Good | Excellent |
| Latency | No extra hop | +0.5–1ms | +0.1ms (CPU for JWT verify) |
| Infrastructure complexity | Low | Medium (Redis cluster) | Low |
| Secret rotation | N/A | Easy | Hard (invalidating issued tokens) |

---

## 5. Numbers to Know

| Metric | Value | Notes |
|---|---|---|
| HAProxy throughput | Up to 1M req/s | L7 HTTP, tuned Linux kernel |
| NGINX concurrent connections | ~50k per worker process | Default `worker_connections 1024`; tune to 50k+ |
| Health check interval | 5–10s typical, 1s aggressive | Aggressive = risk of false positives |
| Unhealthy threshold | 3 consecutive failures | Industry default |
| DNS TTL for GSLB | 30–60 seconds | Balance failover speed vs DNS query load |
| Consistent hash virtual nodes | 150–200 per physical node | Below 100 → uneven distribution |
| Upstream keepalive connections | 10–100 per LB worker | Avoid TCP handshake per request; tune `keepalive` in NGINX |
| L4 vs L7 CPU overhead | ~2–3x for L7 | TLS + HTTP parsing cost |
| L7 added latency | ~0.5–1ms | TLS handshake on new connections; ~0.05ms on reused |
| NAT table limit (iptables) | ~1M entries (default) | Relevant for L4 packet-mode LB at scale |
| AWS ALB max request rate | 1M req/s (auto-scales) | Pre-warm for sudden spikes |
| AWS NLB connections | Millions of concurrent | L4, no state parsing overhead |

---

## 6. Interview Tips

### What Interviewers Actually Probe

1. **"What's the difference between L4 and L7 load balancing?"**
   - Don't just say "one is TCP, one is HTTP." Explain *what routing decisions become possible* at L7, and *what the cost is* (TLS termination, HTTP parsing, ~2–3x CPU). Give a concrete use case for each.

2. **"How would you handle session affinity?"**
   - The expected answer is NOT "use sticky sessions." It's: "sticky sessions create hot spots and are dangerous in failure scenarios — prefer a distributed session store (Redis) or stateless JWTs."

3. **"How does consistent hashing work and why do we need virtual nodes?"**
   - Draw the ring. Explain that without virtual nodes, 3 physical nodes create 3 uneven arcs. With 150–200 vnodes per physical node, distribution is much more uniform. Explain the 1/N key migration property on add/remove.

4. **"How does your load balancer know a backend is unhealthy?"**
   - Cover both active (scheduled HTTP probes) and passive (error-rate-based ejection/circuit breaker). Mention intervals, thresholds, and the trade-off between fast detection and false positives.

5. **"How would you do global load balancing?"**
   - Cover DNS-based GSLB (TTL trade-offs), anycast (BGP), and GeoDNS. Mention that low TTL reduces stale routing but increases DNS query volume.

### Common Mistakes

- **Confusing L4/L7 with TCP/HTTP** — L4 can still proxy HTTP; L7 *parses* HTTP. The distinction is what the LB *sees and acts on*.
- **Recommending IP hash for session affinity** — Correct answer is Redis or JWT.
- **Forgetting virtual nodes** when explaining consistent hashing — interviewers specifically check for this.
- **Not mentioning health check thresholds** — saying "the LB detects failures" without explaining how (probe type, interval, threshold count) is incomplete.
- **Ignoring DNS TTL** when discussing GSLB — a common gap for candidates who haven't operated globally distributed systems.
- **Power of two choices** — many candidates know least-connections but not P2C. Knowing it signals depth.

### Questions You Will Be Asked

- Design a load balancing layer for a service handling 500k req/s globally.
- How would you implement blue-green deployment using a load balancer?
- What happens to in-flight requests when a backend is marked unhealthy?
- How does HAProxy's `random` algorithm compare to `leastconn`?
- Explain how you'd drain a server before deploying a new version (graceful shutdown + connection draining).
- What is a "slow start" or "ramp-up" period for a newly added backend?

---

## 7. Resources

1. **ByteByteGo — Load Balancing Algorithms** — https://bytebytego.com/courses/system-design-interview/scale-from-zero-to-millions-of-users
2. **HAProxy Architecture Guide** — https://www.haproxy.com/documentation/haproxy/architecture/
3. **NGINX Load Balancing Docs** — https://docs.nginx.com/nginx/admin-guide/load-balancer/http-load-balancer/
4. **Envoy Proxy Load Balancing** (covers P2C in depth) — https://www.envoyproxy.io/docs/envoy/latest/intro/arch_overview/upstream/load_balancing/load_balancers
5. **Designing Data-Intensive Applications (DDIA)** — Chapter 6 covers consistent hashing in the context of partitioning (Kleppmann)
