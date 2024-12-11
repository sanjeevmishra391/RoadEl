package Graph;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Arrays;

public class MinimumSpanningTree {


    static int minKey(int keys[], boolean mstSet[]) {
        int min = Integer.MAX_VALUE, idx = -1;
        for(int i=0; i<keys.length; i++) {
            if(!mstSet[i] && keys[i]<min) {
                min = keys[i];
                idx = i;
            }
        }

        return idx;
    }

    static void primsAlgo(int[][] graph) {
        int V = graph.length;
        boolean[] mstSet = new boolean[V];
        int keys[] = new int[V];
        int parent[] = new int[V];

        Arrays.fill(keys, Integer.MAX_VALUE);
        keys[0] = 0;
        parent[0] = -1;

        for(int i=0; i<V; i++) {
            int u = minKey(keys, mstSet);
            mstSet[u] = true;

            for(int v=0; v<V; v++) {
                if(graph[u][v] != 0 && !mstSet[v] && graph[u][v] < keys[v]) {
                    parent[v] = u;
                    keys[v] = graph[u][v];
                }
            }
        }
        
        System.out.println("From edge: ");
        System.out.println(Arrays.toString(parent));
        System.out.println("to edge: ");
        System.out.println(Arrays.toString(new int[]{0, 1, 2, 3, 4}));
        System.out.println("with weight ");
        System.out.println(Arrays.toString(keys));
    }

    static void kruskalsAlgo(List<List<int[]>> adj, int V) {
        UnionFindByRank uf = new UnionFindByRank(V+1);
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[2] - b[2]);

        for (int i = 0; i < adj.size(); i++) {
            for (int[] edge : adj.get(i)) {
                pq.add(new int[]{i, edge[0], edge[1]});
            }
        }

        int edges[][] = new int[V][3];
        int i=0, totalWeight = 0;
        while(!pq.isEmpty()) {
            int vertex[] = pq.remove();
            int u = vertex[0], v = vertex[1];
            if(uf.find(u) != uf.find(v)) {
                totalWeight += vertex[2]; 
                uf.unionByRank(u, v);
                edges[i++] = vertex;
            }
        }

        
        System.out.println("Total weight : " + totalWeight);
        System.out.println("[u, v, w]");
        for(int a[] : edges)
            System.out.println(Arrays.toString(a));

        
    }
    public static void main(String[] args) {
        AdjacencyMatrix adj = new AdjacencyMatrix(5, false, true);
        adj.addEdge(0, 1, 2);
        adj.addEdge(0, 3, 6);
        adj.addEdge(1, 2, 3);
        adj.addEdge(1, 3, 8);
        adj.addEdge(1, 4, 5);
        adj.addEdge(2, 4, 7);
        adj.addEdge(3, 4, 9);
        adj.displayMatrix();

        System.out.println("\nPrim's Algorithm\n");
        primsAlgo(adj.matrix);


        AdjacencyList adjUndirectedWeighted = new AdjacencyList(9, false, true);

        adjUndirectedWeighted.addEdge(0, 1, 4);
        adjUndirectedWeighted.addEdge(0, 7, 8);
        adjUndirectedWeighted.addEdge(1, 2, 8);
        adjUndirectedWeighted.addEdge(1, 7, 11);
        adjUndirectedWeighted.addEdge(2, 3, 7);
        adjUndirectedWeighted.addEdge(2, 5, 4);
        adjUndirectedWeighted.addEdge(2, 8, 2);
        adjUndirectedWeighted.addEdge(3, 5, 14);
        adjUndirectedWeighted.addEdge(3, 4, 9);
        adjUndirectedWeighted.addEdge(4, 5, 10);
        adjUndirectedWeighted.addEdge(5, 6, 2);
        adjUndirectedWeighted.addEdge(6, 8, 6);
        adjUndirectedWeighted.addEdge(6, 7, 1);
        adjUndirectedWeighted.addEdge(7, 8, 7);

        System.out.println("\nKruskal's Algorithm\n");
        kruskalsAlgo(adjUndirectedWeighted.adjWeight, 9);
    }
}
