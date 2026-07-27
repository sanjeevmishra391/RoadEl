# Intervals

## What It Is

An interval problem involves a collection of `[start, end]` pairs. The fundamental operations are: detect overlap, merge overlapping ranges, count concurrently active intervals, and greedily remove or select intervals. Almost every interval problem reduces to one of three patterns: **Merge**, **Insert**, or **Schedule**.

---

## When To Use

| Trigger phrase | Pattern |
|---|---|
| "Merge overlapping intervals" | Sort by start + sweep |
| "Insert a new interval into a sorted list" | Find insertion point + merge |
| "Can one person attend all meetings?" | Sort + check adjacent overlap |
| "Minimum meeting rooms / minimum platforms" | Min-heap of end times |
| "Minimum removals to make non-overlapping" | Greedy: keep interval ending earliest |
| "Partition a string into as many parts as possible" | Last-occurrence map + greedy window |
| "Circular route / gas station" | Total sum check + greedy start tracking |

---

## The Fundamental Interval Operation

```
Sort by start time.
Two intervals [a.start, a.end] and [b.start, b.end] (a.start <= b.start) OVERLAP when:
    a.end >= b.start

They do NOT overlap when:
    a.end < b.start
```

**Three core sort strategies:**

```java
// Sort by start time (Merge Intervals, Meeting Rooms II)
Arrays.sort(intervals, (a, b) -> a[0] - b[0]);

// Sort by end time (Non-Overlapping, Meeting Rooms I)
Arrays.sort(intervals, (a, b) -> a[1] - b[1]);

// Sort by start, break ties by end descending (Insert Interval is handled positionally)
Arrays.sort(intervals, (a, b) -> a[0] != b[0] ? a[0] - b[0] : b[1] - a[1]);
```

---

## Key Problems

### 1. Merge Intervals

**Approach**: Sort by start. Sweep: extend the current interval's end when the next interval overlaps; otherwise commit current and move forward.

```java
// Reference: MergeIntervals.java
public int[][] merge(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);   // sort by start
    List<int[]> result = new ArrayList<>();

    for (int i = 0; i < intervals.length; ) {
        int j = i + 1;
        // Absorb all overlapping intervals into intervals[i]
        while (j < intervals.length && intervals[i][1] >= intervals[j][0]) {
            intervals[i][1] = Math.max(intervals[i][1], intervals[j][1]);
            j++;
        }
        result.add(new int[]{intervals[i][0], intervals[i][1]});
        i = j;  // jump past all absorbed intervals
    }

    return result.toArray(new int[result.size()][]);
}
```

**Key insight**: After sorting by start, you only need to compare the current interval's *end* with the next interval's *start*. When merging, take `Math.max` of ends — never assume the absorbed interval ends before the current one (e.g., `[1,10]` absorbs `[2,3]`).

**Time**: O(n log n) — dominated by sort
**Space**: O(n) — result list (O(log n) if in-place sort is counted as O(1) extra)

---

### 2. Insert Interval

**Approach**: Three-phase sweep on the already-sorted array: (1) add all intervals that end before the new one starts, (2) merge all overlapping intervals into one, (3) add all remaining intervals.

```java
// Reference: InsertIntervals.java
public int[][] insert(int[][] intervals, int[] newInterval) {
    List<int[]> result = new ArrayList<>();
    int i = 0, n = intervals.length;

    // Phase 1: intervals that end before newInterval starts — no overlap
    while (i < n && intervals[i][1] < newInterval[0]) {
        result.add(intervals[i++]);
    }

    // Phase 2: merge all overlapping intervals with newInterval
    while (i < n && intervals[i][0] <= newInterval[1]) {
        newInterval[0] = Math.min(newInterval[0], intervals[i][0]);
        newInterval[1] = Math.max(newInterval[1], intervals[i][1]);
        i++;
    }
    result.add(newInterval);  // add the (possibly merged) new interval

    // Phase 3: remaining intervals that start after newInterval ends
    while (i < n) {
        result.add(intervals[i++]);
    }

    return result.toArray(new int[result.size()][]);
}
```

**Overlap condition for insertion**: `intervals[i][0] <= newInterval[1]` — the next interval starts before or at the new interval's end (they overlap or touch).

**Time**: O(n) — single pass (input is already sorted)
**Space**: O(n) — output list

---

### 3. Meeting Rooms I — Can One Person Attend All?

**Approach**: Sort by start time. If any adjacent pair overlaps (previous end > current start), return false.

