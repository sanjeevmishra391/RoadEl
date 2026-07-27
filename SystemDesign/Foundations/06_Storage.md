# Storage Systems

## 1. What It Is

Storage systems in distributed architecture span three fundamentally different access models: object storage (flat namespace, key-value semantics, HTTP API), block storage (raw disk abstraction presented to an OS, random byte-level I/O), and file storage (POSIX filesystem, directory hierarchy, shared access). Choosing the wrong one for a workload is a common and expensive mistake — the decision determines your latency floor, cost profile, consistency guarantees, and operational complexity.

---

## 2. How It Works

### Storage Type Decision Matrix

```
                    ┌─────────────────────────────────────────────────────────────┐
                    │                    STORAGE DECISION TREE                    │
                    └──────────────────────────┬──────────────────────────────────┘
                                               │
              ┌────────────────────────────────┼────────────────────────────────┐
              ▼                                ▼                                ▼
    "Store arbitrary files,         "OS needs a raw disk,          "Multiple machines need
     images, videos, backups"        database, VM image"            a shared filesystem"
              │                                │                                │
              ▼                                ▼                                ▼
    ┌──────────────────┐           ┌──────────────────┐           ┌──────────────────┐
    │  OBJECT STORAGE  │           │  BLOCK STORAGE   │           │  FILE STORAGE    │
    │  S3, GCS, Azure  │           │  EBS, EBS gp3    │           │  NFS, EFS, CIFS  │
    │  Blob Storage    │           │  Local NVMe      │           │                  │
    └──────────────────┘           └──────────────────┘           └──────────────────┘
    - HTTP GET/PUT API             - iSCSI / local mount          - POSIX semantics
    - Flat namespace               - Random read/write            - fsync, rename, stat
    - Immutable objects            - Low latency (sub-ms)         - Multi-client access
    - Cheap at scale               - Attached to one instance     - Higher cost
    - Eventual→Strong              - Not shared by default        - Lower throughput
      consistency
```

### Access Semantics Comparison

```
┌──────────────────┬─────────────────────┬──────────────────────┬─────────────────────┐
│                  │   Object (S3)       │   Block (EBS gp3)    │   File (EFS/NFS)    │
├──────────────────┼─────────────────────┼──────────────────────┼─────────────────────┤
│ Access model     │ HTTP REST (PUT/GET) │ POSIX read/write     │ POSIX filesystem    │
│ Granularity      │ Whole object        │ Byte-range (512B–4K) │ File / byte-range   │
│ Random I/O       │ Byte-range reads    │ Excellent            │ Good                │
│                  │ (via Range header)  │                      │                     │
│ Write semantics  │ Atomic PUT          │ Random overwrite     │ In-place overwrite  │
│ POSIX semantics  │ No                  │ Yes (full)           │ Yes (full)          │
│ Shared access    │ Yes (global)        │ No (single instance) │ Yes (multi-client)  │
│ Latency (read)   │ 1–100ms             │ 0.1–1ms              │ 1–10ms              │
│ Throughput       │ High (parallel)     │ 1 GB/s (gp3)         │ Moderate            │
│ Cost/GB/month    │ $0.023              │ ~$0.08               │ ~$0.30              │
│ Max size         │ 5TB per object      │ 64TB per volume      │ Petabytes           │
│ Durability       │ 11 nines            │ 99.8–99.9% (SLA)     │ Multi-AZ dependent  │
└──────────────────┴─────────────────────┴──────────────────────┴─────────────────────┘
```

---

## 3. Deep Dive

### S3 Internals

#### Consistency Model
Before December 2020, S3 had only eventual consistency for overwrite PUTs and DELETEs (you could read a stale version immediately after an update). Since Dec 1, 2020, S3 provides **strong read-after-write consistency** for all operations — `GET`, `LIST`, `PUT`, `DELETE`. There is no extra cost or configuration required. This change matters significantly: design docs and architecture patterns written before 2021 that add compensating logic for stale reads are now over-engineered.

