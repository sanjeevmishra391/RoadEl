package Arrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class TopKFrequentElements {

    // Time = O(n + m*log m + k*log m)
    static public int[] topKFrequent(int[] nums, int k) {
        // map - key + values
        // use max heap to store all frequencies
        // return top k
        Map<Integer, Integer> map = new HashMap<>();
        for(int n: nums) {
            map.put(n, map.getOrDefault(n, 0) + 1);
            // map.merge(n, 1, Integer::sum);
        }

        PriorityQueue<int[]> qp = new PriorityQueue<>((a, b) -> b[1] - a[1]);

        for(Map.Entry<Integer, Integer> e: map.entrySet()) {
            qp.add(new int[]{e.getKey(), e.getValue()});
        }

        int res[] = new int[k];
        int i = 0;
        while(k>0) {
            res[i++] = qp.poll()[0];
            k--;
        }

        return res;
    }

    // Time O(n + m*log k + k*log k)
    static public int[] topKFrequentUsingMinHeap(int[] nums, int k) {
        // instead of storing all the elements in heap, create min heap of size k
        // if size exceeds k, then poll from heap
        Map<Integer, Integer> map = new HashMap<>();
        for(int n: nums) {
            map.put(n, map.getOrDefault(n, 0) + 1);
            // map.merge(n, 1, Integer::sum);
        }

        PriorityQueue<int[]> minHeap = new PriorityQueue<>((a, b) -> a[1] - b[1]);

        for(Map.Entry<Integer, Integer> e: map.entrySet()) {
            minHeap.add(new int[]{e.getKey(), e.getValue()});

            if(minHeap.size() > k)
                minHeap.poll();
        }

        // store polled elements from the back of array
        int res[] = new int[k];
        for(int i=k-1; i>=0; i--) {
            res[i] = minHeap.poll()[0];
        }

        return res;
    }

    // Bucket sort: O(n)
    static public int[] topKFrequentUsingBucketSort(int[] nums, int k) {
        Map<Integer, Integer> map = new HashMap<>();
        for(int n: nums) {
            map.put(n, map.getOrDefault(n, 0) + 1);
        }

        // create buckets = at max n
        int n = nums.length;
        List<Integer>[] buckets = new List[n+1];

        // store elements at the index of frequency
        for(Map.Entry<Integer, Integer> e: map.entrySet()) {
            int index = e.getValue();
            if(buckets[index] == null) {
                buckets[index] = new ArrayList<Integer>();
            }

            buckets[index].add(e.getKey());
        }

        // traverse from the back of buckets and check for non-null index data and store in res of size k
        int res[] = new int[k];
        int resIdx = 0;
        for(int i = n; i>0 && resIdx < k; i--) {
            if(buckets[i] != null) {
                for(int j=0; j<buckets[i].size(); j++) {
                    res[resIdx++] = buckets[i].get(j);

                    if(resIdx == k) 
                        break;
                }
            }
        }

        return res;
    }

    public static void main(String[] args) {
        int nums[] = {1,2,2,3,3,3,1,1,1,1,1};
        int k = 2;
        System.out.println(Arrays.toString(topKFrequent(nums, k)));
        System.out.println(Arrays.toString(topKFrequentUsingMinHeap(nums, k)));
        System.out.println(Arrays.toString(topKFrequentUsingBucketSort(nums, k)));
    }
}
