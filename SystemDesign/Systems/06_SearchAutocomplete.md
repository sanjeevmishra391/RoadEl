# Search Autocomplete / Typeahead System — Full System Design Walkthrough

> **Interview Format:** 45 minutes | **Level:** Senior / Staff Engineer  
> **Analogous real systems:** Google Search autocomplete, Amazon search suggestions, YouTube search, Twitter typeahead, LinkedIn search

---

## Table of Contents
1. [Problem Statement](#1-problem-statement)
2. [Clarifying Questions](#2-clarifying-questions)
3. [Functional Requirements](#3-functional-requirements)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [Capacity Estimation](#5-capacity-estimation)
6. [High-Level Design](#6-high-level-design)
7. [Deep Dives](#7-deep-dives)
8. [Trade-offs & Alternatives](#8-trade-offs--alternatives)
9. [Failure Scenarios](#9-failure-scenarios)
10. [Interview Tips](#10-interview-tips)

---

## 1. Problem Statement

Design a **search autocomplete (typeahead) system** that, as a user types a query character by character, returns the top-K most relevant search suggestions in real time. Think of the dropdown that appears under Google's search bar while you type "sys" and it immediately suggests "system design interview", "systemic racism", "system of a down".

The system must:
- Return suggestions within **100ms** of each keystroke (the human perception threshold for "instant")
- Handle **Google-scale traffic** — 5 billion searches per day implies a massive autocomplete query load
- Rank suggestions by a blend of global popularity, recency, and personalization
- Update rankings based on real-time trends (a breaking news story changes what people search for within minutes)
- Support geo-aware suggestions (users in Mumbai should see India-relevant suggestions)

This is fundamentally a **prefix matching + ranking problem at massive scale**. The core tension is freshness vs. latency: the most accurate rankings require processing billions of events, but we need to return results in under 100ms.

---

## 2. Clarifying Questions

### Q1: How many characters typed before we start showing suggestions?
**Expected answer:** Typically after 1-2 characters. After 1 character, suggestions are too broad and often low quality; after 2 characters, the data is more meaningful. Some systems show suggestions after a 200ms debounce delay regardless of character count.  
**Why it matters:** Single-character prefix lookups cover a huge fraction of the trie — they're expensive and the suggestions are low quality. Starting at 2 characters cuts the problem space significantly.

### Q2: How many suggestions should we return — the value of K?
**Expected answer:** 5 to 10 suggestions displayed. We may fetch 20 from the backend and the client filters/ranks the last few.  
**Why it matters:** Fetching top-20 from the server and re-ranking on the client allows the frontend to apply personal history without a full personalization round-trip for every keystroke.

### Q3: Should suggestions be personalized, or globally ranked?
**Expected answer:** Both. A global baseline ranked by overall frequency, with a personalization layer that boosts suggestions the user has clicked before or that match their location and language.  
**Why it matters:** Full real-time personalization is expensive. A practical approach is to serve globally ranked suggestions from a fast cache and apply a lightweight personalization re-ranking at the edge or client-side using the user's stored history.

### Q4: How fresh do suggestions need to be — can we tolerate delay in reflecting trending queries?
**Expected answer:** Trending topics (breaking news, viral events) should appear in suggestions within 10-30 minutes. Regular popularity updates (slower drifts) can be updated once per day via a batch pipeline.  
**Why it matters:** Real-time freshness requires a streaming aggregation pipeline (Kafka + Flink/Spark Streaming). If 30-minute freshness is acceptable, a micro-batch approach (every 5 minutes) is much simpler to operate.

### Q5: Should we support multiple languages and markets?
**Expected answer:** Yes. English globally, with localized suggestions per region. A user typing "fot" in Brazil should see Portuguese suggestions; a user in France should see French ones.  
**Why it matters:** This means the trie or suggestion index must be partitioned by language/region. You cannot serve a single global trie for all languages — the data is too large and the rankings differ completely by market.

### Q6: What about spelling correction / fuzzy matching?
**Expected answer:** Out of scope for this design. We focus on exact prefix matching with suggestions. Spell correction is a separate downstream system (edit-distance based, Levenshtein automaton).  
**Why it matters:** Fuzzy matching adds an entirely different data structure and lookup algorithm. Scope this out to keep the design focused.

### Q7: Should we support query blacklisting (removing offensive or spammy suggestions)?
**Expected answer:** Yes. A content safety layer should filter suggestions before they reach the user. The filter should be applied at the serving layer, not at the aggregation layer, so it can be updated without rebuilding the index.  
**Why it matters:** This is both a product and legal requirement. Real systems maintain a deny-list that is hot-reloaded into the suggestion servers without restart.

---

## 3. Functional Requirements

| # | Requirement |
|---|-------------|
| FR1 | Given a prefix string (1+ characters), return the top-K most relevant search suggestions |
| FR2 | Suggestions are ranked by a combination of global popularity (query frequency), recency, and personalization signals |
| FR3 | Suggestions update to reflect trending queries within 30 minutes |
| FR4 | Support geo-localized suggestions (return market-specific results per user region) |
| FR5 | Support multi-language prefix matching (separate indexes per language/locale) |
| FR6 | Log every query typed and every suggestion clicked to feed the ranking pipeline |
| FR7 | Content safety filtering — suppress blacklisted or offensive suggestions |
| FR8 | Debounced triggering — only send suggestion requests after 100-200ms of no new keystrokes |

**Out of scope:**
- Full-text search (this is prefix search only)
- Spell correction
- Voice input
- Semantic search (query intent understanding beyond string prefix)

---

## 4. Non-Functional Requirements

| Category | Target |
|----------|--------|
| **Latency** | p99 < 100ms end-to-end (including network to client). Backend suggestion service: p99 < 10ms |
| **Throughput** | 5B searches/day → ~58,000 QPS average, 200,000+ QPS peak during high-traffic events |
| **Availability** | 99.99% — search is a core product feature; outage is a major user-facing incident |
| **Freshness** | Trending queries reflected in suggestions within 30 minutes |
| **Accuracy** | Top suggestion should match user intent in >70% of cases (measured by suggestion click-through rate) |
| **Scale** | Handle query data from billions of daily searches, trillions of queries in history |
| **Data Staleness** | Regular popularity re-ranking: once per day. Trending injection: every 15-30 minutes |

---

## 5. Capacity Estimation

### 5.1 Query Volume

```
Searches per day:       5,000,000,000 (5B)
Searches per second:    5B / 86,400 = ~57,870 QPS average

Peak multiplier:        3-5x during major events (sports finals, elections)
Peak QPS:               ~200,000 QPS

Characters per query:   Average 15 characters
Keystrokes per search:  ~7 (user types half the query before selecting a suggestion
                            or types the full query)
Autocomplete requests per search: 7
Total autocomplete QPS: 57,870 × 7 = ~400,000 QPS
Peak autocomplete QPS:  ~1,500,000 QPS
```

### 5.2 Data Volume — Query Log

```
Queries per day:            5B
Average query size:         50 bytes (query string + user ID + timestamp + geo)
Raw query log per day:      5B × 50B = 250 GB/day
Annual query log:           ~90 TB/year

After deduplication:        Unique queries in 1 year ≈ ~5-10 billion distinct queries
                            (Zipf distribution — top 1M queries cover 80% of traffic)
```

### 5.3 Trie / Index Size

```
Unique prefixes to index:   10M popular queries × 15 chars avg = 150M prefix strings
Average per-prefix payload: 200 bytes (top-10 suggestions with scores, IDs)
Total index size:           150M × 200B = ~30 GB per language/locale

English global index:       ~30 GB
Other languages (20×):      ~20 × 15 GB = 300 GB
Total index across locales: ~330 GB — fits in RAM on a cluster of cache nodes
```

### 5.4 Suggestion Server Count

```
QPS per suggestion server:  ~20,000 QPS (trie in-memory lookup is very fast, <1ms each)
Servers needed (avg load):  400,000 / 20,000 = 20 servers
With 3× peak headroom:      60 suggestion servers
With redundancy (N+1):      ~80 suggestion servers across two AZs
```

### 5.5 Storage for Query Logs and Aggregation

```
Query log (raw):                250 GB/day
After Kafka consumer processing: Aggregated counts per (query, day) = ~100 GB/day
Prefix-to-top-K index (rebuilt daily):  ~330 GB total (in-memory, fast SSDs for rebuild)
```

---

## 6. High-Level Design

### 6.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                   CLIENT TIER                                        │
│   ┌─────────────────────────────────────────────────────────────────────────────┐    │
│   │  Browser / Mobile App                                                        │    │
│   │  - 200ms debounce before sending request                                     │    │
│   │  - Cache last N prefix results in localStorage                               │    │
│   │  - Apply client-side personalization re-ranking using user history           │    │
│   └───────────────────────────────┬─────────────────────────────────────────────┘    │
└───────────────────────────────────┼─────────────────────────────────────────────────┘
                                    │  HTTPS /suggest?q=sys&locale=en-US&uid=...
                                    │
┌───────────────────────────────────▼─────────────────────────────────────────────────┐
│                              API GATEWAY / EDGE CDN                                  │
│   - Route by locale (en-US → US cluster, en-IN → India cluster)                     │
│   - Edge cache popular prefixes (top 10K prefixes cached at CDN edge, TTL=60s)      │
│   - TLS termination, rate limiting, abuse detection                                  │
└───────────────────────────────────┬─────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐          ┌───────────────┐          ┌───────────────┐
│  Suggestion   │          │  Suggestion   │          │  Suggestion   │
│  Server 1     │          │  Server 2     │          │  Server N     │
│               │          │               │          │               │
│ Trie (hot)    │          │ Trie (hot)    │          │ Trie (hot)    │
│ in memory     │          │ in memory     │          │ in memory     │
│               │          │               │          │               │
│ ┌───────────┐ │          │ ┌───────────┐ │          │ ┌───────────┐ │
│ │ Safety    │ │          │ │ Safety    │ │          │ │ Safety    │ │
│ │ Filter    │ │          │ │ Filter    │ │          │ │ Filter    │ │
│ └───────────┘ │          │ └───────────┘ │          │ └───────────┘ │
└───────┬───────┘          └───────┬───────┘          └───────┬───────┘
        │                          │                          │
        └──────────────────────────┼──────────────────────────┘
                                   │ Cache miss path
                                   ▼
                         ┌─────────────────────┐
                         │  Prefix Cache (Redis) │
                         │  Long-tail prefixes   │
                         │  not in-memory trie   │
                         └──────────┬────────────┘
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────────────────────┐
│                          AGGREGATION PIPELINE                                      │
│                                                                                    │
│  ┌────────────┐    ┌──────────────────┐    ┌──────────────────────────────────┐   │
│  │  Query Log │    │  Kafka           │    │  Batch Aggregation               │   │
│  │  Collector │───►│  (search-events  │    │  (Spark/MapReduce, daily)        │   │
│  │            │    │   topic)         │    │  - Count query frequency         │   │
│  └────────────┘    └────────┬─────────┘    │  - Compute weighted scores       │   │
│                             │              │  - Build prefix→top-K mapping    │   │
│                             │              │  - Output to index builder        │   │
│                             │              └──────────────┬───────────────────┘   │
│                             │                             │                        │
│                             │              ┌──────────────▼───────────────────┐   │
│                             │              │  Trie / Index Builder             │   │
│                             │              │  - Serialize trie to binary blob  │   │
│                             │              │  - Publish new index version       │   │
│                             │              │  - Suggestion servers hot-reload   │   │
│                             │              └──────────────────────────────────┘   │
│                             │                                                      │
│                             ▼                                                      │
│                   ┌──────────────────┐                                             │
│                   │  Stream          │                                             │
│                   │  Aggregation     │                                             │
│                   │  (Flink,         │                                             │
│                   │   15-min windows)│                                             │
│                   │  - Count-Min     │                                             │
│                   │    Sketch        │                                             │
│                   │  - Trending      │                                             │
│                   │    query inject  │                                             │
│                   └──────────────────┘                                             │
└───────────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Component Responsibilities

**Client (Browser/Mobile):**  
- Implements debounce — waits 100-200ms after last keystroke before sending the autocomplete request. This reduces requests from 15 per query to ~5.
- Caches prefix results: if user typed "sys" and then types "syst", the client checks if "syst" results are already in a local LRU map. It shows "sys" results immediately while the "syst" request is in-flight.
- Applies client-side personalization: takes the server's top-20 results and re-ranks them using the user's local history (stored in localStorage). This keeps the backend globally ranked while personalizing the UX.

**API Gateway / Edge CDN:**  
- The top 10,000 most frequent prefixes are cached at CDN edge nodes (Cloudflare, Fastly) with a 60-second TTL. These cover an enormous fraction of traffic because query distribution is heavily Zipf-distributed.
- Routes requests by locale to the regional cluster.
- Rate-limits per user to prevent scraping of the suggestion index.

**Suggestion Server:**  
- Maintains the full prefix-to-top-K trie in memory. Each server has the complete index for its assigned locale.
- On receiving a query `GET /suggest?q=sys&locale=en-US`, it traverses the trie to the "sys" node and returns the top-K suggestions stored there.
- Applies the safety filter (hot-reloaded deny-list) to remove blacklisted suggestions.
- Falls back to the Redis prefix cache for long-tail prefixes not in the in-memory trie.

**Query Log Collector:**  
- Every search typed by a user is logged as an event: `{query, user_id, timestamp, locale, geo, clicked_suggestion}`.
- Events are streamed into Kafka for both batch and stream processing.

---

## 7. Deep Dives

### 7.1 Trie Data Structure and Sharding

**What is a Trie?**

A trie (prefix tree) is a tree where each node represents a character, and the path from root to a node represents a prefix. Each node stores the top-K most frequent queries that begin with that prefix.

```
                    (root)
                   /  |  \
                  s   g   p
                 /     \
                sy      go
               /  \       \
             sys   sta      goo
            /               \
          syst               goog
            |                  \
          syste                 googl
            |                    \
          system                  google
          design                  [top-K: "google maps", "google translate",
          design                            "google drive", ...]
          interview
         (top-K: "system design interview",
                 "system of a down",
                 "systematic", ...)
```

**Trie Node Structure:**

```python
class TrieNode:
    children: Dict[char, TrieNode]   # up to 26 children for lowercase English
    top_k: List[Suggestion]          # pre-computed top-K suggestions for THIS prefix
    # Memory layout: ~200 bytes per node for top-10 suggestions with scores

class Suggestion:
    query: str          # full query string, e.g., "system design interview"
    score: float        # blended score: frequency × recency_decay × geo_boost
    query_id: int       # 8 bytes — maps to canonical query metadata
```

**Critical optimization — store top-K at every node:**

Naive trie traversal: to get top-K for prefix "sys", traverse all descendants of "sys" and find the most frequent. This is O(number of descendants) — too slow at query time.

**Solution:** Pre-compute and store `top_k` at every node during the index build. At query time, prefix lookup is O(prefix_length) — just traverse the trie path and return the pre-stored top-K. Index build is O(total_nodes × K), which happens offline in the batch pipeline.

**Memory estimate for trie:**

```
Unique prefixes in English:     ~150M nodes
Memory per node:                ~200B (children pointers + top-10 suggestions)
Total memory:                   150M × 200B = 30 GB
Compressed (common prefix sharing): ~15-20 GB

This fits on a single 64GB RAM machine, but for redundancy
we replicate it across all suggestion servers.
```

**Trie vs other data structures:**

| Structure | Lookup | Build | Memory | Top-K retrieval |
|-----------|--------|-------|--------|-----------------|
| Trie with stored top-K | O(P) | O(N log N) | High | O(1) after traversal |
| Inverted index | O(1) | O(N) | Medium | O(K log K) |
| Sorted prefix array (binary search) | O(log N) | O(N log N) | Low | O(K) scan |
| FST (Finite State Transducer) | O(P) | High | Very low | Complex |

**Recommendation:** Trie with pre-stored top-K is the industry standard for this use case. The memory cost is acceptable (30GB), and O(P) lookup at query time with P≤15 means microsecond-level lookups.

**Sharding the Trie:**

The full trie for all locales is too large to fit in a single machine's RAM (330GB total). Two sharding approaches:

**Option 1: Shard by locale (recommended)**  
Each suggestion server holds the complete trie for one or a few locales. `en-US` trie on servers 1-10 (with replication), `zh-CN` trie on servers 11-20, etc. The API gateway routes by `Accept-Language` / geo.

```
Shard 1-3 (3 replicas):  en-US   (30 GB)
Shard 4-6 (3 replicas):  zh-CN   (25 GB)
Shard 7-9 (3 replicas):  hi-IN   (15 GB)
...
```

**Option 2: Shard by prefix range**  
Split the alphabet: servers 1-N handle prefixes starting with 'a'-'m', servers N+1-2N handle 'n'-'z'. Problem: 's' queries are 4x more frequent than 'x' queries — leads to uneven load. Requires careful range partitioning based on traffic analytics.

**Trie hot reload:**

The batch pipeline rebuilds the trie every 24 hours. Rather than a hard restart (service unavailable for seconds while loading 30GB), use a double-buffering strategy:

```
Version A (active):     Serving requests from trie_v17 in memory
Background:             Load trie_v18 into a secondary buffer (~30 seconds to load 30GB from SSD)
Atomic swap:            Update atomic pointer from trie_v17 to trie_v18
GC/free:                Release trie_v17 after confirming no in-flight requests
```

### 7.2 Top-K Aggregation with Count-Min Sketch

**The problem:** We need to maintain frequency counts for billions of distinct queries. A naive HashMap<String, Long> requires:
- 5B unique queries × (avg 20 bytes query + 8 bytes count + hash overhead) = hundreds of GB

This is too large for fast in-memory aggregation, especially in the streaming path.

**Count-Min Sketch (CMS):**

CMS is a probabilistic data structure that estimates the frequency of items in a stream using O(1/epsilon × log(1/delta)) space — dramatically smaller than exact counting.

```
Structure: 2D array of counters [d rows × w columns]
  d = number of hash functions = ceil(log(1/delta))
  w = number of buckets per row = ceil(e/epsilon)

To UPDATE count for query q:
  for i in range(d):
      j = hash_i(q) % w
      sketch[i][j] += 1

To QUERY estimated count for query q:
  min(sketch[i][hash_i(q) % w] for i in range(d))
  → Returns the minimum across all rows (overcount due to hash collisions)
  → CMS always overcounts, never undercounts (one-sided error)
```

**CMS parameters for this use case:**

```
Target: 1% false positive rate, 99% confidence
epsilon = 0.01 (1% relative error)
delta = 0.01 (1% failure probability)

w = ceil(e / epsilon) = ceil(2.718 / 0.01) = 272 buckets per row
d = ceil(log(1/delta)) = ceil(log(100)) ≈ 5 rows

Memory:
  5 rows × 272 columns × 4 bytes/counter = 5.44 KB

vs.
  HashMap for 10M queries × 30 bytes = 300 MB

CMS is 55,000× more memory efficient for frequency estimation.
```

**Top-K with CMS + Min-Heap:**

CMS alone only answers "how frequent is query X?" You need a different structure to answer "what are the top-K queries?". Combine CMS with a min-heap of size K:

```python
class TopKWithCMS:
    def __init__(self, k, epsilon=0.01, delta=0.01):
        self.cms = CountMinSketch(epsilon, delta)
        self.heap = MinHeap(k)       # min-heap of (count, query) with K elements
        self.heap_set = HashSet()    # O(1) membership test

    def add(self, query: str):
        self.cms.update(query)
        estimated_count = self.cms.query(query)

        if query in self.heap_set:
            self.heap.update_key(query, estimated_count)
        elif len(self.heap) < K:
            self.heap.push((estimated_count, query))
            self.heap_set.add(query)
        elif estimated_count > self.heap.peek_min():
            evicted = self.heap.pop_min()
            self.heap_set.remove(evicted.query)
            self.heap.push((estimated_count, query))
            self.heap_set.add(query)

    def get_top_k(self) -> List[str]:
        return [item.query for item in sorted(self.heap, reverse=True)]
```

This maintains a running top-K with O(log K) per update — fast enough for streaming aggregation.

**Distributed CMS aggregation:**

In the streaming pipeline, each Flink/Spark Streaming worker maintains a local CMS + top-K heap. Every 15 minutes, all workers merge their sketches:

```
Merging CMS sketches:
  merged[i][j] = sum(sketch_r[i][j] for all workers r)
  → CMS supports lossless merging: just sum the corresponding cells
  → Merge K top-K heaps: combine all K heaps, re-run top-K selection

Time window aggregation:
  - 15-minute tumbling window: snapshot of most popular queries in last 15 minutes
  - 24-hour sliding window: background batch counts for stable popularity
  - Trending = (15-min-count / 24-hour-avg) > threshold → inject into trie as trending
```

### 7.3 Offline vs Real-Time Aggregation Pipeline

**Two pipelines run simultaneously:**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           KAFKA TOPIC: search-events                     │
│  {query, user_id, timestamp, locale, clicked, session_id}                │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
              ┌────────────────┼─────────────────────┐
              │                │                     │
              ▼                ▼                     ▼
   ┌──────────────────┐  ┌────────────┐  ┌──────────────────────┐
   │  BATCH PIPELINE  │  │  STREAM    │  │  CLICK-THROUGH SINK  │
   │  (Spark/Hadoop)  │  │  PIPELINE  │  │  (for personalization│
   │  Runs nightly    │  │  (Flink)   │  │   model training)    │
   │                  │  │  15-min    │  └──────────────────────┘
   │  1. Count all    │  │  windows   │
   │     queries in   │  │            │
   │     last 30 days │  │  1. CMS    │
   │  2. Apply time   │  │     update │
   │     decay weights│  │  2. Top-K  │
   │  3. Build scored │  │     heap   │
   │     prefix→top-K │  │  3. Trend  │
   │     mapping      │  │     detect │
   │  4. Serialize    │  │  4. Inject │
   │     trie to blob │  │     trends │
   │  5. Push to S3   │  │     into   │
   │  6. Trigger hot  │  │     Redis  │
   │     reload       │  │     overlay│
   └──────────────────┘  └────────────┘
```

**Batch Pipeline — Nightly (runs at 2 AM):**

1. **Input:** 30 days of raw query logs from HDFS/S3 (yesterday's data freshly landed)
2. **Time decay weighting:** Queries from yesterday count more than queries from 30 days ago
   ```
   score = sum(count_day_d × exp(-lambda × age_in_days))
   lambda = 0.1 → yesterday's queries are ~10% more valuable than queries 10 days ago
   ```
3. **Top-K extraction per prefix:** For every prefix in the vocabulary, find the top-K queries by score
4. **Trie serialization:** Write the trie as a compressed binary blob to S3/HDFS (~30GB compressed to ~8GB with Snappy)
5. **Hot reload signal:** Push new version ID to all suggestion servers via a Kafka notification. Each server downloads the blob from S3 and hot-reloads.

**Stream Pipeline — Real-Time (Flink, 15-minute windows):**

1. **Input:** Live Kafka stream of search events
2. **Aggregation:** CMS + min-heap per 15-minute tumbling window
3. **Trend detection:** A query's 15-minute count exceeds `trend_threshold` (e.g., 5× the baseline rate)
4. **Inject into Redis overlay:** Trending queries are written into a Redis sorted set: `ZADD trending:en-US:prefix <score> <query>`
5. **Suggestion servers check Redis overlay:** When returning top-K for a prefix, merge the batch trie's top-K with trending queries from Redis, applying a freshness boost to trending items

**Redis Overlay Schema:**

```
Key:   "trending:{locale}:{prefix}"
Value: Sorted set of (score, query) pairs
TTL:   30 minutes (trending items expire if they stop being searched)

Example:
  ZADD trending:en-US:bi 9800.5 "bitcoin crash"
  ZADD trending:en-US:bi 8200.0 "bill gates arrested"
  ZADD trending:en-US:b  9800.5 "bitcoin crash"   ← also indexed for shorter prefix
```

Suggestion server merging logic:
```python
def get_suggestions(prefix, locale, k=10):
    # Primary: batch trie (stable, comprehensive)
    batch_results = trie.lookup(prefix, locale)  # returns [(score, query), ...]

    # Overlay: real-time trending (fresh, sparse)
    trending = redis.zrevrange(f"trending:{locale}:{prefix}", 0, 4)

    # Merge: trending items get a 2× score boost to surface at top
    merged = merge_and_rerank(batch_results, trending, trending_boost=2.0)

    # Safety filter
    filtered = [q for q in merged if q not in deny_list]

    return filtered[:k]
```

### 7.4 Caching Prefix Results and TTL Strategy

**The caching hierarchy:**

```
Layer 1: Client-side cache (localStorage/memory)
         - Stores last 50 prefix results in the current session
         - Immediate response for previously typed prefixes
         - No TTL within session; cleared on session end

Layer 2: CDN edge cache (Cloudflare / Fastly)
         - Top 10K most frequent prefixes globally cached
         - TTL: 60 seconds (balance freshness vs. CDN hit rate)
         - Covers ~90% of autocomplete traffic (heavy Zipf distribution)
         - Invalidate on trie hot-reload by changing URL version param

Layer 3: Redis prefix cache (centralized)
         - Long-tail prefixes not in the in-memory trie
         - TTL: 5 minutes for regular prefixes
         - TTL: 30 seconds for trending-overlaid results (needs freshness)
         - Populated on cache miss from trie lookup

Layer 4: In-memory trie on Suggestion Server
         - Handles top ~1M prefixes (those with historical search volume)
         - No TTL — refreshed via hot-reload every 24 hours + trending overlay
         - Sub-millisecond lookup

Layer 5: Database (HDFS/S3 blob)
         - Ground truth trie blob rebuilt nightly
         - Loaded by suggestion servers on startup and version change
```

**TTL strategy by prefix type:**

| Prefix type | Example | CDN TTL | Redis TTL | Reasoning |
|------------|---------|---------|-----------|-----------|
| Very high frequency | "g", "go", "goo" | 120s | N/A (in trie) | Very stable; changes slowly |
| High frequency | "google m", "amazon p" | 60s | 5 min | Daily batch is sufficient |
| Trending | "bitcoin c" | 15s | 30s | Must expire quickly; trend may end |
| Long-tail | "obscure band na" | 300s | 10 min | Rarely changes; freshness less critical |

**Cache warming on deploy:**

After a trie hot-reload, the Redis cache is stale. Strategy:
1. On hot-reload, push the new trie version ID into a versioned URL parameter: `/suggest?q=sys&v=18`
2. CDN automatically treats this as a new URL — cache starts cold but suggestion servers answer from trie directly
3. Within minutes, CDN fills from organic traffic
4. Alternatively: after building the trie, a warming daemon proactively populates CDN for the top 10K prefixes using the new version ID

**Cache key design:**

```
Redis key: suggest:{locale}:{version}:{prefix_hash}
           suggest:en-US:v18:CRC32("syst")

Including version in the key:
  - Prevents stale cross-version cache collisions
  - Old version keys naturally expire (TTL-based eviction)
  - Allows instant cache invalidation by changing version number

Prefix hash vs raw string:
  - Raw: suggest:en-US:v18:syst  → 22 bytes
  - Hashed: CRC32 is 4 bytes → 8 hex chars → shorter keys, lower Redis memory
  - Trade-off: collision risk with CRC32 is ~1/4B — acceptable
```

---

## 8. Trade-offs & Alternatives

### Trie vs Inverted Index for Autocomplete

**Inverted index approach:** Build an inverted index of query terms → query strings. To serve prefix "sys", search the inverted index for all queries containing terms starting with "sys". This is how Elasticsearch `prefix` queries work.

| Dimension | Trie | Inverted Index |
|-----------|------|----------------|
| Prefix lookup speed | O(prefix_length) | O(terms × posting_length) |
| Build complexity | High (pre-compute top-K at each node) | Low |
| Memory per prefix | High (stores top-K at every node) | Low (shared postings lists) |
| Fuzzy matching | Hard | Easy (edit-distance scoring) |
| Streaming updates | Hard (requires trie rebuild) | Easy (update posting list) |
| Industry usage | Google, Amazon, most typeahead systems | Elasticsearch, Solr |

**Verdict:** For pure prefix autocomplete with top-K retrieval, trie wins on latency. For a system that also needs to support fuzzy matching, spell correction, or semantic search, an inverted index is more flexible. Build the trie for autocomplete, use a separate Elasticsearch cluster for full-text search.

### Frequency Estimation: Count-Min Sketch vs Exact Counts

| Approach | Memory | Accuracy | Update speed |
|----------|--------|----------|--------------|
| Exact HashMap | O(distinct queries) = GB | 100% | O(1) |
| Count-Min Sketch | O(1/epsilon × log(1/delta)) = KB | ~99% | O(d) per update |
| HyperLogLog | N/A (counts distinct, not frequency) | ~1% error for cardinality | O(1) |

For streaming frequency estimation, CMS is the right tool. Run exact counting in the batch pipeline (you have hours, not milliseconds) and CMS in the streaming pipeline (you have 15 seconds per window).

### Global vs Personalized Suggestions

**Full personalization approach:** For each keystroke, call a personalization service that takes the prefix + user profile and returns a custom ranking. 

Problem: adds 30-50ms of latency (ML inference), requires user data to be accessible at low latency globally, and is expensive (200M× model inference per day = enormous compute cost).

**Practical approach (client-side re-ranking):**
1. Server returns globally ranked top-20 suggestions (fast, shared cache)
2. Client stores last 100 user-clicked queries in localStorage
3. Client re-ranks the top-20 by boosting queries that match historical patterns
4. Net result: feels personalized, costs nothing on the server side

---

## 9. Failure Scenarios

### Scenario 1: Suggestion Server OOM (Out of Memory)

**Cause:** New trie version is larger than expected; server crashes during hot-reload.  
**Detection:** OOM kill signal, health check fails, load balancer removes server from rotation.  
**Impact:** Traffic redistributes to other suggestion servers (they have the same trie — all servers are stateless peers). 20% traffic spike on remaining servers.  
**Recovery:** Auto-scaling spins up replacement server; it downloads trie from S3 and starts serving in ~60 seconds.  
**Prevention:** Shadow-load new trie version on 1 server before rolling out to all; alert on trie size > 20GB.

### Scenario 2: Batch Pipeline Failure (Trie Not Rebuilt for 48 Hours)

**Impact:** Suggestions become stale. Newly popular queries don't appear; no longer popular queries stay visible.  
**Mitigation:** Streaming pipeline continues to inject trending queries into Redis overlay. Core suggestions remain (just based on 2-day-old data, which is acceptable).  
**Recovery:** Batch pipeline re-runs on recovery; hot-reload pushed to all servers.  
**SLO breach?** No — the streaming overlay keeps trending suggestions fresh. The batch pipeline failure degrades quality but does not break functionality.

### Scenario 3: Redis Trending Cache Unavailable

**Impact:** Trending queries no longer injected into results.  
**Mitigation:** Suggestion servers degrade gracefully to batch trie only. Service still functional, just less fresh.  
**Detection:** Redis health check fails; circuit breaker trips on suggestion servers.  
**Recovery:** Redis recovers via failover replica; trending queries repopulate within one 15-minute stream window.

### Scenario 4: CDN Misconfiguration Causes Cache Invalidation Loop

**Cause:** Version ID in URL changes on every request (e.g., timestamp-based version instead of build version).  
**Impact:** CDN never caches; all requests hit origin servers. 10× spike in suggestion server QPS.  
**Prevention:** Version ID must be deterministic (e.g., `v{build_number}`, updated only on trie hot-reload). CDN cache key must not include user-specific parameters.

---

## 10. Interview Tips

### Time management

```
0-5 min:   Clarifying questions — probe scale, freshness, personalization
5-10 min:  Functional + non-functional requirements
10-15 min: Capacity math — keystrokes × searches/day = autocomplete QPS
15-25 min: Architecture diagram — both pipelines (batch + stream)
25-40 min: Deep dives — trie structure, CMS, offline vs real-time pipeline
40-45 min: Trade-offs (trie vs inverted index), failure scenarios
```

### The key insight to land early

The problem has **two distinct data flows** that must exist simultaneously:
1. **Serving path:** User types → suggestion server looks up trie → returns top-K in <10ms
2. **Aggregation path:** User types → event logged → aggregated → trie rebuilt → hot-reloaded

Making this two-pipeline structure explicit on the whiteboard within the first 15 minutes shows strong architectural thinking.

### Common mistakes

1. **Forgetting the debounce.** Without debouncing, a 10-character query fires 10 requests. That changes your QPS calculation by 3-5×.
2. **Drawing a single-server trie.** The trie must be replicated across N suggestion servers; it's not a database you query — it's in-memory on each server.
3. **Not mentioning the trie hot-reload strategy.** Interviewers specifically probe: "what happens when the trie is being updated?" Double-buffering + atomic pointer swap is the answer.
4. **Using CMS without a heap for top-K.** CMS only counts; you need the heap to maintain the top-K list. Describe both together.
5. **Missing the CDN layer.** "auto" and "amazon" are typed millions of times per hour. The CDN should answer these, not your origin servers.

### What to draw on the whiteboard

1. The trie structure — root → 's' → 'sy' → 'sys' with top-K stored at each node
2. The two pipeline diagram (batch + stream) feeding into the serving trie
3. The caching hierarchy (client → CDN → Redis → in-memory trie)
4. CMS structure (d × w matrix) with the min across rows for query estimation

### Numbers to know cold

- 5B searches/day → ~58K QPS → ~400K autocomplete QPS (7 keystrokes × searches)
- Trie: 30GB for English, sub-millisecond lookup
- CMS: 5 rows × 272 cols × 4 bytes = 5KB for 1% error, 99% confidence
- CDN hit rate for top 10K prefixes: covers ~90% of traffic
- Trie hot-reload: ~60 seconds to load 30GB from SSD (500 MB/s SSD read speed)
- Debounce: 200ms reduces requests by ~3×

---

*Practice delivering the two-pipeline insight in 90 seconds as your opening thesis: "This system has two distinct data flows — a low-latency serving path and a high-throughput aggregation pipeline — and the core design challenge is keeping them synchronized." Interviewers will nod and ask you to build on it.*
