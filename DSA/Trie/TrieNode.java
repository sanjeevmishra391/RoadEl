package Trie;

import java.util.Arrays;
import java.util.List;

public class TrieNode {
    TrieNode[] child;
    boolean wordEnd;
    TrieNode() {
        child = new TrieNode[26];
        wordEnd = false;
    }

    static void insertKey(TrieNode root, String key) {
        TrieNode curr = root;

        for(char c: key.toCharArray()) {
            if(curr.child[c - 'a'] == null) {
                TrieNode newNode = new TrieNode();
                curr.child[c - 'a'] = newNode;
            }

            curr = curr.child[c - 'a'];
        }

        curr.wordEnd = true;
    }

    static boolean searchKey(TrieNode root, String key) {
        TrieNode curr = root;

        for(char c : key.toCharArray()) {
            if(curr.child[c-'a'] == null)
                return false;
            
            curr = curr.child[c-'a'];
        }

        return curr.wordEnd;
    }

    public static void main(String[] args) {
        TrieNode root = new TrieNode();
        List<String> arr = Arrays.asList(
            "and", "ant", "do", "geek", "dad", "ball");
        for (String s : arr) {
            insertKey(root, s);
        }

        // One by one search strings
        List<String> searchKeys = 
              Arrays.asList("do", "gee", "bat");
        for (String s : searchKeys) {
            System.out.println("Key : " + s);
            if (searchKey(root, s)) 
                System.out.println("Present");
            else 
                System.out.println("Not Present");        
        } 
    }
}
