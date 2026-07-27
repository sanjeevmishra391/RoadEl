# Backtracking

## What It Is

Backtracking is a systematic search that builds a solution incrementally and abandons ("backtracks") a partial solution as soon as it determines the partial solution cannot lead to a valid complete solution. It explores a decision tree: at each node you **choose** an option, **explore** with that choice, then **unchoose** (undo the choice) to try the next option.

---

## When To Use

| Trigger phrase | Pattern |
|---|---|
| "All permutations / arrangements" | Permutations archetype |
| "All subsets / power set" | Subsets archetype |
| "All combinations that sum to X" | Combination Sum archetype |
| "Find all valid configurations" (N-Queens, Sudoku) | Constraint-based search with pruning |
| "Partition a string into valid pieces" | Palindrome Partitioning / DFS on string |
| "Word search / path in a grid" | DFS with visited marking |
| "Minimum number of transactions to settle debt" | Backtracking on a reduced balance list |

**Key heuristic**: if the problem asks for *all* valid solutions (not just count or existence), backtracking is almost always the approach.

---

## The Backtracking Template

```java
void backtrack(/* state */, /* choices */, /* result */) {
    // Base case: reached a valid complete solution
    if (isSolution(state)) {
        result.add(new ArrayList<>(current));  // COPY — never add the mutable reference
        return;
    }

    for (each choice in choices) {
        if (!isValid(state, choice)) continue;  // pruning — skip invalid choices early

        // CHOOSE
        makeChoice(state, choice);

        // EXPLORE
        backtrack(/* updated state */, /* remaining choices */, result);

        // UNCHOOSE (undo exactly what makeChoice did)
        undoChoice(state, choice);
    }
}
```

**The three archetypes — what changes between them:**

| Archetype | Start index? | Reuse element? | Sort needed? |
|---|---|---|---|
| Subsets | `i` (each element once, move forward) | No | No (but yes for dedup) |
| Permutations | 0 (revisit all, track used[]) | No | No (but yes for dedup) |
| Combinations | `i` or `i+1` (no reuse or reuse) | Optional | Yes for sum pruning |

---

## Permutations vs Combinations vs Subsets

```
Input: [1, 2, 3]

SUBSETS (power set) — order doesn't matter, each element used at most once:
[], [1], [2], [3], [1,2], [1,3], [2,3], [1,2,3]  → 2^n solutions

PERMUTATIONS — order matters, each element used exactly once:
[1,2,3], [1,3,2], [2,1,3], [2,3,1], [3,1,2], [3,2,1]  → n! solutions

COMBINATIONS — order doesn't matter, pick exactly k elements:
k=2: [1,2], [1,3], [2,3]  → C(n,k) solutions
```

---

## Key Problems

### 1. Subsets (Power Set)

**Approach**: Two approaches — bitmask (iterative, clean) and backtracking (extensible to pruning). For the backtracking version, at each index we decide to include or skip the element.

```java
// APPROACH 1: Backtracking — O(n * 2^n) time, O(n) stack space
public List<List<Integer>> subsets(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    backtrack(nums, 0, new ArrayList<>(), result);
    return result;
}

private void backtrack(int[] nums, int start, List<Integer> current,
                        List<List<Integer>> result) {
    result.add(new ArrayList<>(current));  // every partial state is a valid subset

    for (int i = start; i < nums.length; i++) {
        current.add(nums[i]);                    // CHOOSE
        backtrack(nums, i + 1, current, result); // EXPLORE (i+1: no reuse)
        current.remove(current.size() - 1);      // UNCHOOSE
    }
}

// APPROACH 2: Bitmask — O(n * 2^n) time, O(1) extra space (excluding output)
public List<List<Integer>> subsets_Bitmask(int[] nums) {
    int n = nums.length;
    List<List<Integer>> result = new ArrayList<>();

    for (int mask = 0; mask < (1 << n); mask++) {  // 2^n subsets
        List<Integer> subset = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if ((mask >> i & 1) == 1) {  // bit i is set — include nums[i]
                subset.add(nums[i]);
            }
        }
        result.add(subset);
    }
    return result;
}
```

