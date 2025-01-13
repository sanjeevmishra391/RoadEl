package Tree;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class BinaryTree {

    public static TreeNode createBinaryTree(Integer[] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null) {
            return null; // Return null for empty array or null root
        }

        // Create the root of the tree
        TreeNode root = new TreeNode(arr[0]);
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);

        int i = 1; // Index for the array

        // Iterate through the array and construct the tree
        while (!queue.isEmpty() && i < arr.length) {
            TreeNode current = queue.poll();

            // Create left child if valid
            if (i < arr.length && arr[i] != null) {
                current.left = new TreeNode(arr[i]);
                queue.add(current.left);
            }
            i++;

            // Create right child if valid
            if (i < arr.length && arr[i] != null) {
                current.right = new TreeNode(arr[i]);
                queue.add(current.right);
            }
            i++;
        }

        return root;
    }

    public static int getTreeHeight(TreeNode root) {
        if (root == null) {
            return 0;
        }
        return 1 + Math.max(getTreeHeight(root.left), getTreeHeight(root.right));
    }

    public static int getNodeDepth(TreeNode root, int k) {
        if(root == null)
            return (int) 1e5;

        if(root.val == k)
            return 0;

        return 1 + Math.min(getNodeDepth(root.left, k), getNodeDepth(root.right, k));
    }

    public static boolean ifNodeExists(TreeNode root, int k) {
        if(root == null)
            return false;

        if(root.val == k)
            return true;

        return ifNodeExists(root.left, k) || ifNodeExists(root.right, k);
    }

    public static TreeNode getParent(TreeNode root, TreeNode parent, int k) {
        if(root == null)
            return null;

        if(root.val == k)
            return parent;

        TreeNode res = getParent(root.left, root, k);
        if(res!=null)
            return res;

        return getParent(root.right, root, k);
    }

    public static void main(String[] args) {
        Integer[] arr=  {5, 10, 15, 20, 25, 30, 35, null, null, null, 45};
        TreeNode root = BinaryTree.createBinaryTree(arr);
        Traversal.levelOrder(root);
        System.out.println(getNodeDepth(root, 25));
        System.out.println(ifNodeExists(root, 31));
        System.out.println(Optional.ofNullable(getParent(root, null, 15)).orElse(new TreeNode(-1)).val);
    }
}