#### Multipart Upload

```
Client                               S3
  │                                   │
  ├── InitiateMultipartUpload ────────►│  Returns: UploadId
  │                                   │
  ├── UploadPart(1, bytes 0–100MB) ──►│  Returns: ETag-1
  ├── UploadPart(2, bytes 100–200MB)─►│  Returns: ETag-2
  ├── UploadPart(3, bytes 200–300MB)─►│  Returns: ETag-3
  │   (parts can be uploaded in       │
  │    parallel — this is the key)    │
  │                                   │
  ├── CompleteMultipartUpload ────────►│  S3 assembles parts
  │   [ETag-1, ETag-2, ETag-3]        │  Returns: final ETag
  │                                   │
```

- **Threshold:** Use multipart for objects > 100MB (recommended), **required** for objects > 5GB (single PUT limit).
- **Part size:** 5MB minimum (except last part), 5GB maximum, up to 10,000 parts.
- **Parallelism:** Upload parts simultaneously to saturate bandwidth. For a 10GB file with 100MB parts, you can saturate a 10Gbps link with ~10 parallel uploads.
- **Resumability:** If a part upload fails, only that part needs to be retried — not the whole file.
- **Cleanup:** Incomplete multipart uploads accumulate storage charges. Use lifecycle policies to abort incomplete uploads after N days.

#### Presigned URLs
```
Flow:
  App Server (has IAM credentials)
       │
       ├── GeneratePresignedURL(key, method=GET, expires=3600s)
       │   Returns: https://bucket.s3.amazonaws.com/key?
       │            X-Amz-Algorithm=...&X-Amz-Credential=...
       │            &X-Amz-Expires=3600&X-Amz-Signature=...
       │
       └── Send URL to client
  
  Client ──── GET <presigned URL> ────► S3  (no app server in the path)
```

- Client accesses S3 directly — offloads bandwidth from your application servers.
- Signature is HMAC-SHA256 over the request parameters. S3 validates without calling your app.
- **Max expiry:** 7 days (604800s) for IAM user credentials; 15 minutes for temporary role credentials. This surprises engineers: if you generate presigned URLs with an assumed role, they expire with the role session, not the URL TTL parameter.

#### S3 Select
- Push a SQL-like predicate into S3: `SELECT * FROM s3object WHERE age > 30`
- S3 filters data server-side, returning only matching records — reduces data transfer by up to 80% for columnar/JSON data.
- Supports CSV, JSON, Parquet (columnar scan). Not a general query engine — no joins, no aggregations beyond basic filtering.

#### S3 Throughput Scaling

```
Single S3 prefix limit:
  PUT/COPY/POST/DELETE: 3,500 req/s
  GET/HEAD:             5,500 req/s

To scale beyond this, shard across prefixes:
  /uploads/2024/01/file.jpg
  /uploads/2024/02/file.jpg
  /a3b8f/file.jpg          ← Random prefix sharding

With 10 prefixes:
  PUT: 35,000 req/s
  GET: 55,000 req/s
```

- **Before 2018**, S3 had a per-key-prefix throughput limit that required explicit prefix sharding with random hashes. After an internal optimization, S3 auto-scales per prefix, but the 3500/5500 baseline per-prefix figures still apply. For very high-throughput workloads (>10k req/s to the same prefix), spread across prefixes.

---

