# Google L5 Coding Prep

> Loop: 1 phone screen (45 min) → 4 virtual onsite rounds (3 coding + 1 Googleyness & Leadership).
> Bar: **optimal solution, no hints, under 45 min**, real Java (no pseudocode), dry-run and tested, explained out loud.
> Recruiter warning: L5s struggle most with **DP, Graphs (BFS/DFS), Trees, Recursion**.

This plan covers the gaps between what's already in `DSA/` and what the recruiter email asks for.
It assumes 4 weeks. Shrink weeks 1–3 if the screen is sooner, but always keep the mocks.

---

## Reply to the recruiter (today)
- [ ] Send updated CV
- [ ] Preferred language: **Java**

---

## Gap analysis (repo vs. Google list)

| Topic | In repo | Gap |
|---|---|---|
| DP | Strong (knapsack/LCS/MCM families) | LIS O(n log n), DP on grids/graphs, interval DP (burst balloons), bitmask DP, string DP like Word Break |
| Graphs | Representations, traversal, topo, Dijkstra, MST, SCC, DSU | **Grid BFS/flood fill, multi-source BFS, BFS with state**, implicit graphs (Word Ladder), A* |
| Trees | Traversals, BST, Morris | **Almost no problems**: LCA, path sums, serialize, construct, distance-K |
| Recursion/Backtracking | NQueens, Sudoku, Permutations | Subsets/combinations with pruning, Word Search II |
| Sliding window | None | Add the whole pattern |
| Heap | None (only Top K) | Two heaps, k-way merge, scheduling |
| Monotonic stack | 2 problems | Trapping rain water, sum of subarray minimums |
| Binary search | Basic + answer-space | Rotated arrays, median of two sorted arrays |
| Hash table internals | None | **Implement from arrays** (asked explicitly) |
| Balanced BST | None | AVL rotations, or red-black properties |
| Segment tree / Fenwick | None | One implementation of each |
| Math | None | Combinatorics, probability, modular arithmetic |
| NP-complete | Knapsack only | Recognise problems in disguise |

---

## Daily routine (non-negotiable)

1. **2 problems a day, timed**: 20 min for a medium, 40 min for a hard. Write them in a **plain editor or Google Doc**, never an IDE.
2. Follow the interview framework on every problem, out loud even when alone:
   1. Clarify inputs, constraints, and edge cases. Ask about n's size.
   2. State the brute force in **one sentence** with its complexity. Don't code it.
   3. Find the optimal approach and state its time and space complexity **before** coding.
   4. Code it cleanly. Use helper methods and meaningful names.
   5. **Dry-run** one normal case and one edge case line by line.
   6. Only then compile it. Log every bug the compiler or tests caught in `Bugs.md`. Those are the mistakes you'll repeat in the interview.
3. Add `// Time: O(..) Space: O(..)` to every file.
4. If you haven't found the approach in 20 minutes, read the solution. Then **re-solve from scratch 3 days later** without looking.

---

## Week 1 — Graphs, Trees, Recursion (the L5 weak spots)

