# Observability in System Design Interviews

## What it is / Why it matters in interviews
Observability is tested at senior/staff level as the answer to "how do you know your system is working?" Interviewers probe whether you've operated systems in production — not just built them. A candidate who designs a distributed system but can't say how they'd debug it under load or detect a failure is missing a dimension. The three-pillars model (logs, metrics, traces) is the framework interviewers expect.

---

## Core Concepts

### The Three Pillars

| Pillar | Question it answers | What it can't answer |
|---|---|---|
| Logs | What happened on this specific request? What error occurred? | Aggregate trends, cross-service latency |
| Metrics | Is the system healthy right now? What are the trends? | Why a specific request failed |
| Traces | Why is this request slow? Which service in the chain is the bottleneck? | Aggregate trends (without aggregation layer) |

**ASCII Diagram — Which pillar catches which failure:**

```
Incoming request flows:   Client → API GW → ServiceA → ServiceB → DB

Scenario 1: DB is slow
  Logs:    ServiceB logs "DB query took 8000ms"  → helps, but you find it after the fact
  Metrics: ServiceB P99 latency spikes           → ALERT fires, you know something's wrong
  Traces:  Span for DB query is red/long         → you PINPOINT it immediately

Scenario 2: ServiceA throws NullPointerException on specific input
  Logs:    ERROR log with stack trace             → ESSENTIAL. Trace ID links to the request.
  Metrics: Error rate counter ticks up            → tells you it's happening, not why
  Traces:  Span for ServiceA shows error          → confirms where, not the root cause

Scenario 3: Memory leak in ServiceB (gradual degradation over 6 hours)
  Logs:    Unhelpful unless you log memory stats  → misses this
  Metrics: ServiceB heap usage gauge trends up    → CATCHES this. Alert before OOM.
  Traces:  Individual requests look fine          → misses the trend

Scenario 4: ServiceA → ServiceB network is dropping 5% of packets
  Logs:    Scattered timeout errors               → hard to correlate
  Metrics: ServiceB error rate 5%, ServiceA error rate 5% → visible but cause unclear
  Traces:  HTTP span for A→B call shows retries   → PINPOINTS the network hop
```

The key insight: **you need all three**. None is sufficient alone.

---

## Logging

### Structured vs Unstructured Logging

**Unstructured (bad at scale):**
```
2024-01-15 10:32:01 ERROR Failed to process order 12345 for user 67890: timeout after 5000ms
```
Machine-parseable only with fragile regex. Hard to aggregate, filter, or alert on.

**Structured (JSON — good):**
```json
{
  "timestamp": "2024-01-15T10:32:01Z",
  "level": "ERROR",
  "service": "order-service",
  "trace_id": "abc123def456",
  "user_id": "67890",
  "order_id": "12345",
  "event": "order_processing_failed",
  "error": "timeout",
  "duration_ms": 5000
}
```
Every field is queryable. You can `filter by order_id`, `group by error`, `alert on count(level=ERROR) > 100/min`.

### Log Levels

| Level | When to log | Example |
|---|---|---|
| DEBUG | Detailed diagnostic info — disabled in prod | Variable values, loop iterations |
| INFO | Normal operations — key business events | "Order 123 placed", "User logged in" |
| WARN | Something unexpected but recoverable | "Retry attempt 2/3", "Cache miss rate >80%" |
| ERROR | Operation failed, requires investigation | "DB connection failed", "Payment timeout" |
| FATAL/CRITICAL | System cannot continue | "Unable to bind port", "Config missing" |

**Rule:** Log enough to reconstruct a request's lifecycle. Don't log passwords, PII, or tokens. Don't log in tight loops (use sampling or aggregation).

### Correlation ID / Trace ID Threading

**The problem:**
```
ServiceA log: "Processing request" (no context)
ServiceB log: "DB query failed" (no context)
How do you know these are the same request?
```

**The solution — inject a trace/correlation ID at the entry point and propagate it:**
```
Client → API Gateway
          | generates X-Trace-ID: abc123
          |
          v
       ServiceA
          | logs: { trace_id: "abc123", event: "order_received" }
          | passes X-Trace-ID header to ServiceB
          v
       ServiceB
          | logs: { trace_id: "abc123", event: "db_query_start" }
          | logs: { trace_id: "abc123", event: "db_query_failed", error: "timeout" }
```

Now you can filter all logs by `trace_id = "abc123"` and reconstruct the full request lifecycle across services.

**Implementation options:**
- W3C `traceparent` header (standard, used by OpenTelemetry)
- AWS X-Ray `X-Amzn-Trace-Id` header
- Custom `X-Correlation-Id` header (common in older systems)
- Thread-local storage / async context propagation within a service

### Log Aggregation Stacks

