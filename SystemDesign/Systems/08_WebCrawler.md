# Distributed Web Crawler — Full System Design Walkthrough

> **Interview Format:** 45 minutes | **Level:** Senior / Staff Engineer  
> **Analogous real systems:** Googlebot, Bingbot, Common Crawl, Internet Archive Heritrix, Apache Nutch

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

Design a **distributed web crawler** that systematically browses the World Wide Web, downloading web pages and extracting links to discover new pages. The crawler feeds content into a search engine index, a web archive, or a data pipeline. It must:

- Start from a set of seed URLs and discover new URLs by parsing HTML
- Download 1 billion pages, respecting website policies (robots.txt, crawl-delay)
- Avoid re-crawling the same URL (deduplication at massive scale)
- Handle the dynamic nature of the web — pages change, new pages appear, old pages disappear
- Scale across hundreds of crawler workers while being polite to web servers
- Store extracted content for downstream processing (indexing, archiving)

The central tensions in web crawling are:
1. **Politeness vs throughput:** Crawling too fast gets you IP-banned; crawling too slow means you miss freshness windows.
2. **Freshness vs coverage:** A finite crawler fleet must choose between recrawling popular/changing pages frequently and discovering new pages broadly.
3. **Deduplication at scale:** 1 billion URLs sounds like a lot until you realize the web has near-infinite spam and mirror sites creating near-duplicate content.

---

## 2. Clarifying Questions

### Q1: What is the goal of the crawl — search indexing, archiving, or data extraction?
**Expected answer:** General-purpose search engine crawl (like Googlebot). We want to index the content of pages for a search engine, not just archive them. This means we care about page freshness (high-priority pages recrawled frequently) and we want to prioritize quality pages over spam.  
**Why it matters:** An archiving crawler (Common Crawl) aims for completeness; a search engine crawler aims for quality + freshness. These change how we prioritize URLs.

