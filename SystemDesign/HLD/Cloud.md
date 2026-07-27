# AWS for System Design Interviews

## What it is / Why it matters in interviews
AWS services are the de-facto vocabulary of system design at FAANG and most tech companies. Interviewers expect you to name specific services, explain trade-offs between them, and justify choices based on access patterns, scale, and cost. Vague answers like "I'd put this in the cloud" don't cut it at senior/staff level.

---

## Core Concepts

### Compute

#### EC2 vs Lambda vs ECS/EKS

| Dimension | EC2 | Lambda | ECS/EKS |
|---|---|---|---|
| State | Stateful or stateless | Stateless (ephemeral) | Stateless (containers) |
| Startup | Minutes (AMI boot) | Cold start 100–500ms | Seconds (container pull) |
| Duration | Unlimited | Max 15 minutes | Unlimited |
| Scaling | Manual / ASG (minutes) | Auto, per-request | Auto (task/pod level) |
| Cost model | Per hour (on-demand/spot/reserved) | Per invocation + GB-s | Per task/hour + EC2 underneath |
| Best for | Long-running workloads, persistent daemons | Event-driven, infrequent bursts | Microservices, portability |
| Avoid when | Short bursty jobs (cost inefficient) | Long jobs, VPC cold start sensitivity | Simple one-off scripts |

**Lambda limits to memorize:**
- Timeout: **15 minutes max**
- Memory: **128 MB – 10 GB** (CPU scales proportionally)
- Concurrency: **1,000 default** per region (soft limit, can request increase)
- Deployment package: 50 MB zipped, 250 MB unzipped
- Cold start: **100–500ms** (JVM/Python worse; SnapStart helps Java)
- Ephemeral storage (/tmp): **512 MB – 10 GB**

**Cold start anatomy:**
```
[Request arrives]
      |
      v
[Find/provision container]  <-- Cold start cost (~100-500ms)
      |
      v
[Load runtime + code]       <-- Warm start skips to here
      |
      v
[Execute handler]
```

**ECS vs EKS:** ECS is AWS-native and simpler to operate. EKS is Kubernetes — portable across clouds, richer ecosystem, higher operational overhead. Pick EKS if you need multi-cloud portability or already run Kubernetes.

---

### Storage

#### S3

**Use cases:** Static assets, data lake raw storage, backups, event archives, ML training data, log archives.

**Consistency model:** Strong read-after-write consistency for all operations since December 2020 (previously eventually consistent for overwrite PUTs).

**Key features:**
- **Presigned URLs** — generate a time-limited URL for private objects without exposing credentials. Used for direct browser uploads/downloads bypassing your servers.
- **S3 Select** — run SQL queries against CSV/JSON/Parquet objects. Reduces data transfer vs loading the full object.
- **Lifecycle policies** — auto-transition objects: S3 Standard → S3-IA (30 days) → S3 Glacier (90 days) → delete.
- **Multipart upload** — for objects > 100 MB. Parallel upload parts, then assemble.
- **S3 Transfer Acceleration** — routes uploads through CloudFront edge locations.

```
S3 Storage Classes (cost vs access latency):
  Standard       → frequent access, ms latency
  Standard-IA    → infrequent access, ms latency, retrieval fee
  One Zone-IA    → same as IA, single AZ (cheaper, less durable)
  Glacier Instant→ archive, ms retrieval
  Glacier Flex   → archive, minutes-hours retrieval
  Glacier Deep   → archive, 12 hours retrieval (cheapest)
```

#### EBS vs EFS vs S3

| | EBS | EFS | S3 |
|---|---|---|---|
| Type | Block storage | File storage (NFS) | Object storage |
| Protocol | Block device | NFS/POSIX | HTTP REST API |
| Attach to | Single EC2 (usually) | Multiple EC2 concurrently | Any service, browser |
| Latency | Sub-ms | Low ms | Low ms (network) |
| Max size | 64 TB per volume | Scales automatically (PB) | Unlimited |
| Use case | OS disk, DB data files | Shared config, CMS, HPC | Media, backups, data lake |
| Durability | Single AZ (snapshot to S3 for DR) | Multi-AZ | 11 nines (99.999999999%) |

---

#### DynamoDB

**Core model:** Key-value and document store. Partition key (PK) required; sort key (SK) optional. Single digit millisecond latency at any scale.

**Partition key design:** The PK determines which physical partition stores the item. Bad PK design creates hot partitions.
```
Hot partition (bad):          Even distribution (good):
PK = "status"                 PK = "userId"
  "active" → 95% of traffic     user_1 → ~equal load
  "inactive" → 5%               user_2 → ~equal load
```

**Indexes:**
- **GSI (Global Secondary Index)** — different PK/SK than base table. Separate read/write capacity. Async replication (eventual consistency).
- **LSI (Local Secondary Index)** — same PK, different SK. Must be defined at table creation. Strongly consistent reads possible.