| Stack | Components | Best for |
|---|---|---|
| ELK | Elasticsearch + Logstash + Kibana | Full-text search, complex queries, large scale |
| EFK | Elasticsearch + Fluentd + Kibana | Kubernetes-native (Fluentd is the k8s standard) |
| Loki + Grafana | Loki (label-indexed log store) + Grafana | Cost-efficient, tight Prometheus/Grafana integration |
| CloudWatch Logs | AWS-managed | AWS-native, no infra to manage, costlier at scale |

**ELK vs Loki:**
- ELK indexes full log content → powerful queries, expensive storage
- Loki only indexes labels (service, level, trace_id) → cheaper, queries can be slower for ad-hoc searches
- Loki is the right choice if you already use Prometheus/Grafana and want a unified stack

### Log Sampling

At 100k RPS, logging every request costs ~$10k+/month in storage/ingestion. Options:
- **Head-based sampling:** Log X% of requests (e.g., 1%) — simple, but misses rare errors
- **Tail-based sampling:** Buffer requests, log 100% of errors + sample of successes — complex but ideal
- **Dynamic sampling:** Increase log rate when error rate spikes

---

## Metrics

### Metric Types

| Type | Definition | Examples |
|---|---|---|
| Counter | Monotonically increasing count | `http_requests_total`, `errors_total`, `bytes_sent_total` |
| Gauge | Current value (can go up or down) | `memory_usage_bytes`, `active_connections`, `queue_depth` |
| Histogram | Distribution of observations (with buckets) | `request_duration_seconds{bucket}` — P50/P95/P99 |
| Summary | Pre-calculated quantiles on the client | `rpc_duration_seconds{quantile="0.99"}` — less flexible than histogram |

**Why histogram over summary?** Histograms aggregate across replicas (e.g., 10 pods). Summaries cannot be meaningfully aggregated. Use histograms for latency at scale.

### RED Method vs USE Method

**RED (for services — user-facing behavior):**
- **R**ate — requests per second
- **E**rrors — error rate (% or count)
- **D**uration — latency distribution (P50, P95, P99)

**USE (for resources — infra layer):**
- **U**tilization — % time the resource is busy
- **S**aturation — work waiting in queue (run queue, disk queue)
- **E**rrors — hardware errors, disk errors

```
Rule of thumb:
  API/service dashboards → RED method
  EC2/DB/network infra   → USE method
```

### Prometheus Scrape Model

```
Prometheus Server
    |
    |-- HTTP GET /metrics --> ServiceA (port 9090/metrics)
    |-- HTTP GET /metrics --> ServiceB (port 9090/metrics)
    |-- HTTP GET /metrics --> Node Exporter (host metrics)

Pull model: Prometheus initiates the scrape (default every 15s)
Push model: Pushgateway lets batch jobs push metrics before they exit

Pull advantages: Prometheus knows if a target is down (scrape fails)
Push advantages: Short-lived jobs (cron, Lambda) can't be scraped
```

### Grafana Dashboard — What to Put on a Service Dashboard

**Minimum viable dashboard for any service:**
```
Row 1: Golden Signals
  - Request rate (RPS)          [graph - counter rate]
  - Error rate (% or count)     [graph + threshold alert]
  - P50 / P95 / P99 latency    [graph - histogram]
  - Saturation (queue depth)    [graph]

Row 2: Dependencies
  - Downstream service error rates
  - DB query latency P99
  - Cache hit rate

Row 3: Resources (USE)
  - CPU utilization
  - Memory usage (gauge vs limit)
  - Network in/out

Row 4: Business metrics (optional but impressive)
  - Orders processed/min
  - Active users
```

### Alerting Philosophy

**Alert on symptoms, not causes:**

```
BAD alerts (cause-based):      GOOD alerts (symptom-based):
- CPU > 80%                    - Error rate > 1% for 5 min
- Memory > 85%                 - P99 latency > 2000ms for 10 min
- Disk > 70%                   - Availability < 99.9% over 30 min

Why? High CPU might not affect users. Error rate ALWAYS affects users.
```

**Alert categories:**
- **Page (PagerDuty/OpsGenie)** — customer impact NOW. Error rate. Latency. Availability.
- **Ticket (Jira/Slack)** — investigate soon. Disk trending up. Memory creep. Cert expiring in 14 days.
- **Informational** — FYI only. Deployment happened. Traffic spike absorbed.

---

## Distributed Tracing

### How Spans and Traces Work

```
Trace ID: abc-123 (single request end-to-end)

[API Gateway      ] |----- Span 1: 250ms total ------|
  [ServiceA       ]   |--- Span 2: 220ms ---|
    [ServiceB     ]     |- Span 3: 180ms --|
      [DB query   ]       |Span 4: 150ms|   ← bottleneck

Trace = tree of spans
Span = a unit of work (has: trace_id, span_id, parent_span_id, start, duration, tags, status)

trace_id propagates via HTTP headers (W3C traceparent standard):
  traceparent: 00-{trace-id}-{parent-span-id}-{flags}
```

