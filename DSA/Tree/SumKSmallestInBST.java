package Tree;

public class SumKSmallestInBST {
    static void calculateSum(TreeNode root, int[] kArr, int[] ans) {
        if(root.left != null)
            calculateSum(root.left, kArr, ans);

        if(kArr[0] > 0) {
            ans[0] += root.val;
            kArr[0]--;
        } else {
            return;
        }

        if(root.right != null)
            calculateSum(root.right, kArr, ans);
    }

    static int sum(TreeNode root, int k) {
        int ans[] = {0};
        int kArr[] = {k};
        calculateSum(root, kArr, ans);
        return ans[0];
    }

    public static void main(String[] args) {
        int[] arr = {8, 7, 10, 2, 9, 13};
        BinarySearchTree bst = new BinarySearchTree();
        TreeNode root = bst.createBST(arr);
        Traversal.levelOrder(root);
        System.out.println(sum(root, 3));
    }
}
