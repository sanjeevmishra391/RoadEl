package Stack;

import java.util.Stack;

public class LargestRectangeInHistrogram {

    static int largestRectangleArea(int[] heights) {
        int n = heights.length;
        Stack<Integer> st = new Stack<>();
        int left[] = new int[n];
        int right[] = new int[n];

        for(int i=0; i<n; i++) {
            // while curr is smaller than top pop and set their left till before curr
            while(!st.empty() && heights[st.peek()] > heights[i]) {
                right[st.pop()] = i-1;
            }
            st.push(i);
        }
        while(!st.isEmpty()) {
            right[st.pop()] = n-1;
        }

        for(int i=n-1; i>=0; i--) {
            while(!st.isEmpty() && heights[st.peek()] > heights[i]) {
                left[st.pop()] = i+1;
            }
            st.push(i);
        }

        while(!st.isEmpty()) {
            left[st.pop()] = 0;
        }

        int ans = 0;
        for(int i=0; i<n; i++) {
            ans = Math.max(ans, ((i - left[i]) + (right[i] - i) + 1)*heights[i]);
        }

        return ans;
    }
    public static void main(String[] args) {
        int heights[] = {2, 1, 5, 6, 2, 3};
        // [2, 1, 5, 6, 2, 3]
        // [0, 1, 2, 3, 4, 5] idx
        // [0, 5, 3, 3, 5, 5] to the right
        // [0, 0, 2, 3, 2, 5] to the left
        System.out.println(largestRectangleArea(heights));
    }
}
