# Tree

## Pattern Recognition Triggers
- "Level order" / "layer by layer" → **BFS**
- "Path from root to leaf" / "max depth" → **DFS**
- "Validate BST" / "kth smallest" → **Inorder DFS**
- "Lowest common ancestor" → **DFS with backtracking**
- "Serialize / deserialize" → **Preorder DFS**

## Complexity Cheat Sheet
| Operation | BST (avg) | BST (worst/skewed) | Balanced BST |
|---|---|---|---|
| Search | O(log n) | O(n) | O(log n) |
| Insert | O(log n) | O(n) | O(log n) |
| Delete | O(log n) | O(n) | O(log n) |
| Traversal | O(n) | O(n) | O(n) |
Space for recursion: O(h) where h = height. Balanced: O(log n), skewed: O(n)

## DFS Templates

### Recursive DFS (most common)
```java
void dfs(TreeNode node) {
    if (node == null) return;   // base case
    // preorder: process node here
    dfs(node.left);
    // inorder: process node here
    dfs(node.right);
    // postorder: process node here
}
```

### Iterative BFS (level order)
```java
Queue<TreeNode> queue = new LinkedList<>();
queue.offer(root);
while (!queue.isEmpty()) {
    int size = queue.size();        // capture level size BEFORE loop
    for (int i = 0; i < size; i++) {
        TreeNode node = queue.poll();
        // process node
        if (node.left != null)  queue.offer(node.left);
        if (node.right != null) queue.offer(node.right);
    }
}
```

## Key Interview Problems

| Problem | Approach | Key Insight |
|---|---|---|
| Max Depth | Postorder DFS | return 1 + max(left, right) |
| Diameter | Postorder DFS | at each node: left_height + right_height |
| Level Order | BFS | capture queue size before inner loop |
| Lowest Common Ancestor | DFS | if both found in subtrees, current node is LCA |
| Validate BST | Inorder DFS | pass min/max bounds down, not just parent |
| Path Sum | Preorder DFS | subtract node value, check 0 at leaf |
| Symmetric Tree | DFS | compare mirror nodes simultaneously |
| Construct from Preorder+Inorder | Recursion | preorder[0] = root; find in inorder to split |
| Kth Smallest in BST | Inorder DFS | inorder of BST = sorted order |
| Right Side View | BFS | take last node of each level |

## Common Mistakes
- Forgetting `if (node == null) return` base case
- In BST validation: checking only against parent, not full range → use `(min, max)` bounds
- Level order: not capturing `queue.size()` before the inner for-loop

## When to Use Iterative vs Recursive
- **Recursive:** cleaner, use when stack overflow is not a concern (balanced trees)
- **Iterative DFS with stack:** deep/skewed trees, explicit path tracking needed
- **Iterative BFS:** level-by-level processing, shortest path in unweighted tree

---

A Tree Data Structure can be traversed in following ways:

- Depth First Search or DFS
    - Inorder Traversal (Left -> Root -> Right) : In BST, gives nodes in non-decreasing order.
    - Preorder Traversal (Root -> Left -> Right) : Used to get prefix expressions on an expression tree.
    - Postorder Traversal (Left -> Right -> Root) : Used to delete the tree. Get postfix expression of an expression tree. Can help in garbage collection algorithms
- Level Order Traversal or Breadth First Search or BFS

## Binary Search Tree

Each node in a Binary Search Tree has at most two children, a left child and a right child, with the left child containing values less than the parent node and the right child containing values greater than the parent node. This hierarchical structure allows for efficient searching, insertion, and deletion operations on the data stored in the tree.