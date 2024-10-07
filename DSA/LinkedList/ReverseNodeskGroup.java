package LinkedList;


import java.util.List;
import java.util.Arrays;

// 1. Iterate and reverse and keep count
//     1.1 if count is k, return joining nodes
//     1.2 if count is less than k, return list as it is

public class ReverseNodeskGroup {

    List<ListNode> reverse(ListNode head, int k) {
        ListNode prev = null, curr = head, agla = null;

        int count = 0;

        while(curr!=null && count<k) {
            agla = curr.next;
            curr.next = prev;
            prev = curr;
            curr = agla;
            count++;
        }

        if(count == k) {
            return Arrays.asList(new ListNode[]{prev, curr});
        }

        return Arrays.asList(new ListNode[]{prev});
    }

    ListNode subReverse(ListNode head, int k) {
        ListNode prev = null, curr = head, newHead = head;
        while(curr!=null) {
            // prev = a;
            // curr = b;
            List<ListNode> cd = reverse(curr, k); // [c, d]
            if(cd.size() == 2) {
                if(prev == null) {
                    newHead = cd.get(0);
                    curr.next = cd.get(1);
                } else {
                    prev.next = cd.get(0); // c
                    curr.next = cd.get(1); // d
                }
            } else {
                cd = reverse(cd.get(0), k);
                if(prev == null) {
                    newHead = head;
                } else {
                    prev.next = cd.get(0); // c
                }
            }
            prev = curr;
            curr = cd.size() == 2 ? cd.get(1) : null;
        }

        return newHead;
    }
    public static void main(String[] args) {
        // a > b .... c > d
        // a > c .... b > d
        // length of b....c is k
        LinkedList ll = new LinkedList();
        ListNode head = ll.create(new int[]{1,2,3,4,5,6,7});
        ReverseNodeskGroup rr = new ReverseNodeskGroup();
        ListNode rev = rr.subReverse(head, 3);
        ll.printList(rev);
        
    }
}