### Q2: How large is the target web? How many pages do we need to crawl?
**Expected answer:** 1 billion pages initially, representing a meaningful crawl of the accessible web (the web has ~50-100 billion indexable pages; we're targeting a significant but finite subset).  
**Why it matters:** 1B pages × avg page size = storage requirement. It also determines the Bloom filter size for deduplication.

### Q3: What crawl rate are we targeting, and over what time window?
**Expected answer:** 10 pages per second per crawler worker. With enough workers, we want to complete the initial crawl of 1B pages within a reasonable timeframe (days to weeks, not months).  
**Why it matters:** This is the primary capacity driver. At 10 pages/sec/worker, to crawl 1B pages in 30 days requires ~386 workers crawling round the clock.

### Q4: How should we handle dynamic/JavaScript-heavy pages?
**Expected answer:** Out of scope for this design. We assume all content is server-side rendered HTML. JavaScript rendering (headless Chrome) is 10-50× slower than raw HTTP fetching and is a separate problem.  
**Why it matters:** JavaScript rendering (like Googlebot's second-wave rendering) requires a separate fleet of headless browsers and a completely different pipeline. Scoping this out is correct.

### Q5: What is our policy for how frequently to recrawl already-visited pages?
**Expected answer:** Tiered recrawl schedule. High-quality, frequently changing pages (news sites, Wikipedia) recrawled daily. Mid-tier pages recrawled weekly. Low-quality/static pages recrawled monthly.  
**Why it matters:** Defines the recrawl scheduling component and how much of the crawler capacity is devoted to recrawling vs new page discovery.

### Q6: How do we handle robots.txt and crawl delays?
**Expected answer:** We must fully comply with robots.txt. If a domain specifies `Crawl-delay: 10`, we wait 10 seconds between requests to that domain. We also set a respectful default crawl delay (1-5 seconds per domain) even when robots.txt doesn't specify one.  
**Why it matters:** This is both ethical and practical — crawling without politeness gets your IP ranges banned, which breaks the crawler entirely. The politeness requirement fundamentally shapes the URL frontier architecture.

### Q7: Do we need to handle URL canonicalization and near-duplicate detection?
**Expected answer:** URL canonicalization yes (remove session IDs, sort query params, normalize case). Near-duplicate content detection (SimHash) is desirable but can be a follow-up. Exact duplicate detection (via content hash) is required.  
**Why it matters:** URLs like `http://example.com/page?id=1&session=abc123` and `http://example.com/page?id=1&session=xyz456` are the same content. Without canonicalization, we crawl the same content billions of times.

---

## 3. Functional Requirements

| # | Requirement |
|---|-------------|
| FR1 | Accept a set of seed URLs to begin the crawl |
| FR2 | Download the HTML content of each URL |
| FR3 | Parse HTML to extract all hyperlinks from the page |
| FR4 | Deduplicate URLs — do not crawl the same URL twice |
| FR5 | Respect robots.txt — check Disallow rules and Crawl-delay directives |
| FR6 | Apply crawl rate limits per domain (politeness) |
| FR7 | Store page content (HTML, metadata) for downstream processing |
| FR8 | Schedule recrawls of previously crawled pages based on change frequency |
| FR9 | Detect and handle spider traps (infinite URL generation) |
| FR10 | Support URL prioritization (high-quality domains crawled first/more frequently) |

**Out of scope:**
- JavaScript rendering
- Authentication-required pages
- Crawling dark web / Tor
- Real-time indexing (indexing is downstream of crawling)
- CAPTCHA solving

---

## 4. Non-Functional Requirements

| Category | Target |
|----------|--------|
| **Throughput** | 400 pages/second aggregate (10 pages/sec × 40 active workers) — scaling up to 4,000+ pages/second |
| **Initial crawl completion** | 1B pages in 30 days at 400 pages/second |
| **Politeness** | Maximum 1 request every 5 seconds to any single domain (configurable per domain based on their robots.txt) |
| **Freshness** | High-priority pages re-crawled within 24 hours; mid-priority within 7 days |
| **Storage** | 1B pages × avg 50KB HTML = 50 TB raw; 10 TB after compression and deduplication |
| **Deduplication accuracy** | <0.1% miss rate (a previously crawled URL must not be re-crawled within the same crawl cycle) |
| **Availability** | Crawler workers can fail; system must continue without manual intervention |
| **Scalability** | Linear scale-out by adding worker nodes |

---

## 5. Capacity Estimation

### 5.1 Crawl Rate and Worker Count

```
Target: crawl 1B pages in 30 days

Pages per second to meet target:
  1,000,000,000 / (30 × 86,400) = 1,000,000,000 / 2,592,000 ≈ 386 pages/second

With 10 pages/second per worker:
  Workers needed: 386 / 10 = 39 workers → round up to 50 workers
  (includes headroom for failed workers, slow domains, politeness delays)

At 50 workers × 10 pages/second = 500 pages/second
1B pages / 500 = 2,000,000 seconds = 23 days  ← meets target with margin
```

### 5.2 Storage Requirements

```
Average HTML page size:         50 KB (uncompressed)
After gzip compression:         ~10 KB per page
1B pages compressed:            1B × 10 KB = 10 TB

URL storage (dedup index):
  Average URL length:           80 bytes
  1B URLs (SHA-256 fingerprint): 1B × 32 bytes = 32 GB  ← for exact hash dedup
  Bloom filter for 1B URLs:     1B × 10 bits = 10 Gb = 1.25 GB  ← for approximate dedup

URL frontier (unprocessed URLs):
  Estimated discovered URLs:    5-10B (most filtered as duplicates or disallowed)
  Active frontier size:         ~100M URLs at any time
  URL queue storage:            100M × 100 bytes = 10 GB  ← in Redis / Kafka
```

### 5.3 Network Bandwidth

```
Pages crawled per second:       500
Average response size:          50 KB (headers + HTML)
Download bandwidth:             500 × 50 KB = 25 MB/second = 200 Mbps
Per worker:                     200 Mbps / 50 = 4 Mbps/worker  ← trivial

DNS lookups:
  New domains per day:          ~1M (most pages on domains already seen)
  DNS lookups/second:           ~12/second — use aggressive DNS caching
```

### 5.4 robots.txt Compliance

```
Domains encountered:            ~100M unique domains in 1B pages
robots.txt fetches:             1 per domain = 100M robots.txt files
robots.txt average size:        2 KB
robots.txt storage:             100M × 2 KB = 200 GB
robots.txt TTL (cache):         24 hours (re-fetch daily)
Daily robots.txt refreshes:     100M / 86,400 ≈ 1,157 refreshes/second  ← non-trivial
  → Cache in Redis with 24h TTL; most domains accessed rarely, cache works well
```

---

## 6. High-Level Design

### 6.1 Architecture Overview

```
                        ┌──────────────────────────────────┐
                        │         SEED URL INPUT            │
                        │  (Alexa top 1M, manual input,    │
                        │   previously crawled URL sets)    │
                        └──────────────┬───────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                           URL FRONTIER (Central)                                  │
│                                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │  PRIORITY QUEUE (Redis Sorted Set by score)                              │     │
│  │  Score = f(domain_rank, page_rank_estimate, crawl_delay_next_eligible)  │     │
│  │  High score = crawl first                                                │     │
│  └──────────────────────────────────┬──────────────────────────────────────┘     │
│                                      │  Pop next eligible URL                     │
│  ┌───────────────────────────────────▼──────────────────────────────────────┐    │
│  │  POLITENESS QUEUE (per-domain back-queues)                                │    │
│  │  domain:nytimes.com → [url1, url2, url3, ...]  (FIFO per domain)         │    │
│  │  domain:example.com → [url4, url5, ...]                                  │    │
│  │  Selector: only emit URL from domain if last_crawled_ts + crawl_delay    │    │
│  │            < now (domain is eligible for crawling)                        │    │
│  └───────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────┬────────────────────────────────────────────┘
                                       │ Next URL to crawl
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                      CRAWLER WORKER FLEET (50-500 workers)                        │
│                                                                                   │
│  Worker 1              Worker 2              Worker N                             │
│  ┌───────────────┐    ┌───────────────┐    ┌───────────────┐                     │
│  │ Fetch URL     │    │ Fetch URL     │    │ Fetch URL     │                     │
│  │ via HTTP/S    │    │ via HTTP/S    │    │ via HTTP/S    │                     │
│  │               │    │               │    │               │                     │
│  │ Check         │    │ Check         │    │ Check         │                     │
│  │ robots.txt    │    │ robots.txt    │    │ robots.txt    │                     │
│  │ (cached)      │    │ (cached)      │    │ (cached)      │                     │
│  │               │    │               │    │               │                     │
│  │ Parse HTML    │    │ Parse HTML    │    │ Parse HTML    │                     │
│  │ Extract links │    │ Extract links │    │ Extract links │                     │
│  │               │    │               │    │               │                     │
│  │ Canonicalize  │    │ Canonicalize  │    │ Canonicalize  │                     │
│  │ URLs          │    │ URLs          │    │ URLs          │                     │
│  └───────┬───────┘    └───────┬───────┘    └───────┬───────┘                     │
│          │                    │                    │                             │
│          └────────────────────┼────────────────────┘                             │
└───────────────────────────────┼──────────────────────────────────────────────────┘
                                │
              ┌─────────────────┼─────────────────────┐
              │                 │                     │
              ▼                 ▼                     ▼
   ┌─────────────────┐  ┌────────────────┐  ┌────────────────────────────┐
   │  URL DEDUP      │  │  CONTENT       │  │  RECRAWL SCHEDULER          │
   │  SERVICE        │  │  STORE         │  │                            │
   │                 │  │                │  │  - Record crawl_ts + hash   │
   │  Bloom Filter   │  │  Raw HTML      │  │  - Detect page change rate  │
   │  (1.25 GB RAM)  │  │  → Kafka topic │  │  - Schedule next crawl      │
   │                 │  │  → S3 (Parquet)│  │    based on change freq     │
   │  SHA-256 DB     │  │  → Metadata DB │  │  - Write back to URL        │
   │  (32 GB)        │  │    (PostgreSQL) │  │    Frontier with new score  │
   └─────────────────┘  └────────────────┘  └────────────────────────────┘
              │
              │ New (not seen) URLs
              ▼
   Back to URL Frontier
```

### 6.2 Component Responsibilities

**URL Frontier:**  
The brain of the crawler. Manages the queue of URLs to be crawled, ordered by priority and gated by politeness constraints. This is the most architecturally complex component — see Deep Dive 7.1.

**Crawler Workers:**  
Stateless workers that each maintain a pool of open HTTP connections. Each worker:
1. Pops a URL from the frontier
2. Checks robots.txt (from cache or fetches it fresh)
3. Makes the HTTP GET request (follows redirects up to 5 hops)
4. Parses the HTML response (Jsoup/BeautifulSoup equivalent)
5. Extracts all `<a href="...">` links
6. Canonicalizes extracted URLs
7. Submits extracted URLs to the dedup service
8. Submits page content to the content store
9. Reports crawl result to the recrawl scheduler

**URL Dedup Service:**  
Decides whether a URL has been seen before. Uses a two-layer approach: fast Bloom filter for the common case, exact SHA-256 hash database for confirmed deduplication. See Deep Dive 7.2.

**Content Store:**  
Stores the raw HTML and metadata for each crawled page:
- Raw HTML streamed to S3 as compressed Parquet partitioned by crawl date
- Metadata (URL, crawl timestamp, HTTP status, content hash, canonical URL) in PostgreSQL
- Extracted page content (title, body text, links) published to Kafka for downstream indexing pipeline

**Recrawl Scheduler:**  
Analyzes the content hash history of each page to determine its change frequency. Pages that change frequently get recrawled more often. Pushes recrawl tasks back into the URL Frontier with a timestamp and priority score.

---

## 7. Deep Dives

### 7.1 URL Frontier Architecture — Priority and Politeness Queues

**The core challenge:** The URL Frontier must simultaneously satisfy two competing constraints:
1. **Priority:** Crawl important/high-PageRank pages first
2. **Politeness:** Never make two requests to the same domain within the crawl delay period

A naive single FIFO queue violates politeness (top 1000 URLs might all be from `amazon.com`). A naive priority queue violates politeness for the same reason (most high-priority URLs come from a few major domains).

**Two-tier queue architecture:**

```
TIER 1: PRIORITY QUEUES (F priority queues, F = 5-10)
  Queue P1 (highest priority): Top-tier domains (Wikipedia, BBC, Reuters)
  Queue P2 (high priority):    PageRank > 0.8, news sites
  Queue P3 (medium priority):  PageRank 0.5-0.8, popular blogs
  Queue P4 (low priority):     PageRank 0.2-0.5
  Queue P5 (lowest priority):  Newly discovered, unknown domains

  URL → Priority Score → assigned to one of F queues
  Biased selection: pick from P1 with probability 0.5, P2 with 0.25, etc.

  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
  │  P1 ██▓▓ │ │  P2 ████ │ │  P3 ████ │ │  P4 ████ │ │  P5 ████ │
  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘
       └─────────────┴────────────┴────────────┴────────────┘
                              │ URL Router
                              ▼

TIER 2: BACK QUEUES (B back-queues, one per active domain group)
  Each back queue holds URLs for a single domain, strictly FIFO
  
  BackQ 1:  [nytimes.com/article1, nytimes.com/article2, nytimes.com/article3]
  BackQ 2:  [en.wikipedia.org/wiki/Python, en.wikipedia.org/wiki/Java]
  BackQ 3:  [reddit.com/r/news/1, reddit.com/r/news/2]
  ...
  BackQ B:  [smallblog.com/post1]

  Heap table: maps domain → (back_queue_id, next_eligible_crawl_time)
  - next_eligible = last_crawled_ts + crawl_delay_for_this_domain
  - Workers select the back queue with the smallest next_eligible time that is ≤ now

                              │ Worker requests next URL
                              ▼
  ┌────────────────────────────────────────────────────────────────────┐
  │  HEAP: domain → next_eligible_ts                                    │
  │  heap.peek() → domain with smallest next_eligible_ts               │
  │  if next_eligible_ts ≤ now: pop URL from that domain's back queue  │
  │  else: wait (or work on other tasks)                               │
  └────────────────────────────────────────────────────────────────────┘
```

**Router (Priority → Back Queue mapping):**

When a URL is popped from a priority queue, the Router assigns it to a back queue:
- If the URL's domain already has a back queue, add to that queue
- If not, create a new back queue for this domain (up to B total back queues)
- If all B back queues are occupied and this is a new domain, either wait or evict the back queue with the lowest-priority domain

**Implementation in Redis:**

```
Priority queues:
  Redis Sorted Set: frontier:priority:p1, frontier:priority:p2, ...
  Score = priority_score (higher = more important)
  Member = URL string
  Command: ZPOPMAX frontier:priority:p1 → pop highest-priority URL

Back queues:
  Redis List: frontier:back:{domain_hash}
  Command: RPUSH frontier:back:{domain_hash} {url}
           LPOP  frontier:back:{domain_hash}

Eligibility heap:
  Redis Sorted Set: frontier:eligibility
  Score = next_eligible_timestamp
  Member = domain_hash
  Command: ZRANGE frontier:eligibility 0 0 WITHSCORES → peek minimum
           ZPOPMIN frontier:eligibility → pop most eligible domain
```

**Worker selection algorithm:**

```python
def get_next_url_to_crawl() -> URL:
    while True:
        # Find the most eligible domain (ready to crawl and not in cooldown)
        domain_hash, eligible_ts = redis.zrange(
            "frontier:eligibility", 0, 0, withscores=True
        )

        if eligible_ts > time.now():
            # All domains are in cooldown — brief sleep to avoid busy-wait
            time.sleep(0.1)  # 100ms, then re-check
            continue

        # Pop the URL from this domain's back queue
        url = redis.lpop(f"frontier:back:{domain_hash}")
        if url is None:
            # Domain's back queue is empty — remove from eligibility set
            redis.zrem("frontier:eligibility", domain_hash)
            continue

        # Update the domain's next eligible crawl time
        crawl_delay = get_crawl_delay(domain_hash)  # from robots.txt cache
        next_eligible = time.now() + crawl_delay
        redis.zadd("frontier:eligibility", {domain_hash: next_eligible})

        return url
```

**Sizing the back queues:**

```
Number of back queues B should be ≥ number of workers.
With 50 workers, have B = 500 back queues (10× workers).
This ensures workers rarely have to wait for an eligible domain.

If only 50 back queues existed, as soon as a worker finishes a request,
it must wait for one of those 50 domains to become eligible. With a 5-second
crawl delay and 50 workers each needing a new URL every 1-10 seconds,
workers would frequently sit idle.
```

### 7.2 Deduplication with Bloom Filters

**The problem:** Before adding a newly discovered URL to the frontier, we must check if it has already been crawled (or is already in the frontier). With 1 billion crawled URLs and 5-10 billion discovered URLs, an exact hash lookup in a database for every discovered URL is too slow and too expensive.

**What is a Bloom Filter?**

A Bloom filter is a probabilistic, space-efficient data structure that answers the question "have I seen this item before?" with the following properties:
- **No false negatives:** If the filter says "not seen", the item was definitely never added
- **Possible false positives:** If the filter says "seen before", there is a small probability it is wrong (false positive rate ε)
- **Cannot delete:** Once an item is added, it cannot be removed (use Counting Bloom Filter or Cuckoo Filter if deletion is needed)

**Bloom filter mechanics:**

```
Data structure: bit array of size m bits, k independent hash functions

ADD(url):
    for i in range(k):
        bit_position = hash_i(url) % m
        bit_array[bit_position] = 1

QUERY(url):  → returns "maybe seen" or "definitely not seen"
    for i in range(k):
        bit_position = hash_i(url) % m
        if bit_array[bit_position] == 0:
            return "definitely not seen"   ← at least one bit is 0 → cannot be in set
    return "maybe seen"                    ← all bits are 1 → probably in set (or collision)
```

**False positive rate formula:**

Given:
- n = number of items inserted
- m = size of bit array in bits
- k = number of hash functions

```
Optimal k = (m/n) × ln(2)
False positive probability ε = (1 - e^(-kn/m))^k

For ε = 0.01 (1% false positive rate):
    m/n = -ln(ε) / (ln(2))² = 9.6 bits per element
    k = -(log₂ ε) = -(log₂ 0.01) = 6.64 → use k = 7 hash functions

For n = 1B items, ε = 0.01:
    m = 9.6 × 1B = 9.6 Gb = 1.2 GB  ← fits entirely in RAM

For ε = 0.001 (0.1% false positive rate):
    m/n = 14.4 bits per element
    m = 14.4 × 1B = 14.4 Gb = 1.8 GB  ← still fits in RAM

For ε = 0.0001 (0.01% false positive rate):
    m = 19.2 Gb = 2.4 GB  ← still fits in RAM
```

**Key insight:** Even at 0.01% false positive rate, we only use 2.4 GB of RAM to index 1 billion URLs. This is 13× more efficient than storing just the SHA-256 hashes (32B × 1B = 32 GB), and infinitely more efficient than storing full URL strings.

**The trade-off of false positives in crawling:**

A false positive means: a URL that was NOT previously crawled is incorrectly flagged as "already seen" and is never crawled. For a web crawler, this is acceptable — we miss crawling some URLs (a small fraction), but we never crawl a URL twice. The search engine index may be missing some pages, but it won't have massive duplication.

If we instead used a false negative (allowed URLs to be re-crawled when they shouldn't), we'd crawl the same URL infinitely — catastrophic.

**Two-tier deduplication (Bloom filter + URL fingerprint database):**

The Bloom filter handles the common case (already seen). But Bloom filters cannot tell you when a URL was last crawled, its content hash, or its priority score. For those, use a secondary URL fingerprint database (Redis or Cassandra):

```
Layer 1: Bloom Filter (in RAM, all workers share it via Redis)
  - Check: "Is this URL in the filter?"
  - If NO: URL is new → add to filter + URL fingerprint DB + frontier
  - If YES: URL was likely seen before → check Layer 2

Layer 2: URL Fingerprint Database (Cassandra, persistent)
  Key:   SHA-256(canonicalized_url) → 32 bytes (hex: 64 chars)
  Value: {url, first_crawled_ts, last_crawled_ts, content_hash, recrawl_due_ts}

  - If key not found: this was a Bloom false positive → treat as new URL
  - If key found: URL is a confirmed duplicate → discard
  - If key found and recrawl_due_ts < now: this URL is due for recrawl → add to frontier
```

**Bloom filter scaling:**

As we crawl more pages over time, the Bloom filter fills up and the false positive rate increases. Solutions:

1. **Rotate periodically:** Keep two filters: the current one (for this crawl cycle) and a cumulative one. Start a fresh filter at the beginning of each monthly crawl cycle.
2. **Counting Bloom Filter:** Allows deletion at the cost of 4-8× more memory per element.
3. **Cuckoo Filter:** Modern alternative to Bloom filter, supports deletion, lower space overhead (1 bit/element at 0.1% FPR vs 9.6 bits/element for Bloom).

**Bloom filter implementation considerations:**

```python
class DistributedBloomFilter:
    """
    Shared across all worker nodes via Redis bit operations.
    Redis SETBIT and GETBIT allow atomic single-bit operations.
    """
    def __init__(self, n_items=1_000_000_000, error_rate=0.001):
        self.m = math.ceil(-n_items * math.log(error_rate) / (math.log(2)**2))
        # m = 14.4B bits = 1.8 GB for n=1B, ε=0.001

        self.k = math.ceil(math.log(2) * self.m / n_items)
        # k = 10 hash functions

        self.hash_functions = [mmh3_hash_variant(seed=i) for i in range(self.k)]

    def add(self, url: str):
        for fn in self.hash_functions:
            bit_pos = fn(url) % self.m
            redis.setbit("bloom:urls", bit_pos, 1)

    def contains(self, url: str) -> bool:
        for fn in self.hash_functions:
            bit_pos = fn(url) % self.m
            if not redis.getbit("bloom:urls", bit_pos):
                return False
        return True  # Probably seen (small false positive probability)
```

**Alternative: URL fingerprinting without Bloom filter**

Content-based fingerprinting: instead of (or in addition to) URL deduplication, detect near-duplicate content using SimHash or MinHash. Two pages with identical SimHash signatures (within Hamming distance 3) are considered duplicates regardless of URL. This catches:
- Mirror sites (same content, different domain)
- Scraped content (site A copies site B's articles)
- Paginated near-duplicates ("/page/1" and "/page/2" of a mostly identical template)

SimHash is a locality-sensitive hash: similar documents hash to similar bit patterns. Hamming distance between two SimHashes correlates with document similarity.

### 7.3 Politeness and robots.txt Compliance

**robots.txt protocol:**

The Robots Exclusion Protocol (RFC 9309) defines a text file at `https://example.com/robots.txt` that instructs crawlers which pages they may or may not access.

```
# Example robots.txt for nytimes.com
User-agent: *
Disallow: /search/
Disallow: /account/
Crawl-delay: 10

User-agent: Googlebot
Disallow: /ads/
Crawl-delay: 1

User-agent: BadBot
Disallow: /
```

**Parsing rules:**
- `User-agent: *` applies to all crawlers
- `User-agent: Mybot` applies only to crawlers that identify themselves as "Mybot" in their User-Agent header
- `Disallow: /path/` — do not crawl any URL starting with /path/
- `Allow: /path/specific.html` — can override a more general Disallow
- `Crawl-delay: N` — wait N seconds between requests to this domain
- `Sitemap: https://example.com/sitemap.xml` — here are all my URLs (a gift to crawlers)

**Sitemaps:**  
If a site provides a sitemap (most large sites do), parse it first. A sitemap lists all canonical URLs with their last-modified dates and change frequencies — exactly what we need for recrawl scheduling. Sitemaps are higher quality than link extraction because they only include URLs the site owner wants indexed.

**robots.txt caching strategy:**

```
Fetch robots.txt:   Once per domain, on first encounter
Cache TTL:          24 hours (robots.txt rarely changes)
Cache location:     Redis Hash, key="robots:{domain}", field="content"
Cache miss:         Fetch from origin, parse, store in Redis

robots.txt size:    Usually < 5 KB (occasionally megabytes on large sites)
Total cache size:   100M domains × 2 KB avg = 200 GB → too large for Redis RAM
  → Use Redis for hot domains (top 1M), Cassandra for the long tail

robots.txt fetch rate limiting:
  We fetch robots.txt before the first URL per domain
  With 100M domains and 30-day crawl: ~38 new domains/second
  robots.txt fetches: 38/second — trivial
```

**Politeness beyond robots.txt:**

Even if robots.txt doesn't specify a crawl delay, we set a default:

```
Default crawl delay: 1 second between requests to the same domain (our default)
Minimum crawl delay: 1 second (regardless of robots.txt — overrides Crawl-delay: 0)
Maximum requests:    1 concurrent request per domain at any time

Respectful crawl rate:
  nytimes.com: Crawl-delay: 5 → 1 request / 5 seconds
  small-blog.com: Crawl-delay: 0 (or unspecified) → use default 1 request / 1 second

This means: a 50-worker crawler servicing 100,000 domains
  can crawl each domain at 1 req/sec without any domain getting more than 1 req/sec
  Total throughput: 50 workers × each waiting for eligible domains
  → Workers spend some time waiting; this is acceptable and by design
```

**Spider trap detection:**

A spider trap is a web page that generates infinite URLs — either a calendar with infinite forward/back navigation (`?month=1&year=9999`), a session ID in the URL, or a dynamic page that generates a new URL for every access.

Detection strategies:
1. **URL length limit:** Do not crawl URLs longer than 500 characters. Many spider traps generate very long URLs.
2. **Depth limit:** Do not follow more than 50 links deep from the starting page. A site that requires 50 clicks to reach a page is either a trap or worthless.
3. **Path component repeat detection:** If a URL contains the same path component more than 3 times (`/a/b/a/b/a/b/...`), it's a trap.
4. **Per-domain URL count limit:** Do not crawl more than 100,000 URLs from any single domain (unless it's a very large legitimate site, add an allowlist). Most sites have fewer than 10,000 pages; 100,000 is already generous.

```python
def is_spider_trap(url: str, domain: str) -> bool:
    # Check 1: URL too long
    if len(url) > 500:
        return True

    # Check 2: Too deep in the path
    path_depth = url.count('/')
    if path_depth > 30:
        return True

    # Check 3: Repeating path components
    path_parts = urllib.parse.urlparse(url).path.split('/')
    if len(path_parts) != len(set(path_parts)) * 0.8:  # >20% repeats
        return True

    # Check 4: Domain has too many crawled URLs
    domain_count = url_db.count_by_domain(domain)
    if domain_count > DOMAIN_URL_LIMIT.get(domain, 100_000):
        return True

    # Check 5: URL contains session ID pattern
    if re.search(r'(session|sess|sid|jsessionid)=[a-z0-9]{16,}', url, re.I):
        return True

    return False
```

### 7.4 Content Storage and Recrawl Scheduling

**Content storage pipeline:**

```
Crawler Worker fetches page
    │
    │ Raw HTTP response
    ▼
Content Processor
    │
    ├── Extract metadata:
    │     title, description, canonical_url, last_modified
    │     HTTP headers: Content-Type, ETag, Last-Modified, Cache-Control
    │     HTTP status: 200, 301, 404, 429, 503
    │
    ├── Compute content hash:
    │     SHA-256(normalized_html)  ← detect if page changed since last crawl
    │
    ├── Store raw HTML:
    │     S3 path: s3://crawl-bucket/{date}/{domain}/{url_hash}.html.gz
    │     Partition by crawl date for efficient archival and deletion
    │
    ├── Publish to Kafka (raw-pages topic):
    │     Payload: {url, content_hash, html, metadata, crawl_ts}
    │     Downstream: indexing pipeline, link graph builder, language detector
    │
    └── Update URL metadata database (PostgreSQL / Cassandra):
          url_hash, canonical_url, last_crawled_ts, content_hash,
          http_status, crawl_depth, domain, outlinks_count

    │
    ▼
Recrawl Scheduler reads the URL metadata DB
```

**Recrawl scheduling — change frequency detection:**

The goal is to crawl frequently-changing pages more often and rarely-changing pages less often. We learn change frequency from history:

```python
class RecrawlScheduler:
    def schedule_recrawl(self, url: str, new_content_hash: str):
        history = url_db.get_crawl_history(url)  # list of (ts, content_hash) tuples

        if len(history) < 2:
            # Not enough history — use domain-default crawl interval
            next_crawl = now() + domain_default_interval(url.domain)
        else:
            # Compute change frequency from history
            changes = sum(
                1 for i in range(1, len(history))
                if history[i].content_hash != history[i-1].content_hash
            )
            observations = len(history) - 1
            change_rate = changes / observations  # probability of change per crawl

            # Optimal recrawl interval: crawl more often if change_rate is high
            # Formula: interval = max(1h, target_freshness / change_rate)
            target_freshness_seconds = 86400  # we want to know about changes within 24h
            interval_seconds = min(
                max(3600, target_freshness_seconds / max(change_rate, 0.001)),
                30 * 86400  # max 30 days between crawls
            )
            next_crawl = now() + interval_seconds

        # Push back into frontier with new score
        priority_score = compute_priority(url, next_crawl)
        frontier.push(url, scheduled_time=next_crawl, priority=priority_score)
```

**Content hash comparison to detect real changes:**

Not every re-fetch with a different content hash is a meaningful change. Dynamic pages embed timestamps, ad slots, and counters that change on every request. We want to detect changes to the meaningful content, not ephemeral decorations.

```
Normalization before hashing:
  1. Remove <script> tags (JavaScript often changes without content change)
  2. Remove <style> tags
  3. Remove HTML comments
  4. Collapse whitespace
  5. Remove dynamic attribute values (timestamps, session tokens in class names)
  6. Lowercase all text

Content hash = SHA-256(normalized_html)

If content_hash_new == content_hash_old:
    → Page didn't meaningfully change
    → Back off: double the recrawl interval (up to 30 days max)

If content_hash_new != content_hash_old:
    → Page changed
    → Maintain or decrease recrawl interval
```

**HTTP conditional requests — ETags and Last-Modified:**

Before fetching the full page, send a conditional HTTP request:

```http
GET /article/12345 HTTP/1.1
Host: nytimes.com
If-None-Match: "686897696a7c876b7e"   ← ETag from previous crawl
If-Modified-Since: Tue, 23 Jan 2024 12:00:00 GMT
```

If the page hasn't changed, the server responds with `304 Not Modified` — no body. This saves:
- Bandwidth (no body download)
- Content storage (nothing to store)
- CPU (no parsing)

Servers that support ETags / Last-Modified allow the crawler to dramatically reduce its bandwidth footprint for recrawls. Not all servers support this, but many high-quality sites do.

**Storage tiering and retention:**

```
Hot storage (S3 Standard):
  Current crawl snapshot + previous crawl (for change detection)
  Retention: 60 days
  Size: 2 snapshots × 10 TB = 20 TB

Warm storage (S3 Infrequent Access):
  Archive of all crawls for 2 years
  Size: 24 monthly snapshots × 10 TB = 240 TB
  Cost: ~$3/TB/month on S3 IA = ~$720/month

Cold storage (S3 Glacier):
  Long-term archive (Common Crawl style)
  Retention: indefinite
  Cost: ~$0.004/GB/month
```

---

## 8. Trade-offs & Alternatives

### Kafka vs Redis for URL Frontier

| Approach | Throughput | Ordering | Durability | Complexity |
|----------|------------|----------|------------|------------|
| Redis Sorted Set | Very high | By score | Persistence optional | Low |
| Kafka | Extremely high | By partition | Yes (configurable retention) | High |
| PostgreSQL queue | Medium | Flexible | Yes | Low |

**Recommendation:** Redis for the active URL frontier (the hot path — workers pulling URLs). Kafka for the ingest pipeline (newly discovered URLs flowing in). The URL Frontier service sits between them, deduplicating and scoring URLs from Kafka before writing to Redis.

Using Kafka as the frontier directly is tempting but has problems: Kafka doesn't support priority-based consumption natively (you'd need a consumer that reads the whole topic and re-prioritizes). Redis sorted sets give you O(log N) priority operations naturally.

### Bloom Filter vs Exact Hash Database for Dedup

For **primary deduplication** (seen before in this crawl cycle): Bloom filter wins. 1.8 GB for 1B items at 0.1% FPR — unbeatable.

For **recrawl scheduling** (when was this URL last crawled? did its content change?): Exact database wins. The Bloom filter doesn't store metadata. Use Cassandra for the URL fingerprint store (wide column, time-series queries for change history).

**Never use only the exact database for the hot path.** Even Cassandra at 10ms p99 read latency × 500 discovered URLs per second = 5 seconds of blocking reads. The Bloom filter must handle the hot path.

### BFS vs Priority-First Crawl Order

**BFS (Breadth-First Search):** Start from seed URLs, discover their outlinks (level 1), then all outlinks of level-1 pages (level 2), etc. Simple, complete for reachable graph.

**Problem with BFS:** The web is massive and heavily spammy. Strict BFS would quickly reach spam farms that link to each other infinitely. BFS with unlimited depth = spider trap without explicit trap detection.

**Priority-First:** Score every URL before it enters the frontier. High-score URLs (from high-PageRank domains, linked to from many pages) are crawled first regardless of BFS level. This is what Googlebot does — a quality-weighted BFS.

**Recommendation:** Priority-first with a BFS-like score boost for URLs discovered from high-quality pages. A URL on Wikipedia is worth crawling even if it hasn't been linked to many times yet.

### DNS Caching

Each crawl request requires DNS resolution. With 500 pages/second, if each page is a new domain, that's 500 DNS lookups/second — too many for public DNS resolvers.

**Solution:** Dedicated DNS caching resolver in the crawler infrastructure (PowerDNS, Unbound). Cache TTL = max(DNS record TTL, 3600 seconds). Pre-warm with Alexa top 1M domain resolutions.

```
DNS cache hit rate: 99%+ (most pages are from a few thousand high-traffic domains)
Miss rate: ~1% = 5 DNS misses/second at 500 pages/second → trivial
```

---

## 9. Failure Scenarios

### Scenario 1: Crawler Worker Crashes Mid-Fetch

**Impact:** The URL was dequeued from the frontier but not crawled. It is lost.  
**Prevention:** Use a **two-phase dequeue**: the URL is marked as "in-flight" when dequeued, not removed. If the worker doesn't mark it "completed" within 60 seconds (the in-flight timeout), it is re-queued. Implement with Redis: `GETSET url:inprogress {url} → set with TTL 60s`. If key still exists after 60s → re-add to frontier.

### Scenario 2: URL Frontier Redis Node OOM

**Impact:** No new URLs can be dequeued. All workers stall.  
**Mitigation:** Frontier Redis uses persistence (RDB snapshots every 5 minutes + AOF log). On OOM: evict low-priority URLs from the priority queues (they can be re-discovered). The URL fingerprint database (Cassandra) has the ground truth of all known URLs; the frontier can be rebuilt by a re-scan query.

### Scenario 3: Target Website Returns 429 (Too Many Requests)

**Cause:** Our crawler is too aggressive despite following robots.txt crawl-delay.  
**Response:** Respect the `Retry-After` header in the 429 response. Implement exponential backoff: double the crawl delay for this domain (from 1s → 2s → 4s → max 60s). Alert if >1% of requests to a domain return 429.

### Scenario 4: Bloom Filter False Positive Rate Exceeds Threshold

**Cause:** After crawling 1B pages, the Bloom filter is at capacity. FPR increases from 0.1% to 1%.  
**Detection:** Monitor: `bloom_elements_added / bloom_capacity`. Alert when > 90%.  
**Recovery:** Rotate the Bloom filter (start a new one for the next crawl cycle). The URL fingerprint DB (Cassandra) contains the definitive dedup record; the Bloom filter is just a fast cache in front of it.

### Scenario 5: Spider Trap Causes One Domain to Flood the Frontier

**Cause:** www.infinitecalendar.com generates `?date=2024-01-01`, `?date=2024-01-02`, ..., `?date=2099-12-31` — 27,394 URLs discovered in 30 seconds.  
**Detection:** Per-domain URL count exceeds DOMAIN_URL_LIMIT (100,000). Alert.  
**Response:** Stop enqueuing URLs from this domain. Add domain to a temporary blocklist. Review manually and add spider trap rules.

### Scenario 6: Sudden Surge of Redirects (301/302 Chains)

**Cause:** A large website migrates from HTTP to HTTPS, creating 301 redirects for every URL. Crawler follows redirect chains and stores both the original URL and the redirect target.  
**Impact:** Temporary surge in URLs processed. Both old and new URLs may appear in the frontier.  
**Mitigation:** After following a redirect chain, canonicalize the final URL as the authoritative one. Dedup against the canonical URL, not the original. Update the URL fingerprint DB to map old URL → new canonical URL.

---

## 10. Interview Tips

### Time management

```
0-5 min:   Clarifying questions — scale, goal, politeness requirements
5-10 min:  Functional + non-functional requirements (emphasize politeness)
10-15 min: Capacity math — pages/day, workers needed, storage
15-25 min: High-level architecture — frontier, workers, dedup, content store
25-40 min: Deep dives — URL frontier two-tier design, Bloom filter math
40-45 min: Recrawl scheduling, failure scenarios, trade-offs
```

### The key insight to land early

State this in your opening 2 minutes: **"A web crawler is fundamentally a BFS over a graph of URLs, where the main constraints are politeness (rate limiting per domain), deduplication at scale (Bloom filters), and freshness (recrawl scheduling based on change frequency). The URL frontier design must satisfy all three simultaneously."**

This frames the problem and shows you understand the non-obvious constraints.

### Most common interview follow-ups

1. **"How would you handle JavaScript-heavy sites?"** Answer: a separate two-pass system. First pass: raw HTML crawl (fast, cheap). Second pass: headless Chrome rendering for pages where the first-pass HTML was mostly empty. Queue second-pass pages separately, use a much smaller worker fleet (JS rendering is 50× more expensive).

2. **"How do you prioritize which pages to crawl first?"** Answer: Multi-factor score: domain PageRank (from link graph analysis), inlink count for the specific URL, freshness score (time since last crawl × predicted change rate), topic relevance (if we're building a vertical crawler).

3. **"What happens when a page returns 404?"** Answer: Mark the URL as dead in the URL fingerprint DB. Stop recrawling. If it was a high-value page (many inlinks), set an alert to re-check in 7 days (some 404s are transient). Notify the indexing pipeline to remove the page from the search index.

4. **"How do you crawl the same site from multiple workers in parallel without violating politeness?"** Answer: The URL frontier's politeness queue ensures only one URL per domain is in-flight at a time across all workers. The back queue + eligibility heap is global (shared Redis), so two workers cannot concurrently crawl the same domain.

### What to draw on the whiteboard

1. The two-tier URL frontier: priority queues (F queues by score) → router → back queues (per domain) → eligibility heap
2. Bloom filter structure: m-bit array with k hash functions, showing ADD and QUERY
3. The worker pipeline: fetch → check robots.txt → parse HTML → canonicalize links → dedup check → enqueue
4. The content hash change detection table: URL, crawl dates, hashes, computed change frequency

### Numbers to know cold

- Bloom filter: 10 bits/element → ~1% FPR; 14.4 bits/element → ~0.1% FPR
- 1B URLs × 10 bits = 1.25 GB — fits in RAM on one machine
- k optimal = (m/n) × ln(2) ≈ 7 hash functions for ε=0.01
- 50 workers × 10 pages/sec = 500 pages/sec → 1B pages in 23 days
- robots.txt: fetch once per domain, cache 24h, ~2KB avg per file
- DNS cache hit rate: ~99% (most pages share a few thousand domains)
- 1B pages × 50KB avg × gzip 5× = 10 TB compressed storage

---

*Practice explaining the Bloom filter math from memory. Every interviewer who asks about web crawlers will ask about deduplication, and showing you can derive the space requirement from epsilon and n in real-time (m ≈ n × -log(ε) / ln(2)²) is an immediate signal of deep knowledge rather than memorized talking points.*
