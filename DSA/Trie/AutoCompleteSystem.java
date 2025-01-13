package Trie;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

class Trie {
    Trie[] children;
    String word;
    int times;

    Trie() {
        children = new Trie[27];
        word = "";
        times = 0;
    }

    void insert(String key, int t) {
        // to insert the string check if current char's position in children is empty of filled
        Trie curr = this;
        for(char c : key.toCharArray()) {
            int idx = c == ' ' ? 26 : c - 'a';
            if(curr.children[idx] == null) {
                Trie temp = new Trie();
                curr.children[idx] = temp;
            }
            curr = curr.children[idx];
        }
        curr.times += t;
        curr.word = key;
    }

    Trie search(String key) {
        Trie curr = this;
        for(char c : key.toCharArray()) {
            int idx = c == ' ' ? 26 : c - 'a';
            if(curr.children[idx] == null)
                return null;
            curr = curr.children[idx];
        }
        return curr;
    }
}

public class AutoCompleteSystem {
    private Trie trie;
    private StringBuilder searchParam;

    AutoCompleteSystem(String sentences[], int times[]) {
        trie = new Trie();
        searchParam = new StringBuilder();

        for(int i=0; i<sentences.length; i++) {
            trie.insert(sentences[i], times[i]);
        }
    }

    public List<String> input(char c) {
        // return the hottest sentence.
        // if character is # that marks the end of word search

        List<String> res = new ArrayList<>();

        if(c == '#') {
            trie.insert(searchParam.toString(), 1);
            searchParam = new StringBuilder();
            return res;
        }

        searchParam.append(c);

        Trie node = trie.search(searchParam.toString());
        if(node == null)
            return res;

        // sorting accourting to frequency; if it's same then sort according to word
        PriorityQueue<Trie> pq = new PriorityQueue<>((a, b) ->
                                    a.times == b.times ? b.word.compareTo(a.word) : a.times - b.times );


        dfs(node, pq);

        while(!pq.isEmpty()) {
            res.add(0, pq.poll().word);
        }

        return res;
    }

    private void dfs(Trie node, PriorityQueue<Trie> pq) {
        if(node == null)
            return;

        if(node.times > 0) {
            pq.offer(node);
            if(pq.size() > 3) {
                pq.poll();
            }
        }

        for(Trie next : node.children) {
            dfs(next, pq);
        }
    }
}

class Driver {
    public static void main(String[] args) {
        String sentences[] = {"i love you", "i like you", "au", "fu", "cat"};
        int times[] = {4, 4, 1, 8, 6};

        AutoCompleteSystem acs = new AutoCompleteSystem(sentences, times);
        System.out.println(acs.input('i'));
        System.out.println(acs.input('#'));
    }
}