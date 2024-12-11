package Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

public class TopologicalSort {

    static void dfs(List<List<Integer>> adj, int v, HashSet<Integer> visited, Stack<Integer> stack) {
        if(visited.contains(v))
            return;

        visited.add(v);
        for(int i : adj.get(v))
            dfs(adj, i, visited, stack);

        stack.push(v);
    }

    static List<Integer> topologicalSort(List<List<Integer>> adj) {
        Stack<Integer> stack = new Stack<>();
        HashSet<Integer> visited = new HashSet<>();
        for(int i=0; i<adj.size(); i++) {
            if(!visited.contains(i))
                dfs(adj, i, visited, stack);
        }

        List<Integer> ans = new ArrayList<>();
        while(!stack.isEmpty()) {
            ans.add(stack.pop());
        }

        return ans;
    }

    static int[] kahnsAlgo(List<List<Integer> > adj, int V) {
        // Array to store indegree of each vertex
        int[] indegree = new int[V];
        for (int i = 0; i < V; i++) {
            for (int it : adj.get(i)) {
                indegree[it]++;
            }
        }

        // Queue to store vertices with indegree 0
        Queue<Integer> q = new LinkedList<>();
        for (int i = 0; i < V; i++) {
            if (indegree[i] == 0) {
                q.offer(i);
            }
        }

        int[] result = new int[V];
        int index = 0;
        while (!q.isEmpty()) {
            int node = q.poll();
            result[index++] = node;

            // Decrease indegree of adjacent vertices as the
            // current node is in topological order
            for (int it : adj.get(node)) {
                indegree[it]--;

                // If indegree becomes 0, push it to the
                // queue
                if (indegree[it] == 0) {
                    q.offer(it);
                }
            }
        }

        // Check for cycle
        if (index != V) {
            System.out.println("Graph contains cycle!");
            return new int[0];
        }

        return result;
    }

    public static void main(String[] args) {
        AdjacencyList adj = new AdjacencyList(6, true);
        adj.addEdge(5, 0);
        adj.addEdge(4, 0);
        adj.addEdge(5, 2);
        adj.addEdge(2, 3);
        adj.addEdge(3, 1);
        adj.addEdge(4, 1);
        System.out.println(topologicalSort(adj.adj).toString());

        System.out.println(Arrays.toString(kahnsAlgo(adj.adj, 6)));
    }
}
