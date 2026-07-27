# Heap (Priority Queue)

## What It Is

A heap is a complete binary tree satisfying the heap property: in a **min-heap** every parent is ≤ its children (root = minimum); in a **max-heap** every parent is ≥ its children (root = maximum). Java's `PriorityQueue` is a **min-heap by default**; pass `Collections.reverseOrder()` or a reverse comparator to get a max-heap.

---

## When To Use

Look for these trigger phrases in the problem statement:

| Trigger | Pattern |
|---|---|
| "Kth largest / Kth smallest" | Fixed-size heap of size K |
| "Top K frequent / closest / heaviest" | Fixed-size heap of size K |
| "Merge K sorted lists/arrays/streams" | K-way merge with min-heap |
| "Median from a stream / sliding median" | Two-heap (lower-half max-heap + upper-half min-heap) |
| "Schedule tasks / CPU scheduling" | Max-heap on frequency |
| "Continuously find min/max as elements arrive" | Heap as dynamic sorted structure |
| "Sliding window maximum/minimum" | Monotonic deque (not heap — but often conflated) |

**Key rule**: reach for a heap whenever you need *repeated* access to the current min/max while the dataset is changing.

---

## Core Templates

### 1. Min-Heap and Max-Heap Declaration

```java
// MIN-HEAP — natural order (default)
PriorityQueue<Integer> minHeap = new PriorityQueue<>();

// MAX-HEAP — reverse natural order
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());

// MAX-HEAP — lambda (equivalent, works for any type)
PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> b - a);

// CUSTOM OBJECT — sort by first element ascending (min-heap on first)
PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);

// CUSTOM OBJECT — sort by first element descending (max-heap on first)
PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> b[0] - a[0]);

// With initial capacity hint (does NOT set a cap — heap grows automatically)
PriorityQueue<Integer> pq = new PriorityQueue<>(k);

// IMPORTANT: Integer subtraction a - b overflows for very negative values.
// Safe comparator for Integers:
PriorityQueue<Integer> safe = new PriorityQueue<>(Integer::compare);           // min
PriorityQueue<Integer> safeMax = new PriorityQueue<>(Comparator.reverseOrder()); // max
```

### 2. Fixed-Size K Heap (Top K pattern)

```java
// Keep the K LARGEST elements — use a MIN-heap of size K
// (smallest of the K largest sits at the top; evict if new element is bigger)
PriorityQueue<Integer> minHeap = new PriorityQueue<>();  // min-heap, size <= K

for (int num : nums) {
    minHeap.offer(num);
    if (minHeap.size() > k) {
        minHeap.poll();   // evict the smallest — we only want the K largest
    }
}
// minHeap.peek() == Kth largest
// minHeap contains all K largest elements

// Keep the K SMALLEST elements — use a MAX-heap of size K
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());

for (int num : nums) {
    maxHeap.offer(num);
    if (maxHeap.size() > k) {
        maxHeap.poll();   // evict the largest — we only want the K smallest
    }
}
```

### 3. Two-Heap (Median Pattern)

```java
// maxHeap holds the lower half  → top = max of lower half
// minHeap holds the upper half  → top = min of upper half
// Invariant: maxHeap.size() == minHeap.size()  OR  maxHeap.size() == minHeap.size() + 1

PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder()); // lower
PriorityQueue<Integer> minHeap = new PriorityQueue<>();                            // upper

void addNum(int num) {
    maxHeap.offer(num);                         // always add to lower half first
    minHeap.offer(maxHeap.poll());              // balance: push max of lower to upper
    if (minHeap.size() > maxHeap.size()) {      // keep lower half >= upper half in size
        maxHeap.offer(minHeap.poll());
    }
}

double findMedian() {
    if (maxHeap.size() > minHeap.size()) return maxHeap.peek();
    return (maxHeap.peek() + minHeap.peek()) / 2.0;   // note: 2.0 for double division
}
```

### 4. K-Way Merge Pattern

```java
// Each entry in the heap: [value, listIndex, elementIndex]
PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]); // min on value

// Seed the heap with the first element of each list
for (int i = 0; i < lists.length; i++) {
    if (lists[i] != null) {
        pq.offer(new int[]{lists[i].val, i, 0});
    }
}

while (!pq.isEmpty()) {
    int[] curr = pq.poll();
    int val = curr[0], listIdx = curr[1], elemIdx = curr[2];
    // process val ...
    // push next element from the same list
    if (elemIdx + 1 < lists[listIdx].length) {
        pq.offer(new int[]{lists[listIdx][elemIdx + 1], listIdx, elemIdx + 1});
    }
}
```

---

## Key Problems

### 1. Kth Largest Element in an Array

