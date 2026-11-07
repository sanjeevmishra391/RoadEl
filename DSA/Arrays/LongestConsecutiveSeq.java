package Arrays;

import java.util.*;

public class LongestConsecutiveSeq {

    // sort O(n*log n)
    static private int longestConsecutive(int[] nums) {
        int n = nums.length;
        Arrays.sort(nums);

        int i= 0, maxLen = 0, currLen = 0;
        while(i<n) {
            int curr = nums[i];
            while(i<n && curr == nums[i]) {
                curr = nums[i];
                i++;
            }
            currLen++;
            maxLen = Math.max(maxLen, currLen);
            if (i<n && nums[i] != curr + 1) {
                currLen = 0;
            }
        }

        return maxLen;
    }

    static private int longestConsecutiveUsingSet(int[] nums) {
        Set<Integer> set = new HashSet<>();
        for(int n: nums) {
            set.add(n);
        }

        // if n-1 doesn't exist in set then n is start of new seq.
        // keep checking while n+length is in set
        int res = 0;
        for(int i=0; i<nums.length; i++) {
            if(!set.contains(nums[i]-1)) {
                int len = 1;
                while(set.contains(nums[i]+len)) {
                    len++;
                }
                res = Math.max(res, len);
            }
        }

        return res;
    }

    static private int longestConsecutiveUsingMap(int[] nums) {
        // left: length of seq ending at num-1
        // right: length of seq starting at num+1
        // newly formed seq = left + right + 1

        Map<Integer, Integer> map = new HashMap<>();
        int res = 0;
        for(int num: nums) {
            if(map.containsKey(num))
                continue;
            int leftSeq = map.getOrDefault(num-1, 0);
            int rightSeq = map.getOrDefault(num+1, 0);
            int length = leftSeq + rightSeq + 1;

            map.put(num, length);
            map.put(num-leftSeq, length);
            map.put(num+rightSeq, length);
            res = Math.max(res, length);
        }
        return res;
    }

    public static void main(String[] args) {
        int nums[] = {100, 4, 200, 1, 3, 2};
        System.out.println(longestConsecutive(nums));
        System.out.println(longestConsecutiveUsingSet(nums));
        System.out.println(longestConsecutiveUsingMap(nums));
    }
}