**Time**: O(n * 2^n) — 2^n subsets, each takes O(n) to copy
**Space**: O(n) recursion depth (bitmask: O(1) extra)

---

### 2. Permutations

**Approach**: Two approaches — swap-based (modifies input in place) and used-array (preserves input). Swap-based is cache-friendly and slightly cleaner. For duplicates, sort and skip same-value elements at the same recursion level.

```java
// APPROACH 1: Swap-based — O(n * n!) time, O(n) stack space
// Reference: Permutations.java
public List<List<Integer>> permute(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    backtrackSwap(nums, 0, result);
    return result;
}

private void backtrackSwap(int[] nums, int start, List<List<Integer>> result) {
    if (start == nums.length) {
        // Convert int[] to List<Integer> and copy
        List<Integer> perm = new ArrayList<>();
        for (int n : nums) perm.add(n);
        result.add(perm);
        return;
    }
    for (int i = start; i < nums.length; i++) {
        swap(nums, start, i);                     // CHOOSE: put nums[i] at position start
        backtrackSwap(nums, start + 1, result);   // EXPLORE
        swap(nums, start, i);                     // UNCHOOSE: restore original order
    }
}

private void swap(int[] a, int i, int j) {
    int tmp = a[i]; a[i] = a[j]; a[j] = tmp;
}

// APPROACH 2: used[] array — easier to extend to duplicates
public List<List<Integer>> permute_Used(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    boolean[] used = new boolean[nums.length];
    backtrackUsed(nums, used, new ArrayList<>(), result);
    return result;
}

private void backtrackUsed(int[] nums, boolean[] used,
                            List<Integer> current, List<List<Integer>> result) {
    if (current.size() == nums.length) {
        result.add(new ArrayList<>(current));
        return;
    }
    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;
        used[i] = true;
        current.add(nums[i]);
        backtrackUsed(nums, used, current, result);
        current.remove(current.size() - 1);
        used[i] = false;
    }
}
```

**Time**: O(n * n!) — n! permutations, each takes O(n) to copy
**Space**: O(n) for recursion stack

---

### 3. Combination Sum

**Approach**: Sort the array (enables pruning: if current element exceeds remaining target, stop). For combination sum *with reuse*, pass same index `i` in the recursive call. Without reuse, pass `i + 1`.

```java
// WITH REUSE (Combination Sum I) — same element can be used multiple times
public List<List<Integer>> combinationSum(int[] candidates, int target) {
    Arrays.sort(candidates);  // enables early termination
    List<List<Integer>> result = new ArrayList<>();
    backtrack(candidates, 0, target, new ArrayList<>(), result);
    return result;
}

private void backtrack(int[] candidates, int start, int remaining,
                        List<Integer> current, List<List<Integer>> result) {
    if (remaining == 0) {
        result.add(new ArrayList<>(current));
        return;
    }
    for (int i = start; i < candidates.length; i++) {
        if (candidates[i] > remaining) break;  // PRUNING: sorted, so rest are larger
        current.add(candidates[i]);
        backtrack(candidates, i, remaining - candidates[i], current, result); // i (reuse ok)
        current.remove(current.size() - 1);
    }
}

// WITHOUT REUSE, NO DUPLICATES (Combination Sum II) — each element used at most once
public List<List<Integer>> combinationSum2(int[] candidates, int target) {
    Arrays.sort(candidates);
    List<List<Integer>> result = new ArrayList<>();
    backtrack2(candidates, 0, target, new ArrayList<>(), result);
    return result;
}

private void backtrack2(int[] candidates, int start, int remaining,
                         List<Integer> current, List<List<Integer>> result) {
    if (remaining == 0) {
        result.add(new ArrayList<>(current));
        return;
    }
    for (int i = start; i < candidates.length; i++) {
        if (candidates[i] > remaining) break;
        // Skip duplicates at the same recursion level (same position in sorted array)
        if (i > start && candidates[i] == candidates[i - 1]) continue;
        current.add(candidates[i]);
        backtrack2(candidates, i + 1, remaining - candidates[i], current, result); // i+1: no reuse
        current.remove(current.size() - 1);
    }
}
```

