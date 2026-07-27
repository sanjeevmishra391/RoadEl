# Networking — Staff-Level System Design Reference

---

## 1. What It Is

Networking is the substrate every distributed system runs on. At interview depth, it means understanding the latency budget, protocol trade-offs, and failure modes that determine whether a design is viable — not just that HTTP exists. Every architectural decision (sync vs async, CDN vs origin, REST vs gRPC) is ultimately a networking decision.

---

## 2. How It Works

### DNS Resolution

Two resolution modes exist. The difference matters for where load and latency live.

**Iterative Resolution** — the resolver does the work, each server returns a referral:

```
Client
  |
  v
Recursive Resolver (ISP / 8.8.8.8)
  |-- query root (.)       --> root returns: "ask .com NS"
  |-- query .com NS        --> .com NS returns: "ask example.com NS"
  |-- query example.com NS --> authoritative NS returns: 93.184.216.34
  |
  v
Returns final A record to client
```

**Recursive Resolution** — each server delegates fully to the next (rare in practice; most resolvers use iterative internally).

```
                 ┌─────────────────────────────────────────────┐
                 │           DNS ITERATIVE RESOLUTION           │
                 └─────────────────────────────────────────────┘

  Browser          Stub         Recursive        Root        TLD (.com)    Authoritative
  (Client)        Resolver      Resolver          NS             NS            NS
     │               │              │              │              │              │
     │─ query ──────>│              │              │              │              │
     │               │─ forward ───>│              │              │              │
     │               │              │─ query ─────>│              │              │
     │               │              │<─ referral ──│  (.com NS)   │              │
     │               │              │─ query ──────────────────>  │              │
     │               │              │<─ referral ─────────────────│ (auth NS)    │
     │               │              │─ query ────────────────────────────────>   │
     │               │              │<─ A record ────────────────────────────────│
     │               │<─ answer ───-│              │              │              │
     │<─ answer ─────│              │              │              │              │

  Total RTT (cold): ~120-200 ms (3 network hops to authoritative + processing)
  With caching (TTL hit at recursive resolver): ~1-5 ms
```

**DNS Record Types You Must Know:**
- `A` — IPv4 address
- `AAAA` — IPv6 address
- `CNAME` — canonical name alias (cannot coexist with other records at apex)
- `ALIAS/ANAME` — CNAME-like but resolves at the DNS server; safe at apex (used by Route 53)
- `NS` — name server delegation
- `SOA` — start of authority (serial, refresh, retry, expire, TTL)
- `SRV` — service location (used by Kubernetes, gRPC discovery)
- `TXT` — arbitrary text (SPF, DKIM, domain verification)

**TTL strategy:** Low TTL (60 s) for failover agility. High TTL (3600+ s) for performance. During a migration: lower TTL 48 h before, execute switch, raise TTL again.

---

### TCP vs UDP

| Dimension | TCP | UDP |
|---|---|---|
| Connection | 3-way handshake (SYN, SYN-ACK, ACK) | Connectionless |
| Reliability | Guaranteed delivery, ordering, retransmission | Best-effort, no ordering |
| Flow control | Sliding window (receiver-driven) | None |
| Congestion control | CUBIC, BBR, RENO | None (application handles) |
| Head-of-line blocking | Yes (stream level) | No |
| Latency | +1.5 RTT before first byte (cold) | ~0 RTT (just send) |
| Typical latency overhead | 30-100 ms extra on lossy links | Near zero |
| Use cases | HTTP, SSH, databases, file transfer | DNS, video streaming, gaming, QUIC |

**TCP 3-way handshake cost:** 1 RTT before data. TLS 1.2 adds 2 RTTs on top → 3 RTTs to first byte. TLS 1.3 reduces to 1 RTT (or 0-RTT on resumption).

**TCP slow start:** New connections start at cwnd=10 MSS (~14KB). Doubles each RTT until congestion. This is why a 1MB file over a fresh TCP connection to a far server feels slow — cwnd hasn't ramped up yet.

**BBR vs CUBIC:** CUBIC backs off multiplicatively on packet loss. BBR models bandwidth × RTT (BDP) and doesn't react to loss directly — better for long-fat pipes and lossy WiFi. YouTube and Google switched to BBR; reported 4% throughput gain globally, 14% on lossy links.