```java
// Reference: MeetingRooms.java
public boolean canAttendMeetings(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);  // sort by start

    for (int i = 1; i < intervals.length; i++) {
        // If previous meeting ends AFTER current starts → conflict
        if (intervals[i - 1][1] > intervals[i][0]) {
            return false;
        }
    }
    return true;
}
```

**Note**: Meetings that end and start at the same time (e.g., `[0,8]` and `[8,10]`) do NOT overlap — the condition is strict `>`, not `>=`.

**Time**: O(n log n)
**Space**: O(1) extra (O(log n) sort stack)

---

### 4. Meeting Rooms II — Minimum Rooms Needed

**Approach**: Sort by start. Use a min-heap of *end times* of currently active meetings. For each new meeting, if the earliest-ending room has finished (heap top ≤ current start), reuse it (pop and push new end). Otherwise, allocate a new room (just push new end). Heap size = number of rooms in use = answer.

```java
public int minMeetingRooms(int[][] intervals) {
    if (intervals.length == 0) return 0;
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);  // sort by start

    // Min-heap of END times — top = earliest ending active meeting
    PriorityQueue<Integer> endTimes = new PriorityQueue<>();

    for (int[] interval : intervals) {
        if (!endTimes.isEmpty() && endTimes.peek() <= interval[0]) {
            // Reuse the room that ended earliest
            endTimes.poll();
        }
        endTimes.offer(interval[1]);  // assign this meeting to a room (record its end)
    }

    return endTimes.size();  // rooms in use = total rooms allocated
}
```

**Why min-heap of ends?** We want to check whether *any* room is free by the time the next meeting starts. The room that frees up first (minimum end time) is the only one that could possibly be reused.

**Alternative**: Sort starts and ends separately; use two pointers — but the heap approach is more intuitive and generalizes better.

**Time**: O(n log n) — sort + heap ops
**Space**: O(n) — heap in worst case holds all meetings

---

### 5. Non-Overlapping Intervals

**Approach**: Greedy — to maximize the number of intervals we can *keep* (equivalently, minimize removals), always keep the interval that ends earliest. Sort by end time; greedily advance the end pointer, count overlaps.

```java
// Reference: NonOverlappingIntervals.java
public int eraseOverlapIntervals(int[][] intervals) {
    if (intervals.length == 0) return 0;
    Arrays.sort(intervals, (a, b) -> a[1] - b[1]);  // sort by END time

    int removals = 0;
    int prevEnd = intervals[0][1];  // end of the last interval we KEPT

    for (int i = 1; i < intervals.length; i++) {
        if (intervals[i][0] < prevEnd) {
            // Overlap — remove current interval (it ends later because of sort)
            removals++;
            // prevEnd stays the same (we keep the earlier-ending one)
        } else {
            // No overlap — keep this interval
            prevEnd = intervals[i][1];
        }
    }
    return removals;
}
```

**Key insight**: Sorting by *end* time and always keeping the earlier-ending interval is the same greedy strategy as Activity Selection. You never regret keeping the one that ends first.

**Time**: O(n log n)
**Space**: O(1) extra

---

### 6. Partition Labels

**Approach**: Precompute the last occurrence index of each character. Sweep left to right, extending the current partition's end whenever a character's last occurrence is beyond the current boundary. Close the partition when `i == partitionEnd`.

```java
// Reference: PartitionLabels.java (alternative clean implementation)
public List<Integer> partitionLabels(String s) {
    // Step 1: last occurrence of each character
    int[] last = new int[26];
    for (int i = 0; i < s.length(); i++) {
        last[s.charAt(i) - 'a'] = i;
    }

    List<Integer> result = new ArrayList<>();
    int start = 0, end = 0;

    // Step 2: sweep and expand current partition
    for (int i = 0; i < s.length(); i++) {
        end = Math.max(end, last[s.charAt(i) - 'a']);  // extend if needed
        if (i == end) {                                  // partition is complete
            result.add(end - start + 1);
            start = end + 1;                             // next partition starts here
        }
    }
    return result;
}
```

**Why this is an interval problem**: Each character defines an implicit interval `[first_occurrence, last_occurrence]`. You are merging these character-intervals into non-overlapping label partitions — identical in structure to Merge Intervals.

**Time**: O(n)
**Space**: O(1) (26-char array is constant)

---

### 7. Gas Station

**Approach**: Two observations — (1) if `sum(gas) < sum(cost)`, no solution exists; (2) if we fail at station `i`, no station between `start` and `i` can be the answer (their prefix sums are already worse). Reset `start = i + 1` and reset `surplus = 0`.

