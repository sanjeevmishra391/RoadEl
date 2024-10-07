package LinkedList;

public class LinkedList {
    ListNode create(int arr[]) {
        ListNode head = null, temp = null;

        for(int i=0; i<arr.length; i++) {
            ListNode newNode = new ListNode(arr[i]);
            if(i == 0) {
                head = newNode;
                temp = newNode;
                continue;
            }
            temp.next = newNode;
            temp = newNode;
        }

        return head;
    }

    void printList(ListNode head) {
        ListNode temp = head;
        while(temp!=null) {
            System.out.printf("%d -> ", temp.value);
            temp = temp.next;
        }
        System.out.println("null");
    }

    ListNode reverseList(ListNode head) {
        ListNode prev = null, curr = head, agla = head;

        while(curr!=null) {
            agla = curr.next;
            curr.next = prev;
            prev = curr;
            curr = agla;
        }

        return prev;
    }

    ListNode middleNode(ListNode head) {
        ListNode slow = head, fast = head;
        while(fast!=null && fast.next!=null) {
            slow = slow.next;
            fast = fast.next.next;
        }

        return slow;
    }

    int length(ListNode head) {
        int count = 0;
        if(head==null)
            return count;
        
        ListNode temp = head;
        while(temp!=null) {
            count++;
            temp = temp.next;
        }

        return count;
    }

    ListNode remove(ListNode head, int k) {
        if(k==1)
            return head.next;

        ListNode temp = head;
        k--;
        while(k>1) {
            temp = temp.next;
            k--;
        }
        temp.next = temp.next.next;
        return head;
    }

    boolean hasCycle(ListNode head) {
        if(head == null || head.next == null)
            return false;

        ListNode slow = head, fast = head;
        do {
            slow = slow.next;
            fast = fast.next.next;
        } while(fast!=null && fast.next!=null && slow!=fast);

        return slow == fast;
    }
}