**Time**: O(2^(target/min_candidate)) — exponential, bounded by branching factor and depth
**Space**: O(target / min_candidate) — maximum recursion depth

---

### 4. N-Queens

**Approach**: Place one queen per row. For each row, try all columns. Prune if the candidate cell is attacked by any previously placed queen (same column, diagonal, anti-diagonal). Use three boolean arrays for O(1) attack checks instead of scanning the board.

```java
// Reference: NQueens.java (this version uses O(1) attack checks instead of full scan)
public List<List<String>> solveNQueens(int n) {
    List<List<String>> result = new ArrayList<>();
    boolean[] cols     = new boolean[n];       // column i is under attack
    boolean[] diag     = new boolean[2 * n];   // row - col + n (avoid negatives)
    boolean[] antiDiag = new boolean[2 * n];   // row + col

    char[][] board = new char[n][n];
    for (char[] row : board) Arrays.fill(row, '.');

    backtrack(board, 0, cols, diag, antiDiag, result);
    return result;
}

private void backtrack(char[][] board, int row, boolean[] cols,
                        boolean[] diag, boolean[] antiDiag,
                        List<List<String>> result) {
    if (row == board.length) {
        List<String> solution = new ArrayList<>();
        for (char[] r : board) solution.add(new String(r));
        result.add(solution);
        return;
    }
    int n = board.length;
    for (int col = 0; col < n; col++) {
        int d  = row - col + n;  // diagonal index
        int ad = row + col;      // anti-diagonal index
        if (cols[col] || diag[d] || antiDiag[ad]) continue;  // PRUNING

        // CHOOSE
        board[row][col] = 'Q';
        cols[col] = diag[d] = antiDiag[ad] = true;

        // EXPLORE
        backtrack(board, row + 1, cols, diag, antiDiag, result);

        // UNCHOOSE
        board[row][col] = '.';
        cols[col] = diag[d] = antiDiag[ad] = false;
    }
}
```

**Diagonal key insight**: All cells on the same top-left→bottom-right diagonal share the same value of `row - col`. All cells on the same anti-diagonal share the same `row + col`. These are the O(1) attack indicators.

**The `isSafe` in NQueens.java scans all four directions — O(n) per check.** The boolean arrays above reduce each check to O(1), making the total faster in practice.

**Time**: O(n!) — n choices for row 0, n-1 for row 1, etc.
**Space**: O(n) — recursion depth + O(n) boolean arrays

---

### 5. Word Search

**Approach**: DFS from every cell. Mark the current cell as visited (modify in place) before recursing; restore it after returning. This avoids allocating a separate visited matrix.

```java
public boolean exist(char[][] board, String word) {
    int rows = board.length, cols = board[0].length;
    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            if (dfs(board, word, r, c, 0)) return true;
        }
    }
    return false;
}

private boolean dfs(char[][] board, String word, int r, int c, int idx) {
    if (idx == word.length()) return true;   // matched all characters
    if (r < 0 || r >= board.length ||
        c < 0 || c >= board[0].length) return false;  // out of bounds
    if (board[r][c] != word.charAt(idx)) return false; // mismatch

    char saved = board[r][c];
    board[r][c] = '#';   // CHOOSE: mark visited — in-place, O(1) space

    boolean found = dfs(board, word, r + 1, c, idx + 1) ||
                    dfs(board, word, r - 1, c, idx + 1) ||
                    dfs(board, word, r, c + 1, idx + 1) ||
                    dfs(board, word, r, c - 1, idx + 1);

    board[r][c] = saved;  // UNCHOOSE: restore
    return found;
}
```

**Time**: O(rows * cols * 4^L) — L = word length; from each starting cell, at most 4^L paths
**Space**: O(L) — recursion depth equals word length

---

### 6. Palindrome Partitioning

**Approach**: DFS + backtracking on substring start index. At each position, try all end positions; if the substring is a palindrome, include it in the current partition and recurse from the end.