**Capacity modes:**
| | On-Demand | Provisioned |
|---|---|---|
| When | Unpredictable spikes | Stable, predictable traffic |
| Cost | Pay per request (higher unit cost) | Pay for reserved RCU/WCU (lower unit cost) |
| Scaling | Instant | Auto-scaling (minutes) |

**Single-table design:** Store multiple entity types in one table, differentiated by PK/SK patterns (e.g., `USER#123`, `ORDER#456`). Reduces round trips. Required for complex relational queries — use GSIs to support different access patterns.

**DAX (DynamoDB Accelerator):** In-memory cache in front of DynamoDB. Read latency drops from single-digit ms to **microseconds**. Write-through cache. No code changes needed (API compatible). Use when you need read-heavy caching and can tolerate microsecond staleness.

---

#### RDS vs DynamoDB — Decision Framework

```
Start here:
         Is the data relational?
          /              \
        Yes               No
         |                 |
   Do you need         Key-value or
   ACID transactions?   document access?
     /      \              |
   Yes        No         DynamoDB
    |          |
   RDS      Consider
  (MySQL,   DynamoDB +
  Postgres)  transactions
             (for some cases)

Also ask:
- Do queries use ad-hoc JOINs?  → RDS
- Do you need full-text search?  → RDS + pg_trgm or Elasticsearch
- Need >100k TPS read?           → DynamoDB
- Need flexible schema?          → DynamoDB
- Complex reporting/analytics?   → RDS or Redshift
```

| Factor | RDS | DynamoDB |
|---|---|---|
| Schema | Fixed, relational | Flexible, schemaless |
| Query flexibility | High (SQL, JOINs, GROUP BY) | Low (access pattern must match index) |
| Max throughput | ~tens of thousands TPS | Millions of TPS |
| Consistency | Strong (ACID) | Strong or eventual (configurable) |
| Scaling | Vertical + read replicas | Horizontal, automatic |
| Operations | Manage instance size, Multi-AZ | Fully managed, serverless |

---

### Messaging

#### SQS vs SNS vs EventBridge

```
SNS (Pub/Sub fanout):
  Publisher → Topic → [SQS Queue A]
                    → [Lambda B]
                    → [Email C]

SQS (Queue / decoupling):
  Producer → Queue → Consumer (polls)

EventBridge (Event routing):
  Source → Event Bus → Rules → [Lambda]
                             → [SQS]
                             → [Step Functions]
```

| | SQS | SNS | EventBridge |
|---|---|---|---|
| Pattern | Queue (point-to-point) | Pub/Sub (fanout) | Event routing (rules-based) |
| Consumers | One consumer per message | All subscribers get it | Matched targets only |
| Retention | Up to 14 days | No retention | No retention |
| Ordering | FIFO queue (optional) | No guaranteed order | No guaranteed order |
| Best for | Task queues, decoupling | Notifications, fanout | Cross-service events, SaaS integrations |

**SQS important details:**
- **Visibility timeout** — when a consumer receives a message, it's hidden from other consumers for N seconds (default 30s). If not deleted in time, it becomes visible again (redelivery). Set to > your expected processing time.
- **Dead-letter queue (DLQ)** — after maxReceiveCount failures, messages move to DLQ. Essential for diagnosing poison-pill messages.
- **Long polling** — consumer waits up to 20 seconds for a message. Reduces empty responses and cost vs short polling.
- **Standard vs FIFO:** Standard = at-least-once delivery, best-effort ordering, ~unlimited TPS. FIFO = exactly-once, strict ordering, max 300 TPS (3000 with batching).

---

### Networking

#### API Gateway

| Type | Use case | Key feature |
|---|---|---|
| REST API | Full-featured REST APIs | Caching, request validation, usage plans |
| HTTP API | Low-latency proxies, Lambda | 70% cheaper, faster, limited features |
| WebSocket API | Real-time bidirectional (chat, games) | Manages connection state |

- **Throttling:** Default 10,000 RPS per region, 5,000 burst. Can set per-route limits.
- **Caching:** REST API only. Cache TTL 0–3600s, cache key includes headers/query params.

#### CloudFront

CDN with 400+ edge locations globally. Caches content close to users.

```
User (Sydney)
     |
     v
CloudFront Edge (Sydney)  ← Cache HIT: serves in <10ms
     |                    ← Cache MISS: fetches from origin
     v
Origin (us-east-1)
```

- **Origins:** S3, ALB, EC2, API Gateway, custom HTTP.
- **Cache behaviors:** Route by path pattern (e.g., `/api/*` no cache, `/static/*` cache 1 day).
- **Lambda@Edge:** Run Node.js/Python at edge locations. Use for auth checks, header manipulation, A/B testing. Latency-sensitive operations that should run near the user.
- **OAC (Origin Access Control):** Restrict S3 bucket access to CloudFront only.

#### VPC Networking (Conceptual)

```
VPC (10.0.0.0/16)
├── Public Subnet (10.0.1.0/24)
│   ├── Load Balancer
│   └── NAT Gateway
└── Private Subnet (10.0.2.0/24)
    ├── EC2 / ECS tasks
    └── RDS (isolated from internet)
```

