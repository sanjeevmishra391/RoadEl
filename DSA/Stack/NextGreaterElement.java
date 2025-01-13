package Stack;

import java.util.Arrays;
import java.util.Stack;

public class NextGreaterElement {

    static int[] nge(int arr[]) {
        Stack<Integer> stack = new Stack<>();
        // if current is greater than top of stack then that number is nge for current element
        // inp = [6,  8, 0, 1,  3]
        // ans = [8, -1, 1, 3, -1]
        // idx = [0,  1, 2, 3,  4]
        int op[] = new int[arr.length];
        for(int i=0; i<arr.length; i++) {
            // push only index
            // curr element = arr[i]
            // element on top = arr[stack.peek()]
            // index of top element  = stack.pop()
            while(!stack.isEmpty() && (arr[stack.peek()] < arr[i])) {
                op[stack.pop()] = arr[i];
            }
            stack.push(i);
        }

        while(!stack.isEmpty()) {
            op[stack.pop()] = -1;
        }

        return op;
    }

    public static void main(String[] args) {
        int arr[] = {6,  8, 0, 1,  3};
        System.out.println(Arrays.toString(nge(arr)));
    }
}
