package Bit_Manipulation;

public class Bit {

    static void allSubsets() {
        int n = 3;
        char[] c = {'a', 'b', 'c'};

        for(int i=0; i<(1<<n); i++) {
            for(int j=0; j<n; j++) {
                if((i & (1 << j)) != 0)
                    System.out.print(c[j] + " ");
            }
            System.out.println();
        }
    }

    // https://leetcode.com/problems/single-number/description/
    // Given a non-empty array of integers nums, every element appears twice except for one. Find that single one.
    static void singleNumber() {
        int[] nums = {2,2,1};
        int a = nums[0];

        for(int i=1; i<nums.length; i++) {
            a = a^nums[i];
        }

        System.out.println(a);
    }


    // https://leetcode.com/problems/counting-bits/description/
    // Given an integer n, return an array ans of length n + 1 such that for each i (0 <= i <= n), ans[i] is the number of 1's in the binary representation of i.
    static void countingBits() {
        int n = 10;
        int res[] = new int[n+1];
        
        for(int i=1; i<=n; i++) {
            res[i] = res[i >> 1]  + (i&1);
            System.out.println(i + " " + res[i]);
        }
    }

    static void sumTwoNumbersUsingBinary() {
        int a = 10, b = 36;
        int res = 0, i = 0, carry = 0;
        while(a!=0 || b!=0) {
            int lastA = a&1;
            int lastB = b&1;

            int xor = lastA ^ lastB ^ carry;
            if((lastA == 1 && lastB == 1) || (((lastA ^ lastB) == 1) && carry == 1))
                carry = 1;
            else
                carry = 0;

            res = (xor<<i) | res;

            a = a>>>1;
            b = b>>>1;

            i++;
        }

        if(i<32 && carry!=0)
            res = res | carry<<i;
    
        
        System.out.println(res);
    }


    public static void main(String[] args) {
        // allSubsets();
        // singleNumber();
        // countingBits();
        sumTwoNumbersUsingBinary();
    }
}