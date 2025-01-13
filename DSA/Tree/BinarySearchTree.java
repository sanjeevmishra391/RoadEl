package Tree;

public class BinarySearchTree {

    public TreeNode createBST(int arr[]) {
        TreeNode root = null;
        for(int a : arr) {
            if(root == null)
                root = insert(null, a);
            else
                insert(root, a);
        }

        return root;
    }

    public TreeNode insert(TreeNode root, int k) {
        if(root == null)
            return new TreeNode(k);

        if(root.val == k)
            return root;

        if(k > root.val) {
            root.right = insert(root.right, k);
        } else {
            root.left = insert(root.left, k);
        }
        return root;
    }

    public TreeNode search(TreeNode root, int k) {
        if(root == null || root.val == k)
            return root;

        if(root.val < k)
            return search(root.right, k);

        return search(root.left, k);
        
    }

    /*
     * Case 1: Delete a leaf node. Assign node to null
     * Case 2: Delete a node with a single child > Copy the child to current and delete the child
     * Case 3: Delete a node with two children > find the inorder successor of the node. Copy contents of the inorder successor to the node, and delete the inorder successor.
     */
    public TreeNode delete(TreeNode root, int k) {
        if(root == null)
            return null;

        if(root.val > k) {
            root.left = delete(root.left, k);
        } else if(root.val < k) {
            root.right = delete(root.right, k);
        } else {
            if(root.left == null)
                return root.right;

            if(root.right == null)
                return root.left;

            TreeNode succNode = getSuccessor(root);
            root.val = succNode.val;
            root.right = delete(root.right, succNode.val);
        }

        return root;
    }

    public TreeNode getSuccessor(TreeNode root) {
        root = root.right;
        while(root!=null && root.left!=null) {
            root = root.left;
        }
        return root;
    }
}
