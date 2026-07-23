# How to Prepare for DSA Interviews

> Follow this guide exactly for 4 weeks and pattern recognition becomes instinctive.

---

## The Mindset Shift

**Wrong approach:** "I will solve 200 problems and memorize solutions."
**Right approach:** "I will learn 15 patterns and recognize which one applies within 2 minutes."

Interviewers don't give the same problem twice. They give a new problem in a familiar pattern. If you've memorized solutions, you're stuck. If you know patterns, you adapt.

---

## Master Pattern Trigger Table

Consult this before every problem. Identify the pattern **before** writing any code.

| Pattern | Trigger Words / Conditions | First Question to Ask |
|---|---|---|
| **Sliding Window** | subarray/substring, contiguous, window of size k | Is the input contiguous? Is there a window condition? |
| **Two Pointers** | sorted array, pair/triplet sum, palindrome, in-place | Is the array sorted? Do I need a pair? |
| **Fast & Slow Pointers** | cycle detection, middle of list, nth from end | Is there a cycle? Do I need two speeds? |
| **Binary Search** | sorted array, "find minimum X that satisfies Y", feasibility | Can I binary search on the answer space? |
| **BFS** | shortest path, level order, minimum steps | Is this shortest path in unweighted graph? |
| **DFS / Backtracking** | all combinations, all permutations, explore all paths | Do I need to enumerate ALL possibilities? |
| **Dynamic Programming** | optimal/count/possible, overlapping subproblems | Can a decision at step i affect step i+1? |
| **Monotonic Stack** | next greater/smaller, histogram, temperatures | Do I need the nearest larger/smaller element? |
| **Heap / Priority Queue** | kth largest/smallest, top K, median stream | Do I need the kth element efficiently? |
| **Merge Intervals** | overlapping intervals, meeting rooms, calendar | Are there intervals that might overlap? |
| **Topological Sort** | dependency ordering, course schedule, task ordering | Is there a directed dependency between items? |
| **Union-Find** | connected components, cycle in undirected, network | Are there grouping / connectivity questions? |
| **Trie** | prefix search, autocomplete, word dictionary | Do I need prefix-based search? |
| **Prefix Sum** | range sum, subarray sum equals k | Do I need sum of a subarray quickly? |

---

## Complexity Quick Reference

Use this to verify your approach is fast enough **before** coding.

| Complexity | Max Input Size (~1 sec) | Examples |
|---|---|---|
| O(1) | Any | HashMap lookup |
| O(log n) | ~10^18 | Binary search |
| O(n) | ~10^8 | Sliding window, two pointers |
| O(n log n) | ~10^6 | Merge sort, Dijkstra |
| O(n²) | ~10^4 | Nested loops, bubble sort |
| O(n³) | ~500 | Floyd-Warshall, some DP |
| O(2^n) | ~20 | Backtracking, subsets |
| O(n!) | ~11 | Permutations |

> If `n ≤ 20`, O(2^n) is fine. If `n ≤ 10^5`, you need O(n log n) or better.

---

## The Daily Problem-Solving Process (5 Steps)

### Step 1: Read (5 min, no code)

**Read 1 — What is the exact output?**
Array? Integer? Boolean? Path? Be precise.

**Read 2 — What are the constraints?**
- Array size? → determines acceptable complexity
- Sorted? → Two pointers or binary search
- Duplicates? → need dedup logic
- Negative numbers? → Kadane's or Bellman-Ford
- Connected / graph structure? → BFS/DFS/Union-Find

**Read 3 — Map to a pattern:**
```
Is there a contiguous subarray/substring?     → Sliding Window
Is the array sorted and I need pairs?         → Two Pointers
Do I need ALL combinations/permutations?      → Backtracking
Is this a shortest path problem?              → BFS
Does choosing an element affect future steps? → DP
Do I need the kth largest/smallest?           → Heap
Are there dependencies between tasks?         → Topological Sort
Is there a yes/no feasibility on a range?     → Binary Search on Answer
```

### Step 2: Plan (3 min, talk out loud)
- State your approach in plain English before writing any code
- Write time and space complexity **before** coding — confirm it fits the constraints
- Think about edge cases: empty input, single element, all duplicates, all same

### Step 3: Code (15-20 min)
- Write clean code with meaningful variable names
- Handle edge cases explicitly
- Get it correct first — optimize after

### Step 4: Test (5 min)
- Trace through your code manually with a small example (3-4 elements)
- Test one edge case (empty array, single element)
- Verify the output matches expected

### Step 5: Pattern Debrief (2 min) — THE MOST IMPORTANT STEP
```
Problem:
Pattern used:
Why this pattern (trigger):
Key insight / trick:
Time: O(?)   Space: O(?)
Gotcha: [what would have tripped you up]
Variations this pattern handles:
```
**Skip this step and the problem won't transfer to future problems.**
Keep these entries in a personal log — after 30 problems, patterns become instinctive.

---

## How to Handle "I've Never Seen This Before"

Every problem feels new. The question is whether the **structure** is familiar. Ask in order:

```
1. Is there a contiguous subarray/substring?   → Sliding Window
2. Is the array sorted and I need pairs?        → Two Pointers
3. Do I need all combinations/permutations?     → Backtracking
4. Is this a graph/tree traversal?              → BFS or DFS
5. Do I need the kth largest or top K?          → Heap
6. Can I make a choice at each step?            → DP
7. Is there a yes/no feasibility on a range?    → Binary Search on Answer
```

