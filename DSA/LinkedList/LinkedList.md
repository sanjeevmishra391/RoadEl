# Linked List

## What It Is

A linked list is a sequence of nodes where each node holds a value and a pointer to the next node (singly linked) or both next and previous pointers (doubly linked). Java has no built-in singly linked list node — you define `ListNode` yourself. The key insight is that a linked list gives O(1) pointer manipulation but O(n) access by index.

---

## When To Use

| Trigger phrase | Pattern |
|---|---|
| "Detect a cycle / find cycle entry" | Fast/slow pointer (Floyd's algorithm) |
| "Find middle of list" | Fast/slow pointer |
| "Reverse a linked list / reverse in K-groups" | Iterative three-pointer or recursive |
| "Remove Nth from end / Kth from end" | Two pointers with a gap of N |
| "Merge two/K sorted lists" | Dummy head + min-heap (for K lists) |
| "Reorder list: 1→2→3→4 → 1→4→2→3" | Find middle + reverse second half + merge |
| "O(1) get/put cache with LRU eviction" | Doubly linked list + HashMap |
| "Palindrome linked list" | Find middle + reverse second half + compare |

---

## Core Patterns

### ListNode Definition (from ListNode.java)

```java
public class ListNode {
    public int val;          // use 'value' in repo, 'val' on LeetCode — know both
    public ListNode next;

    ListNode() { next = null; }
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
}
```

### Pattern 1 — Fast/Slow Pointer (Floyd's)

```java
// Find middle: when fast reaches end, slow is at middle
// For even-length [1,2,3,4]: slow stops at 3 (second middle)
// For odd-length  [1,2,3,4,5]: slow stops at 3 (true middle)
ListNode slow = head, fast = head;
while (fast != null && fast.next != null) {
    slow = slow.next;
    fast = fast.next.next;
}
// slow == middle node

// Detect cycle: slow and fast meet if and only if there is a cycle
ListNode slow = head, fast = head;
while (fast != null && fast.next != null) {
    slow = slow.next;
    fast = fast.next.next;
    if (slow == fast) return true; // cycle detected
}
return false;
```

### Pattern 2 — Reverse a Linked List (iterative)

```java
// Three pointers: prev, curr, next
// Reference: LinkedList.java — reverseList()
ListNode prev = null, curr = head;
while (curr != null) {
    ListNode next = curr.next;  // save next before overwriting
    curr.next = prev;           // reverse the pointer
    prev = curr;                // advance prev
    curr = next;                // advance curr
}
return prev;  // prev is the new head
```

### Pattern 3 — Dummy Head

```java
// Use when the head itself might change (e.g., merge, remove)
ListNode dummy = new ListNode(0);
dummy.next = head;
ListNode curr = dummy;
// ... manipulate list via curr ...
return dummy.next;  // new head
```

---

## Key Problems

### 1. Reverse Linked List

**Approach**: Iterative three-pointer is O(1) space. Recursive is clean but O(n) stack space — know both.

```java
// ITERATIVE — O(n) time, O(1) space
// Reference: LinkedList.java — reverseList()
public ListNode reverseList(ListNode head) {
    ListNode prev = null, curr = head;
    while (curr != null) {
        ListNode next = curr.next;  // 1. save
        curr.next = prev;           // 2. reverse pointer
        prev = curr;                // 3. advance prev
        curr = next;                // 4. advance curr
    }
    return prev;  // prev is the new head (old tail)
}

// RECURSIVE — O(n) time, O(n) stack space
public ListNode reverseList_Recursive(ListNode head) {
    if (head == null || head.next == null) return head;  // base case
    ListNode newHead = reverseList_Recursive(head.next); // reverse rest
    head.next.next = head;  // make next node point back to head
    head.next = null;       // disconnect head's forward pointer
    return newHead;
}
```

**Time**: O(n) | **Space**: Iterative O(1), Recursive O(n)

---

### 2. Detect Cycle (Floyd's Algorithm) + Find Cycle Start

**Approach**: Phase 1 — detect meeting point. Phase 2 — find cycle entry: reset one pointer to head, advance both one step at a time; they meet at the cycle's entry node.

```java
// Reference: LinkedList.java — hasCycle()
public boolean hasCycle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        if (slow == fast) return true;
    }
    return false;
}

// FIND CYCLE START (LeetCode 142)
public ListNode detectCycle(ListNode head) {
    ListNode slow = head, fast = head;

    // Phase 1: find meeting point inside cycle
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        if (slow == fast) break;
    }
    if (fast == null || fast.next == null) return null;  // no cycle

    // Phase 2: find cycle entry
    // Math: distance from head to entry == distance from meeting point to entry
    slow = head;
    while (slow != fast) {
        slow = slow.next;
        fast = fast.next;  // both move ONE step now
    }
    return slow;  // cycle entry node
}
```

**Why Phase 2 works**: If the cycle starts at distance `F` from the head and the cycle length is `C`, the meeting point is at distance `F` from the cycle start. Resetting one pointer to head and advancing both by 1 step at a time brings them to the entry after exactly `F` steps.

**Time**: O(n) | **Space**: O(1)

---

### 3. Remove Nth Node From End of List

**Approach**: Two pointers. Advance `fast` by `n+1` steps first (creates a gap of n). Then advance both until `fast == null`. `slow.next` is the node to remove.

```java
// Reference: RemoveNthFromLast.java (this is the clean two-pointer version)
public ListNode removeNthFromEnd(ListNode head, int n) {
    ListNode dummy = new ListNode(0);
    dummy.next = head;
    ListNode fast = dummy, slow = dummy;

    // Advance fast by n+1 steps (gap of n between fast and slow)
    for (int i = 0; i <= n; i++) {
        fast = fast.next;
    }

    // Advance both until fast reaches null
    while (fast != null) {
        slow = slow.next;
        fast = fast.next;
    }

    // slow.next is the nth node from end — remove it
    slow.next = slow.next.next;
    return dummy.next;
}
```

**Why `n+1` steps for fast?** We want `slow` to stop at the node *before* the target (so we can unlink it). If `fast` starts `n+1` ahead of `slow`, when `fast` hits `null`, `slow` is exactly one step before the target.

**Time**: O(n) — single pass | **Space**: O(1)

---

### 4. Merge Two Sorted Lists

**Approach**: Use a dummy head. Compare heads of both lists; always attach the smaller one to the result. When one list is exhausted, attach the other.

```java
public ListNode mergeTwoLists(ListNode l1, ListNode l2) {
    ListNode dummy = new ListNode(0);
    ListNode curr = dummy;

    while (l1 != null && l2 != null) {
        if (l1.val <= l2.val) {
            curr.next = l1;
            l1 = l1.next;
        } else {
            curr.next = l2;
            l2 = l2.next;
        }
        curr = curr.next;
    }
    // Attach remaining non-null list
    curr.next = (l1 != null) ? l1 : l2;
    return dummy.next;
}
```

**Time**: O(m + n) | **Space**: O(1) — no new nodes created, pointers rearranged in place

---

### 5. Merge K Sorted Lists

**Approach**: Seed a min-heap with the head of each list. Poll minimum, add to result, push that node's next. The heap size is always ≤ K.

```java
// Reference: MergekSortedLists.java — mergeKLists2()
public ListNode mergeKLists(ListNode[] lists) {
    // Min-heap on node value
    PriorityQueue<ListNode> pq = new PriorityQueue<>((a, b) -> a.val - b.val);

    for (ListNode node : lists) {
        if (node != null) pq.offer(node);  // null check — never offer null to PQ
    }

    ListNode dummy = new ListNode(0);
    ListNode tail = dummy;

    while (!pq.isEmpty()) {
        tail.next = pq.poll();      // take current minimum
        tail = tail.next;
        if (tail.next != null) {    // push next node from the same list
            pq.offer(tail.next);
        }
    }
    return dummy.next;
}
```

**Why not just pairwise merge?** Pairwise merging K lists is O(NK) in the worst case. Heap-based K-way merge is O(N log K) where N = total nodes.

**Time**: O(N log K) | **Space**: O(K) for the heap

---

### 6. Reorder List

**Approach**: Three steps — (1) find middle with fast/slow, (2) reverse the second half, (3) interleave first half and reversed second half.

```java
// Reference: ReorderList.java
public void reorderList(ListNode head) {
    if (head == null || head.next == null) return;

    // Step 1: Find middle (slow stops at mid; for [1,2,3,4,5] slow = 3)
    ListNode slow = head, fast = head;
    while (fast.next != null && fast.next.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    // slow is now the last node of the first half
    ListNode secondHalf = slow.next;
    slow.next = null;  // cut the list in half

    // Step 2: Reverse the second half
    ListNode prev = null, curr = secondHalf;
    while (curr != null) {
        ListNode next = curr.next;
        curr.next = prev;
        prev = curr;
        curr = next;
    }
    ListNode reversedSecond = prev;

    // Step 3: Interleave first half and reversed second half
    ListNode first = head, second = reversedSecond;
    while (second != null) {
        ListNode tmp1 = first.next;
        ListNode tmp2 = second.next;
        first.next  = second;
        second.next = tmp1;
        first  = tmp1;
        second = tmp2;
    }
}
```

**Why cut the list before reversing?** Reversing a list without cutting leaves the old tail pointing to a mid-list node, creating a cycle during interleaving.

**Time**: O(n) | **Space**: O(1)

---

### 7. Reverse Nodes in K-Group

**Approach**: Count K nodes ahead. If fewer than K remain, leave them as-is. Reverse the K nodes in place, connect the reversed segment to the previous segment, recurse/iterate on the rest.

```java
// Reference: ReverseNodeskGroup.java (clean recursive version)
public ListNode reverseKGroup(ListNode head, int k) {
    // Check if there are at least k nodes remaining
    ListNode check = head;
    int count = 0;
    while (check != null && count < k) { check = check.next; count++; }
    if (count < k) return head;  // fewer than k nodes — leave as-is

    // Reverse k nodes
    ListNode prev = null, curr = head;
    for (int i = 0; i < k; i++) {
        ListNode next = curr.next;
        curr.next = prev;
        prev = curr;
        curr = next;
    }
    // After reversal:
    //   prev = new head of this segment
    //   head = old head (now tail of reversed segment)
    //   curr = start of remaining list

    // head.next connects tail of reversed segment to the recursively reversed rest
    head.next = reverseKGroup(curr, k);
    return prev;
}
```

**Time**: O(n) — every node is reversed exactly once
**Space**: O(n/k) — recursion stack depth (O(1) if iterative)

---

### 8. LRU Cache

**Design**: Doubly linked list (most-recent at head, LRU at tail) + HashMap (key → node). Both `get` and `put` are O(1) because the HashMap gives direct node access and the doubly linked list allows O(1) removal from any position.

```java
// Reference: LRUCache.java
class LRUCache {
    private class Node {
        int key, val;
        Node prev, next;
        Node(int k, int v) { key = k; val = v; }
    }

    private final int capacity;
    private final Map<Integer, Node> map;
    private final Node head, tail;  // sentinel dummy nodes — never removed

    public LRUCache(int capacity) {
        this.capacity = capacity;
        map = new HashMap<>();
        head = new Node(0, 0);   // most-recently-used sentinel
        tail = new Node(0, 0);   // LRU sentinel
        head.next = tail;
        tail.prev = head;
    }

    // Remove node from its current position in the DLL
    private void remove(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    // Insert node right after head (= most recently used position)
    private void insertAtFront(Node node) {
        node.next = head.next;
        node.next.prev = node;
        node.prev = head;
        head.next = node;
    }

    public int get(int key) {
        Node node = map.get(key);
        if (node == null) return -1;
        remove(node);          // move to front (most recently used)
        insertAtFront(node);
        return node.val;
    }

    public void put(int key, int value) {
        Node existing = map.get(key);
        if (existing != null) {
            existing.val = value;
            remove(existing);
            insertAtFront(existing);
        } else {
            if (map.size() == capacity) {
                // Evict LRU: the node just before the tail sentinel
                Node lru = tail.prev;
                remove(lru);
                map.remove(lru.key);  // CRITICAL: use node.key, not the new key
            }
            Node newNode = new Node(key, value);
            insertAtFront(newNode);
            map.put(key, newNode);
        }
    }
}
```

**Why sentinel head and tail?** Sentinels eliminate null checks in `remove` and `insertAtFront` — every real node always has both a prev and a next. This is the cleanest implementation under interview time pressure.

**Why store key in the node?** When evicting the LRU node (tail.prev), you must remove it from the HashMap. Without the key stored in the node, you cannot do this lookup.

**Time**: O(1) for both get and put
**Space**: O(capacity)

---

## Common Mistakes

1. **Not using a dummy head**. When the head node itself might be removed (Remove Nth From End, Merge Two Sorted Lists), handling the head as a special case adds conditional logic that causes bugs. Always use `ListNode dummy = new ListNode(0); dummy.next = head` — then return `dummy.next`.

2. **Losing the `next` pointer before overwriting it during reversal**. The reversal loop's first line must always be `ListNode next = curr.next` — before `curr.next = prev`. Missing this severs the rest of the list. It's the single most common linked list bug.

3. **Off-by-one in fast/slow pointer middle-finding**. The standard `while (fast != null && fast.next != null)` stops slow at the *second* middle for even-length lists. For Reorder List and Palindrome List you need to stop at the *first* middle: use `while (fast.next != null && fast.next.next != null)`. Drawing a 4-node example always resolves which condition you need.

4. **LRU Cache: using `map.remove(key)` instead of `map.remove(node.key)` during eviction**. During eviction you remove the `tail.prev` node. The variable `key` in scope is the *new key being inserted*, not the evicted node's key. Always evict with `map.remove(lruNode.key)`.

5. **Offering `null` to `PriorityQueue` in Merge K Lists**. If any list in the input array is `null`, `pq.offer(null)` throws `NullPointerException`. Always null-check before offering: `if (node != null) pq.offer(node)`.

---

## Interview Tips

**What interviewers probe:**

- **Draw the pointer state before and after every mutation.** For reversal, Reorder List, and K-Group, draw boxes for each node with arrows and update the arrows step by step. Verbal hand-waving without a diagram will miss the edge case.
- **LRU sentinel design.** Interviewers specifically ask "why two dummy nodes?" — the answer is that they make `remove` and `insertAtFront` branchless: every real node always has a valid `prev` and `next`.
- **Floyd's cycle detection proof.** Know why resetting one pointer to head after the meeting point finds the cycle entry. The invariant: `dist(head, entry) == dist(meeting_point, entry)`.

**Common follow-up questions:**

- *Reverse Linked List*: "Can you do it without recursion in O(1) space?" → Yes, the iterative three-pointer approach.
- *LRU Cache*: "How would you implement LFU (Least Frequently Used) Cache?" → Requires a min-heap or a doubly nested structure (frequency → DLL of nodes at that frequency).
- *Merge K Lists*: "What is the time complexity and why not O(NK)?" → The heap always has at most K elements; each poll + offer is O(log K); there are N total pops → O(N log K).
- *Reverse K-Group*: "What if you should reverse in groups of K but keep the last group as-is vs also reversing it?" → A `count < k` check at the start handles the as-is case; remove it and call with the remainder to also reverse.
- *Reorder List*: "What is the space complexity?" → O(1) — we only rearrange existing nodes without allocating new ones.
- *Detect Cycle Start*: "Prove that Phase 2 finds the entry node." → Draw the distance equations: let F = distance to cycle entry, L = cycle length, D = distance from entry to meeting point. From `F + D ≡ 0 (mod L)` derive `F ≡ L - D (mod L)`, which equals the remaining distance from meeting point to entry.