- **Security Groups:** Stateful firewall at the instance level. Allow rules only. Return traffic allowed automatically.
- **NACLs:** Stateless firewall at the subnet level. Allow and deny rules. Both inbound and outbound rules needed. Applied in order.
- Rule of thumb: use Security Groups for most access control. NACLs for subnet-level blocking (e.g., block an IP range).

#### Route 53 Routing Policies

| Policy | When to use |
|---|---|
| Simple | Single resource, no health checks |
| Weighted | A/B testing, gradual traffic shifting (canary deploy) |
| Latency | Route to lowest-latency region |
| Failover | Active-passive DR — primary fails → Route 53 redirects to secondary |
| Geolocation | Compliance (EU data stays in EU), localization |
| Geoproximity | Route by physical distance + bias |
| Multivalue | Basic load balancing with health checks (not a replacement for LB) |

---

## Decision Framework

**When to pick Lambda over EC2:**
- Processing time < 15 minutes
- Event-driven (S3 event, SQS message, API request)
- Traffic is bursty or unpredictable
- You want zero ops overhead

**When to pick ECS/EKS over Lambda:**
- Long-running jobs (ML inference, batch)
- Need to run arbitrary container images
- Warm start performance is critical
- Need sidecar pattern (service mesh, log agent)

**When to pick DynamoDB over RDS:**
- High throughput (>10k TPS)
- Simple access patterns known upfront
- Infinite scale without sharding concerns
- Semi-structured or variable schema data

**When to pick SQS over direct service call:**
- Consumer processes slower than producer
- Need retry/DLQ for failures
- Want to decouple producers from consumers
- Need to buffer traffic spikes

---

## Numbers to Know

| Service | Limit / Number |
|---|---|
| Lambda timeout | 15 minutes |
| Lambda memory | 10 GB max |
| Lambda concurrency | 1,000 default (soft limit) |
| Lambda cold start | 100–500ms (JVM can be 1–2s) |
| S3 object size | 5 TB max |
| S3 multipart threshold | 100 MB (recommended) |
| S3 durability | 11 nines (99.999999999%) |
| DynamoDB item size | 400 KB max |
| DynamoDB partition throughput | 3,000 RCU or 1,000 WCU |
| SQS message size | 256 KB |
| SQS retention | 14 days max |
| SQS visibility timeout | 30s default, 12 hours max |
| SQS FIFO throughput | 300 TPS (3,000 with batching) |
| API Gateway timeout | 29 seconds max |
| API Gateway throttle | 10,000 RPS default |
| CloudFront edge locations | 400+ globally |

---

## How It Shows Up in System Design

**URL Shortener (like bit.ly):**
- DynamoDB: short_code → long_url mapping (PK = short code, simple key-value)
- CloudFront: cache redirect responses at edge
- Lambda or EC2 behind API Gateway for redirect logic

**Image Processing Pipeline:**
- S3 for raw uploads (presigned URL for direct browser upload)
- S3 event → SQS → Lambda/ECS for async resizing
- CloudFront for serving processed images

**Ride-sharing / Real-time app:**
- API Gateway WebSocket for driver location updates
- DynamoDB for trip state (hot reads/writes)
- SQS/SNS for decoupled notifications (email, push)

**E-commerce:**
- RDS (Postgres) for orders/inventory (ACID transactions)
- ElastiCache (Redis) in front of RDS for product catalog reads
- SQS for order processing pipeline (decouple checkout from fulfillment)
- DynamoDB for shopping cart (user-scoped, flexible schema, high write)

---

## Interview Tips

**What interviewers probe:**
1. "Why DynamoDB and not RDS here?" — they want to hear access patterns, not just "it scales."
2. "What happens if the Lambda times out?" — error handling, DLQ, idempotency.
3. "How would you handle SQS message redelivery?" — idempotent consumers, DLQ.
4. "What's the risk of using a single S3 prefix?" — prefix throttle limit (3,500 PUT/5,500 GET per prefix per second).
5. "How do you secure your S3 bucket?" — OAC with CloudFront, bucket policy, no public access.

**Common mistakes:**
- Using RDS for everything (ignoring scale ceilings)
- Ignoring Lambda cold starts in latency-sensitive paths
- Not mentioning DLQ when adding SQS to a design
- Using public S3 URLs instead of presigned URLs or CloudFront
- Forgetting that SQS FIFO has a throughput ceiling (300 TPS)
- Not mentioning caching at any layer (ElastiCache, DAX, CloudFront)

**How to bring up AWS naturally:**
> "For the user session store, I'd use ElastiCache for Redis — low latency reads, built-in TTL. For the user profile data, DynamoDB makes sense because our access pattern is always by userId and we need horizontal scale at launch — we can start on-demand capacity and switch to provisioned once traffic stabilizes."

Name the service, justify with the access pattern, mention the operational mode. That's what senior-level looks like.
