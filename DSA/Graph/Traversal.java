package Graph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Traversal {

    // neighbours first
    static void bfs(List<List<Integer>> adj, int start) {
        Queue<Integer> q = new LinkedList<>();
        boolean visited[] = new boolean[adj.size()];
        visited[start] = true;

        q.add(start);
        while(!q.isEmpty()) {
            int curr = q.poll();
            System.out.println(curr+" ");

            for(int x : adj.get(curr)) {
                if(!visited[x]) {
                    visited[x] = true;
                    q.add(x);
                }
            }
        }
    }

    static void dfs(List<List<Integer>> adj, boolean visited[], int curr) {
        if(visited[curr])
            return;

        visited[curr] = true;
        System.out.println(curr);

        for(int x : adj.get(curr)) {
            if(!visited[x])
                dfs(adj, visited, x);
        }
    }

    static void addEdge(List<List<Integer>> adj, int u, int v) {
        adj.get(u).add(v);
        adj.get(v).add(u); // Undirected graph
    }

    public static void main(String[] args) {
      
        // Number of vertices in the graph
        int V = 5;
        
        // Adjacency list representation of the graph
        List<List<Integer>> adj = new ArrayList<>(V);
        for (int i = 0; i < V; i++) {
            adj.add(new ArrayList<>());
        }
        
        // Add edges to the graph
        addEdge(adj, 0, 1);
        addEdge(adj, 0, 2);
        addEdge(adj, 1, 3);
        addEdge(adj, 1, 4);
        addEdge(adj, 2, 4);
        
        // Perform BFS traversal starting from vertex 0
        System.out.println("BFS starting from 0:");
        bfs(adj, 0);
        boolean[] visited = new boolean[V];
        dfs(adj, visited, 0);
    }
}