**Approach**: Two solutions shown — QuickSelect averages O(n) but degrades to O(n²) worst case; heap approach is always O(n log k). For an interview, state both and implement the one asked.

```java
// APPROACH 1: Min-Heap of size K — O(n log k) time, O(k) space
public int findKthLargest_Heap(int[] nums, int k) {
    // Use a MIN-heap of size k — the root is the kth largest
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    for (int num : nums) {
        minHeap.offer(num);
        if (minHeap.size() > k) {
            minHeap.poll();           // evict smallest; keep top K largest
        }
    }
    return minHeap.peek();            // root = kth largest
}

// APPROACH 2: QuickSelect (Lomuto partition) — O(n) avg, O(n²) worst, O(1) extra space
public int findKthLargest_QuickSelect(int[] nums, int k) {
    int target = nums.length - k;    // kth largest = (n-k)th smallest (0-indexed)
    return quickSelect(nums, 0, nums.length - 1, target);
}

private int quickSelect(int[] nums, int lo, int hi, int target) {
    int pivot = nums[hi];
    int i = lo;
    for (int j = lo; j < hi; j++) {
        if (nums[j] <= pivot) {
            swap(nums, i++, j);
        }
    }
    swap(nums, i, hi);               // pivot is now at index i

    if (i == target) return nums[i];
    if (i < target)  return quickSelect(nums, i + 1, hi, target);
    return quickSelect(nums, lo, i - 1, target);
}

private void swap(int[] nums, int a, int b) {
    int tmp = nums[a]; nums[a] = nums[b]; nums[b] = tmp;
}
```

**Time**: Heap — O(n log k) | QuickSelect — O(n) avg, O(n²) worst
**Space**: Heap — O(k) | QuickSelect — O(1) extra (O(n) stack worst case)

---

### 2. Top K Frequent Elements

**Approach**: Count frequencies; then either use a max-heap (O(n log k)) or bucket sort (O(n)) where bucket index = frequency.

```java
// APPROACH 1: Min-Heap of size K — O(n log k) time, O(n) space
public int[] topKFrequent_Heap(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int n : nums) freq.merge(n, 1, Integer::sum);

    // Min-heap keyed on frequency — keeps K most frequent
    PriorityQueue<Map.Entry<Integer, Integer>> minHeap =
        new PriorityQueue<>((a, b) -> a.getValue() - b.getValue());

    for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
        minHeap.offer(entry);
        if (minHeap.size() > k) minHeap.poll(); // evict least frequent
    }

    int[] res = new int[k];
    for (int i = k - 1; i >= 0; i--) res[i] = minHeap.poll().getKey();
    return res;
}

// APPROACH 2: Bucket Sort — O(n) time, O(n) space
public int[] topKFrequent_Bucket(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int n : nums) freq.merge(n, 1, Integer::sum);

    // Bucket index = frequency; bucket[i] holds all numbers with frequency i
    List<Integer>[] bucket = new List[nums.length + 1];
    for (int i = 0; i <= nums.length; i++) bucket[i] = new ArrayList<>();
    for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
        bucket[e.getValue()].add(e.getKey());
    }

    int[] res = new int[k];
    int idx = 0;
    for (int i = nums.length; i >= 0 && idx < k; i--) { // iterate high freq → low
        for (int num : bucket[i]) {
            if (idx == k) break;
            res[idx++] = num;
        }
    }
    return res;
}
```

**Time**: Heap — O(n log k) | Bucket — O(n)
**Space**: Both O(n)

---

### 3. K Closest Points to Origin

**Approach**: Maintain a max-heap of size K keyed on squared distance. Evict the farthest point whenever size exceeds K. No `Math.sqrt` needed — compare squared distances.

```java
public int[][] kClosest(int[][] points, int k) {
    // MAX-heap on squared distance — root is the FARTHEST among the K closest
    PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
        (a, b) -> (b[0]*b[0] + b[1]*b[1]) - (a[0]*a[0] + a[1]*a[1])
    );

    for (int[] p : points) {
        maxHeap.offer(p);
        if (maxHeap.size() > k) {
            maxHeap.poll();   // evict farthest point
        }
    }

    int[][] res = new int[k][2];
    for (int i = 0; i < k; i++) res[i] = maxHeap.poll();
    return res;
}
```

**Time**: O(n log k) — each of n points does at most one offer + one poll on a size-k heap
**Space**: O(k)

---

### 4. Merge K Sorted Lists

**Approach**: Seed a min-heap with the head of each list. Poll the minimum node, add it to the result, then push that node's `next` (if non-null) back into the heap.