```java
// Reference: GasStation.java — canCompleteCircuit2 is the O(n) solution
public int canCompleteCircuit(int[] gas, int[] cost) {
    int totalSum = 0;   // tracks whether a solution exists at all
    int surplus  = 0;   // running fuel surplus from current candidate start
    int start    = 0;   // candidate starting station

    for (int i = 0; i < gas.length; i++) {
        int net = gas[i] - cost[i];
        totalSum += net;
        surplus  += net;

        if (surplus < 0) {
            // Can't reach station i+1 from current start — discard all stations up to i
            surplus = 0;
            start = i + 1;
        }
    }

    // If total gas < total cost, no solution possible
    return totalSum >= 0 ? start : -1;
}

// BRUTE FORCE for reference — O(n²) — never use in interviews
public int canCompleteCircuit_Brute(int[] gas, int[] cost) {
    int n = gas.length;
    for (int i = 0; i < n; i++) {
        int fuel = 0, stops = 0, j = i;
        while (stops < n) {
            fuel += gas[j % n] - cost[j % n];
            if (fuel < 0) break;
            stops++;
            j++;
        }
        if (stops == n && fuel >= 0) return i;
    }
    return -1;
}
```

**Why the greedy proof works**: If `surplus` goes negative at index `i`, then starting at any station `s` in `[start, i]` also leads to a negative balance before or at `i` (because the sub-segments from `s` are subsets of the segment starting at `start`, which already failed). So we safely jump to `i + 1`.

**Time**: O(n)
**Space**: O(1)

---

## Common Mistakes

1. **Using `>=` vs `>` for the overlap check**. Intervals `[0,8]` and `[8,10]` touch but do not overlap (one meeting ends as another begins). The overlap condition is `a.end > b.start` (strict). For Merge Intervals the condition to *extend* is `intervals[i][1] >= intervals[j][0]` (merge intervals that touch). Confusing these two flavors fails edge cases like `[[1,4],[4,5]]`.

2. **Sorting by the wrong field**. Meeting Rooms I/II and Merge Intervals need sort by *start*. Non-Overlapping Intervals needs sort by *end*. Confusing them gives wrong answers that still pass some test cases.

3. **Forgetting `Math.max` when merging ends**. When absorbing `[2,3]` into `[1,10]`, the merged end is `max(10, 3) = 10`. Writing `intervals[i][1] = intervals[j][1]` blindly overwrites a larger end with a smaller one. This is the most common merge bug.

4. **Off-by-one in Insert Interval phases**. The boundary between phase 1 and phase 2 is `intervals[i][1] < newInterval[0]` (strictly less than — ends before new one starts). Phase 2 boundary is `intervals[i][0] <= newInterval[1]` (starts at or before new one ends). Swapping these conditions produces subtle insertion bugs.

5. **Gas Station: checking `totalSum` only at the end**. The greedy approach requires a single pass that updates both `surplus` (local) and `totalSum` (global). A common mistake is to check `totalSum >= 0` before the loop and short-circuit incorrectly, or to forget to add the net gain to `totalSum` every iteration.

---

## Interview Tips

**What interviewers probe:**

- **The overlap condition stated precisely.** Say: "Two intervals overlap when the earlier-ending one's end is strictly greater than the later-starting one's start." Interviewers check whether you handle touch-points correctly.
- **Why sort by end for Non-Overlapping but by start for Merge.** Have a one-sentence answer ready: "For greedy activity selection, keeping the interval that ends earliest maximizes future choices, so sort by end. For merging, we need to process in chronological order, so sort by start."
- **Meeting Rooms II without a heap.** Interviewers sometimes push for the two-pointer approach on sorted starts and ends arrays — O(n log n) same complexity, O(n) space but avoids heap constant factors.

**Common follow-up questions:**

- *Merge Intervals*: "What if intervals are already sorted?" → Skip the sort; O(n) linear pass.
- *Meeting Rooms II*: "What if you want to return the actual room assignments?" → Store `(endTime, roomId)` in the heap and maintain a room counter.
- *Non-Overlapping*: "Relate this to Longest Non-Overlapping Subsequence." → The number of intervals kept = LIS by end time; removals = n - kept.
- *Gas Station*: "Why is there always at most one valid answer?" → Because the total gas is exactly equal to total cost in a feasible circuit; if one starting point works, the uniqueness follows from the uniqueness of the "net gain tipping point."
- *Partition Labels*: "What if characters can appear in multiple partitions?" → Reframe as Merge Intervals on character ranges.