**What traces answer:**
- Where did this request spend its 2 seconds? (service breakdown)
- Which dependency is the bottleneck?
- Did retries happen? How many?
- What was the DB query / URL called?

### Sampling Strategies

**Head-based sampling (decide at trace entry):**
- Decide at the first span whether to sample this trace (e.g., 1% of all requests)
- Pros: simple, low overhead
- Cons: you miss rare errors (a 0.01% error rate might have 0 sampled traces)

**Tail-based sampling (decide after the fact):**
- Buffer all spans, decide to keep the trace after seeing the outcome
- Keep 100% of error traces, 100% of slow traces (P99+), 1% of normal traces
- Pros: never miss an interesting trace
- Cons: higher memory/buffering overhead, needs a collector (OpenTelemetry Collector)

```
Head sampling:                    Tail sampling:
Entry → [50/50 coin flip] →       Entry → buffer spans →
  keep: send spans downstream       Outcome known: error? keep 100%
  drop: discard immediately                         slow? keep 100%
                                                    normal? keep 1%
```

### Tracing Tools

| Tool | Hosted by | Best for |
|---|---|---|
| Jaeger | Self-hosted (CNCF) | Open-source, Kubernetes-native |
| Zipkin | Self-hosted | Simple, mature, lightweight |
| AWS X-Ray | AWS managed | AWS-native, integrates with Lambda/ECS |
| Datadog APM | SaaS | Full observability platform, easy setup |
| Tempo + Grafana | Self-hosted | Integrates with Loki + Prometheus (unified) |

### OpenTelemetry (OTel) — Why It Matters

OpenTelemetry is the vendor-neutral standard for telemetry instrumentation. It unifies the previously fragmented landscape (each tool had its own SDK).

```
Before OTel:
  App code ← Datadog SDK
  App code ← Jaeger SDK
  App code ← X-Ray SDK
  (switching vendors requires rewriting instrumentation)

With OTel:
  App code ← OTel SDK (one standard API)
               |
               v
        OTel Collector
               |
        ┌──────┴──────┐
        v             v
    Jaeger        Datadog
    (or switch     (or switch
     later)         later)
```

OTel covers logs, metrics, AND traces under one standard. AWS, Google, Datadog, and Honeycomb all accept OTel data. Mentioning OTel in an interview signals awareness of the modern observability ecosystem.

---

## SLOs, SLIs, SLAs

### Definitions with Concrete Examples

**SLI (Service Level Indicator)** — the actual measurement.
A specific metric you track.
```
Examples:
  - % of HTTP requests returning 2xx in a rolling 30-day window
  - P99 latency of the checkout API
  - % of successful payment transactions
```

**SLO (Service Level Objective)** — the internal target you set for an SLI.
What you commit to within your engineering team.
```
Examples:
  - 99.9% of requests return 2xx (allows 43.8 min/month downtime)
  - P99 latency < 500ms for 95% of the rolling 30-day window
  - 99.99% of payments succeed
```

**SLA (Service Level Agreement)** — the contractual commitment to customers.
Usually looser than your SLO (you need headroom).
```
Examples:
  - 99.9% monthly uptime (AWS EC2 SLA)
  - If uptime < 99.9%, customer receives 10% service credit
  - If uptime < 95%, customer receives 30% service credit
```

```
Relationship:
  SLI (measured) ≥ SLO (internal target) > SLA (external contract)

  SLO = 99.95% availability
  SLA = 99.9% availability
  Buffer = 0.05% — gives you room to breach SLO before breaching SLA
```

### Error Budget

**Concept (Google SRE):**
```
SLO = 99.9% availability
Error budget = 100% - 99.9% = 0.1% of requests can fail

In a 30-day month:
  Total minutes = 43,200
  Allowed downtime = 43.2 minutes

If you've used 40 of those 43.2 minutes this month:
  → Feature launches pause (risk of pushing the budget)
  → On-call team investigates reliability
  → SRE team has veto on risky deployments

If budget is fully intact:
  → Accelerate feature development
  → Take more risks (fewer reliability constraints)
```

Error budget aligns incentives: product team wants to ship features, SRE team wants reliability. Both teams watch the same budget.

### Common SLIs by Service Type

| Service Type | Typical SLIs |
|---|---|
| Web/API service | Availability (% 2xx), P99 latency |
| Storage service | Durability (% objects readable), write latency |
| Batch pipeline | % jobs completing within SLA window, data freshness |
| Message queue | Message delivery rate, delivery latency |
| Search | Query success rate, P95 latency |