### HDFS Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         HDFS CLUSTER                                │
│                                                                     │
│  ┌──────────────────────────────┐                                   │
│  │         NameNode             │  ← Active (HA mode)               │
│  │  - File → Block mapping      │     Standby NameNode (hot)        │
│  │  - Block → DataNode mapping  │     ZooKeeper for failover        │
│  │  - All metadata in RAM       │                                   │
│  │  - SPOF without HA           │                                   │
│  └──────────────┬───────────────┘                                   │
│                 │ Block locations (heartbeat every 3s)              │
│      ┌──────────┼──────────────┐                                    │
│      ▼          ▼              ▼                                    │
│  ┌────────┐  ┌────────┐  ┌────────┐                                 │
│  │  DN-1  │  │  DN-2  │  │  DN-3  │  DataNodes (store blocks)      │
│  │ Rack A │  │ Rack A │  │ Rack B │  ← Rack-aware replication      │
│  └────────┘  └────────┘  └────────┘                                 │
│                                                                     │
│  Block: file.parquet (640MB)                                        │
│  Block-1 (128MB): DN-1, DN-2, DN-3   ← 3-way replication           │
│  Block-2 (128MB): DN-2, DN-3, DN-1      1 replica on each rack      │
│  Block-3 (128MB): DN-3, DN-1, DN-2   ← rack-awareness guarantees   │
│  Block-4 (128MB): DN-1, DN-3, DN-2      1 replica survives rack fail│
│  Block-5 (128MB): DN-2, DN-1, DN-3                                  │
└─────────────────────────────────────────────────────────────────────┘
```

#### NameNode: The Critical SPOF
The NameNode holds the entire filesystem namespace **in RAM** (~150 bytes per file + block). A cluster with 100M files needs ~15GB RAM just for metadata. This is why HDFS is not suitable for billions of tiny files — metadata pressure kills the NameNode.

**HA NameNode:**
- Active + Standby NameNode, both connected to a set of JournalNodes (Quorum Journal Manager).
- Edits are written to JournalNodes (Paxos-based quorum). Standby replays edits to stay current.
- ZooKeeper ZKFC (Failover Controller) detects active NameNode failure and promotes standby in ~30s.

#### Block Size and Replication Pipeline

```
Default block size: 128MB
Replication factor: 3

Write pipeline:
  Client → DN-1 → DN-2 → DN-3
           (acks propagate back)

Rack-awareness rule:
  Replica 1: Same rack as client (or any rack)
  Replica 2: Different rack
  Replica 3: Same rack as replica 2 (different node)
  → Survives single rack failure
```

#### HDFS vs S3 for Analytics Workloads

| | HDFS | S3 |
|---|---|---|
| Data locality | Yes (co-locate compute + storage) | No (network hop always) |
| Latency | Lower (local read) | Higher (100–200ms first byte) |
| Cost | High (EC2 + EBS for every DN) | Low (pay per GB stored) |
| Scaling | Manual (add DataNodes) | Automatic, infinite |
| Maintenance | High (cluster ops, upgrades) | Zero (managed service) |
| When it wins | Latency-sensitive streaming MapReduce | Cloud-native Spark, EMR, Athena |
| When it loses | When cluster is idle (pay for all nodes 24/7) | Compute-heavy joins (S3 network latency adds up) |

**Modern trend:** "Disaggregated storage/compute" — run Spark/EMR on ephemeral compute, store data in S3. The ~50–100ms S3 latency for first byte is acceptable for batch analytics where jobs run for minutes. HDFS wins only when the job is latency-sensitive (streaming) or when data locality is critical (tight feedback loop).

---

### Blob Storage for Media

#### Chunked Upload Flow
```
Client                          App Server                      S3/Blob Store
  │                                 │                                │
  ├── POST /upload/init ───────────►│                                │
  │                                 ├── CreateMultipartUpload ──────►│
  │                                 │◄── UploadId ───────────────────┤
  │◄── { uploadId, partUrls[] } ────┤                                │
  │                                 │                                │
  ├── PUT partUrl[0] (chunk 0) ─────────────────────────────────────►│
  ├── PUT partUrl[1] (chunk 1) ─────────────────────────────────────►│
  ├── PUT partUrl[2] (chunk 2) ─────────────────────────────────────►│
  │   (parallel, via presigned URLs)│                                │
  │                                 │                                │
  ├── POST /upload/complete ────────►│                               │
  │   { uploadId, parts: [etags] }  ├── CompleteMultipartUpload ────►│
  │                                 │◄── Final object URL ───────────┤
  │◄── { url: cdn.example.com/… } ──┤                                │
