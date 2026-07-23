# Trie

## Pattern Recognition Triggers
- "Autocomplete / prefix search" → **Trie**
- "Count words with a given prefix" → **Trie**
- "Word search in a dictionary" → **Trie**
- "Longest common prefix" → **Trie**
- "Maximum XOR of two numbers" → **Binary Trie**

## Complexity
| Operation | Time | Space |
|---|---|---|
| Insert | O(L) | O(L × alphabet) |
| Search | O(L) | O(1) |
| Prefix search | O(L) | O(1) |

L = length of word. Alphabet size is typically 26 for lowercase English.

## Standard Trie Implementation
```java
class TrieNode {
    TrieNode[] children = new TrieNode[26];
    boolean isEndOfWord = false;
}

class Trie {
    TrieNode root = new TrieNode();

    void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null)
                node.children[idx] = new TrieNode();
            node = node.children[idx];
        }
        node.isEndOfWord = true;
    }

    boolean search(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null) return false;
            node = node.children[idx];
        }
        return node.isEndOfWord;
    }

    boolean startsWith(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null) return false;
            node = node.children[idx];
        }
        return true;  // isEndOfWord check not needed for prefix
    }
}
```

## Key Interview Problems

| Problem | Key Insight |
|---|---|
| Implement Trie | Standard insert/search/startsWith |
| Word Search II | Build Trie from dictionary, DFS on board |
| Design Search Autocomplete | Trie + store top-3 results at each node |
| Replace Words | Trie of roots, replace each word with shortest matching root |
| Maximum XOR of Two Numbers | Binary Trie (store bits 0/1), greedily pick opposite bit |

## Common Mistakes
- Confusing `search` (exact word) with `startsWith` (prefix only) — `isEndOfWord` matters
- Not initializing `children` array → NullPointerException
- For deletion: need to clean up nodes only if no other word uses them

---

A Trie data structure consists of nodes connected by edges. Each node represents a character or a part of a string. The root node, the starting point of the Trie, represents an empty string. Each edge emanating from a node signifies a specific character. The path from the root to a node represents the prefix of a string stored in the Trie.

 