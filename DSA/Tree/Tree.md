A Tree Data Structure can be traversed in following ways:

- Depth First Search or DFS
    - Inorder Traversal (Left -> Root -> Right) : In BST, gives nodes in non-decreasing order.
    - Preorder Traversal (Root -> Left -> Right) : Used to get prefix expressions on an expression tree.
    - Postorder Traversal (Left -> Right -> Root) : Used to delete the tree. Get postfix expression of an expression tree. Can help in garbage collection algorithms
- Level Order Traversal or Breadth First Search or BFS

## Binary Search Tree

Each node in a Binary Search Tree has at most two children, a left child and a right child, with the left child containing values less than the parent node and the right child containing values greater than the parent node. This hierarchical structure allows for efficient searching, insertion, and deletion operations on the data stored in the tree.