---

### HTTP/1.1 vs HTTP/2 vs HTTP/3

```
HTTP/1.1 (1997)                HTTP/2 (2015)                 HTTP/3 (2022)
────────────────               ────────────────              ────────────────
  TCP connection                TCP connection               QUIC (UDP-based)
  │                             │                             │
  │ req1 ──>                    │ stream 1 ──>                │ stream 1 ──>
  │ <── resp1                   │ stream 2 ──>                │ stream 2 ──>
  │ req2 ──>  (pipelining       │ stream 3 ──>                │ stream 3 ──>
  │ <── resp2  rarely used)     │ (multiplexed, 1 connection) │ (no HoL at transport)
  │ ...                         │                             │
  6 connections/domain          1 connection/server          1 connection/server
  head-of-line blocking         HoL at TCP layer             No HoL blocking
  text headers                  binary frames + HPACK         QPACK header compression
                                header compression            0-RTT connection setup
```

**HTTP/2 internals:**
- Binary framing layer: each message split into frames (HEADERS, DATA, SETTINGS, PUSH_PROMISE)
- Streams: bidirectional byte sequences, multiplexed over one TCP connection
- Stream weight + dependency: priority tree (rarely implemented well by servers)
- Server push: server can preemptively send resources — largely abandoned in practice (Chrome removed support 2022)
- HPACK: header compression using static table (61 entries) + dynamic table + Huffman encoding. Reduces header overhead from ~800 bytes to ~50 bytes on repeat requests

