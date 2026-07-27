# Consistent Hashing

## 1. What It Is

Consistent hashing is a technique that maps both keys and nodes onto a circular hash ring, so that adding or removing a node only remaps a fraction (1/n) of keys instead of rehashing everything. It exists because distributed systems need to spread load across nodes while surviving dynamic membership changes without catastrophic data movement.

---

## 2. The Problem It Solves

**Naive modulo hashing:** `node = hash(key) % N`

You have 4 cache nodes. Key "user:123" hashes to 57, so 57 % 4 = 1 → Node 1.

Now Node 2 goes down. N becomes 3. Every key that previously mapped to nodes 2, 3 is now wrong:
- 57 % 3 = 0 → Node 0 (not Node 1 anymore)

Result: ~100% of keys are invalidated. For a cache, this causes a thundering herd. For a database partition, this means migrating almost all data. Scaling from N to N+1 nodes invalidates (N/(N+1)) ≈ 100% of mappings.

**What consistent hashing achieves:** Adding/removing 1 node only remaps ~1/N of all keys (the keys that "belonged" to that node's arc on the ring).

---

## 3. How It Works

### Basic Ring

Hash both nodes and keys into the same hash space [0, 2^32). Arrange the space as a ring. Each key is assigned to the first node clockwise from its hash position.

```
                        0 (= 2^32)
                   ┌────────────────┐
              330° │                │ 30°
                   │    Ring        │
         Node C    │                │    Node A
          (270°) ──┼──              │── (45°)
                   │                │
              240° │                │ 90°
                   │                │
                   └────────────────┘
                        180°
                      Node B (180°)

Key K1 at 100° → clockwise → Node B (180°)
Key K2 at 350° → clockwise → Node A (45°, wrapping around 0°)
Key K3 at 200° → clockwise → Node C (270°)
```

**Node removal:** If Node B is removed, its keys (100°–179°) now walk clockwise to Node C (270°). Only those keys move. Nodes A and C are unaffected.

**Node addition:** If Node D joins at 120°, keys 100°–120° shift from Node B to Node D. Everything else stays.

### Virtual Nodes (Vnodes)

**Problem with basic ring:** With 3 real nodes, the arcs are unequal — Node A might own 30% of the ring, Node B 50%, Node C 20%. Load is uneven. When a node leaves, all its keys pile onto one successor.

**Solution:** Each physical node gets V virtual node positions on the ring. Each virtual node is hashed as `hash("NodeA#1")`, `hash("NodeA#2")`, ..., `hash("NodeA#V")`.

```
Ring with Virtual Nodes (V=3 per node):

     A1    B2   A2
  ●  ●     ●    ●    ●    ●    ●    ●    ●  ← positions on ring
  C3        A3       B1       C1       B3  C2

Physical nodes: A, B, C
Virtual nodes:  A1,A2,A3 → Physical Node A
                B1,B2,B3 → Physical Node B
                C1,C2,C3 → Physical Node C

Key K hashes to position between B2 and A2 → assigned to A2 → Physical Node A
```

**Why 150–200 vnodes per node (industry standard):**
- Statistical load balancing: with V=100, standard deviation of load ≈ 10%. With V=200, ≈ 7%.
- When a node fails, its V vnodes are distributed among ~V different successors, spreading the load spike.
- Cassandra default: 256 vnodes. DynamoDB: uses similar virtual node concept internally.
- Cost: memory for the ring data structure — 3 nodes × 200 vnodes = 600 entries in a TreeMap. Negligible.

### Hotspot Problem

Even with vnodes, a single key can be hot (a celebrity's timeline, a viral post). Consistent hashing distributes keys across nodes, but it doesn't help when one key receives disproportionate traffic. Solutions:
1. **Add a random suffix:** `hash(key + random_suffix)` to spread reads across replicas.
2. **Replication with read distribution:** replicate the key to K nodes, read-balance across them.
3. **Application-level caching:** local in-process cache for the hot key.

---

## 4. Algorithm / Implementation

### Finding the Responsible Node (Java, TreeMap)

```java
import java.util.SortedMap;
import java.util.TreeMap;
import java.security.MessageDigest;
import java.nio.ByteBuffer;

public class ConsistentHashRing {
    private final SortedMap<Long, String> ring = new TreeMap<>();
    private final int virtualNodes;

    public ConsistentHashRing(int virtualNodes) {
        this.virtualNodes = virtualNodes;
    }

    public void addNode(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(node + "#" + i);
            ring.put(hash, node);
        }
    }

    public void removeNode(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(node + "#" + i);
            ring.remove(hash);
        }
    }

    public String getNode(String key) {
        if (ring.isEmpty()) return null;
        long hash = hash(key);

        // Find first node clockwise from key's hash
        SortedMap<Long, String> tailMap = ring.tailMap(hash);

        // If no node has a hash >= key's hash, wrap around to the first node
        long nodeHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(nodeHash);
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes("UTF-8"));
            // Use first 4 bytes as a long in range [0, 2^32)
            return ByteBuffer.wrap(digest).getInt() & 0xFFFFFFFFL;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

**Key operations:**
- `addNode`: O(V log V) — V insertions into TreeMap
- `removeNode`: O(V log V)
- `getNode`: O(log(N * V)) — binary search on TreeMap

**Why MD5 over Java's hashCode():** Java's `String.hashCode()` is not uniformly distributed over 2^32. MD5 (or MurmurHash) gives better distribution. Production: use MurmurHash3 (non-cryptographic, faster).

### Replication

For a replication factor of R=3, a write to key K goes to the first 3 distinct physical nodes clockwise from K's hash. In `getNode`, walk the tailMap collecting unique physical nodes until you have R.

```java
public List<String> getNodes(String key, int replicas) {
    List<String> nodes = new ArrayList<>();
    if (ring.isEmpty()) return nodes;

    long hash = hash(key);
    SortedMap<Long, String> tailMap = ring.tailMap(hash);

    // Iterate ring starting from hash position, wrap around
    Iterator<Long> it = Iterables.concat(tailMap.keySet(), ring.keySet()).iterator();
    while (it.hasNext() && nodes.size() < replicas) {
        String node = ring.get(it.next());
        if (!nodes.contains(node)) {
            nodes.add(node);
        }
    }
    return nodes;
}
```

---

## 5. Trade-offs

### Pros
- **Minimal key remapping** on membership change: ~1/N keys move when 1 node is added/removed.
- **Horizontal scalability**: adding nodes automatically absorbs ~1/N of the load.
- **No central coordinator** needed for key-to-node mapping — every node can independently compute the ring.

### Cons
- **Uneven load without vnodes**: basic ring causes hotspots. Vnodes fix this at the cost of complexity.
- **Cascading failure on node loss**: the physical successor absorbs all the dead node's traffic (mitigated by vnodes, R-factor replication).
- **Hot keys are not solved**: consistent hashing distributes key space, not key access frequency.
- **Rebalancing is not instant**: when a node joins, data migration to it takes time; the node must catch up before serving traffic.
- **Vnode overhead**: more metadata in gossip protocols (Cassandra gossip carries the full token ring).

### Failure Modes
- **Node failure + no replication**: data on that node's arc is lost until the node recovers.
- **Network partition**: ring split into two groups each thinking they own certain keys → write conflicts (handled by vector clocks / last-write-wins in Cassandra).
- **Clock drift (not directly applicable)**: consistent hashing is clock-independent; this is not a concern here (contrast with Redlock).

### Alternatives
- **Rendezvous hashing (highest random weight)**: for each key, compute a score for every node, pick max. No ring needed. Simpler, but O(N) per lookup vs O(log N).
- **Jump consistent hash**: Google's algorithm — O(ln N) with no data structure, but only works if nodes are added/removed at the tail (no arbitrary failures).
- **Directory-based sharding**: a central metadata server maps keys to nodes. Simpler to reason about; becomes a bottleneck and single point of failure.

---

## 6. Where It Appears in Real Systems

| System | Usage |
|--------|-------|
| **Apache Cassandra** | Full ring-based partitioning; default 256 vnodes per node since 3.x |
| **Amazon DynamoDB** | Internally uses consistent hashing for partition routing |
| **Redis Cluster** | 16,384 hash slots (a variant); each node owns a range of slots |
| **Memcached** | Client-side consistent hashing libraries (libketama) |
| **Akamai CDN** | Original paper co-authored by Akamai founders (Karger et al., 1997) |
| **Riak** | Ring-based partitioning, configurable ring size (default 64 partitions) |
| **Nginx/HAProxy** | Consistent hashing load balancing for upstream selection |
| **Chord (P2P DHT)** | The foundational algorithm; Consistent Hashing is Chord's lookup mechanism |

---

## 7. Numbers to Know

| Metric | Value |
|--------|-------|
| Keys remapped on node add/remove (no vnodes) | ~1/N |
| Keys remapped on node add/remove (with vnodes) | Same ~1/N, but spread across many successors |
| Typical vnodes per physical node | 150–256 (Cassandra: 256) |
| Load imbalance std dev with V=100 | ~10% |
| Load imbalance std dev with V=200 | ~7% |
| Redis Cluster hash slots | 16,384 |
| TreeMap `getNode` time complexity | O(log(N × V)) |
| Hash space (MD5-based rings) | 0 to 2^32 − 1 |
| Cassandra gossip convergence | Seconds (configurable gossip interval ~1s) |

---

## 8. Interview Tips

### What You'll Be Asked

**"Design a distributed cache. How do you distribute keys?"**
- Start with naive modulo → explain the failure on node change → introduce consistent hashing → mention vnodes proactively.

**"What happens when a node fails?"**
- Successor node absorbs traffic. With R=3 replication, no data loss. Without replication, data on that arc is unavailable.

**"How do you handle hot keys?"**
- Consistent hashing doesn't solve this. Answer: replication + read load balancing, local in-process LRU cache for top-N hot keys, random suffix for fan-out.

**"How do you add a node without downtime?"**
- New node joins the ring, announces itself via gossip. Keys migrate asynchronously from the successor. During migration, reads can be served from both old and new node (double-read until fully migrated), or the system accepts slightly stale reads.

### Common Follow-ups
- "Why 150–200 vnodes and not 10 or 1000?" → statistical balance vs memory/gossip overhead.
- "How does Cassandra's ring compare to Redis Cluster's slot approach?" → Ring is continuous; Redis Cluster uses 16,384 discrete slots that are explicitly assigned, making rebalancing more deterministic.
- "What's the CAP trade-off in a consistent hashing system?" → depends on quorum reads/writes, not on hashing itself. Hashing determines *where* data lives; CAP is about *how many* replicas must agree.

### Mistakes to Avoid
- Saying "consistent hashing solves hot keys" — it distributes keys by hash, not by traffic volume.
- Forgetting to mention virtual nodes until asked — bring them up immediately after the basic ring.
- Confusing hash space size with number of vnodes.
- Not knowing that Redis Cluster does NOT use a ring — it uses fixed 16,384 slots.
