package Tree;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Traversal {

    public static void inorder(TreeNode root) {
        if(root == null)
            return;

        inorder(root.left);
        System.out.println(root.val);
        inorder(root.right);
    }

    public static void preorder(TreeNode root) {
        if(root == null)
            return;

        System.out.println(root.val);
        preorder(root.left);
        preorder(root.right);
    }

    public static void postorder(TreeNode root) {
        if(root == null)
            return;

        postorder(root.left);
        postorder(root.right);
        System.out.println(root.val);
    }

    public static void levelOrder(TreeNode root) {
        if (root == null) {
            System.out.println("Tree is empty");
            return;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);

        int height = BinaryTree.getTreeHeight(root);
        int maxWidth = (int) Math.pow(2, height) - 1;

        for (int level = 0; level < height; level++) {
            int nodesOnLevel = (int) Math.pow(2, level);
            int spacesBetween = (maxWidth / nodesOnLevel);
            int leadingSpaces = spacesBetween / 2;

            System.out.print(" ".repeat(leadingSpaces));
            for (int i = 0; i < nodesOnLevel; i++) {
                TreeNode node = queue.poll();
                if (node != null) {
                    System.out.print(node.val);
                    queue.add(node.left);
                    queue.add(node.right);
                } else {
                    System.out.print("n");
                    queue.add(null);
                    queue.add(null);
                }
                if (i < nodesOnLevel - 1) {
                    System.out.print(" ".repeat(spacesBetween));
                }
            }
            System.out.println();
        }
    }

    static void morrisTravesal(TreeNode root) {
        List<Integer> list = new ArrayList<>();
        TreeNode curr = root;

        while(curr != null) {
            if(curr.left == null) {
                list.add(curr.val);
                curr = curr.right;
            } else {
                // If the left child is not NULL, find the predecessor (rightmost node in the left subtree)
                TreeNode prev = curr.left;
                while(prev.right!=null && prev.right != curr) {
                    prev = prev.right;
                }

                // If the predecessor's right child is NULL, establish a temporary link and move to the left child
                if(prev.right == null) {
                    prev.right = curr;
                    curr = curr.left;
                } else {
                    // If the predecessor's right child is already linked, remove the link, add current node to inorder list, and move to the right child
                    prev.right = null;
                    list.add(curr.val);
                    curr = curr.right;
                }
            }
        }

        System.out.println(list);
    }

    public static void main(String[] args) {
        Integer[] arr = {1, 2, 3, null, 5, 6, null};
        TreeNode root = BinaryTree.createBinaryTree(arr);
        levelOrder(root);
        morrisTravesal(root);
    }
}
