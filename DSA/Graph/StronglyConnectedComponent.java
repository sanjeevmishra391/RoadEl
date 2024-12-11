package Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class StronglyConnectedComponent {

    static void kosarajuHelperDFS(List<List<Integer>> adj, int vertex, boolean visited[], Stack<Integer> stack) {
        if(visited[vertex])
            return;

        visited[vertex] = true;
        for(int neigh : adj.get(vertex)) {
            kosarajuHelperDFS(adj, neigh, visited, stack);
        }
        stack.add(vertex);
    }

    static void kosarajuHelperDFS2(List<List<Integer>> adj, int vertex, boolean visited[]) {
        visited[vertex] = true;
        for(int neigh : adj.get(vertex)) {
            if(!visited[neigh]) 
                kosarajuHelperDFS2(adj, neigh, visited);
        }
    }
    
    static void kosarajuAlgo(List<List<Integer>> adj) {
        int V = adj.size();
        boolean visited[] = new boolean[V];
        // 1. dfs and end time
        Stack<Integer> stack = new Stack<>();
        for(int i=0; i<V; i++)
            kosarajuHelperDFS(adj, i, visited, stack);

        // 2. Transpose graph
        List<List<Integer>> adjTrans = new ArrayList<>();
        for(int i=0; i<V; i++)
            adjTrans.add(new ArrayList<>());

        for(int i=0; i<V; i++) {
            for(int neigh : adj.get(i))
                adjTrans.get(neigh).add(i);
        }

        // 3. dfs 
        int scc = 0;
        Arrays.fill(visited, false);
        while(!stack.isEmpty()) {
            int vertex = stack.pop();
            if(!visited[vertex]) {
                scc++;
                kosarajuHelperDFS2(adjTrans, vertex, visited);
            }
        }

        System.out.println("Number of strongly connected components : " + scc);
    }

    public static void main(String[] args) {
        AdjacencyList adj = new AdjacencyList(8, true, false);

        adj.addEdge(0, 1);
        adj.addEdge(1, 2);
        adj.addEdge(2, 0);

        adj.addEdge(1, 3);
        adj.addEdge(3, 4);

        adj.addEdge(4, 5);
        adj.addEdge(5, 6);
        adj.addEdge(6, 7);
        adj.addEdge(7, 4);
        adj.addEdge(7, 5);

        adj.displayAdjList();

        kosarajuAlgo(adj.adj);
    }
}