**Graphs / BFS / DFS**
- [ ] 200 Number of Islands (DFS + BFS + DSU, all three)
- [ ] 994 Rotting Oranges (multi-source BFS)
- [ ] 417 Pacific Atlantic Water Flow
- [ ] 133 Clone Graph
- [ ] 210 Course Schedule II (Kahn's algorithm + DFS cycle detection)
- [ ] 127 Word Ladder (implicit graph)
- [ ] 269 Alien Dictionary
- [ ] 743 Network Delay Time (Dijkstra with PriorityQueue)
- [ ] 1631 Path With Minimum Effort (Dijkstra on a grid)
- [ ] 787 Cheapest Flights Within K Stops (BFS/Bellman-Ford with state)
- [ ] 1293 Shortest Path in a Grid with Obstacles Elimination (BFS with state, Google favourite)
- [ ] 721 Accounts Merge (DSU)
- [ ] 778 Swim in Rising Water

**Trees**
- [ ] 98 Validate BST
- [ ] 236 Lowest Common Ancestor
- [ ] 105 Construct Tree from Preorder + Inorder
- [ ] 124 Binary Tree Maximum Path Sum
- [ ] 297 Serialize / Deserialize Binary Tree
- [ ] 863 All Nodes Distance K
- [ ] 987 Vertical Order Traversal
- [ ] 199 Right Side View
- [ ] 222 Count Complete Tree Nodes (O(log² n))
- [ ] 437 Path Sum III (prefix sum on a tree)

**Theory**
- [ ] Graph representations (pointers, matrix, adjacency list): pros and cons, plus time and space for BFS/DFS on each
- [ ] Dijkstra vs Bellman-Ford vs 0-1 BFS vs A* (heuristic = admissible lower bound)
- [ ] N-ary trees and trie (`Trie/` exists, so re-implement it from memory)

---

## Week 2 — Sliding Window, Heaps, Stack, Binary Search, Hashing

- [ ] 3 Longest Substring Without Repeating Characters
- [ ] 424 Longest Repeating Character Replacement
- [ ] 239 Sliding Window Maximum (monotonic deque)
- [ ] 992 Subarrays with K Different Integers (atMost(k) − atMost(k−1))
- [ ] 295 Find Median from Data Stream (two heaps)
- [ ] 621 Task Scheduler
- [ ] 502 IPO
- [ ] 42 Trapping Rain Water (two pointers + stack)
- [ ] 907 Sum of Subarray Minimums
- [ ] 33 Search in Rotated Sorted Array
- [ ] 410 Split Array Largest Sum
- [ ] 4 Median of Two Sorted Arrays
- [ ] 981 Time Based Key-Value Store

**Implement from scratch (repo files)**
- [ ] `HashTable/MyHashMap.java`: array buckets with chaining, `hash & (cap-1)`, resize at load factor 0.75, generic `K, V`. Then a version using open addressing with linear probing and tombstones. Must fit in about 30 minutes.
- [ ] `Heap/MinHeap.java`: array-based, siftUp/siftDown, heapify in O(n)
- [ ] Re-write merge sort and quicksort from memory (randomised pivot). Know why merge sort suits linked lists and external sorting, and that it's stable.

---

## Week 3 — DP (Google flavour) + Advanced

Your knapsack/LCS foundation is solid. Google DP is usually **less templated**, so practise finding the state.

- [ ] 300 Longest Increasing Subsequence (O(n²), then O(n log n) patience sort)
- [ ] 139 Word Break
- [ ] 91 Decode Ways
- [ ] 221 Maximal Square
- [ ] 329 Longest Increasing Path in a Matrix (DFS + memo)
- [ ] 312 Burst Balloons (interval DP)
- [ ] 97 Interleaving String
- [ ] 309 Best Time to Buy/Sell with Cooldown (state machine DP)
- [ ] 1235 Maximum Profit in Job Scheduling (DP + binary search)
- [ ] 698 Partition to K Equal Sum Subsets (bitmask)
- [ ] 847 Shortest Path Visiting All Nodes (BFS + bitmask)
- [ ] 212 Word Search II (trie + backtracking)
- [ ] 307 Range Sum Query Mutable (segment tree **and** Fenwick tree)
- [ ] 315 Count of Smaller Numbers After Self (Fenwick or merge sort)

**Theory**
- [ ] AVL: balance factor and the 4 rotations (LL, RR, LR, RL). Red-black: 5 properties and why height is ≤ 2·log(n+1). Java `TreeMap` is red-black.
- [ ] NP-complete: definition (in NP and NP-hard), and recognising disguises:
  TSP → "visit all cities", which becomes bitmask DP for n ≤ 20 · Knapsack/Subset Sum → "pick items under a budget", pseudo-polynomial DP · Graph colouring, Vertex cover, Clique, SAT, Hamiltonian path.
  When you spot one, say so, then offer exponential DP for small n or a greedy approximation.

---

## Week 4 — Math, OS, Mocks, Googleyness

**Math (Discrete Math 101)**
- [ ] nCr: Pascal's triangle, modular inverse (Fermat), overflow-safe computation
- [ ] Permutations with repetition, stars and bars, inclusion–exclusion, Catalan (`Dynamic/Catalan.java`)
- [ ] Probability: expected value, linearity of expectation, conditional/Bayes, reservoir sampling (382), Fisher–Yates shuffle, rand7 from rand5 (470)
- [ ] GCD/LCM, fast exponentiation, sieve

**OS / Concurrency** (mostly covered in `Java/Concurrency/`)
- [ ] Process vs thread resources, context switch steps (save registers/PC, TLB flush for processes)
- [ ] Mutex vs semaphore vs monitor, deadlock (4 Coffman conditions and lock ordering) vs livelock vs starvation
- [ ] Scheduling: FCFS, SJF, round robin, priority, MLFQ

**Mocks**: at least **6 timed mocks** this week (pramp.com, interviewing.io, a friend, or Claude). Use a Google Doc, a camera, and talk the whole time.

**Googleyness & Leadership**: prepare 6–8 STAR stories in `Behavioural/STAR.md` covering ambiguity, disagreement with a teammate or manager, failure, mentoring, a decision with incomplete data, putting the user first, and handling feedback.

---

## Java without an IDE — know these cold

```java
Map<Integer, List<Integer>> g = new HashMap<>();
g.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
map.merge(key, 1, Integer::sum);
map.getOrDefault(key, 0);

PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0])); // not a[0]-b[0] (overflow)
Deque<Integer> stack = new ArrayDeque<>();   // push/pop/peek
Deque<int[]> q = new ArrayDeque<>();         // offer/poll/peek
TreeMap<Integer, Integer> tm;                // floorKey, ceilingKey, firstKey, pollFirstEntry
Arrays.sort(arr, (a, b) -> a[0] - b[0]);     // only on Integer[] / int[][]
Arrays.fill(dp, -1);  int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
String.valueOf(chars); sb.reverse(); s.toCharArray(); Character.isLetterOrDigit(c);
```

Pitfalls to look for during the dry run: `Integer == Integer` above 127, `int` overflow in `mid` and sums (use `long`), off-by-one errors in window bounds, modifying a collection while iterating it, and forgetting `visited` before enqueueing in BFS.

---

## Before each interview
- [ ] Prepare 2 questions to ask the interviewer
- [ ] Check camera and mic, and have a blank Google Doc open
- [ ] Remember: clarify → brute force in 1 line → optimal with complexity → code → dry run → edge cases → optimise
