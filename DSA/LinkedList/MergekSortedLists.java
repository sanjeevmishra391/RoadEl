package LinkedList;

import java.util.PriorityQueue;

public class MergekSortedLists {
    
    public ListNode mergeKLists(ListNode[] lists) {
        ListNode[] itr = new ListNode[lists.length];
        for(int i=0; i<lists.length; i++) {
            itr[i++] = lists[0];
        }

        ListNode res = null, head = null;
        boolean allNull = false;

        while(!allNull) {
            int idx = 0, svalue = Integer.MAX_VALUE, nullCount = 0;
            for(int j=0; j<lists.length; j++) {
                if(lists[j] == null)
                    nullCount++;
                else
                    if(lists[j].value < svalue) {
                        svalue = lists[j].value;
                        idx = j;
                    }
            }

            if(nullCount == lists.length)
                break;

            lists[idx] = lists[idx].next;
            ListNode temp = new ListNode(svalue);
            if(head == null) {
                res = temp;
                head = temp;
                continue;
            }

            res.next = temp;
            res = res.next;
        }

        return head;
    }

    public ListNode mergeKLists2(ListNode[] lists) {
        PriorityQueue<ListNode> queue = new PriorityQueue<>(lists.length, (a, b) -> a.value - b.value);

        for(ListNode node: lists) {
            if(node!=null)
                queue.add(node);
        }

        ListNode dummy = new ListNode(0);
        ListNode tail=dummy;

        while(!queue.isEmpty()) {
            tail.next = queue.poll();
            tail = tail.next;

            if(tail.next!=null) {
                queue.add(tail.next);
            }
        }

        return dummy.next;
    }

    public static void main(String[] args) {
        ListNode lists[] = new ListNode[3];

        LinkedList ll = new LinkedList();
        lists[0] = ll.create(new int[] {1});

        LinkedList ll2 = new LinkedList();
        lists[1] = ll2.create(new int[] {1,3,4});

        LinkedList ll3 = new LinkedList();
        lists[2] = ll3.create(new int[] {2,6});

        MergekSortedLists obj = new MergekSortedLists();

        // ListNode res = obj.mergeKLists(lists);

        // while(res!=null) {
        //     System.out.println(res.value);
        //     res = res.next;
        // }

        ListNode res2 = obj.mergeKLists2(lists);

        while(res2!=null) {
            System.out.println(res2.value);
            res2 = res2.next;
        }
    }
}