```java
// ListNode definition: int val; ListNode next;
public ListNode mergeKLists(ListNode[] lists) {
    // Min-heap on node value
    PriorityQueue<ListNode> pq = new PriorityQueue<>((a, b) -> a.val - b.val);

    // Seed with heads (skip null lists)
    for (ListNode node : lists) {
        if (node != null) pq.offer(node);
    }

    ListNode dummy = new ListNode(0);
    ListNode tail = dummy;

    while (!pq.isEmpty()) {
        tail.next = pq.poll();          // take the current minimum
        tail = tail.next;
        if (tail.next != null) {        // push that list's next node
            pq.offer(tail.next);
        }
    }

    return dummy.next;
}
```

**Time**: O(N log k) — N = total nodes, k = number of lists (heap ops cost log k each)
**Space**: O(k) — heap holds at most one node per list

---

### 5. Find Median from Data Stream

**Approach**: Two-heap invariant — `maxHeap` (lower half) size always equals `minHeap` (upper half) size, or is exactly 1 larger. Median is either `maxHeap.peek()` alone or the average of both tops.

```java
class MedianFinder {
    // maxHeap: lower half — we want its MAXIMUM quickly
    private PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    // minHeap: upper half — we want its MINIMUM quickly
    private PriorityQueue<Integer> minHeap = new PriorityQueue<>();

    public void addNum(int num) {
        maxHeap.offer(num);                       // Step 1: always offer to lower half
        minHeap.offer(maxHeap.poll());            // Step 2: balance — max of lower → upper
        // Step 3: if upper half is bigger, pull one back to lower half
        if (minHeap.size() > maxHeap.size()) {
            maxHeap.offer(minHeap.poll());
        }
        // Invariant: maxHeap.size() >= minHeap.size() (by at most 1)
    }

    public double findMedian() {
        if (maxHeap.size() > minHeap.size()) {
            return maxHeap.peek();                 // odd count — median is middle element
        }
        return (maxHeap.peek() + minHeap.peek()) / 2.0;  // even count — average of two middles
    }
}
```

**Why offer to maxHeap first then re-balance?** It handles the case where `num` belongs in the upper half — by passing the max of lower to upper, you guarantee the partition is always valid regardless of insertion order.

**Time**: addNum — O(log n) | findMedian — O(1)
**Space**: O(n)

---

### 6. Task Scheduler

**Approach**: Count task frequencies. Use a max-heap to always schedule the most frequent task. If no task is available during a cool-down, insert idle cycles. Alternatively, use the math formula (covered below).

```java
public int leastInterval(char[] tasks, int n) {
    int[] freq = new int[26];
    for (char c : tasks) freq[c - 'A']++;

    // Max-heap on frequency
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    for (int f : freq) if (f > 0) maxHeap.offer(f);

    int time = 0;
    while (!maxHeap.isEmpty()) {
        List<Integer> temp = new ArrayList<>();
        // Process up to n+1 tasks per cycle (one cycle = n+1 slots)
        for (int i = 0; i <= n; i++) {
            if (!maxHeap.isEmpty()) {
                int f = maxHeap.poll();
                if (f > 1) temp.add(f - 1);  // still has remaining executions
            }
            time++;
            if (maxHeap.isEmpty() && temp.isEmpty()) break; // done early
        }
        for (int f : temp) maxHeap.offer(f); // put remaining tasks back
    }
    return time;
}

// MATH formula (O(n), O(1) extra) — know this for follow-ups
// time = max((maxFreq - 1) * (n + 1) + countOfMaxFreq, tasks.length)
public int leastInterval_Math(char[] tasks, int n) {
    int[] freq = new int[26];
    for (char c : tasks) freq[c - 'A']++;
    Arrays.sort(freq);
    int maxFreq = freq[25];
    int countOfMaxFreq = 0;
    for (int f : freq) if (f == maxFreq) countOfMaxFreq++;
    return Math.max(tasks.length, (maxFreq - 1) * (n + 1) + countOfMaxFreq);
}
```

**Time**: O(n log 26) = O(n) — heap has at most 26 distinct tasks
**Space**: O(26) = O(1)

---

### 7. Sliding Window Maximum

**Approach**: Use a **monotonic deque** (not a heap). The deque stores indices in decreasing order of their values. For each new element, pop from the back while the back element is smaller — it can never be the window maximum. Pop from the front when that index falls outside the window.

```java
public int[] maxSlidingWindow(int[] nums, int k) {
    int n = nums.length;
    int[] result = new int[n - k + 1];
    // Deque stores INDICES; front = index of current window maximum
    Deque<Integer> deque = new ArrayDeque<>();

    for (int i = 0; i < n; i++) {
        // 1. Remove indices that are outside the window [i-k+1, i]
        while (!deque.isEmpty() && deque.peekFirst() < i - k + 1) {
            deque.pollFirst();
        }
        // 2. Maintain decreasing order: remove smaller elements from back
        //    They can never be the maximum while nums[i] is in the window
        while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i]) {
            deque.pollLast();
        }
        deque.offerLast(i);

        // 3. Window is full — record the maximum (front of deque)
        if (i >= k - 1) {
            result[i - k + 1] = nums[deque.peekFirst()];
        }
    }
    return result;
}
```