```java
public List<List<String>> partition(String s) {
    List<List<String>> result = new ArrayList<>();
    backtrack(s, 0, new ArrayList<>(), result);
    return result;
}

private void backtrack(String s, int start, List<String> current,
                        List<List<String>> result) {
    if (start == s.length()) {
        result.add(new ArrayList<>(current));
        return;
    }
    for (int end = start + 1; end <= s.length(); end++) {
        String sub = s.substring(start, end);
        if (isPalindrome(sub)) {   // PRUNING: only recurse if sub is a palindrome
            current.add(sub);
            backtrack(s, end, current, result);
            current.remove(current.size() - 1);
        }
    }
}

private boolean isPalindrome(String s) {
    int lo = 0, hi = s.length() - 1;
    while (lo < hi) {
        if (s.charAt(lo++) != s.charAt(hi--)) return false;
    }
    return true;
}

// OPTIMIZATION: precompute dp[i][j] = true if s[i..j] is a palindrome
// Reduces isPalindrome from O(n) to O(1) per check
// dp[i][j] = (s[i] == s[j]) && (j - i < 2 || dp[i+1][j-1])
```

**Time**: O(n * 2^n) — at most 2^n partitions, each sub O(n) to verify
**Space**: O(n) — recursion depth

---

### 7. Sudoku Solver

**Approach**: Scan cell by cell (row-major). Skip filled cells. For empty cells, try digits 1–9; if valid, place the digit and recurse to the next cell. If the recursive call fails (returns false), backtrack by resetting the cell to 0.

```java
// Reference: Sudoku.java
public void solveSudoku(char[][] board) {
    solve(board);
}

private boolean solve(char[][] board) {
    for (int row = 0; row < 9; row++) {
        for (int col = 0; col < 9; col++) {
            if (board[row][col] != '.') continue;  // skip pre-filled cells

            for (char c = '1'; c <= '9'; c++) {
                if (isValid(board, row, col, c)) {
                    board[row][col] = c;          // CHOOSE

                    if (solve(board)) return true; // EXPLORE

                    board[row][col] = '.';         // UNCHOOSE (backtrack)
                }
            }
            return false;  // no digit worked for this cell — backtrack
        }
    }
    return true;  // all cells filled successfully
}

private boolean isValid(char[][] board, int row, int col, char c) {
    for (int i = 0; i < 9; i++) {
        // Check row
        if (board[row][i] == c) return false;
        // Check column
        if (board[i][col] == c) return false;
        // Check 3x3 box — key formula: maps (row,col,i) to box cell
        int boxRow = 3 * (row / 3) + i / 3;
        int boxCol = 3 * (col / 3) + i % 3;
        if (board[boxRow][boxCol] == c) return false;
    }
    return true;
}
```

**The 3x3 box formula**: `row = 3*(row/3) + i/3`, `col = 3*(col/3) + i%3` where `i` goes 0..8. This is the most commonly fumbled part — memorize by tracing: for row=4, col=4 (center box): `boxRow = 3*1 + i/3`, `boxCol = 3*1 + i%3` → iterates cells (3,3) through (5,5). Correct.

**Time**: O(9^(empty_cells)) — each empty cell tries up to 9 digits
**Space**: O(81) = O(1) — recursion depth bounded by grid size

---

### 8. Optimal Account Balancing

**Approach**: Build a net-balance list from all transactions (positive = owed money, negative = owes money). Use backtracking: for each non-zero balance, try to settle it against every subsequent balance with the opposite sign. Minimize total transactions.

```java
// Reference: OptimalAccountBalancing.java
public int minTransfers(int[][] transactions) {
    // Step 1: Compute net balance per person
    Map<Integer, Integer> balanceMap = new HashMap<>();
    for (int[] t : transactions) {
        balanceMap.merge(t[0], -t[2], Integer::sum);  // person t[0] owes t[2]
        balanceMap.merge(t[1],  t[2], Integer::sum);  // person t[1] is owed t[2]
    }

    // Step 2: Collect non-zero balances
    List<Integer> balances = new ArrayList<>();
    for (int b : balanceMap.values()) {
        if (b != 0) balances.add(b);
    }

    return dfs(balances, 0);
}

private int dfs(List<Integer> balances, int idx) {
    // Skip already-settled (zero) balances
    while (idx < balances.size() && balances.get(idx) == 0) idx++;
    if (idx == balances.size()) return 0;  // everyone settled

    int minTxns = Integer.MAX_VALUE;
    for (int i = idx + 1; i < balances.size(); i++) {
        // Only try to settle with someone who has the opposite sign
        if (balances.get(idx) * balances.get(i) < 0) {
            // CHOOSE: settle idx against i
            int saved = balances.get(i);
            balances.set(i, saved + balances.get(idx));

            // EXPLORE: one transaction used, move to next unsettled
            minTxns = Math.min(minTxns, 1 + dfs(balances, idx + 1));

            // UNCHOOSE
            balances.set(i, saved);
        }
    }
    return minTxns;
}
```