```

#### Content-Addressed Storage (Hash-Based Deduplication)
```
SHA-256(file_bytes) → "a3f8e2..."

Store at: /blobs/a3/f8/a3f8e2...

Upload request arrives:
  1. Client computes hash client-side
  2. HEAD /blobs/a3f8e2...  → 200 (exists) → skip upload (dedup)
                            → 404 (missing) → proceed with upload
  3. On upload: S3 verifies hash on arrival (end-to-end integrity)
```

- Used by git (object store), Docker image layers, Dropbox (block-level dedup).
- Enables cross-user dedup — two users uploading identical files store only one copy.
- **Privacy consideration:** Hash-based dedup can reveal whether a file exists in the system (even without access). Dropbox faced scrutiny for this.

---

### Storage Tiers

| Tier | First Byte Latency | Storage Cost/GB/month | Retrieval Cost | Min Storage Duration |
|---|---|---|---|---|
| S3 Standard | Milliseconds | $0.023 | Free | None |
| S3 Standard-IA | Milliseconds | $0.0125 | $0.01/GB | 30 days |
| S3 One Zone-IA | Milliseconds | $0.01 | $0.01/GB | 30 days |
| S3 Glacier Instant | Milliseconds | $0.004 | $0.03/GB | 90 days |
| S3 Glacier Flexible | Minutes (expedited: 1–5m) | $0.0036 | $0.03/GB (standard) | 90 days |
| S3 Glacier Deep Archive | Hours (12h standard) | $0.00099 | $0.02/GB | 180 days |

**Key ratios to remember:**
- Standard → IA: ~46% cheaper storage, pay-per-retrieval.
- Standard → Glacier Deep Archive: ~23x cheaper storage. You pay ~$0.001/GB vs $0.023/GB.
- S3 Standard durability: 11 nines (99.999999999%) — designed to survive concurrent loss of two entire availability zones.

**Lifecycle transition rules:**
```yaml
# Example lifecycle policy
Rules:
  - Transition to S3 Standard-IA after 30 days
  - Transition to Glacier Instant after 90 days
  - Transition to Glacier Deep Archive after 365 days
  - Expire (delete) after 2555 days (7 years)