**Latency targets by tier:**
- Interactive UI (page load): P50 < 200ms, P99 < 1s
- API (external): P50 < 100ms, P99 < 500ms
- Internal RPC: P50 < 10ms, P99 < 100ms
- Batch job: minutes to hours (SLA = completion within window)

---

## How Observability Shows Up in System Design

### Model Answer: "How would you know if this system is healthy?"

When an interviewer asks this about your design, hit these points in order:

1. **Metrics / alerting:** "I'd expose RED metrics from each service — request rate, error rate, P99 latency. Prometheus scrapes them every 15 seconds. Grafana dashboards show the golden signals. Alerts fire on error rate > 1% for 5 minutes or P99 > 2 seconds — both paged to on-call."

2. **Traces:** "For distributed latency debugging, every request gets a trace ID via OpenTelemetry. Jaeger (or Datadog APM) shows the span breakdown — I can immediately see if the DB is slow or if ServiceB is the bottleneck."

3. **Logs:** "Structured JSON logs from every service, correlated by trace ID. ELK stack or CloudWatch for aggregation. I'd alert on ERROR log rate anomalies as a secondary signal."

4. **SLOs:** "I'd define SLOs for the critical path — 99.9% availability and P99 < 500ms. Error budget is tracked monthly. If we're burning budget fast, releases pause until reliability recovers."

### Runbook Reference: What to Look at When an Alert Fires

```
Alert: "checkout-service P99 latency > 2000ms"

Step 1: Grafana → checkout-service dashboard
  - Is error rate also spiking? → might be a failure, not just slowness
  - Is request rate normal? → if 10x spike, might be traffic (scale up)
  - Which dependency latency is elevated?

Step 2: Trace (Jaeger / Datadog APM)
  - Find a slow trace from the alert window
  - Which span is the longest? DB? Payment API? Auth service?

Step 3: Logs (ELK / CloudWatch)
  - Filter by service=checkout, level=ERROR, time=alert window
  - Filter by trace_id from the slow trace found in step 2
  - Any DB connection pool exhaustion? Any timeout errors?

Step 4: Metrics → dependencies
  - DB metrics: query latency, connection pool utilization
  - Cache: hit rate dropped? → more DB pressure
  - Downstream: payment-service error rate?

Step 5: Infrastructure
  - CPU / memory of checkout-service pods
  - Was there a deployment in the last hour? (check deployment events in Grafana)
```

---

## Numbers to Know

| Item | Value |
|---|---|
| Prometheus scrape interval | 15 seconds default |
| Log retention (hot tier) | 7–30 days (warm: 90 days, cold: 1 year+) |
| Typical CloudWatch log cost | ~$0.50/GB ingestion + $0.03/GB storage |
| Log sampling rate at 100k RPS | 1–10% (full for errors) |
| P99 latency target (API) | < 500ms (interactive), < 100ms (internal RPC) |
| P99 latency target (page load) | < 1s |
| Typical SLO for API availability | 99.9% – 99.99% |
| 99.9% SLO — allowed downtime/month | 43.8 minutes |
| 99.99% SLO — allowed downtime/month | 4.38 minutes |
| 99.999% SLO — allowed downtime/month | 26 seconds |
| Trace ID propagation standard | W3C `traceparent` header |
| OTel collector throughput | ~10k–100k spans/sec per instance |
| Typical histogram bucket count | 8–12 buckets |

---

## Interview Tips

**What interviewers probe:**
1. "How would you debug a latency spike at 3am?" — they want trace-first, then logs, then metrics for trend.
2. "What's your alerting strategy?" — want symptom-based, not cause-based.
3. "How do you handle observability at scale?" — log sampling, metric aggregation, tail-based trace sampling.
4. "What's an SLO and how does it drive engineering decisions?" — error budget concept.
5. "How would you know if your service is degraded but not fully down?" — P99 tail latency, error rate < 100%.

**Common mistakes:**
- Mentioning "we'll look at logs" without any structure (no structured logging, no correlation IDs)
- Alerting on CPU/memory instead of latency/error rate
- Not mentioning trace IDs in log records (impossible to correlate otherwise)
- Confusing SLI/SLO/SLA definitions (mixing up measurement vs target vs contract)
- Only instrumenting the happy path (no metrics on retries, cache misses, DLQ depth)
- Treating observability as an afterthought — senior engineers say "I'd instrument this from day one"

**How to bring up observability naturally:**
> "Before I wrap up the design, let me touch on observability. Each service emits RED metrics — request rate, error rate, P99 latency — scraped by Prometheus. I'd define an SLO of 99.9% availability and P99 < 500ms for the user-facing path. Every request carries a trace ID via the W3C traceparent header, so when an alert fires on latency, on-call can pull the trace in Jaeger and see exactly which hop is slow without grep'ing through logs. Structured JSON logs correlated by trace ID feed into our ELK stack for detailed debugging."

That's a complete, senior-level answer in under 60 seconds.