**Why not a max-heap?** A heap gives O(n log n) and cannot efficiently remove elements that leave the window. The deque gives O(n) by keeping only "useful" candidates.

**Time**: O(n) — each element enters and leaves the deque at most once
**Space**: O(k) — deque holds at most k indices

---

### 8. Reorganize String

**Approach**: Count character frequencies. Greedily place the most frequent character that is different from the previous one. Use a max-heap to always access the current most-frequent character.

```java
public String reorganizeString(String s) {
    int[] freq = new int[26];
    for (char c : s) freq[c - 'A']++;

    // Max-heap: [frequency, character_as_int]
    PriorityQueue<int[]> maxHeap = new PriorityQueue<>((a, b) -> b[0] - a[0]);
    for (int i = 0; i < 26; i++) {
        if (freq[i] > 0) maxHeap.offer(new int[]{freq[i], i});
    }

    StringBuilder sb = new StringBuilder();
    while (maxHeap.size() >= 2) {
        int[] first  = maxHeap.poll();  // most frequent
        int[] second = maxHeap.poll();  // second most frequent
        sb.append((char)('A' + first[1]));
        sb.append((char)('A' + second[1]));
        if (--first[0]  > 0) maxHeap.offer(first);
        if (--second[0] > 0) maxHeap.offer(second);
    }
    // At most one character remains
    if (!maxHeap.isEmpty()) {
        int[] last = maxHeap.poll();
        if (last[0] > 1) return "";      // impossible: same char would be adjacent
        sb.append((char)('A' + last[1]));
    }
    return sb.toString();
}
```

**Time**: O(n log 26) = O(n)
**Space**: O(26) = O(1) for the heap; O(n) for the output string

---

## Common Mistakes

1. **Min-heap vs max-heap confusion for "top K largest"**. To keep the K *largest* elements you need a *min*-heap of size K so you can evict the *smallest* intruder. Candidates flip this constantly — the heap type is always the *opposite* of what you are keeping.

2. **Integer overflow in comparators**. `(a, b) -> a - b` is unsafe for large negatives (e.g., `Integer.MIN_VALUE - 1` wraps to positive). Use `Integer.compare(a, b)` or `(a, b) -> a < b ? -1 : a > b ? 1 : 0` for safety. This is a silent bug that passes most tests but fails on edge-case inputs.

3. **Offering `null` to `PriorityQueue`**. `PriorityQueue` throws `NullPointerException` on `offer(null)`. Always null-check node references before offering (critical for Merge K Sorted Lists).

4. **Forgetting `2.0` in the median formula**. `(maxHeap.peek() + minHeap.peek()) / 2` is integer division. The result must be `/ 2.0` or cast one operand: `(double)(a + b) / 2`.

5. **Using a heap for sliding window maximum**. A max-heap approach is O(n log n) and requires lazy deletion (checking if polled element is outside the window). The canonical O(n) solution uses a monotonic deque — interviewers know this and will push you there if you start with a heap.

---

## Interview Tips

**What interviewers probe:**

- **Declare the heap correctly on first try.** They watch whether you reach for `Collections.reverseOrder()` or a lambda without hesitation. Practice the four declarations (min, max, custom asc, custom desc) until they are automatic.
- **Explain the size-K invariant.** Be ready to say: "I use a min-heap because I want to evict the smallest element, keeping only the K largest." One sentence justification, not vague hand-waving.
- **Two-heap balance logic.** Draw the two heaps and the invariant (|maxHeap| - |minHeap| ≤ 1) on the whiteboard before writing code. Interviewers will ask "what happens if they become unbalanced?"

**Common follow-up questions:**

- *Kth Largest*: "What if the array is too large to fit in memory?" → External sort or reservoir sampling.
- *Kth Largest*: "Can you do better than O(n log k)?" → QuickSelect is O(n) average; introduce the tradeoff.
- *Median from Stream*: "What if numbers can be deleted?" → Augment with lazy deletion flags or use an order-statistic tree.
- *Task Scheduler*: "Prove the math formula is equivalent to the greedy heap." → Be ready to explain the (maxFreq - 1) * (n + 1) + countOfMaxFreq derivation.
- *Merge K Lists*: "What is the space complexity?" → O(k) for the heap, O(1) extra per node. Distinguish heap space from output space.
- *Sliding Window Max*: "What if k changes dynamically?" → Deque approach still works; heap approach needs lazy deletion and becomes messy.