```

**Gotchas:**
- **Minimum storage duration:** Deleting a Glacier object after 1 day still charges for 90 days.
- **Retrieval latency:** Glacier Flexible "bulk" retrieval is 5–12 hours and cheaper; "expedited" is 1–5 minutes but expensive. Deep Archive has no expedited tier — minimum 12 hours.
- **S3 Intelligent-Tiering:** Automatically moves objects between tiers based on access patterns. $0.0025/1000 objects/month monitoring fee. Good for unpredictable access patterns.

---

### Checksums and Data Integrity

#### Why This Matters
Silent data corruption (bit rot) is real at scale. A 1-in-10^15 bit error rate sounds negligible, but at 1 exabyte of storage that's ~1000 corrupted bits per hour. Storage systems must detect and repair corruption proactively.

| Algorithm | Speed | Hardware Acceleration | Collision Resistance | Use Case |
|---|---|---|---|---|
| MD5 | Fast | No | Weak (broken) | Legacy only; do not use for security |
| SHA-1 | Medium | No | Weak (broken) | Legacy VCS (git migrating away) |
| SHA-256 | Slower | Yes (SHA-NI instruction) | Strong | Content addressing, digital signatures |
| CRC32C | Very fast | Yes (SSE4.2 / ARM CRC) | Weak (not cryptographic) | Storage integrity — checksums, error detection |
| xxHash | Fastest | No hardware specific | Weak | In-memory hash maps, non-security checksums |

**Why CRC32C for storage integrity (not SHA-256):**
- CRC32C has dedicated hardware instructions (Intel SSE4.2 `CRC32`, ARM `__crc32`). It can checksum at memory bandwidth speed (~10–20 GB/s).
- SHA-256 without hardware acceleration (~500 MB/s) would add unacceptable overhead per block write.
- CRC32C detects accidental corruption (bit flips, partial writes) — its purpose. It is NOT cryptographically secure and cannot prevent intentional tampering.
- S3 uses CRC32C by default for intra-system integrity; you can also pass `Content-MD5` or `x-amz-checksum-sha256` for end-to-end client verification.

**End-to-End Integrity Pattern:**
```
1. Client computes SHA-256(file)
2. Client sends: PUT /key with header x-amz-checksum-sha256: <hash>
3. S3 computes SHA-256 on received bytes
4. S3 compares — rejects upload if mismatch (400 Bad Request)
5. Hash stored as object metadata; client can re-verify on download
```

---

### Replication Factor and Erasure Coding

#### 3-Way Replication vs Erasure Coding

```
3-Way Replication (Hadoop default):

  Block (128MB) → 3 copies = 384MB stored
  Overhead: 3x

  [Block] [Block Copy 1] [Block Copy 2]
  DN-1       DN-2           DN-3

  Read: Any DN can serve (pick nearest)
  Write: Must write to 3 DNs (pipeline)
  Tolerate: 2 simultaneous failures


Reed-Solomon Erasure Coding RS(6,3):

  Block (128MB) → split into 6 data chunks (21.3MB each)
                → compute 3 parity chunks (21.3MB each)
                = 9 chunks × 21.3MB = 192MB stored
  Overhead: 1.5x

  [D1][D2][D3][D4][D5][D6][P1][P2][P3]
   DN1  DN2  DN3  DN4  DN5  DN6  DN7  DN8  DN9

  Read: Any 6 of 9 chunks sufficient (can tolerate 3 failures)
  Write: Must compute and write 9 chunks (CPU cost)
  Tolerate: 3 simultaneous failures
