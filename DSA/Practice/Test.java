package Practice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Test  {

   static int[] maxSumOfThreeSubarrays(int[] nums, int k) {
      // find all the windows of size k with sum
      int n = nums.length;
      int sum=0, start=0, end=0, i=0, ws = n-k+1;
      int[] windowSum = new int[ws];
      while(end<n) {
         if(end<k) {
               sum += nums[end];
         } else {
               windowSum[i++] = sum;
               sum = sum - nums[start] + nums[end];
               start++;
         }
         end++;
      }
      windowSum[i] = sum;

      System.out.println(Arrays.toString(windowSum));

      int maxLeft[] = new int[ws];
      int maxRight[] = new int[ws];

      for(int j=1; j<ws; j++) {
         maxLeft[j] = (windowSum[j] > windowSum[maxLeft[j-1]]) ? j : maxLeft[j-1];
      }

      maxRight[ws-1] = ws-1;
      for(int j=ws-2; j>=0; j--) {
         maxRight[j] = (windowSum[j] >= windowSum[maxRight[j+1]]) ? j : maxRight[j+1];
      }

      System.out.println(Arrays.toString(maxLeft));
      System.out.println(Arrays.toString(maxRight));

      
      int maxThree[] = new int[3];
      int maxSum = 0;

      for(int j=k; j< ws-k; j++) {
         int currSum = windowSum[maxLeft[j-k]] + windowSum[j] + windowSum[maxRight[j+k]];
         if(currSum > maxSum) {
               maxSum = currSum;
               maxThree = new int[]{maxLeft[j - k], j, maxRight[j + k] };
         }
      }

      return maxThree;
  }

   static int countIdentical(String s) {
      int i=0, ans = 0;
      while(i<s.length()) {
         int j = i+1;
         while(j<s.length() && s.charAt(i) == s.charAt(j)) {
            j++;
         }

         ans = Math.max(ans, j-i);
         i=j;
      }
      return ans;
   }

   static boolean containsDigit(int n) {
      HashMap<Integer, Integer> map = new HashMap<>();
      map.put(0, 0);
      map.put(1, 1);
      map.put(6, 9);
      map.put(8, 8);
      map.put(9, 6);

      int newNumber = 0, original = n;
      while(n>0) {
         int d = n%10;
         if(!map.containsKey(d))
            return false;
         newNumber = newNumber*10 + map.get(d);
         n=n/10;
      }
      return newNumber != original;
   }

   static int confusingNumber(int n) {
      int count = 0;
      for(int i=1; i<=n; i++) {
         if(containsDigit(i))
            count++;
      }
      return count;
   }

   static ArrayList<Integer> maxset(ArrayList<Integer> A) {
      int maxSum=0, i = 0, start = 0, end = 0;
      System.out.println();
      while(i<A.size()) {
         while(i<A.size() && A.get(i)<0)
               i++;
               
         int j = i, sum = 0;
         while(j<A.size() && A.get(j)>=0) {
               sum += A.get(j);
               j++;
         }

         System.out.println(i + " " + j + " " + sum);
         
         if(sum>maxSum) {
               start = i;
               maxSum = sum;
               end = j;
         }
         i=j;
      }
      
      
      System.out.println(start + " " + end + " " + maxSum);
      ArrayList<Integer> res = new ArrayList<>();

      if(start>=A.size())
         return res;

      for(i = start; i < end; i++) {
         res.add(A.get(i));
      }
      return res;
  }

  static String solve(int A) {
      ArrayList<String> list = new ArrayList<>();
      list.add("1");
      long i = 2;
      while(i<=A) {
         int n = list.size()-1;
         long carry = 0;
         while(n>=0) {
            long v = Long.parseLong(list.get(n));
            long mul = v*i;
            list.set(n, ((mul + carry)%10) + ""); 
            carry = (mul + carry)/10;
            n--;
         }
         while(carry!=0) {
            list.add(0, (carry%10) +"");
            carry = carry/10;
         }
         i++;
      }
      
      StringBuilder sb = new StringBuilder();
      for(String a : list) {
         sb.append(a);
      }
      
      return sb.toString();
}

   public static void main(String[] args) {
      int nums[] = {1,2,1,2,6,7,5,1};
      int k = 2;
      System.out.println(Arrays.toString(maxSumOfThreeSubarrays(nums, k)));

      System.out.println(countIdentical("0010001110101111"));

      ArrayList<Integer> list = new ArrayList<>();
      list.add(-1);
      list.add(-2);
      list.add(-5);
      list.add(-7);
      list.add(-2);
      list.add(-3);
      int arr[] = list.stream().mapToInt(Integer::intValue).toArray();
      int max = Arrays.stream(arr).max().orElseThrow();
      System.out.println(max);
      
      String str = "apple";
      boolean res = str.matches("^[aeiou].*");
      System.out.println(res);
      
      System.out.println(confusingNumber(100));
      list.stream().forEach(s -> System.out.print(s.intValue() + " "));

      System.out.println(maxset(list));

      System.out.println(solve(50));
   }
} 