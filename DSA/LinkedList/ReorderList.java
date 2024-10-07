// https://leetcode.com/problems/reorder-list/description/

package LinkedList;

public class ReorderList {

    static LinkedList ll;

    static void reorderList(ListNode head) {
        if(head == null || head.next == null)
            return;
        
        ListNode middle = ll.middleNode(head);
        ListNode rev = ll.reverseList(middle);

        ListNode start = head, secondItr = rev; 
        while(start!=null && secondItr!=null) {
            ListNode temp = start.next;
            start.next = secondItr;
            secondItr = secondItr.next;
            start.next.next = temp;
            start = temp;
        }

        if(start!=null)
            start.next = null;
        
    }


    public static void main(String[] args) {
        ll = new LinkedList();
        int arr[] = {1,2,3,4,5,6,7};
        
        ListNode head = ll.create(arr);
        ll.printList(head);
        reorderList(head);
        ll.printList(head);
    }
}