```

| | 3-way Replication | RS(6,3) Erasure Coding |
|---|---|---|
| Storage overhead | 3x | 1.5x |
| Read performance | Fast (direct replica read) | Slower if reconstruction needed |
| Write performance | Fast (parallel pipeline) | Slower (parity computation) |
| CPU overhead | Low | High (polynomial math) |
| Failure tolerance | 2 nodes | 3 nodes |
| Reconstruction cost | None (replica exists) | High (6 chunks + XOR/GF math) |
| Best for | Hot data, low-latency reads | Warm/cold data, cost optimization |
| Used by | HDFS hot data, primary DBs | HDFS warm tier, S3 (internally), Azure Cool |

**Key insight:** HDFS uses 3-way replication by default because MapReduce tasks rely on data locality — reading from a local replica at disk speed (500MB/s) vs reconstructing from 6 shards across the network. Erasure coding shines for cold/archive data where reconstruction cost is acceptable and storage cost savings (50%) are significant over millions of GBs.

**HDFS Erasure Coding in practice (HDFS-7285, Hadoop 3.0+):**
- Configurable per directory: `hdfs ec -setPolicy -policy RS-6-3-1024k /cold-data`
- 1024k cell size means each chunk is 1MB.
- Only practical for large files (> several GB). For small files, overhead of 9 DNs exceeds benefit.

---

## 4. Trade-offs

### Storage Type Selection Guide

| Workload | Recommended | Why |
|---|---|---|
| Static website assets, images, videos | Object (S3) | Cheap, CDN-friendly, infinite scale |
| Database storage (PostgreSQL, MySQL) | Block (EBS gp3) | Random I/O, POSIX semantics |
| VM disk images | Block (EBS) | OS requires raw block device |
| Shared config files across EC2 instances | File (EFS) | POSIX, multi-instance mount |
| ML training datasets | Object (S3) or HDFS | Depends on latency sensitivity |
| Backup and archive | Object (S3 Glacier) | Cheap, durable, infrequent access |
| Video streaming (serve to users) | Object (S3 + CloudFront) | CDN edge caching reduces latency |
| OLAP analytics at scale | S3 + Parquet + Athena | Column-oriented, serverless |

### When NOT to Use S3

- **High-frequency random writes** — S3 objects are immutable on write (you rewrite the whole object). For write-heavy workloads, use block storage.
- **Sub-millisecond latency** — S3 first byte is 1–100ms. Use EBS (0.1–1ms) or local NVMe (<0.1ms) for latency-critical paths.
- **POSIX filesystem operations** — No rename atomicity (it's a copy + delete), no hardlinks, no `fsync`. Don't use S3 as a filesystem substitute.
- **Transactional workloads** — No ACID, no locking. Use a database.

### S3 vs EBS vs EFS Cost Comparison (approximate, us-east-1)

| | S3 Standard | EBS gp3 | EFS Standard |
|---|---|---|---|
| Storage cost | $0.023/GB/month | $0.08/GB/month | $0.30/GB/month |
| For 10TB/month | $230 | $800 | $3,000 |
| IOPS | N/A (req/s model) | 16,000 (3,000 baseline) | ~300k shared |
| Throughput | N/A (req/s model) | 1 GB/s | 1–3 GB/s |

---

## 5. Numbers to Know

| Metric | Value | Notes |
|---|---|---|
| S3 PUT throughput per prefix | 3,500 req/s | Scale by adding prefixes |
| S3 GET throughput per prefix | 5,500 req/s | Scale by adding prefixes |
| S3 Standard price | ~$0.023/GB/month | us-east-1 |
| S3 Standard-IA | ~$0.0125/GB/month | + $0.01/GB retrieval |
| S3 Glacier Flexible | ~$0.0036/GB/month | Minutes to hours retrieval |
| S3 Glacier Deep Archive | ~$0.00099/GB/month | 12–48 hour retrieval |
| S3 durability | 99.999999999% (11 nines) | Designed for 10M objects, expect to lose 1 per 10,000 years |
| S3 multipart: recommended threshold | > 100MB | Required above 5GB |
| S3 multipart: single PUT limit | 5GB | Hard limit |
| S3 max object size | 5TB | Via multipart |
| EBS gp3 IOPS | 16,000 IOPS (provisioned) | Baseline 3,000 free |
| EBS gp3 throughput | 1 GB/s | Baseline 125 MB/s free |
| HDFS default block size | 128MB | Tunable (256MB for large files) |
| HDFS replication factor | 3 (default) | Configurable per file |
| RS(6,3) storage overhead | 1.5x | vs 3x for 3-way replication |
| Reed-Solomon failure tolerance | 3 out of 9 chunks | k=6 data, m=3 parity |
| CRC32C speed (hardware) | ~10–20 GB/s | SSE4.2 acceleration |
| SHA-256 speed (software) | ~500 MB/s | Without SHA-NI; 2–4 GB/s with |
| S3 presigned URL max expiry (role) | 15 minutes–12 hours | Limited by STS session token |
| S3 presigned URL max expiry (user) | 7 days | IAM user long-term credentials |
| NameNode RAM per file | ~150 bytes | 100M files ≈ 15GB metadata |

---

## 6. Interview Tips

### What Interviewers Actually Probe

1. **"Design YouTube / Netflix video storage"**
   - Expected answer covers: S3 for raw + processed video, multipart upload, presigned URLs for upload (offload bandwidth from your servers), CDN (CloudFront) for delivery, multiple transcoded renditions per video, content-addressed naming (video-id/720p.mp4).

2. **"S3 vs EBS — when would you use each?"**
   - Wrong answer: "S3 for files, EBS for databases." You need to articulate *why*: block storage gives random byte-level I/O with sub-millisecond latency that databases need. S3 is immutable-object semantics — you can't do a 4KB random write at offset 512KB.

3. **"How does HDFS handle NameNode failure?"**
   - Cover HA NameNode architecture: Active + Standby + JournalNodes (QJM). ZooKeeper ZKFC for automatic failover. Mention that the NameNode is a SPOF not because failover doesn't exist, but because failover takes ~30s and the entire cluster is unavailable during that window.

4. **"Explain erasure coding vs replication"**
   - Start with the overhead numbers: 3x replication vs 1.5x for RS(6,3). Explain the read path difference: replication = direct read from one node; erasure coding = potentially read 6 chunks + XOR. Give the use case: replication for hot data (low-latency reads), erasure coding for cold data (storage cost optimization).

5. **"How would you ensure data integrity end-to-end in a storage pipeline?"**
   - Client computes hash → sends with upload → server verifies → stores hash as metadata → client can re-verify on download. Mention the hash algorithm choice: CRC32C for speed (hardware acceleration), SHA-256 for tamper detection / content addressing.

6. **"S3 consistency model?"**
   - Must know the Dec 2020 change to strong consistency. Pre-2020 architecture patterns added read-after-write compensation logic that is now obsolete. Interviewers test whether you know current behavior vs legacy knowledge.

### Common Mistakes

- **Recommending sticky S3 presigned URLs with IAM roles without noting the expiry limitation.** Role-based credentials cap at the session duration, not the URL expiry parameter.
- **Saying "HDFS is better for everything Hadoop."** Modern cloud-native Spark runs on S3 (compute + storage disaggregation). HDFS wins only when data locality is critical.
- **Forgetting the HDFS small file problem.** NameNode RAM pressure limits cluster to ~hundreds of millions of files. If your workload generates billions of small files, HDFS is the wrong choice.
- **Not knowing the S3 PUT/GET per-prefix limits** (3500/5500). This is a real bottleneck for high-throughput image or event storage systems and a common follow-up question.
- **Conflating S3 Glacier with "just cheap S3."** The retrieval latency (hours for Deep Archive) and minimum storage duration (180 days) make it unsuitable for data you might need to access in minutes.
- **Using MD5 for integrity checks.** MD5 is cryptographically broken. For integrity, use CRC32C (speed) or SHA-256 (security). Bringing this up proactively signals depth.

### Questions You Will Be Asked

- How would you design a photo storage service for 1 billion users (Instagram scale)?
- A client uploads a 50GB video — walk through the entire flow from client to durable storage.
- How would you implement deduplication in a file storage service?
- What's the difference between S3 Standard-IA and Glacier Instant Retrieval?
- Why does HDFS use 128MB blocks instead of 4KB blocks like a traditional filesystem?
- How does rack-awareness in HDFS protect against failure?
- You have 1PB of archive data that is accessed once per year. Design the storage strategy with cost estimates.
- How would you handle a corrupted block in HDFS? (NameNode detects under-replication from DataNode heartbeats → schedules re-replication from a healthy copy)

---

## 7. Resources

1. **AWS S3 Documentation — Storage Classes** — https://docs.aws.amazon.com/AmazonS3/latest/userguide/storage-class-intro.html
2. **Designing Data-Intensive Applications (DDIA), Martin Kleppmann** — Chapter 3 (Storage Engines) and Chapter 10 (Batch Processing / HDFS)
3. **Netflix Tech Blog — Evolutionof Media Storage** — https://netflixtechblog.com/
4. **ByteByteGo — Object vs Block vs File Storage** — https://bytebytego.com/courses/system-design-interview/
5. **HDFS Architecture Guide (Apache)** — https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/HdfsDesign.html
6. **AWS S3 Performance Optimization** — https://docs.aws.amazon.com/AmazonS3/latest/userguide/optimizing-performance.html