**HTTP/3 / QUIC internals:**
- QUIC runs over UDP; implements its own reliable, ordered delivery per stream
- Streams are independent: packet loss in stream 1 does NOT block stream 2 (eliminates TCP's HoL blocking)
- Connection ID instead of 4-tuple: survives IP changes (WiFi → LTE handoff without reconnect)
- 0-RTT resumption: client sends data in first packet for known servers
- Integrated TLS 1.3: no separate TLS handshake round trip
- Adoption: ~26% of web traffic as of 2024 (Cloudflare data)

**When NOT to use HTTP/2/3:** Internal microservice communication over a stable LAN — HTTP/1.1 pipelining overhead is negligible, and gRPC (which uses HTTP/2) may be more appropriate for typed contracts.

---

### WebSockets vs SSE vs Long-Polling

```
LONG-POLLING                SSE                         WebSocket
──────────────              ──────────────              ──────────────
Client ──GET──> Server      Client ──GET──> Server      Client ──Upgrade──> Server
        (waits)                    │                            │
        (30s timeout)              │ text/event-stream          │ full-duplex
<── response when ready            │ (chunked response)         │ binary or text
 ──GET──> Server again    <── event1                    <──── msg1
        (repeat)           <── event2                    ──── msg2 ──>
                           (auto-reconnect)              (any time)

Overhead per message:      ~0 bytes (connection open)   2-14 bytes (WS frame)
New HTTP request each time No (streaming response)      No
Server→Client only?        No (client polls)            Yes (one direction)
Browser support            Universal                    Universal (IE10+)
Load balancer sticky?      No (new conn each time)      Yes (must be sticky)
Firewall friendly?         Yes                          Sometimes blocked (ws://)
```

**Decision matrix:**
- Chat / gaming / collaborative editing → WebSocket (true bidirectional, low overhead)
- Live dashboard / notifications → SSE (server-push only, simpler, HTTP/2 compatible, auto-reconnect built-in)
- Compatibility with aggressive firewalls / proxies → Long-polling (pure HTTP, always works)
- SSE over HTTP/2: each SSE stream is just one HTTP/2 stream; you can have many per connection without the sticky-session problem WebSockets introduce

---

### CDN: Push vs Pull, Edge Caching

```
PULL CDN (most common: CloudFront, Fastly, Akamai)
─────────────────────────────────────────────────
  User (Tokyo)                   Origin (US-East)
      │                                │
      │── GET /image.jpg ──────> [Tokyo PoP]
      │                           │ MISS
      │                           │── GET /image.jpg ──────────> Origin
      │                           │<── 200 + Cache-Control: max-age=86400
      │<── 200 (cached) ──────────│
      │                           │ (subsequent requests: HIT, ~5ms vs ~150ms)

PUSH CDN
─────────────────────────────────────────────────
  You push content proactively to all PoPs.
  Storage is pre-allocated per GB.
  Use case: known static assets (OS updates, game patches, JS bundles on deploy)
  Downside: cache invalidation is your problem; stale content at every PoP
```

**Cache-Control directives:**
- `max-age=N` — cache for N seconds
- `s-maxage=N` — CDN-specific override (ignores max-age for shared caches)
- `stale-while-revalidate=N` — serve stale, revalidate async (excellent for latency)
- `stale-if-error=N` — serve stale if origin returns 5xx
- `immutable` — never revalidate (use with content-addressed filenames like `app.abc123.js`)
- `no-store` — never cache (PII, personalized responses)
- `vary: Accept-Encoding` — cache separate copies per encoding (gzip vs br)

**CDN latency math:**
- PoP to user (last mile): 5-30 ms depending on geography
- Without CDN (US origin → Tokyo user): ~150-200 ms RTT
- With CDN hit (Tokyo PoP): 5-15 ms
- Improvement: 10-15x for cacheable content

**Cache invalidation strategies:**
1. TTL expiry — simple, eventual consistency
2. Surrogate keys / cache tags — tag objects with logical keys (e.g., `product:123`), purge by tag on update (Fastly, Varnish)
3. URL versioning — `bundle.v2.js` (immutable caching, zero invalidation needed)
4. API-based purge — CDN provider API call on deploy (CloudFront CreateInvalidation — costs $0.005/1000 paths)

---

### TLS Handshake

**TLS 1.2 (2 RTTs):**
```
Client                                Server
  │── ClientHello (ciphers, random) ───>│
  │<── ServerHello + Certificate ───────│
  │<── ServerHelloDone ─────────────────│   (1 RTT)
  │── ClientKeyExchange (pre-master) ──>│
  │── ChangeCipherSpec + Finished ─────>│
  │<── ChangeCipherSpec + Finished ─────│   (2 RTTs)
  │── [Application Data] ─────────────>│
```

**TLS 1.3 (1 RTT, 0-RTT resumption):**
```
Client                                Server
  │── ClientHello + key_share ─────────>│
  │<── ServerHello + key_share ─────────│
  │<── {EncryptedExtensions, Cert, CV} ─│   (1 RTT — keys derived immediately)
  │── {Finished} ──────────────────────>│
  │── [Application Data] ─────────────>│

0-RTT (session resumption):
  │── ClientHello + early_data ─────────>│   (data in first packet)
  │<── ServerHello ... ──────────────────│
  Risk: replay attacks (safe only for idempotent requests)
```

**Certificate chain:** Leaf cert → Intermediate CA → Root CA. OCSP stapling: server attaches signed OCSP response to avoid client-side revocation round trip.

---

### REST vs gRPC vs GraphQL

| Dimension | REST | gRPC | GraphQL |
|---|---|---|---|
| Protocol | HTTP/1.1 or HTTP/2 | HTTP/2 (required) | HTTP/1.1 or HTTP/2 |
| Payload | JSON (text) | Protobuf (binary) | JSON |
| Schema | OpenAPI (optional) | .proto (required) | SDL (required) |
| Code generation | Optional | Built-in (protoc) | Optional |
| Serialization cost | High (JSON parse) | Low (binary decode) | High |
| Streaming | No (SSE workaround) | Native (4 modes) | Subscriptions (WS) |
| Caching | HTTP cache (GET) | Hard (POST-based) | Hard (POST-based) |
| Browser support | Native | grpc-web only | Native |
| Versioning | URL or header | Proto field numbers (backward compat) | Schema evolution |
| Best for | Public APIs, CRUD | Internal microservices, low latency | Client-driven queries, BFF |

**gRPC streaming modes:**
1. Unary — one request, one response (like REST)
2. Server streaming — one request, many responses (e.g., watch updates)
3. Client streaming — many requests, one response (e.g., upload chunks)
4. Bidirectional streaming — full duplex (e.g., chat, telemetry)

**gRPC performance:** Protobuf is 3-10x smaller than equivalent JSON and 5-7x faster to serialize/deserialize. For a service handling 100k RPS with 1KB average payload, this translates to ~100MB/s bandwidth savings and measurable CPU reduction.

**GraphQL N+1 problem:** A query for 100 posts with authors naively issues 100 author lookups. Fix: DataLoader (batch + deduplicate within a single tick — Facebook's pattern). Without DataLoader, GraphQL performance is often worse than REST.

---

## 3. Deep Dive

### HTTP/2 Head-of-Line Blocking

Even with HTTP/2 multiplexing, TCP's reliability guarantee means a single lost packet blocks ALL streams on that connection until retransmitted. On a 1% packet-loss link, HTTP/2 performs worse than HTTP/1.1 with 6 parallel connections because those 6 connections have statistically independent loss events.

This is precisely the problem QUIC solves: each stream has independent delivery, so stream 2 is not blocked waiting for a retransmit of stream 1's lost packet.

### QUIC Connection Migration

QUIC identifies connections by a 64-bit connection ID rather than the 4-tuple (src IP, src port, dst IP, dst port). When a mobile user switches from WiFi to LTE:
- TCP: connection terminates, new TCP + TLS handshake (1.5-3 RTTs of latency spike)
- QUIC: same connection ID, sends path validation probe, seamlessly migrates

### DNS Security (DNSSEC)

DNSSEC signs DNS records with asymmetric keys chained from the root. Each zone's public key is published as a DNSKEY record; the parent zone signs the child's key hash (DS record). Verifiers walk the chain from root to authoritative NS. Adds ~10-40% latency to uncached lookups (signature verification). DNSSEC adoption is ~30% of TLDs but significantly less at the leaf level.

---

## 4. Trade-offs

### Protocol Selection

```
Use REST when:
  - Public-facing API (browsers, third-party clients)
  - Team unfamiliar with gRPC tooling
  - Caching is important (GET semantics)
  - Simple CRUD with no streaming

Use gRPC when:
  - Internal microservice-to-microservice communication
  - High throughput / low latency critical
  - Strong typed contracts across multiple languages
  - Streaming required (bidirectional or server-push)
  - Teams already using proto for data contracts

Use GraphQL when:
  - Multiple clients with different data needs (mobile vs web)
  - BFF (Backend-for-Frontend) pattern
  - Rapidly evolving API where overfetch/underfetch is a pain
  - NOT when caching matters (hard without persisted queries)
  - NOT when N+1 is not solved (DataLoader must be in place)
```

### CDN Push vs Pull

| | Pull | Push |
|---|---|---|
| Setup effort | Low (point DNS to CDN) | High (upload pipeline) |
| Storage cost | Pay for cached data | Pay upfront per PoP |
| Cold miss latency | Origin RTT | Never misses |
| Cache invalidation | TTL or API purge | Manual push |
| Best for | Long-tail content, user uploads | Software updates, immutable bundles |

---

## 5. Numbers to Know

| Metric | Value | Context |
|---|---|---|
| DNS lookup (warm) | 1-5 ms | Cached at recursive resolver |
| DNS lookup (cold) | 100-200 ms | Full iterative resolution |
| DNS TTL typical | 300 s (5 min) | Balance failover vs perf |
| TCP handshake (same DC) | < 1 ms | Loopback / VPC |
| TCP handshake (US-EU) | ~70-90 ms | Cross-Atlantic RTT ~140 ms, so 1 RTT |
| TLS 1.2 overhead | +2 RTT on top of TCP | Total ~3 RTT to first byte |
| TLS 1.3 overhead | +1 RTT on top of TCP | 0-RTT on resumption |
| CDN cache HIT latency | 5-30 ms | Depends on PoP distance |
| CDN cache MISS latency | 150-300 ms | Origin RTT |
| CDN hit rate (typical) | 80-95% | Depends on TTL and content variety |
| HTTP/2 default stream limit | 100 concurrent streams | RFC default; Nginx default is 128 |
| HTTP/2 max header size | 4-8 KB | HPACK dynamic table limit |
| Protobuf vs JSON size | 3-10x smaller | Depends on field names / structure |
| gRPC serialization speedup | 5-7x vs JSON | CPU-bound serialization benchmarks |
| WebSocket frame overhead | 2-14 bytes/frame | vs 400-800 bytes for HTTP headers |
| QUIC 0-RTT | First packet carries data | For known server sessions |
| TCP slow start initial cwnd | 10 MSS ≈ 14 KB | Defined in RFC 6928 |
| BGP convergence | 30-180 seconds | Why DNS TTL matters during failover |
| Internet backbone RTT (US-EU) | 70-80 ms | Physical speed-of-light limit |
| US-Asia RTT | 130-160 ms | Undersea fiber routes |

---

## 6. Interview Tips

### What Interviewers Probe

1. **"Why did you choose HTTP/2 over gRPC?"** — They want you to distinguish the protocol from the framework. gRPC uses HTTP/2 but adds proto contracts, code generation, and streaming semantics. HTTP/2 alone gives you multiplexing; gRPC gives you the full typed RPC framework.

2. **"How does your CDN handle a deploy?"** — Weak answer: "we invalidate the cache." Strong answer: use content-addressed filenames (`bundle.[hash].js`) to make assets immutable + max-age=forever, and only invalidate the HTML entry point (tiny surface area). Eliminates invalidation storms on large deploys.

3. **"How do WebSockets scale?"** — They expect you to know: WebSockets are stateful (pinned to one server), so horizontal scaling requires sticky sessions (consistent hashing on connection ID) or a pub/sub backbone (Redis pub/sub, Kafka) so any server can receive a message and fan it out to the right connection.

4. **"Design a global low-latency API"** — Expected answer touches: CDN edge functions (Cloudflare Workers, Lambda@Edge) for cacheable responses; anycast routing; gRPC with protobuf internally; DNS-based geo-routing (Route 53 latency-based routing); keep-alive connections to avoid repeated TLS handshakes.

5. **"What happens when a DNS record changes?"** — Walk through: TTL expiry in caches, BGP propagation for anycast, OS-level DNS cache (nscd), browser DNS cache (1 min in Chrome). Practical implication: during failover, you can't assume traffic shifts in <5 min even with TTL=60 because OS/browser caches ignore TTL updates.

### Common Mistakes

- Saying "HTTP/2 is always faster than HTTP/1.1" — False on low-loss LAN; false when multiplexing gain doesn't offset HOL blocking on lossy links.
- Ignoring TLS handshake cost when claiming "REST is stateless and cheap" — cold connections have 2-3 RTT overhead before any data flows.
- Treating CDN as purely a performance tool — CDN also absorbs DDoS (absorb at edge before hitting origin), offloads bandwidth cost, and enables geographic compliance (geo-blocking).
- GraphQL without DataLoader — naive GraphQL causes N+1 queries and is often worse than REST in practice.
- Not mentioning sticky sessions for WebSockets — interviewers specifically look for this.

### Questions You'll Be Asked

- "Walk me through what happens when I type google.com and press Enter." (DNS → TCP → TLS → HTTP)
- "How would you reduce latency for users in Asia accessing your US-based service?"
- "REST vs gRPC — when would you choose each?"
- "How does HTTP/2 multiplexing work, and what are its limitations?"
- "How do you handle WebSocket scaling to 1 million concurrent connections?"
- "What's the difference between CDN push and pull? When would you use each?"
- "How does TLS 1.3 differ from TLS 1.2 in terms of handshake latency?"

---

## 7. Resources

- [High Performance Browser Networking — Ilya Grigorik (O'Reilly, free online)](https://hpbn.co/) — The canonical reference for HTTP/2, QUIC, WebSockets, and TLS with actual numbers
- [Cloudflare Blog — QUIC is now RFC 9000](https://blog.cloudflare.com/quic-version-1-is-live-on-cloudflare/) — Production QUIC data from one of the world's largest CDNs
- [Google: QUIC at Google (IETF)](https://www.chromium.org/quic/) — Original QUIC design rationale and performance data from Google's deployment
