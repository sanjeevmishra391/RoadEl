package LinkedList;

import java.util.HashMap;
import java.util.Map;

/*
 * 1. Create Double Linked List(maintain head and tail) and HashMap
 * 2. Put: 
 *      2.1: Check if key if present in map
 *      2.2: If key is not found:
 *          2.2.1: Create a node and insert it into map with key as key and value as Node
 *          2.2.2: Add node to the start of linked list
 *          2.2.3: Increase count
 *          2.2.4: If count is more than capacity, remove element from end and remove from map.
 *      2.3: If key is found:
 *          2.3.1: Remove node from linked list and insert it in the front (most recent one)
 * 3. Get:
 *      3.1: If key is not present in map, return -1
 *      3.2: If key is present in map, remove it from linked list and move it to front
 */

public class LRUCache {
    private class Node {
        int key, value;
        Node prev, next;
        Node(int k, int v) {
            this.key = k;
            this.value = v;
        }

        Node() {
            this(0,0);
        }
    }

    private int capacity, count;
    private Map<Integer, Node> map;
    private Node head, tail;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.count = 0;
        map = new HashMap<>();
        head = new Node();
        tail = new Node();
        head.next = tail;
        tail.prev = head;
    }

    private void update(Node node) {
        delete(node);
        add(node);
    }

    private void delete(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void add(Node node) {
        node.next = head.next;
        node.next.prev= node;
        node.prev = head;
        head.next = node;
    }
    
    public int get(int key) {
        Node n = map.get(key);
        if(n == null)
            return -1;

        update(n);
        return n.value;
    }
    
    public void put(int key, int value) {
        Node n = map.get(key);
        if(n==null) {
            n = new Node(key, value);
            map.put(key, n);
            add(n);
            ++count;
        } else {
            n.value = value;
            update(n);
        }

        if(count>capacity) {
            Node del = tail.prev;
            delete(del);
            map.remove(del.key);
            --count;
        }
    }

    void print() {
        Node temp = head;
        while(temp!=tail) {
            System.out.printf("%d -> ", temp.value);
            temp = temp.next;
        }
        System.out.println();
    }

    public static void main(String[] args) {
        LRUCache cache = new LRUCache(2);
        cache.put(1, 1);
        cache.print();
        cache.put(2,2);
        cache.print();
        System.out.println(cache.get(1));
        cache.print();
        cache.put(3,3);
        cache.print();
    }
}
