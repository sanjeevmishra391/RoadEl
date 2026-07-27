# Uber-Like Ride-Sharing Platform — Full System Design Walkthrough

> **Interview Format:** 45 minutes | **Level:** Senior / Staff Engineer  
> **Analogous real systems:** Uber, Lyft, Grab, Didi, Ola

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

Design a **ride-sharing platform** like Uber. A rider opens their phone, requests a car from their current location to a destination, and within seconds a nearby driver is notified and dispatched. The platform must:

- Track millions of active driver locations in real time (updating every 4 seconds)
- Match riders to drivers based on proximity, ETA, driver rating, and surge zone
- Compute accurate ETAs using live road network data
- Handle the complete trip lifecycle: request → match → pickup → trip → payment
- Apply surge pricing dynamically based on local supply-demand imbalance
- Scale to 100 million rides per day across global markets

The core challenges are **geospatial indexing at scale** (finding nearby drivers in milliseconds), **matching under concurrency** (a driver can't be matched to two riders simultaneously), and **real-time location tracking** (5 million drivers sending GPS pings every 4 seconds = 1.25 million writes/second).

---

## 2. Clarifying Questions

### Q1: What geographic precision do we need for driver matching?
**Expected answer:** We need to find drivers within ~3-5 km of a rider for city rides. For rural or airport pickups, the radius may expand to 10-15 km. Sub-meter GPS precision is not necessary — 100 meter precision is sufficient.  
**Why it matters:** Determines the geospatial index granularity. Geohash precision level 6 (~1.2km × 0.6km cells) vs level 7 (~150m × 150m cells) changes the lookup and storage strategy.

### Q2: Is matching real-time (first available driver) or optimized (global optimal assignment)?
**Expected answer:** Largely real-time with local optimization. We do not run a global matching algorithm over all riders and all drivers simultaneously — that would be computationally intractable. Instead, for each new ride request, we find the best available driver in the area within 500ms.  
**Why it matters:** Global optimal matching (Hungarian algorithm, Kuhn-Munkres) is O(n^3) and only practical as a batch process. Real-time matching is O(nearby_drivers) per request, which is fast and scalable.

### Q3: How do we handle driver and rider cancellations?
**Expected answer:** Drivers and riders can cancel until the driver arrives. Cancellation triggers the trip state machine — the ride returns to "unmatched" state and re-enters the matching queue. High cancellation rates affect driver reputation scoring.  
**Why it matters:** Defines the state machine transitions and idempotency requirements. A cancellation must atomically free the driver and re-queue the ride.

### Q4: Do we need multi-modal support (pool rides, shared rides, bikes, scooters)?
**Expected answer:** Focus on standard ride-hailing (X, XL, Black). Shared/pool rides are out of scope — they require a separate route optimization problem. Different vehicle categories (X vs XL) should be supported via ride type filtering.  
**Why it matters:** Pool rides need a completely different matching algorithm (combining multiple rider origins/destinations). Scoping to single-passenger rides makes the matching problem tractable.

### Q5: What is the SLA for matching a rider to a driver?
**Expected answer:** The rider should receive a match confirmation within 3-5 seconds of requesting a ride. The match selection process itself should complete in <500ms; the remaining time is for driver notification, driver acceptance, and acknowledgment round-trip.  
**Why it matters:** 500ms for matching means we can do 1-3 rounds of candidate evaluation: find nearby drivers, rank by ETA, attempt to dispatch first choice, fall back if unavailable.

### Q6: Should surge pricing be real-time or near-real-time?
**Expected answer:** Near-real-time — recalculate surge multipliers every 1-2 minutes per geographic zone. Riders see the surge price before confirming the ride.  
**Why it matters:** Real-time surge (per-second) creates extreme volatility. 1-minute buckets are granular enough to respond to events (sports game ending, rainstorm starting) while being stable enough for a good user experience.

### Q7: Do we need to support offline maps / GPS-denied areas?
**Expected answer:** No. Assume always-online, always-GPS-available. Offline support is a significant mobile engineering challenge out of scope.

---

## 3. Functional Requirements

| # | Requirement |
|---|-------------|
| FR1 | Rider submits a ride request with pickup location, destination, and ride type |
| FR2 | System finds available drivers within a configurable radius |
| FR3 | System matches rider to the best available driver (lowest ETA, acceptable rating) |
| FR4 | Driver receives ride request notification and can accept or decline |
| FR5 | Driver location is tracked in real time (every 4 seconds while app is active) |
| FR6 | Rider can track driver location on the map during pickup and trip |
| FR7 | System computes ETA from driver to rider and from rider to destination |
| FR8 | Surge pricing multiplier is calculated per geographic zone based on supply/demand |
| FR9 | Trip lifecycle state machine: REQUESTED → MATCHED → DRIVER_EN_ROUTE → IN_PROGRESS → COMPLETED / CANCELLED |
| FR10 | Payment processing on trip completion |

**Out of scope:**
- Carpooling / shared rides
- Ride scheduling (future booking)
- Driver navigation (turn-by-turn, handled by external map SDK)
- Rewards/loyalty programs

---

## 4. Non-Functional Requirements

| Category | Target |
|----------|--------|
| **Location update throughput** | 1.25M writes/second (5M drivers × 1 update/4 seconds) |
| **Matching latency** | Match confirmed to rider within 3-5 seconds of request |
| **Location query latency** | Find drivers in area: p99 < 50ms |
| **Availability** | 99.99% — platform outage directly impacts driver income |
| **Consistency** | A driver must never be matched to two riders simultaneously (strong consistency for the matching state) |
| **Scale** | 5M active drivers, 100M rides/day, globally distributed |
| **ETA accuracy** | Within ±20% of actual arrival time |
| **Notification latency** | Driver receives ride request notification within 1 second of match |

---

## 5. Capacity Estimation

### 5.1 Driver Location Updates

```
Active drivers globally:        5,000,000
Location update interval:       4 seconds
Location updates per second:    5,000,000 / 4 = 1,250,000 writes/second (1.25M QPS)
Location update payload:        ~100 bytes (driver_id, lat, lon, heading, speed, timestamp)
Bandwidth for location updates: 1.25M × 100B = 125 MB/s ingest

Peak hours (rush hour, 2× active drivers):
  Peak writes:                  2,500,000 writes/second (2.5M QPS)
```

### 5.2 Ride Requests

```
Rides per day:                  100,000,000
Rides per second (average):     100M / 86,400 ≈ 1,157 ride requests/second
Peak rides per second:          ~5,000 requests/second (morning/evening rush hour)

Matching operations per request:
  - Find nearby drivers:         1 geospatial query
  - Attempt dispatch (3 tries):  3 state writes
  Total DB ops per ride:         ~10 operations
  Peak DB ops:                   50,000 ops/second
```

### 5.3 Storage

```
Active driver locations (in-memory index):
  5M drivers × 200B (geohash entry + metadata) = 1 GB → fits in Redis comfortably

Location history (for analytics / dispute resolution):
  1.25M writes/second × 100B × 86,400 seconds = 10.8 TB/day (raw)
  After compression + downsampling for history: ~1 TB/day retained
  30-day retention: 30 TB  → store in Cassandra or S3

Trip records:
  100M trips/day × 500B per trip = 50 GB/day
  1 year retention: ~18 TB  → PostgreSQL (sharded) or Cassandra
```

### 5.4 Infrastructure

```
Location service nodes (Redis cluster):
  1 GB data, but 1.25M writes/second throughput is the constraint
  Redis: ~200K ops/second per node
  Nodes needed for throughput: 1.25M / 200K = ~7 primaries
  With replication (1:1): 14 Redis nodes

  Better approach: Use Kafka as write buffer → Redis. Kafka absorbs bursts;
  Redis consumers write at controlled rate.

Matching service instances:
  5,000 ride requests/second peak
  Each matching takes ~50ms (geo query + ETA calc + dispatch attempt)
  RPS per instance (50ms latency, 20 concurrent reqs): ~400 RPS per core
  With 8 cores: 3,200 RPS per instance
  Instances needed: 5,000 / 3,200 ≈ 2 instances
  Production (with redundancy + headroom): 8-10 instances
```

---

## 6. High-Level Design

### 6.1 Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                           CLIENT TIER                                             │
│  ┌─────────────────────────────────┐    ┌───────────────────────────────────┐    │
│  │        RIDER APP                │    │          DRIVER APP                │    │
│  │  - Request ride                 │    │  - Send GPS ping every 4s         │    │
│  │  - Show driver on map           │    │  - Receive ride request push notif│    │
│  │  - WebSocket for live updates   │    │  - Accept / decline                │    │
│  └──────────────┬──────────────────┘    └─────────────────┬─────────────────┘    │
└─────────────────┼────────────────────────────────────────┼────────────────────┘
                  │ HTTPS/WSS                               │ HTTPS (location pings)
                  ▼                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          API GATEWAY / LOAD BALANCER                              │
│              (TLS termination, auth, rate limiting, routing)                     │
└───────┬──────────────────────────────────────────────────────┬───────────────────┘
        │                                                       │
        ▼                                                       ▼
┌──────────────────┐                              ┌───────────────────────────────┐
│   RIDE SERVICE   │                              │    LOCATION SERVICE            │
│                  │                              │                               │
│ - Accept ride    │                              │  ┌─────────────────────────┐  │
│   requests       │                              │  │  Kafka (location-events) │  │
│ - Trip state     │                              │  │  1.25M writes/second     │  │
│   machine        │                              │  └──────────┬──────────────┘  │
│ - Persist trips  │                              │             │                 │
│   to PostgreSQL  │                              │  ┌──────────▼──────────────┐  │
└────────┬─────────┘                              │  │  Location Processor     │  │
         │                                        │  │  - Updates Redis geohash│  │
         │                                        │  │    index (driver cells) │  │
         ▼                                        │  │  - Writes location hist │  │
┌─────────────────────┐                           │  │    to Cassandra         │  │
│   MATCHING SERVICE  │                           │  └──────────┬──────────────┘  │
│                     │                           │             │                 │
│ - Find nearby       │◄──── geospatial query ───►│  ┌──────────▼──────────────┐  │
│   drivers           │                           │  │  Redis Geospatial Index  │  │
│ - Calculate ETA     │                           │  │  (driver location store) │  │
│ - Dispatch request  │                           │  │  GEOADD / GEORADIUS     │  │
│ - Handle accept/    │                           │  └─────────────────────────┘  │
│   decline           │                           └───────────────────────────────┘
└────────┬────────────┘
         │
         ▼
┌────────────────────────────────────────────────────────────────────────────────┐
│                            SUPPORTING SERVICES                                  │
│                                                                                 │
│  ┌───────────────┐  ┌────────────────┐  ┌──────────────────┐  ┌─────────────┐ │
│  │  ETA SERVICE  │  │  SURGE PRICING │  │  NOTIFICATION    │  │  PAYMENT    │ │
│  │               │  │  SERVICE       │  │  SERVICE         │  │  SERVICE    │ │
│  │ - Road graph  │  │                │  │                  │  │             │ │
│  │   (OSRM /     │  │ - Supply/demand│  │ - Push notif     │  │ - Charge    │ │
│  │   Google Maps)│  │   per geohash  │  │   (APNs/FCM)     │  │   rider     │ │
│  │ - Dijkstra /  │  │ - Surge mult.  │  │ - WebSocket to   │  │ - Pay driver│ │
│  │   A* on graph │  │   every 1 min  │  │   rider app      │  │ - Receipt   │ │
│  └───────────────┘  └────────────────┘  └──────────────────┘  └─────────────┘ │
└────────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Component Responsibilities

**Location Service:**  
Handles the firehose of 1.25M driver location pings per second. Kafka decouples the write ingest from the Redis update. Each location ping is published to Kafka; the Location Processor consumer group reads from Kafka and updates the Redis geospatial index. This prevents Redis from being overwhelmed by direct writes from 5M drivers simultaneously.

**Redis Geospatial Index:**  
Uses Redis `GEOADD` to store driver positions as longitude/latitude pairs. Redis encodes these internally as geohashes in a sorted set. `GEORADIUS` (or the newer `GEOSEARCH`) finds all drivers within a given radius: `GEOSEARCH drivers:available FROMLONLAT <lon> <lat> BYRADIUS 5 km ASC COUNT 20`.

**Matching Service:**  
On a new ride request, the Matching Service calls the Location Service to get candidate drivers, calls the ETA Service to compute ETAs, applies filters (vehicle type, minimum rating), ranks candidates, and dispatches a push notification to the top candidate. If the driver doesn't respond within 15 seconds or declines, the next candidate is tried.

**ETA Service:**  
Computes estimated time of arrival using a pre-loaded road network graph. Uses Dijkstra or A* for shortest path. Real production systems (Uber, Google) use Contraction Hierarchies (CH) for faster repeated queries — CH pre-computes shortcuts that allow O(100ms) queries on continental road networks vs O(seconds) for vanilla Dijkstra.

**Surge Pricing Service:**  
Every minute, counts available drivers and pending ride requests per geohash cell (level 5 = ~4.9km × 4.9km). Computes `surge_multiplier = f(demand / supply)`. Publishes surge map to a Redis hash; the Ride Service reads this when a rider requests a ride to show the surge price before confirmation.

---

## 7. Deep Dives

### 7.1 Geospatial Indexing — Geohash vs H3

**The fundamental problem:** GPS coordinates are continuous real numbers. To find "all drivers within 5km", you need a way to spatially index points so that nearby points are stored/retrieved together. A sorted index on latitude alone doesn't help (nearby points can have very different longitudes).

**Geohash:**

Geohash encodes a latitude/longitude pair into a short alphanumeric string by interleaving bits of the latitude and longitude binary representations. The result is a string where common prefixes = geographic proximity.

```
Geohash precision levels:
  Level | Cell width  | Cell height | String length
  1     | 5,009 km    | 4,992 km    | 1 char
  2     | 1,252 km    | 624 km      | 2 chars
  3     | 156 km      | 156 km      | 3 chars
  4     | 39 km       | 19 km       | 4 chars
  5     | 4.9 km      | 4.9 km      | 5 chars   ← used for surge zones
  6     | 1.2 km      | 0.6 km      | 6 chars   ← good for driver search radius
  7     | 153 m       | 153 m       | 7 chars
  8     | 38 m        | 19 m        | 8 chars

Example:
  Times Square, NYC: lat=40.758, lon=-73.985
  Geohash level 6: "dr5ru7"
  All neighbors share prefix "dr5ru" (level 5 cell)
```

**Algorithm: find drivers near a rider using geohash**

```python
def find_nearby_drivers(rider_lat, rider_lon, radius_km=5, max_results=20):
    # Get the rider's geohash cell at precision 6 (~1.2km cells)
    rider_cell = geohash.encode(rider_lat, rider_lon, precision=6)

    # Get all 8 neighboring cells + the center cell
    cells_to_check = geohash.neighbors(rider_cell) + [rider_cell]
    # 9 cells × 1.2km ≈ 3.6km radius, good enough for 5km search

    # For larger radius: also check neighbors of neighbors (level 5 cells)
    if radius_km > 5:
        cells_to_check = expand_to_level5(rider_cell)  # 9 level-5 cells

    # Query Redis for all drivers in any of these cells
    candidate_drivers = []
    for cell in cells_to_check:
        drivers = redis.smembers(f"drivers:available:{cell}")
        candidate_drivers.extend(drivers)

    # Filter by exact Haversine distance (geohash cells are rectangular approximations)
    nearby = [d for d in candidate_drivers
              if haversine(rider_lat, rider_lon, d.lat, d.lon) <= radius_km]

    return sorted(nearby, key=lambda d: d.eta)[:max_results]
```

**The geohash edge case problem:**

Two points right next to each other can have different geohashes if they sit on opposite sides of a cell boundary. For example, a driver at the very east edge of cell "dr5ru7" and a rider at the very west edge of "dr5ru8" are adjacent but have different geohashes.

**Solution:** Always query the 8 neighboring cells in addition to the rider's cell. With 9 cells queried at precision 6 (~1.1km cells), you cover a ~3.3km radius comfortably for a 5km search.

**Uber H3 — Hexagonal Hierarchical Geospatial Indexing:**

H3 is Uber's open-source geospatial indexing system, released in 2018 and now widely adopted. It divides the world into hexagonal cells at 16 resolution levels.

```
H3 Resolution | Avg cell area | Avg edge length
0             | 4.25M km²     | 1,107 km
5             | 252 km²       | 9.0 km
6             | 36 km²        | 3.5 km
7             | 5.1 km²       | 1.3 km
8             | 0.74 km²      | 461 m
9             | 0.105 km²     | 174 m
```

**Why hexagons beat rectangles:**

1. **Uniform distance to all neighbors:** In a square grid, the center is closer to edge neighbors than corner neighbors (1.0 vs 1.41 units). In a hexagon grid, all 6 neighbors are equidistant. This means proximity queries have no directional bias.
2. **No edge artifacts:** The boundary discontinuity problem in geohash is reduced (not eliminated) because hexagonal cells tile more smoothly.
3. **Hierarchical containment:** H3 cells at resolution N are contained within resolution N-1 cells (approximately — not perfectly due to the sphere/hexagon packing problem). This enables efficient drill-down.

**Geohash vs H3 comparison:**

| Dimension | Geohash | H3 |
|-----------|---------|-----|
| Cell shape | Rectangle | Hexagon |
| Neighbor distance uniformity | Poor (8 neighbors, different distances) | Excellent (6 neighbors, equal distance) |
| Precision levels | 12 | 16 |
| Edge discontinuity | Common problem | Less common |
| Widespread adoption | Very high (Redis native) | High (Uber, Airbnb, many others) |
| Redis native support | Yes (GEOADD uses geohash internally) | No (must implement lookup) |

**What Redis actually does:**  
Redis's `GEOADD` stores coordinates as geohashes in a sorted set. `GEORADIUS` / `GEOSEARCH` queries a bounding box from the geohash encoding and then filters results by exact Haversine distance. You get the performance of geohash lookup with the accuracy of exact distance filtering.

**Recommendation:** Use Redis's native geospatial commands for the driver location index — they use geohash internally and handle the neighbor-cell query automatically. For surge zone computation and analytics, use H3 (better spatial analysis properties). Uber does this exactly: H3 for analytics and zone management, Redis geospatial for live driver queries.

### 7.2 Driver Location Update Pipeline

**The challenge:** 5M drivers × 1 ping/4 seconds = 1.25M location writes/second. Each write must:
1. Update the driver's position in the geospatial index (for matching)
2. Store the location in history (for trip replay, dispute resolution)
3. Broadcast the new position to any rider who is currently watching this driver on their map

**Architecture:**

```
Driver App
    │
    │ POST /location {driver_id, lat, lon, heading, speed, ts}
    ▼
Location Ingest Service
    │
    │  publish(topic="driver-locations", key=driver_id, value=payload)
    ▼
Kafka (driver-locations topic, 100 partitions)
    │
    ├──────────────────────────────────────┐
    │                                      │
    ▼                                      ▼
Location Index Consumer               Location History Consumer
(Consumer Group: location-index)      (Consumer Group: location-history)
    │                                      │
    │ GEOADD drivers:available:            │ INSERT INTO location_history
    │         <lon> <lat> <driver_id>      │ (driver_id, lat, lon, ts) → Cassandra
    ▼                                      │
Redis Geospatial Index                     ▼
                                       Cassandra
    │                                  (time-series, TTL 30 days)
    └──────────────────────────────────────────────────────┐
                                                           │
                                               Location Fan-Out Consumer
                                               (Consumer Group: location-fanout)
                                                           │
                                               For each driver update:
                                               - Check if driver has active trip
                                               - If yes: push location to rider's
                                                 WebSocket connection
                                                           │
                                                           ▼
                                               WebSocket Server
                                               → Rider App (live map update)
```

**Kafka partition key:** Use `driver_id` as the partition key. This ensures all location updates for a given driver go to the same partition, maintaining ordering. With 100 partitions, updates for any driver are processed in order.

**Location update rate per driver:**  
Drivers send pings every 4 seconds. The Location Index Consumer updates Redis at this rate. Redis can handle ~200K writes/second per node; with 1.25M/sec we need ~7 primary Redis nodes. **Better approach:** Batch updates — the Kafka consumer reads 100ms worth of events, groups by driver, takes the latest coordinate per driver (discarding intermediate positions), and writes batches to Redis. This reduces Redis writes from 1.25M/sec to significantly lower (drivers don't move meaningfully in 100ms).

**Fan-out to rider app:**  
When a rider is tracking a driver en-route, they need live location updates. The WebSocket connection per rider is maintained by a WebSocket server. The Location Fan-Out Consumer checks, for each driver update, whether that driver is in an active trip with a waiting rider, and if so, pushes the update over the WebSocket. This is O(1) per update (the trip state stores the WebSocket server ID and connection ID for the rider).

**Driver availability state in Redis:**

```
Key:   "driver:{driver_id}"
Type:  Hash
Fields:
  lat:           40.758
  lon:           -73.985
  heading:       270  (degrees, 0=north)
  speed:         35   (km/h)
  status:        AVAILABLE | MATCHED | IN_TRIP | OFFLINE
  geohash:       dr5ru7
  vehicle_type:  X
  rating:        4.85
  last_ping_ts:  1706400000

Key:   "drivers:available:{geohash_cell}"
Type:  Set
Value: {driver_id1, driver_id2, ...}
```

When a driver goes offline (no ping for 30 seconds), the Location Service marks them OFFLINE and removes them from the availability set.

### 7.3 Matching Algorithm and Trip State Machine

**Trip State Machine:**

```
                              ┌─────────────┐
                              │  REQUESTED  │ ← Rider submits request
                              └──────┬──────┘
                                     │ Matching service finds driver
                              ┌──────▼──────┐
                              │   MATCHED   │ ← Driver notified
                              └──────┬──────┘
                                     │ Driver accepts (within 15s)
                              ┌──────▼───────────┐
                              │ DRIVER_EN_ROUTE  │ ← Driver navigating to pickup
                              └──────┬───────────┘
                                     │ Driver arrives at pickup
                              ┌──────▼──────┐
                              │   ARRIVED   │ ← Driver at pickup location
                              └──────┬──────┘
                                     │ Rider boards (driver taps "Start Trip")
                              ┌──────▼───────────┐
                              │   IN_PROGRESS    │ ← Trip underway
                              └──────┬───────────┘
                                     │ Driver taps "End Trip" at destination
                              ┌──────▼──────┐
                              │  COMPLETED  │ ← Payment triggered
                              └─────────────┘

Cancellation transitions (from any pre-IN_PROGRESS state):
  → CANCELLED_BY_RIDER
  → CANCELLED_BY_DRIVER
  → CANCELLED_NO_DRIVER_FOUND (timeout after trying 5 drivers)
  → CANCELLED_TIMEOUT (no driver accepted within 60 seconds)
```

**Matching Algorithm — Step by Step:**

```python
def match_ride(ride_request: RideRequest) -> MatchResult:
    # Step 1: Find candidate drivers
    candidates = location_service.find_drivers(
        lat=ride_request.pickup_lat,
        lon=ride_request.pickup_lon,
        radius_km=5,
        vehicle_type=ride_request.vehicle_type,
        max_results=20
    )

    if len(candidates) == 0:
        # Expand search radius
        candidates = location_service.find_drivers(radius_km=15, max_results=10)

    if len(candidates) == 0:
        return MatchResult(status=NO_DRIVERS_AVAILABLE)

    # Step 2: Get ETA for top candidates (too expensive to ETA all 20)
    top_candidates = candidates[:10]  # pre-filtered by straight-line distance
    etas = eta_service.batch_eta(
        origins=[(d.lat, d.lon) for d in top_candidates],
        destination=(ride_request.pickup_lat, ride_request.pickup_lon)
    )

    # Step 3: Score and rank
    scored = []
    for driver, eta_seconds in zip(top_candidates, etas):
        surge = surge_service.get_multiplier(driver.geohash)
        score = compute_score(
            eta_seconds=eta_seconds,
            driver_rating=driver.rating,
            acceptance_rate=driver.acceptance_rate,
            heading_alignment=heading_toward(driver, ride_request)  # bonus if driver already heading toward pickup
        )
        scored.append((score, driver, eta_seconds))

    ranked = sorted(scored, key=lambda x: x[0], reverse=True)

    # Step 4: Dispatch in order, first acceptance wins
    for score, driver, eta_seconds in ranked[:3]:  # try top 3 candidates
        result = attempt_dispatch(driver, ride_request)
        if result == ACCEPTED:
            transition_trip_state(ride_request.trip_id, MATCHED)
            set_driver_status(driver.id, MATCHED, ride_id=ride_request.trip_id)
            return MatchResult(driver=driver, eta=eta_seconds)
        # If DECLINED or TIMEOUT (15s), try next candidate

    return MatchResult(status=NO_DRIVER_ACCEPTED)

def attempt_dispatch(driver: Driver, ride: RideRequest) -> DispatchResult:
    # Atomic check-and-set: ensure driver is still AVAILABLE
    success = redis.set(
        f"driver:lock:{driver.id}",
        ride.trip_id,
        nx=True,      # only set if not exists
        ex=20         # lock expires in 20 seconds (trip acceptance window)
    )
    if not success:
        return ALREADY_MATCHED  # another ride request beat us to this driver

    # Send push notification to driver
    notification_service.push(
        driver_id=driver.id,
        payload=RideRequest(trip_id=ride.trip_id, pickup=ride.pickup, ...)
    )

    # Wait for driver response (up to 15 seconds)
    response = wait_for_driver_response(driver.id, ride.trip_id, timeout=15)

    if response == ACCEPTED:
        return ACCEPTED
    else:  # DECLINED or TIMEOUT
        redis.delete(f"driver:lock:{driver.id}")  # release lock
        return DECLINED
```

**Concurrency control — why Redis NX lock is critical:**

Without atomic locking, two matching service instances could both identify the same driver as the best candidate and both dispatch to them. The driver receives two simultaneous ride requests and accepts one — but both rides are now in state MATCHED with the same driver. The Redis NX (set only if not exists) lock ensures only one dispatch wins.

**Driver response timeout:**

If a driver doesn't respond in 15 seconds, the dispatch lock is released and the next candidate is tried. High-decline rate drivers are deprioritized in future matching (acceptance rate factor in score).

### 7.4 ETA Service and Road Network Graph

**Why not straight-line distance for ETA?**

Straight-line distance (Haversine formula) is useless for ETA. A driver 500m away in a straight line might be 3km by road due to one-way streets, a river, a highway with no nearby exit. Accurate ETA requires routing on the actual road network.

**Road Network Graph:**

```
Graph representation:
  Nodes (vertices): road intersections, ~100M nodes for a country
  Edges:            road segments between intersections
  Edge weights:     travel time (seconds), updated with real-time traffic

Edge attributes:
  - Distance (meters)
  - Speed limit (km/h)
  - Road class (highway, arterial, residential)
  - Turn restrictions (no U-turn, no left-turn)
  - One-way flag
  - Real-time speed (from GPS traces, updated every 5 minutes)

Graph size (US):
  ~30M nodes, ~70M edges
  Each edge: ~100 bytes
  Total graph: 7 GB — fits in RAM of a dedicated ETA server
```

**Routing algorithms:**

**Dijkstra:** O(E log V) — too slow for large graphs (the entire US graph takes seconds).

**A\* (heuristic):** O(E log V) with good heuristic (straight-line distance to destination). 5-10× faster than Dijkstra in practice. Still slow on large graphs.

**Contraction Hierarchies (CH):**  
The algorithm used in production by Uber, Google, and most mapping services.

1. **Preprocessing (offline, runs once per hour when traffic data updates):**
   - Contract nodes in order of "importance" — a node is important if many shortest paths pass through it
   - When contracting node v: add "shortcut" edges (u → w) whenever the shortest path u → v → w is the only shortest path from u to w
   - Result: a hierarchy where highways are at the top (many shortcuts through them), local roads at the bottom
   
2. **Query (online, per ETA request):**
   - Run bidirectional Dijkstra that only considers upward edges (into the hierarchy)
   - Queries on national-scale graphs run in ~1-10ms
   - 1000× faster than vanilla Dijkstra

**ETA with real-time traffic:**

```
Base ETA:       CH shortest path with historical average speeds by hour-of-day
Traffic overlay: Every 5 minutes, ingest GPS trace data from all active drivers
                 Use probe vehicle speeds to estimate current link speeds
                 Update edge weights in the graph (partial re-contraction for changed edges)

Final ETA formula:
  eta_seconds = sum(edge.length_m / current_speed_ms
                    for edge in path_edges)
              + turn_penalty_seconds
              + traffic_signal_delay_seconds
```

**ETA accuracy:**

Raw shortest-path ETA has ~30% error (traffic, parking, driver delays). Production systems apply a ML model on top:

```
features:
  - Predicted route travel time (from CH)
  - Time of day
  - Day of week
  - Historical delay for driver_id in this area
  - Current surge level (proxy for traffic congestion)
  - Weather (rain adds 15-20% to travel times)

target: actual_arrival_time - request_time

Model: LightGBM gradient boosted tree, trained on 6 months of trip history
Accuracy: within ±15% for 80% of trips
```

---

## 8. Trade-offs & Alternatives

### WebSocket vs Polling vs SSE for Location Updates to Rider

| Approach | Latency | Server load | Connection overhead |
|----------|---------|-------------|---------------------|
| Short polling (every 3s) | 3-6s | High (constant HTTP requests) | Low per request, high in aggregate |
| Long polling | 1-3s | Medium | Medium |
| WebSocket (persistent) | ~100ms | Low (one connection per session) | High (5M concurrent connections) |
| Server-Sent Events (SSE) | ~100ms | Low | Medium (unidirectional) |

**Recommendation:** WebSocket for rider-facing location tracking. SSE for one-way push notifications (ride status updates). Long polling for driver app on low-memory devices.

### Geospatial Index: Redis GEOSEARCH vs Custom Geohash Sets vs PostGIS

| Solution | Throughput | Latency | Operational complexity |
|----------|------------|---------|----------------------|
| Redis GEOADD/GEOSEARCH | Very high | <1ms | Low |
| Custom geohash sets (Redis sets) | High | <1ms | Medium (manual neighbor computation) |
| PostGIS (PostgreSQL extension) | Medium | 5-50ms | Low (SQL-based) |
| Elasticsearch geo_point | High | 5-20ms | Medium |

**Recommendation:** Redis GEOSEARCH for the hot path (real-time driver lookup). PostGIS for analytics (trip heatmaps, driver coverage reports, zone analysis). Never use PostGIS for real-time matching — it cannot handle 1.25M writes/second.

### Surge Pricing: Rule-Based vs ML-Based

**Rule-based:** Surge multiplier = `max(1.0, demand/supply)`, capped at 5.0. Simple, explainable, fast.  
**ML-based:** Predict future supply/demand 15 minutes out, set surge proactively to incentivize drivers before the shortage occurs. Uber uses this to pre-position drivers before airport arrivals, concert endings, etc.

**Recommendation for this design:** Rule-based. ML-based surge is a separate project with significant data pipeline and model serving requirements.

---

## 9. Failure Scenarios

### Scenario 1: Redis Location Index Node Fails

**Impact:** Driver lookups for 1/7th of the geographic grid fail. Matching Service cannot find drivers in affected areas.  
**Response:** Redis failover to replica (15-20 seconds). During failover: matching service retries with exponential backoff; after 3 failures, returns "No drivers available" to rider gracefully.  
**Recovery:** Replica promoted. Location Processor resumes writing to new primary. State re-populates from Kafka consumer lag (missing last 15-20 seconds of location updates).

### Scenario 2: Matching Service Crashes Mid-Dispatch

**Impact:** A ride is in state MATCHED but the Redis driver lock was acquired before the crash. Driver is notified but Matching Service dies before recording the match.  
**Recovery:** Redis lock has a 20-second TTL — it expires automatically. Trip remains in REQUESTED state. Matching Service restarts and re-runs matching for the trip. The driver receives a second notification (idempotent — driver sees the trip request and can accept).  
**Prevention:** Trip state writes to PostgreSQL are idempotent (upsert by trip_id). The Matching Service is stateless — any instance can pick up where another left off.

### Scenario 3: ETA Service Degraded (CH Graph Stale/Unavailable)

**Impact:** Cannot compute accurate ETAs for driver ranking.  
**Fallback:** Use straight-line distance / estimated speed (e.g., 30 km/h in urban areas) as a rough ETA proxy. Accuracy degrades but matching continues.  
**Alert:** ETA service SLO breach triggers oncall page. The degraded-mode fallback should be tested quarterly.

### Scenario 4: Notification Service Drops Driver Push

**Impact:** Driver receives no notification of ride request. Dispatch attempt times out (15 seconds). Matching tries next candidate.  
**Recovery:** Automatic — next candidate is dispatched. If all 3 candidates time out, ride returns to unmatched queue and re-matching is attempted.  
**Prevention:** Use delivery receipts (APNs/FCM delivery confirmations). If delivery receipt is not received within 2 seconds, attempt delivery via SMS fallback.

### Scenario 5: Geohash Level Mismatch — Surge Zone vs Driver Search

**Cause:** Surge pricing uses geohash level 5 (4.9km cells), but driver search uses level 6 (1.2km cells). A rider at the edge of a surge zone may be shown the surge price of the wrong zone.  
**Fix:** Always compute the rider's surge multiplier from the rider's geohash level 5 cell, not from the driver's cell. Surge applies to the ride request, not the driver's current location.

---

## 10. Interview Tips

### Time management

```
0-5 min:   Clarifying questions — scale, matching SLA, GPS precision
5-10 min:  Functional requirements + trip state machine (draw it early)
10-15 min: Capacity — driver pings/sec, rides/sec, storage
15-25 min: High-level architecture — location service + Kafka + Redis
25-40 min: Deep dives — geohash/H3 (always asked), matching algorithm with Redis NX lock
40-45 min: ETA service, failure scenarios
```

### The state machine is your anchor

Draw the trip state machine in the first 10 minutes. Every other component's design flows from it:
- Location Service feeds the AVAILABLE driver set
- Matching Service drives REQUESTED → MATCHED
- Notification Service is the trigger for each transition
- Payment Service fires on COMPLETED

If you can explain every state and every transition with the component responsible for it, you have demonstrated full systems ownership.

### Key insight: the driver lock problem

The single most-asked deep dive question in Uber system design interviews: **"How do you prevent two matching requests from dispatching to the same driver simultaneously?"**

Answer: Redis NX (set if not exists) provides an atomic compare-and-swap. It's a distributed lock with a TTL. The first dispatch request to set the lock wins; all others see the key exists and skip that driver. The lock TTL ensures it releases even if the Matching Service crashes.

### Numbers to know cold

- 5M active drivers × 1/4 pings/sec = 1.25M writes/second
- 100M rides/day → ~1,200 ride requests/second → ~5,000 at peak
- Geohash level 6: ~1.2km × 0.6km cells — 9 cells covers ~3.6km radius
- H3 resolution 7: ~1.3km hexagon edge length, all 6 neighbors equidistant
- Redis GEOSEARCH: p99 < 5ms for 5km radius in dense urban areas
- Contraction Hierarchies: 1-10ms for national routing vs seconds for Dijkstra
- Driver lock TTL: 20 seconds (15s response window + 5s buffer)

### What to draw on the whiteboard

1. The trip state machine — all 7 states with arrows and responsible components
2. The geohash ring/grid showing 9-cell lookup for a rider at a cell boundary
3. The Kafka → Redis write pipeline for location updates
4. The Redis NX lock sequence diagram: two matching services, one lock, one winner

---

*The key to excelling at this interview is recognizing that Uber is fundamentally three separate hard problems: (1) geospatial indexing at 1.25M writes/second, (2) distributed matching with atomic driver assignment, and (3) ETA computation on a real road graph. Structure your answer around each of these, and let the interviewer guide which one they want to go deeper on.*