If none fit → write brute force first, then look for optimisation.

---

## The 5-Minute Stuck Rule

If you haven't identified the pattern in 5 minutes:

1. **Restate** the problem in one sentence out loud
2. **Draw** a small example (4-5 elements) on paper
3. **Ask:** "What information do I need at each step?"
4. **Ask:** "What pattern does this example remind me of?"
5. Look at the **pattern category** (not the solution) in the trigger table above — try again

Only look at a solution after **25 minutes** of genuine attempt. Then:
- Read the approach explanation only (not the code)
- Close it
- Implement from scratch yourself
- Solve a similar problem the next day

---

## Complexity Analysis — Do This Every Time

Write this as a comment at the top of every solution before you start:
```java
// Time: O(n log n) — sorting + single pass
// Space: O(n) — auxiliary array for result
```

| Constraint (n) | Max acceptable complexity |
|---|---|
| n ≤ 20 | O(2^n) — backtracking/subsets ok |
| n ≤ 100 | O(n³) — triple nested loops ok |
| n ≤ 1,000 | O(n²) — nested loops ok |
| n ≤ 100,000 | O(n log n) — sorting, tree ops ok |
| n ≤ 10,000,000 | O(n) — single pass only |

---

## Weekly Problem Plan

### Week 1 — Foundation Patterns (Easy → Easy-Medium)
**Topics:** Arrays, Strings, Two Pointers, Sliding Window, Binary Search

| Day | Pattern | Problems to Solve |
|---|---|---|
| Mon | Two Pointers (opposite ends) | Two Sum II, Valid Palindrome, Container With Most Water |
| Tue | Two Pointers (fast & slow) | Remove Duplicates, Move Zeroes, Remove Element |
| Wed | Sliding Window (fixed) | Max Sum Subarray of Size K, Average of Subarrays |
| Thu | Sliding Window (variable) | Longest Substring Without Repeating, Fruits Into Baskets |
| Fri | Binary Search (basic) | Binary Search, First/Last Position in Sorted Array |
| Sat | Binary Search on answer | Koko Eating Bananas, Minimum Number of Days to Bloom |
| Sun | Review + write Pattern Debrief for each problem solved |

**Target: 14 problems by end of Week 1**

---

### Week 2 — Intermediate Patterns (Medium)
**Topics:** Trees, Graphs, Heaps, Stack/Queue

| Day | Pattern | Problems to Solve |
|---|---|---|
| Mon | Tree DFS (preorder/inorder/postorder) | Max Depth, Path Sum, Same Tree |
| Tue | Tree BFS (level order) | Level Order Traversal, Right Side View, Zigzag |
| Wed | Graph BFS | Rotting Oranges, 01 Matrix, Word Ladder |
| Thu | Graph DFS + Union-Find | Number of Islands, Connected Components |
| Fri | Monotonic Stack | Daily Temperatures, Next Greater Element |
| Sat | Heap / Priority Queue | Top K Frequent Elements, K Closest Points, Merge K Lists |
| Sun | Review + Mock (1 problem from each pattern, timed 25 min each) |

**Target: 28 problems cumulative by end of Week 2**

---

### Week 3 — Hard Patterns
**Topics:** DP, Backtracking, Advanced Graphs

| Day | Pattern | Problems to Solve |
|---|---|---|
| Mon | DP 1D | House Robber, Climbing Stairs, Jump Game |
| Tue | DP 2D | Unique Paths, Minimum Path Sum, LCS |
| Wed | DP Knapsack | 0/1 Knapsack, Coin Change, Target Sum |
| Thu | Backtracking | Subsets, Permutations, Combination Sum |
| Fri | Topological Sort | Course Schedule I & II, Alien Dictionary |
| Sat | Dijkstra + Advanced | Cheapest Flights K Stops, Path With Min Effort |
| Sun | Hard mock: 2 problems, 45 min each |

**Target: 42 problems cumulative by end of Week 3**

---

### Week 4 — Mixed Simulation
Each day: 2 random problems from different patterns. No pattern hints — identify the pattern yourself first.

**Target: 56 problems cumulative by end of Week 4**

---

## Mock Interview Protocol (Every Saturday)

1. Open a **new** problem you haven't seen before
2. Set a **45-minute** timer
3. No hints, no searching — follow the 5-step process above
4. When the timer ends, write down what you got right and what blocked you
5. Review the solution and fill in the Pattern Debrief

**Track each week:**
- Patterns solved without hints → **strong**
- Patterns that needed hints → **review next week**
- Patterns you couldn't identify → **focus area for next week**

---

## Anti-Patterns to Avoid

| Habit | Why It's Harmful | Fix |
|---|---|---|
| Looking at solutions after 5 min | Trains lookup, not recognition | Wait 25 min before looking |
| Solving random problems | No pattern muscle builds | Follow the weekly pattern clusters above |
| Skipping the Pattern Debrief | Problems don't transfer | 2 min debrief after every problem |
| Skipping the planning step | Code becomes messy, complexity wrong | Always plan + state complexity before coding |
| Re-solving the same easy problems | False confidence | Move to harder problems in the same pattern |
| Only reading solutions | Passive understanding, fails under pressure | Always implement from scratch after reading |

---

## Resources

| Resource | What For |
|---|---|
| [NeetCode.io](https://neetcode.io/practice) | Structured problem list by pattern — use this, not random LeetCode |
| [NeetCode YouTube](https://www.youtube.com/@NeetCode) | Video explanations when stuck — watch approach only, then implement yourself |
