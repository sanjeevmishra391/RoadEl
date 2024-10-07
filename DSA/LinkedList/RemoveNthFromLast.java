package LinkedList;

public class RemoveNthFromLast {
    public static void main(String[] args) {
        LinkedList ll = new LinkedList();
        int[] arr = {1,2,3,4,5};
        ListNode head = ll.create(arr);
        ll.printList(head);

        int len = ll.length(head);
        int k = 2;
        int lenFromStart = len - k + 1;

        ll.remove(head, lenFromStart);
        ll.printList(head);
    }
}
