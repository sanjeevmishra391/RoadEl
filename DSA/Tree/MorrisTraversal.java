package Tree;

import java.util.*;

/* 
It maintains pointer from inorder predecessor of current node as pre.right = curr;

- If left of current is null then visit current and move to right
- Else find inorder predecessor of the current node and map it's right to curr
- If it's already mapped then remove the map and move to curr.right
*/


public class MorrisTraversal {

    // Time: O(n), Space: O(1)
    public static List<Integer> inorderTraversal(TreeNode root) {
        List<Integer> inorder = new ArrayList<>();

        TreeNode curr = root;

        while(curr != null) {
            if (curr.left == null) {
                inorder.add(curr.val);
                curr = curr.right;
            } else {
                TreeNode pred = curr.left;
                // pred.right != curr this is checked so that we don't keep moving in cycle
                while( pred.right != null && pred.right != curr) {
                    pred = pred.right;
                }

                if(pred.right == null) {
                    pred.right = curr; // create link from inorder predecessor to root
                    curr = curr.left;
                } else {
                    pred.right = null; // remove link
                    inorder.add(curr.val);
                    curr = curr.right; // move to right tree
                }
            }
        }

        return inorder;
    }

    public static void main(String[] args) {
        int[] arr = {8, 7, 10, 2, 9, 13};
        BinarySearchTree bst = new BinarySearchTree();
        TreeNode root = bst.createBST(arr);
        System.out.println(inorderTraversal(root));
    }
}