**Why reduce to net balances first?** Multiple transactions between the same people cancel out. Reducing to net balances minimizes the search space. The minimum number of transactions to settle n non-zero balances is at most n-1.

**Time**: O(n!) in the worst case — bounded by how quickly balances hit zero
**Space**: O(n) — recursion stack depth

---

## Common Mistakes

1. **Adding the mutable list directly to results**. `result.add(current)` adds a reference — when you later `remove` elements during backtracking, the list in `result` mutates too. Always `result.add(new ArrayList<>(current))`. This is the single most common backtracking bug.

2. **Forgetting to unchoose**. Backtracking requires symmetric choose/unchoose. If you add to a list, remove from it. If you mark a cell visited, unmark it. Asymmetric mutations leave the state corrupted for sibling branches. Review every `add`/`set`/`mark` to confirm a matching undo exists.

3. **Duplicate solutions in Permutations/Subsets with repeated elements**. When the input has duplicates, after sorting, skip `candidates[i]` at a given recursion level if `i > start && candidates[i] == candidates[i-1]`. Without this, you get identical subsets/permutations from choosing the two duplicate elements in different orders.

4. **Passing `i` instead of `i+1` (or vice versa) in Combination Sum**. Using `i` (same index) allows reuse; using `i+1` disallows it. Confusing them produces wrong results that are hard to spot because the structure looks correct.

5. **Incorrect 3x3 box indexing in Sudoku**. The formula `3*(row/3) + i/3` and `3*(col/3) + i%3` must be memorized. A common wrong version uses `row/3 + i/3` (missing the multiplication by 3), which maps to the wrong box cells.

---

## Interview Tips

**What interviewers probe:**

- **State the three-step template explicitly before coding.** Say: "I'll use the choose-explore-unchoose template. The base case is [X], each choice is [Y], and I prune when [Z]." This signals pattern recognition before the first line of code.
- **Identify which archetype applies.** Interviewers watch whether you distinguish subsets (start index moves forward, all sizes) from permutations (used[] array, all orders) from combinations (start index + size constraint). Naming the archetype correctly saves you from writing the wrong loop structure.
- **Verbalize pruning conditions.** For Combination Sum, say "I sort the array and break early because if the current element exceeds the remaining target, all subsequent elements (being larger) will too." Pruning is what separates a correct brute-force from an efficient solution.

**Common follow-up questions:**

- *Subsets*: "How many subsets does an n-element set have?" → 2^n. "How does bitmask generate them?" → Each bit in 0 to 2^n-1 represents include/exclude for each element.
- *Permutations with duplicates*: "How do you avoid duplicate permutations?" → Sort + skip `nums[i] == nums[i-1]` at the same recursion depth (when `i > start` for the swap approach, or `i > 0 && !used[i-1]` for the used-array approach).
- *N-Queens*: "What is the space complexity?" → O(n) for the recursion stack and O(n) for the three boolean arrays — much better than O(n²) for a board-scan-based `isSafe`.
- *Sudoku*: "How would you speed this up?" → Constraint propagation (naked singles, hidden singles) before backtracking, or Dancing Links (Algorithm X). At minimum, use boolean arrays per row/col/box instead of scanning on every `isValid` call.
- *Optimal Account Balancing*: "Why is the brute-force approach n!?" → Each non-zero balance must be matched against every other balance in some order. The backtracking prunes same-sign pairs, but worst case is still factorial.
- *Word Search*: "Can you speed up with Trie?" → Yes — for Word Search II (find all words from a dictionary), build a Trie from the dictionary and prune DFS paths that are not a prefix of any word.
