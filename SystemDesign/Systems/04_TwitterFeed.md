# System Design: Twitter/X Feed (Timeline)

> **Interview Format:** 45-minute Senior/Staff Engineer system design  
> **Difficulty:** Hard  
> **Core Themes:** Fan-out architecture, celebrity problem, Redis sorted sets, real-time delivery

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [Clarifying Questions](#2-clarifying-questions)
3. [Functional Requirements](#3-functional-requirements)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [Capacity Estimation](#5-capacity-estimation)
6. [High-Level Design](#6-high-level-design)
7. [Deep Dives](#7-deep-dives)
8. [Trade-offs and Alternatives](#8-trade-offs-and-alternatives)
9. [Failure Scenarios](#9-failure-scenarios)
10. [Interview Tips](#10-interview-tips)

---

## 1. Problem Statement

Design Twitter's home timeline — the feed of tweets a user sees when they open the app. When a user posts a tweet, it should appear in the home timelines of all their followers. When a user opens the app, they should see a personalized, chronological (or ranked) feed of tweets from people they follow.

This is one of the most famous and instructive system design problems because it has a known, elegant solution that took Twitter years and multiple rewrites to arrive at, and because it perfectly illustrates the tension between write-time fan-out (pushing tweet data to all followers at write time) and read-time fan-out (assembling the feed at read time from raw follow graph data).

The hard problem in this design is the celebrity problem: Katy Perry has 108 million followers. When she tweets, do you write to 108 million fan timeline caches in real time? That's 108 million write operations every time she posts. At the same time, Barack Obama has 133 million followers, and they both tweet within 10 seconds of each other. That's 241 million write operations in 10 seconds — from two people posting.

There is no solution that perfectly handles both regular users and celebrities. The correct answer is a hybrid model that treats them differently. Understanding that hybrid model, being able to explain why it exists, and drawing the architecture that implements it — that is what this interview is testing.

---

## 2. Clarifying Questions

**Q1: What exactly is the "timeline" — home feed, user profile feed, or both?**

> Expected answer: Design the home timeline (tweets from people you follow). Profile feed (all tweets by a specific user) is simpler and can be discussed as an extension.

Why this matters: Home timeline requires fan-out across the follow graph. Profile timeline is just a paginated read of a single user's tweets. They have fundamentally different architectures.

**Q2: Should the timeline be strictly chronological or ranked?**

> Expected answer: Start with reverse chronological (newest first). Ranking/recommendation is a separate system that can be layered on top.

Why this matters: Ranking requires a recommendation engine with ML models, which is a completely different design problem. Explicitly scope it out but mention you know it exists.

**Q3: What is the scale? How many DAU, tweets per day, average followers?**

> Expected answer: 300M DAU, 600M tweets/day. Average user has 200 followers. Some users (celebrities) have millions of followers.

Why this matters: This immediately tells you that fan-out is the core problem. 600M tweets/day with an average 200 followers per tweet = 120 billion fan-out operations per day. This cannot be done naively — you need a smart fan-out strategy.

**Q4: How far back should the timeline go? How many tweets per page?**

> Expected answer: Last 7 days or 800 tweets in feed, whichever is less. Paginated at 20-50 tweets per request.

Why this matters: Knowing the feed depth limits cache size calculation. You don't need to store an infinite timeline — just the last 800 tweets per user. This makes the feed cache a fixed-size bounded structure.

**Q5: Do we need real-time updates — should new tweets appear without a page refresh?**

> Expected answer: Yes. New tweets should appear in real-time as the user is viewing the feed (push new tweets to open clients).

Why this matters: Real-time updates require a WebSocket or Server-Sent Events infrastructure. This is a substantial additional component. Get explicit agreement before designing it.

**Q6: What about retweets? Does retweeting a tweet from a celebrity still cause fan-out issues?**

> Expected answer: Yes, retweets should appear in followers' timelines. A regular user retweeting a celebrity's tweet means their followers see the tweet — triggering another fan-out.

Why this matters: Retweets add a fan-out-of-a-fan-out dimension. You store a reference (tweet_id) not a copy, so the additional storage is small, but the fan-out itself still happens.

**Q7: Should tweets with media (images, video) be handled differently?**

> Expected answer: Yes. Images and video are stored in blob storage (S3) and served via CDN. The tweet just stores a reference to the media URL.

Why this matters: Storing media inline in the tweet table would be disastrous (100x larger). Keeping the tweet record small (100-500 bytes) and using CDN for media is the correct pattern.

---

## 3. Functional Requirements

1. **Post tweet:** Users can post a tweet with text (max 280 characters) and optionally images/videos.
2. **Home timeline:** View a reverse-chronological feed of tweets from followed users (last 800 tweets or 7 days).
3. **Follow/Unfollow:** Users can follow/unfollow other users.
4. **Retweet:** Users can retweet others' tweets. The retweet appears in the retweeter's followers' timelines.
5. **Like:** Users can like tweets. Like counts are displayed on tweets.
6. **Real-time updates:** New tweets from followed users appear in the open app without manual refresh.
7. **Media upload:** Tweets can include up to 4 images or 1 video.
8. **Search:** Full-text search across public tweets (treated as a separate component — out of scope for timeline design).

Out of scope:
- Direct messages
- Notification system (separate design)
- Trending topics / Explore tab
- Tweet ranking/recommendation (ML layer)
- Twitter Spaces / live audio

---

## 4. Non-Functional Requirements

1. **Latency:** Home timeline loads in < 200ms P99. This is the most important performance requirement — the feed is the product.
2. **Availability:** 99.99% for timeline reads. Users tolerate slightly delayed tweets (eventual consistency) but not a broken feed.
3. **Scale:** 300M DAU. 600M tweets/day. Peak read QPS: ~28,000 (300M users opening app over 12 active hours, each making ~4 feed requests/hour).
4. **Fan-out throughput:** Handle 600M tweets × 200 avg followers = 120B fan-out ops/day = ~1.4M fan-out writes/sec at peak.
5. **Consistency:** Eventual consistency for timeline updates. A tweet may take up to 30 seconds to appear in all followers' feeds (acceptable).
6. **Durability:** Tweets once posted are never lost. Tweet storage is the source of truth.
7. **Storage efficiency:** Store timeline references (tweet IDs), not tweet data copies. Tweet data is fetched once on read.

---

## 5. Capacity Estimation

### Write Path (Posting Tweets)

```
Tweets per day:                  600,000,000 (600M)
Seconds per day:                 86,400
Tweet write QPS (average):       600M / 86,400 ≈ 6,944 tweets/sec
Tweet write QPS (peak, 2x):      ~14,000 tweets/sec
```

### Fan-out Write Path

```
Average followers per tweeter:   200
Fan-out ops per tweet:           200 (one write to each follower's timeline cache)
Fan-out QPS (average):           6,944 × 200 ≈ 1,389,000 / sec ≈ 1.4M ops/sec
Fan-out QPS (peak, 2x):          ~2.8M ops/sec

Note: Celebrity tweets (10M followers) create a temporary spike of:
      One celebrity tweet → 10M fan-out ops in <1 second (pulled, not pushed)
```

### Read Path (Timeline Requests)

```
DAU:                             300,000,000
Active hours per day:            12 (users are more active during waking hours)
Feed requests per user per day:  ~10 (opens app ~10 times, each triggering a feed load)
Total feed requests per day:     300M × 10 = 3B/day
Read QPS (average):              3B / 86,400 ≈ 34,722/sec ≈ 35,000 reads/sec
Read QPS (peak, 2x):             ~70,000 reads/sec
```

### Storage

**Tweet storage:**
```
Tweet record size:               ~300 bytes
                                 (tweet_id, user_id, content, created_at, like_count, 
                                  retweet_count, reply_to_id, media_urls JSON)
Tweets per day:                  600M
Tweet storage per day:           600M × 300 bytes = 180 GB/day
Tweet storage per year:          ~65 TB/year
Tweet storage for 5 years:       ~325 TB
```

This requires a distributed database. Cassandra is purpose-built for this — partition by `user_id`, sort by `created_at` for efficient profile-page reads.

**Timeline cache (Redis):**
```
Cached timelines per user:       800 tweet IDs (tweet_id = int64 = 8 bytes)
                                 + metadata (score = timestamp = 8 bytes)
Cache entry size per tweet:      ~16 bytes (tweet_id + score in Redis sorted set)
Cache per user:                  800 × 16 bytes = 12,800 bytes ≈ 12.5 KB
Active users with cached feeds:  300M (all DAU, but use 10% for hot cache estimation)
                                 30M users with warm caches
Cache memory:                    30M × 12.5 KB = 375 GB
Redis cluster size:              375 GB / 60 GB per node = ~7 nodes (use 12 for headroom)
```

**Media storage:**
```
Tweets with media:               10% of 600M = 60M media tweets/day
Average media size:              1 MB (compressed images, thumbnails)
Media storage per day:           60M × 1 MB = 60 TB/day
```

Media is stored in S3 ($23/TB/month = $1,380/day for new media alone). In practice, video is stored at multiple resolutions and old media is tiered to cheaper storage. This is standard CDN + S3 lifecycle policy.

### Follow Graph Storage

```
Average followers per user:      200
Users:                           500M (registered, not just DAU)
Follow relationships:            500M × 200 = 100B edges
Storage per edge:                16 bytes (follower_id + following_id, each int64)
Total follow graph:              100B × 16 bytes = 1.6 TB
```

Follow graph fits in a large relational or graph database. For hot lookups (get all followers of user X), it's cached in Redis as a Set.

---

## 6. High-Level Design

### Architecture Overview

```
                    ┌────────────────────────────────────────────────────────────┐
                    │                       CLIENTS                              │
                    │              (iOS, Android, Web Browser)                   │
                    └──────────────────────┬─────────────────────────────────────┘
                                           │ HTTPS
                                           ▼
                    ┌────────────────────────────────────────────────────────────┐
                    │                   CDN (CloudFront)                         │
                    │   Static assets, media thumbnails, cached public profiles  │
                    └──────────────────────┬─────────────────────────────────────┘
                                           │
                                           ▼
                    ┌────────────────────────────────────────────────────────────┐
                    │              API Gateway / Load Balancer                   │
                    └──────┬────────────────────┬────────────────────────────────┘
                           │                    │
              POST /tweet  │    GET /timeline   │
                           ▼                    ▼
          ┌─────────────────────┐  ┌──────────────────────────────────────────┐
          │  Tweet Write Service │  │          Timeline Read Service           │
          │                     │  │                                          │
          │  1. Validate tweet  │  │  1. Get user_id from auth token          │
          │  2. Write to DB     │  │  2. Fetch timeline from Redis (feed cache)│
          │  3. Upload media    │  │  3. On cache miss: rebuild from DB       │
          │     to S3           │  │  4. Hydrate tweet IDs → tweet objects    │
          │  4. Publish to      │  │     (batch fetch from Tweet DB)          │
          │     Kafka           │  │  5. Return paginated list of tweets      │
          └──────────┬──────────┘  └──────────────────────────────────────────┘
                     │                                │
                     ▼                                ▼
          ┌──────────────────────────────────────────────────────────────────┐
          │                      Kafka                                       │
          │   Topic: tweet.created (triggers fan-out)                        │
          │   Topic: tweet.deleted                                           │
          │   Topic: follow.created / follow.deleted                         │
          └──────────────────────────────────────────────────────────────────┘
                     │
                     ▼
          ┌────────────────────────────────────────────────────────────────────┐
          │                    Fan-out Service                                 │
          │                                                                    │
          │   Receives tweet.created events                                    │
          │                                                                    │
          │   FOR REGULAR USERS (< 1M followers):                             │
          │     1. Load follower list from Graph DB / Cache                   │
          │     2. For each follower: ZADD timeline:{follower_id} score tweet_id │
          │     3. Trim timeline to max 800 entries (ZREMRANGEBYRANK)         │
          │     4. Publish to WebSocket notification topic                    │
          │                                                                    │
          │   FOR CELEBRITIES (>= 1M followers):                              │
          │     1. Skip push fan-out                                          │
          │     2. Mark tweet as "pull-required" in a celebrity tweet index   │
          │     3. Timeline Read Service merges celebrity tweets at read time  │
          └────────┬────────────────────────────────────────────────────────  ┘
                   │                                │
    Fan-out writes │              Celebrity tweets  │
                   ▼                                ▼
     ┌──────────────────────────┐    ┌─────────────────────────────────┐
     │   Redis Cluster          │    │   Celebrity Tweet Index         │
     │   (Feed Cache)           │    │   (Redis Sorted Set per celeb)  │
     │                          │    │                                 │
     │   Key: timeline:{uid}    │    │   Key: celeb_tweets:{celeb_id}  │
     │   Type: Sorted Set       │    │   Used by Timeline Read Service │
     │   Score: tweet_timestamp │    │   to merge at read time         │
     │   Value: tweet_id        │    │                                 │
     └──────────────────────────┘    └─────────────────────────────────┘
                   │
                   ▼
     ┌──────────────────────────────────────────────────────────────────┐
     │                     Cassandra (Tweet Store)                      │
     │   Partition key: user_id                                         │
     │   Sort key: created_at (reverse)                                 │
     │   Full tweet data: content, media, likes, retweets              │
     └──────────────────────────────────────────────────────────────────┘
                   │
                   ▼
     ┌──────────────────────────────────────────────────────────────────┐
     │               Graph DB / PostgreSQL (Follow Graph)               │
     │   Tables: follows (follower_id, following_id, created_at)        │
     │   Cached in Redis as Set: followers:{user_id}                    │
     └──────────────────────────────────────────────────────────────────┘
                   │
                   ▼
     ┌──────────────────────────────────────────────────────────────────┐
     │              WebSocket Server (Real-Time Updates)                │
     │   Clients maintain persistent WebSocket connection               │
     │   Server pushes new tweet_ids to open clients                    │
     │   State managed in Redis (user_id → WebSocket connection ID)     │
     └──────────────────────────────────────────────────────────────────┘
```

---

## 7. Deep Dives

### 7.1 Push vs. Pull vs. Hybrid Fan-out

This is the central intellectual question of the Twitter timeline design. Get this right and the rest falls into place.

**Option A: Pure Push (Fan-out on Write)**

When a user posts a tweet, immediately write the tweet ID to the feed cache of every follower:

```
User A (200 followers) posts tweet_id=12345:
  → Write to timeline:follower_1, timeline:follower_2, ..., timeline:follower_200
  → 200 Redis ZADD operations happen synchronously (async in practice)
  
When follower opens their feed:
  → Read timeline:{follower_id} from Redis → instant, O(1)
  → Hydrate tweet IDs by batch fetching from Cassandra
```

**Pros:**
- Read path is extremely fast — Redis sorted set lookup, no graph traversal
- All followers see the tweet at roughly the same time

**Cons — The Celebrity Problem:**
- Katy Perry (108M followers) posts one tweet
- Fan-out service must write to 108M Redis keys
- At 100,000 Redis writes/second, this takes: 108M / 100,000 = **18 minutes**
- During those 18 minutes, some followers have the tweet; most don't (inconsistent fan-out)
- System is flooded: 108M Redis writes crowd out normal fan-out for regular users
- Katy Perry, Taylor Swift, and Cristiano Ronaldo all tweet within 60 seconds of each other → 300M+ simultaneous writes → system is completely overwhelmed

Pure push fan-out is correct for regular users with few followers. It completely breaks down for celebrities.

**Option B: Pure Pull (Fan-out on Read)**

When a user opens their feed, compute it from scratch:

```
User opens their feed:
  → Fetch all users they follow: SELECT following_id FROM follows WHERE follower_id=user_id
  → For each followed user, fetch their recent tweets from Cassandra
  → Merge and sort all fetched tweets by timestamp
  → Return top 50 tweets
```

**Pros:**
- No fan-out required on write. Celebrity tweets are not a problem.
- Tweet deletes are handled automatically (the tweet is gone from the source; reads just don't find it)

**Cons — The Read Performance Problem:**
- Average user follows 200 people. Each feed load requires:
  - 1 query to follow graph (who do I follow?)
  - 200 queries to Cassandra (get recent tweets for each followed user)
  - 200-way merge sort
- Total: 201 DB queries + 200-way merge sort on every single feed load
- At 35,000 feed loads/sec, that's 35,000 × 201 = **7M DB queries/second**
- P99 latency: 200 DB reads in parallel takes ~100ms, plus merge time → ~150ms P99 under normal load
- Under load, DB read latency degrades → feed latency degrades → users see slow feeds

Pure pull is too expensive at scale. It might work if you have a read-optimized DB and aggressive caching, but the N+1 query problem (1 follow graph read + N tweet reads) is fundamental.

**Option C: Hybrid Fan-out (Twitter's Actual Approach)**

The insight: **most users have few followers; only celebrities have millions.** Apply different strategies based on follower count:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    HYBRID FAN-OUT DECISION                              │
│                                                                         │
│   When tweet is posted by user X:                                       │
│                                                                         │
│   IF X.follower_count < CELEBRITY_THRESHOLD (e.g., 1,000,000):         │
│     → Push: fan-out tweet_id to all followers' Redis timelines          │
│     → This handles 99.9% of all tweets                                  │
│                                                                         │
│   IF X.follower_count >= CELEBRITY_THRESHOLD:                           │
│     → No push fan-out                                                   │
│     → Tweet is stored normally in Tweet DB                              │
│     → Tweet ID added to celeb_tweets:{user_id} sorted set in Redis     │
│     → Read-time merge: when a follower loads their feed, the Timeline   │
│       Read Service also queries celeb_tweets for all celebrities        │
│       they follow, and merges those tweets in                           │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Read Path for Hybrid:**

```python
def get_home_timeline(user_id: str, cursor: int, count: int = 50) -> List[Tweet]:
    # Step 1: Get pre-computed feed (push-based, regular users)
    feed_tweet_ids = redis.zrevrange(
        f"timeline:{user_id}", 
        start=0, 
        end=count - 1, 
        withscores=True
    )
    
    # Step 2: Get celebrity tweet IDs (pull-based, merges at read time)
    followed_celebrities = get_followed_celebrities(user_id)  # cached in Redis
    celeb_tweet_ids = []
    for celeb_id in followed_celebrities:
        celeb_recent = redis.zrevrange(
            f"celeb_tweets:{celeb_id}",
            start=0,
            end=count - 1,
            withscores=True
        )
        celeb_tweet_ids.extend(celeb_recent)
    
    # Step 3: Merge regular feed + celebrity tweets, sort by timestamp (score)
    all_tweet_ids = sorted(
        feed_tweet_ids + celeb_tweet_ids,
        key=lambda x: x.score,  # score = timestamp
        reverse=True
    )[:count]
    
    # Step 4: Hydrate tweet IDs → full tweet objects (batch fetch from Cassandra)
    tweets = tweet_store.batch_get([tid for tid, _ in all_tweet_ids])
    
    return tweets
```

**Why is merging with celebrities cheap?**
- Average user follows ~5 celebrities (out of their 200 total follows)
- `celeb_tweets:{celeb_id}` sorted set holds only the last 100 tweets (small, always in Redis memory)
- 5 Redis sorted set range queries + a 5-way merge sort is trivially fast
- This is O(5 × 100) = 500 comparisons per feed load — microseconds

**The hybrid model perfectly solves the celebrity problem** by never writing to 108M Redis keys, while keeping the read path fast by precomputing feeds for the vast majority of tweets.

### 7.2 The Celebrity Problem — Full Solution

**Defining "Celebrity"**

The threshold is a business decision, but 1 million followers is a reasonable line. At 1M followers:
- Push fan-out at 100,000 Redis writes/second takes 10 seconds (acceptable for most users)
- At 10M followers, it takes 100 seconds — clearly unacceptable

Twitter reportedly uses thresholds much lower (some reports say 50,000 followers). The threshold also dynamically adjusts — a user who suddenly gains millions of followers (viral tweet) is promoted to celebrity treatment.

**Celebrity Detection**

```
On follow event:
  follower_count = HINCRBY user:{user_id}:meta "follower_count" 1
  
  if follower_count > CELEBRITY_THRESHOLD:
    redis.sadd("celebrities", user_id)  # Mark as celebrity
    # Retroactively clean up any pushed tweets:
    # Remove this user's tweets from all followers' timeline caches
    # (This is expensive; in practice, just let old pushed entries expire)
```

The celebrities set is cached in memory on every Fan-out Service instance. It's small (maybe 50,000 celebrities worldwide) and changes rarely.

**What Happens at the Boundary?**

A user hits 999,999 followers — regular user, push fan-out. They tweet, 999,999 Redis writes happen. Then they gain 2 more followers — now a celebrity. Their next tweet goes through pull.

This transition is fine. The inconsistency window is brief. Some followers see the pre-threshold tweets in their cache; others get them via pull merge. Eventually all followers have the tweet (from pull), and the cache eventually evicts old pushed tweets.

**Celebrity Tweets Cache Structure**

```
Key: celeb_tweets:{celeb_id}  (Redis Sorted Set)
Score: tweet_timestamp (Unix milliseconds, for ordering)
Value: tweet_id

Operations:
  On new celebrity tweet: ZADD celeb_tweets:{celeb_id} {timestamp} {tweet_id}
                          ZREMRANGEBYRANK celeb_tweets:{celeb_id} 0 -(MAX_CACHED+1)
                          → Keep only the last 100 tweets cached
  
  On read: ZREVRANGE celeb_tweets:{celeb_id} 0 49 WITHSCORES
           → Returns 50 most recent tweet IDs with timestamps for merge sort
```

**The Retweet of a Celebrity Tweet**

User with 500 followers (regular user) retweets Beyoncé's tweet:
- The retweet event goes through normal push fan-out to 500 followers
- The retweet record is: `{type: "retweet", user_id: retweeter, original_tweet_id: beyonce_tweet_id}`
- Each follower's timeline contains the retweet_id (not the original tweet_id)
- When hydrating, the Timeline Read Service fetches the retweet record, which points to the original tweet, which is fetched from Cassandra

Storage: 1 retweet record (50 bytes) + fan-out of the retweet_id (not the original tweet content) to 500 followers. No data duplication.

### 7.3 Feed Cache with Redis Sorted Sets

**Why Redis Sorted Sets?**

The timeline is a list of tweet IDs ordered by timestamp (reverse chronological). The operations we need:

1. Insert a new tweet ID at a specific position (determined by timestamp)
2. Read the top N tweets (most recent)
3. Read a page starting from a cursor (for pagination)
4. Trim the timeline to max 800 entries

Redis Sorted Set supports all of these with O(log N) writes and O(log N + M) range reads:

```
ZADD timeline:{user_id} {timestamp_ms} {tweet_id}  → Insert
ZREVRANGE timeline:{user_id} 0 49 WITHSCORES       → Read first page (50 tweets)
ZREVRANGEBYSCORE timeline:{user_id} {cursor} -inf WITHSCORES LIMIT 0 50  → Cursor pagination
ZREMRANGEBYRANK timeline:{user_id} 0 -(MAX_SIZE+1) → Trim to max 800 entries
ZCARD timeline:{user_id}                           → Count entries
```

**The Score (Ordering)**

Use the tweet's creation timestamp in milliseconds as the score:
```
Score = created_at_unix_ms   (e.g., 1706000000000)
```

Sorted sets order by score ascending. ZREVRANGE reads in descending order (newest first). This is exactly what we need for a reverse-chronological timeline.

**Pagination with Cursor**

Offset-based pagination (`LIMIT 50 OFFSET 100`) is broken for timelines — new tweets are constantly being inserted, shifting offsets. Use score-based cursor pagination:

```
First request:
  ZREVRANGEBYSCORE timeline:{user_id} +inf -inf WITHSCORES LIMIT 0 50
  → Returns tweets with scores [newest ... oldest], at most 50
  → Last tweet's score = cursor_score

Next request (client sends cursor_score):
  ZREVRANGEBYSCORE timeline:{user_id} (cursor_score -inf WITHSCORES LIMIT 0 50
  → ( is exclusive range: returns tweets with score < cursor_score
  → Correct pagination even if new tweets are inserted between requests
```

This is the same pattern as Twitter's `max_id` / `since_id` pagination parameters.

**Cache Miss: Rebuilding a Timeline**

If `timeline:{user_id}` doesn't exist in Redis (new user, cold cache after Redis restart), rebuild it:

```python
def rebuild_timeline(user_id: str):
    # Get all users this person follows
    following = follow_graph.get_following(user_id)  # list of user_ids
    
    # For each followed user, get their last N tweets from Cassandra
    # This is the N+1 query problem — batch it
    recent_tweets = tweet_store.batch_get_recent_tweets(
        user_ids=following,
        since=now() - timedelta(days=7),
        limit_per_user=20
    )
    
    # Also fetch celebrity tweets (pull path)
    followed_celebs = [uid for uid in following if uid in celebrities_set]
    for celeb_id in followed_celebs:
        celeb_tweets = redis.zrevrange(f"celeb_tweets:{celeb_id}", 0, 49, withscores=True)
        # Add to recent_tweets list
    
    # Sort all by timestamp, take top 800
    all_tweets = sorted(recent_tweets, key=lambda t: t.created_at, reverse=True)[:800]
    
    # Populate Redis sorted set
    pipe = redis.pipeline()
    for tweet in all_tweets:
        pipe.zadd(f"timeline:{user_id}", {tweet.tweet_id: tweet.created_at_ms})
    pipe.execute()
```

**The Timeline TTL**

Inactive users' timelines waste Redis memory. Set a TTL on each timeline key:

```
redis.expire(f"timeline:{user_id}", 7 * 24 * 3600)  # 7 days
```

Every time a user reads their timeline, reset the TTL. Inactive users' timelines expire after 7 days and are rebuilt on next login. This keeps Redis memory bounded.

**Timeline Consistency: What if a Tweet is Deleted?**

When a tweet is deleted:
1. Mark it as deleted in Cassandra (soft delete: `is_deleted = true`)
2. Publish `tweet.deleted` event to Kafka
3. Fan-out service removes the tweet_id from all followers' timelines: `ZREM timeline:{follower_id} {tweet_id}` — but this requires knowing all followers, which is the same fan-out problem

**Practical solution:** Don't remove from Redis caches. When the Timeline Read Service hydrates tweet IDs, it receives a `null` or deleted flag for the deleted tweet. Filter it out before returning to the client.

```python
def hydrate_tweets(tweet_ids: List[int]) -> List[Tweet]:
    tweets = tweet_store.batch_get(tweet_ids)
    return [t for t in tweets if t is not None and not t.is_deleted]
```

This is the same approach Wikipedia uses for "page doesn't exist" — the cache has a stale reference, but the hydration step handles deleted content gracefully.

### 7.4 Real-Time Delivery with WebSocket and SSE

**The Problem**

A user has the Twitter app open. Their friend posts a tweet. The user should see it appear immediately — not on next pull/refresh.

Two approaches:

**Option A: Short Polling**

Client polls `GET /timeline/updates?since={last_tweet_id}` every 5 seconds:

```
Pros:  Simple to implement. Works through all proxies. No persistent connection state.
Cons:  5-second delay. 300M users × 1 poll/5sec = 60M requests/sec just for polling.
       Servers handle empty responses 99.9% of the time (no new tweets).
       Extremely wasteful.
```

At scale, polling is a fire hose of wasted requests. Do not recommend this.

**Option B: Server-Sent Events (SSE)**

Client opens a long-lived HTTP connection. Server pushes new tweet IDs as they arrive:

```
Client: GET /timeline/stream
Server: text/event-stream response (never closes)
Server sends: data: {"tweet_id": "12345", "author_id": "u789"}\n\n
```

SSE uses HTTP/1.1 chunked transfer. Works through standard load balancers. The client automatically reconnects if the connection drops.

**Pros:** Simpler than WebSockets (unidirectional), works through all HTTP proxies, browser reconnects automatically.
**Cons:** Unidirectional (server to client only). 300M persistent connections requires careful infrastructure.

**Option C: WebSockets**

Full bidirectional connection between client and server:

```
Client connects: ws://api.twitter.com/ws
Server sends: {"type": "new_tweet", "tweet_id": "12345"}
Client sends: {"type": "read_receipt", "tweet_id": "12345"}
```

**Pros:** Bidirectional (useful for read receipts, typing indicators). Lower overhead per message vs SSE.
**Cons:** More complex connection management. Load balancers need WebSocket support (sticky sessions or a separate WebSocket gateway).

**My Recommendation: WebSockets for mobile, SSE for web.**

Mobile apps maintain persistent WebSocket connections (better for power efficiency, handles mobile network switching gracefully). Web browsers use SSE (simpler, works without CORS preflight, auto-reconnects).

**Architecture for Real-Time Push**

```
┌──────────────────────────────────────────────────────────────────────┐
│                       Client (iOS/Android)                           │
│   1. On app open: establish WebSocket connection to WS Gateway       │
│   2. Register connection: {user_id → connection_id} in Redis         │
│   3. Receive tweet_id pushes in real time                            │
│   4. Fetch full tweet data from REST API (tweet_id → tweet object)  │
└──────────────────────────────────────────────────────────────────────┘
                                    │ WebSocket
                                    ▼
┌──────────────────────────────────────────────────────────────────────┐
│                   WebSocket Gateway (Stateful)                       │
│   - Manages persistent connections                                   │
│   - Subscribes to Redis Pub/Sub channel: user_updates:{user_id}     │
│   - When message received on channel, forwards to open connection    │
│   - Connection state stored locally + in Redis (for routing)         │
└──────────────────────┬───────────────────────────────────────────────┘
                       │ Redis Pub/Sub
                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      Fan-out Service                                 │
│   After pushing tweet_id to timeline:{follower_id} in Redis:        │
│   PUBLISH user_updates:{follower_id} '{"tweet_id": "12345"}'        │
│                                                                      │
│   Redis Pub/Sub delivers this to all WS Gateway instances that      │
│   have an active connection for follower_id                          │
└──────────────────────────────────────────────────────────────────────┘
```

**Redis Pub/Sub for Cross-Server Delivery**

The challenge: 300M concurrent connections require thousands of WebSocket Gateway servers. A follower's connection is on Server A. The fan-out runs on Server B. Server B cannot push directly to the connection on Server A.

Solution: **Redis Pub/Sub.**

The Fan-out Service publishes a message to a Redis channel. Every WebSocket Gateway server subscribes to the channels of all users connected to it. Redis delivers the message to all subscribers (all WS gateway servers that have this user's connection). The gateway with the matching connection forwards it.

```python
# Fan-out Service (after Redis ZADD):
redis.publish(f"user_updates:{follower_id}", json.dumps({"tweet_id": new_tweet_id}))

# WebSocket Gateway (on new connection from user_id):
redis.subscribe(f"user_updates:{user_id}")
# On message received:
def on_redis_message(channel, message):
    connection = active_connections.get(user_id)
    if connection:
        connection.send(message)
```

**Scaling Consideration for Celebrities**

A celebrity follows 100 people. 300M people follow the celebrity. When someone the celebrity follows posts a tweet, the fan-out writes to `timeline:{celebrity_id}`. Then publishes to `user_updates:{celebrity_id}`. Exactly one WebSocket Gateway receives that message (the one with the celebrity's connection). This is fine — one WS gateway handles one WS connection, regardless of follower count.

The celebrity problem is a fan-out on write problem (celebrity as poster), not a fan-out on read problem (celebrity as follower). The celebrity's own incoming feed (as a consumer) has no special complications.

**Handling Mobile App Backgrounding**

When the app goes into the background, the WebSocket connection closes. New tweets are missed. When the user re-opens the app:

1. App reconnects WebSocket
2. App calls `GET /timeline?since={last_seen_tweet_id}` to catch up
3. New tweets since the last seen tweet are fetched via REST
4. WebSocket resumes real-time delivery for future tweets

This "catch-up on reconnect" pattern handles offline periods gracefully without losing any tweets.

---

## 8. Trade-offs and Alternatives

### Alternative Timeline Storage: Cassandra vs Redis

**Redis Sorted Set (our design):**

```
Pros:
  - Sub-millisecond read latency (in-memory)
  - Perfect data structure for score-ordered data (sorted set)
  - Built-in TTL for inactive timelines
  - ZADD is idempotent — re-inserting the same tweet_id with same score is a no-op

Cons:
  - Memory-limited (expensive at scale)
  - Data loss if Redis fails without persistence (mitigated with Redis AOF/RDB)
  - Limited to tweet IDs (tweet data stored elsewhere, requiring a hydration step)
```

**Cassandra for Timelines:**

```
Key: (user_id, tweet_id)
Sort key: created_at

Pros:
  - Persistent — survives restarts
  - Cheap storage vs Redis
  - Can store more than just tweet IDs (denormalize tweet content)

Cons:
  - 1-5ms read latency (disk-based, even with SSDs)
  - Pagination requires ALLOW FILTERING or separate indexes
  - Wide rows (800 timeline entries per user) are fine for Cassandra but complex to trim
```

**My recommendation:** Redis for the active timeline (last 800 tweets), Cassandra as fallback for rebuilding cold caches. This is the two-tier cache pattern.

### Fanout Service: Synchronous vs Asynchronous

**Synchronous fan-out:**
```
POST /tweet
  → Write to Cassandra
  → Fan out to 200 Redis timelines (200 network ops)
  → Return 200 OK to client

Latency: 200 Redis writes in parallel ≈ 5ms (fast enough)
         But if Redis is slow: latency spikes to 50ms+
```

**Asynchronous fan-out (our design):**
```
POST /tweet
  → Write to Cassandra
  → Publish to Kafka
  → Return 200 OK to client immediately

Fan-out Service (async):
  → Consumes from Kafka
  → Writes to 200 Redis timelines
  
Latency for client: ~5ms (just Cassandra write)
Latency for followers to see tweet: 1-5 seconds (Kafka + fan-out time)
```

**The trade-off:** Async fan-out means followers see a tweet 1-5 seconds after posting. For a social network, this is completely acceptable — nobody expects sub-second cross-user propagation. Synchronous fan-out adds latency to the posting user's experience (they wait for fan-out to complete) and couples the tweet post request to the fan-out reliability.

**Verdict:** Async fan-out is the correct production design. The posting user should not wait 5+ seconds for their tweet to propagate.

### Tweet Hydration: N+1 Problem and How to Avoid It

When the timeline returns 50 tweet IDs from Redis, you need to fetch the full tweet data for each. Naive approach: 50 individual reads from Cassandra = N+1 query problem.

**Solution: Batch fetch with a secondary tweet cache.**

```python
# Naive (bad): 50 separate reads
tweets = [cassandra.get_tweet(tid) for tid in tweet_ids]

# Good: one batch read
tweets = cassandra.batch_get_tweets(tweet_ids)  # Single CQL query with IN clause

# Better: Tweet object cache in Redis
tweets = []
cache_misses = []
for tid in tweet_ids:
    cached = redis.get(f"tweet:{tid}")  # Tweet object cached for 24h
    if cached:
        tweets.append(json.loads(cached))
    else:
        cache_misses.append(tid)

if cache_misses:
    fetched = cassandra.batch_get_tweets(cache_misses)
    for tweet in fetched:
        redis.set(f"tweet:{tweet.id}", json.dumps(tweet), ex=86400)
    tweets.extend(fetched)
```

The tweet object cache is separate from the timeline cache. The timeline stores ordered IDs; the tweet cache stores full objects. Hot tweets (viral tweets seen by millions) are cached in Redis and serve millions of reads without hitting Cassandra.

### Like Count Consistency

Tweet like counts are high-frequency write data. Every like/unlike increments/decrements a counter. At 300M DAU with heavy like behavior: potentially millions of like operations per second.

**Option 1: Update like_count in Cassandra on every like.**
Problem: Cassandra counter operations are expensive. Millions of counter increments per second on popular tweets creates hot partitions.

**Option 2: Store likes in Redis as a counter, batch-write to Cassandra.**

```python
# On like:
redis.incr(f"tweet:{tweet_id}:likes")
kafka.publish("like.created", {tweet_id, user_id, timestamp})

# Background job (every 60 seconds):
for tweet_id in dirty_like_counts:
    count = redis.get(f"tweet:{tweet_id}:likes")
    cassandra.update_like_count(tweet_id, count)
```

Pros: Redis handles millions of increments/second. Batch writes to Cassandra are efficient. Cons: Like count shown to user may be slightly stale (eventually consistent within 60 seconds). Acceptable.

**Option 3: Read like count only from Redis (skip Cassandra for this field).**
For display purposes, serve like_count directly from Redis. Only persist to Cassandra for durability (asynchronously). Fastest read path.

---

## 9. Failure Scenarios

### Scenario 1: Redis Cluster Failure (Timeline Cache Lost)

**Impact:** All 300M users' timeline caches are lost. Every feed request triggers a full timeline rebuild from Cassandra + follow graph.

**Rebuild cost:**
```
35,000 timeline reads/sec
Each rebuild: query follow graph (200 users) + fetch tweets (200 × last 20 tweets) + merge
Cassandra reads: 35,000 × (1 + 200) ≈ 7M reads/sec
```

Cassandra handles this if properly provisioned, but latency degrades from 5ms to ~200ms during the storm. This is the "cache stampede" problem.

**Mitigation:**
1. Redis AOF (Append-Only File) persistence: timelines survive Redis restarts with data loss of only the last second
2. Redis Cluster: 6-shard cluster means a single node failure only loses ~1/6 of timelines
3. Cache rebuild rate limiting: stagger rebuilds using a queue. Don't allow 35,000 simultaneous rebuilds. Rebuild at most 1,000/second and serve slightly stale data to the rest
4. Redis Replica promotion: automatic failover via Redis Sentinel minimizes downtime

### Scenario 2: Fan-out Service Lag (Kafka Consumer Backlog)

**Symptom:** A viral event causes 10M tweets in an hour (abnormal activity). Fan-out service falls behind — Kafka consumer lag grows.

**Impact:** Followers see tweets minutes after posting instead of seconds. Not a system failure, but a user experience degradation.

**Mitigation:**
- Auto-scale Fan-out Service based on Kafka consumer lag metric
- Kafka retains events for 7 days — no data loss, just delayed delivery
- Priority queue: fan-out for regular users (< 100K followers) gets priority over celebrity tweet index updates (lower urgency since those are always pulled at read time)
- Shed load: if lag exceeds 5 minutes, skip fan-out for low-priority tweet types (promotional tweets, bot accounts)

### Scenario 3: Hot Partition in Cassandra (Viral Tweet)

**Symptom:** A single tweet receives 100M likes in an hour. Every like read hits the same Cassandra partition.

**Impact:** The partition node is overwhelmed. Read latency for that tweet spikes. Other requests on the same Cassandra node are affected.

**Mitigation:**
- Like counts are served from Redis, not Cassandra (per our design) — this eliminates the Cassandra hot partition for like counts
- Tweet content is read-only after posting — it's safely cached in the tweet object cache (Redis `tweet:{tweet_id}`)
- For the tweet content itself, once it's cached in Redis, Cassandra is never read for subsequent requests — CDN at the edge handles the bulk of reads

### Scenario 4: Follow Graph Update During Fan-out

**Scenario:** User A follows User B. User B posts a tweet 500ms later. The fan-out service reads User B's follower list — does User A appear in it?

**Race condition:**
1. User A follows User B (write to follow graph DB)
2. User B posts tweet (Kafka event)
3. Fan-out service reads User B's followers from Redis cache (cached 60 seconds ago)
4. Cache doesn't have User A yet (cache miss or stale)
5. User A's timeline doesn't get the tweet

**Impact:** User A doesn't see User B's tweet in their precomputed timeline. They see it when they pull from the celebrity tweet path — but User B is not a celebrity.

**Mitigation:**
1. Accept this as eventual consistency. A newly followed user's feed is slightly delayed (up to 60 seconds from follow).
2. When User A follows User B: immediately invalidate or update the `followers:{user_b_id}` Redis cache.
3. On first load after follow: force a partial timeline rebuild to include User B's recent tweets.

This is the correct production answer: eventual consistency with a bounded window (60 seconds).

### Scenario 5: WebSocket Server Overload During a Viral Event

**Scenario:** A major sports event ends. 50M users simultaneously open Twitter. 50M WebSocket connections try to establish in 30 seconds.

**Impact:** WebSocket gateway servers are overwhelmed. Connections are dropped. Users see no real-time updates.

**Mitigation:**
- Separate WebSocket gateway is horizontally scalable (Kubernetes HPA based on active connection count)
- Connection limiting: each WS gateway server handles max 100K connections. 50M connections / 100K per server = 500 servers needed
- Pre-scale: sports events are predictable — pre-scale WS gateways 1 hour before scheduled end of major events
- Graceful degradation: if WS connection fails, client falls back to polling every 30 seconds. Users still see updated feeds, just less real-time
- Exponential backoff on client reconnects: prevent thundering herd when WS gateway restarts

---

## 10. Interview Tips

### Time Management for 45 Minutes

```
0-2 min:   Read problem, identify the central tension (push vs pull fan-out)
2-8 min:   Clarifying questions (scale, celebrity definition, real-time, retweets)
8-13 min:  Requirements and capacity estimation (show the 120B fan-out ops/day calc)
13-20 min: High-level design — draw the hybrid architecture, name all components
20-40 min: Deep dives:
           - Push vs pull vs hybrid fanout (8 min — this is the core of the interview)
           - Celebrity problem (5 min)
           - Redis sorted set timeline cache (4 min)
           - Real-time WebSocket (3 min)
40-43 min: Trade-offs (async fan-out vs sync, Redis vs Cassandra for timeline)
43-45 min: One failure scenario (fan-out lag or Redis stampede)
```

### What Separates Staff from Senior Candidates

**Senior candidate:** Identifies the push vs pull trade-off, proposes a hybrid model, uses Redis sorted sets for timeline storage, mentions WebSocket for real-time.

**Staff candidate** additionally:
- Calculates the celebrity problem from first principles: 1 tweet × 108M followers × 100K writes/sec = 18 minutes. This is disqualifying for pure push.
- Proposes the celebrity threshold (1M followers) and explains why it works: 99.9% of tweets are from non-celebrities, so 99.9% of tweets still use the fast push path
- Explains the merge algorithm at read time: O(C × K) where C = celebrities followed, K = recent tweets per celebrity — and shows this is small in practice
- Addresses the race condition between follow events and fan-out
- Designs the Redis Pub/Sub architecture for cross-server WebSocket delivery (not just "use WebSockets")
- Explains tweet hydration caching (tweet object cache) to solve the N+1 problem
- Proactively discusses like count eventual consistency as a deliberate design decision

### Common Mistakes to Avoid

1. **Proposing pure push fan-out without addressing celebrities.** The interviewer will immediately ask: "What about Katy Perry's 100M followers?" Have the celebrity threshold answer ready.

2. **Proposing pure pull fan-out at this scale.** 200 DB reads per user per feed request × 35,000 feed requests/sec = 7M DB reads/sec. This requires an enormous DB cluster and has high latency.

3. **Storing tweet content (not just IDs) in the timeline cache.** The timeline cache should store tweet IDs only. Tweet content is stored once in the tweet DB and cached in a separate tweet object cache. Duplicating tweet content in every follower's timeline cache would require enormous storage (120B fan-out ops × 500 bytes = 60 TB/day just for timeline data).

4. **Not addressing pagination correctly.** Offset-based pagination breaks on live feeds (new inserts shift offsets). Always use cursor-based pagination (score-based ZREVRANGEBYSCORE) for timelines.

5. **Forgetting deleted tweets.** Tweets can be deleted. The timeline cache may contain IDs of deleted tweets. The hydration step must handle null/deleted returns gracefully.

6. **Treating the follow graph as a simple list.** The follow graph is large (100B edges), changes frequently (follow/unfollow events), and is queried on every fan-out. It must be cached in Redis (as a Set or List per user), not queried from DB on every tweet.

7. **Underestimating the WebSocket infrastructure challenge.** 300M concurrent WebSocket connections requires dedicated WebSocket gateway servers with Redis Pub/Sub for cross-server delivery. This is not a trivial extension of the REST API — it's a separate stateful service.

### The Hybrid Model as a Framework

The most important intellectual contribution of this problem is the hybrid push/pull model. It teaches you a general principle that applies far beyond Twitter:

**"Apply different strategies to different segments of the population based on the most discriminating characteristic of that segment."**

- Most users (< 1M followers): push fan-out. Fast writes, fast reads.
- Celebrity users (≥ 1M followers): pull at read time. Avoids catastrophic fan-out.

This is the same principle as:
- Small messages: send inline. Large messages: store in S3, send a reference.
- Low-volume users: single database row. High-volume users: time-series partition.
- Non-viral content: serve from origin. Viral content: promote to CDN edge.

The celebrity problem is a specific instance of the "handling outliers in a distribution" problem. Master this mental model and you'll apply it correctly in many other system design contexts.

### Strong Closing Statement

> "To summarize: the home timeline is built on a hybrid fan-out model. Regular users' tweets (follower count < 1M) are pushed to followers' Redis sorted set timeline caches at write time, giving sub-millisecond read latency. Celebrity tweets are stored in per-celebrity sorted sets and merged into the timeline at read time, solving the fan-out problem for accounts with tens of millions of followers. The merge is cheap — a user follows at most 5-10 celebrities, and each merge requires only 5-10 Redis range queries plus a merge sort. The entire timeline fits in Redis (800 tweet IDs × 16 bytes = 12.5 KB per user), and tweet content is fetched in a single batch hydration step from a tweet object cache, avoiding the N+1 query problem. Real-time updates use WebSockets with Redis Pub/Sub for cross-server delivery, and mobile clients fall back to catch-up requests on reconnect after backgrounding."

---

*End of Twitter/X Feed System Design — estimated interview preparation time: 5-6 hours of active practice (the hybrid model requires deep understanding to explain fluently)*
