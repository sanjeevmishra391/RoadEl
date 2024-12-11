package Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;

public class ShortestPath {

    static class Pair {
        int vertex, dist;

        Pair(int vertex, int dist) {
            this.vertex = vertex;
            this.dist = dist;
        }
    }


    static void dfs(List<List<int[]>> adj, int vertex, boolean[] visited, Stack<Integer> stack) {
        if(visited[vertex])
            return;

        visited[vertex] = true;

        for(int i[] : adj.get(vertex)) {
            dfs(adj, i[0], visited, stack);
        }

        stack.push(vertex);
    }

    static int[] viaTopologicalSort(List<List<int[]>> adj, int v, int start) {
        int[] dist = new int[v];
        Arrays.fill(dist, Integer.MAX_VALUE);

        Stack<Integer> sorted = new Stack<>();
        boolean visited[] = new boolean[v];

        for(int i=0; i<v; i++) {
            if(!visited[i]) {
                dfs(adj, i, visited, sorted);
            }
        }

        dist[start] = 0;
        while(!sorted.isEmpty()) {
            int vertex = sorted.pop();
            if(dist[vertex] != Integer.MAX_VALUE)
                for(int neigh[] : adj.get(vertex)) {
                    if(dist[neigh[0]] > dist[vertex] + neigh[1]) {
                        dist[neigh[0]] = dist[vertex] + neigh[1];
                    }
                }
        }

        return dist;
    }

    static int[] shortestPathInUndirectedUnitWeightGraph(List<List<Integer>> adj, int v, int start) {
        int dist[] = new int[v];
        Arrays.fill(dist, Integer.MAX_VALUE);
        boolean visited[] = new boolean[v];

        Queue<int[]> q = new LinkedList<>();
        dist[start] = 0;
        q.add(new int[]{start, 0});
        visited[start] = true;
        
        while(!q.isEmpty()) {
            int vertex[] = q.remove();
            for(int neigh : adj.get(vertex[0])) {
                if(visited[neigh])
                    continue;
                
                if(dist[neigh] > vertex[1]+1) {
                    dist[neigh] = vertex[1] + 1;
                }
                
                q.add(new int[]{neigh, dist[neigh]});   
                visited[neigh] = true;
            }
        }

        return dist;
    }

    static int[] dikstraAlog(List<List<int[]>> adj, int v, int start) {
        int dist[] = new int[v];
        Arrays.fill(dist, Integer.MAX_VALUE);
        boolean shortestSet[] = new boolean[v];
        PriorityQueue<Pair> pq = new PriorityQueue<>((a, b) -> a.dist-b.dist);

        pq.add(new Pair(start, 0));
        dist[start] = 0;

        List<List<Integer>> path = new ArrayList<>();
        for(int i=0; i<v; i++) {
            path.add(new ArrayList<Integer>());
        }

        path.get(start).add(start);

        while(!pq.isEmpty()) {
            Pair p = pq.remove();
            shortestSet[p.vertex] = true;

            for(int a[] : adj.get(p.vertex)) {
                if(!shortestSet[a[0]] && dist[a[0]] > p.dist + a[1]) {
                    dist[a[0]] = p.dist + a[1];
                    pq.add(new Pair(a[0], dist[a[0]]));
                    
                    path.get(a[0]).clear(); // Clear old path
                    path.get(a[0]).addAll(path.get(p.vertex)); // Copy current path
                    path.get(a[0]).add(a[0]); 
                }
            }
        }

        System.out.println(Arrays.toString(dist));


        for(List<Integer> a : path) {
            System.out.println(a);
        }

        return dist;
    }

    static int[] bellmanFordAlgo(List<List<int[]>> adj, int v, int start) {
        int dist[] = new int[v];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        for(int i=0; i<v; i++) {
            for(int itr = 0; itr<adj.size(); itr++) {
                for(int[] a : adj.get(itr)) {
                    if(dist[itr]!=Integer.MAX_VALUE && dist[a[0]] > dist[itr] + a[1]) {
                        dist[a[0]] = dist[itr] + a[1];
                    }
                }
            }
        }

        System.out.println(Arrays.toString(dist));

        return dist;
    }

    static int[][] floydWarshallAlgo(int[][] graph) {
        int V = graph.length;

        for(int i=0; i<V; i++) {
            for(int j=0; j<V; j++) {
                if(i!=j && graph[i][j] == 0) {
                    graph[i][j] = 99999;
                }
            }
        }

        for(int k=0; k<V; k++) {
            for(int i=0; i<V; i++) {
                for(int j=0; j<V; j++) {
                    if(graph[i][j] > graph[i][k] + graph[k][j]) {
                        graph[i][j] = graph[i][k] + graph[k][j];
                    }
                }
            }
        }

        for(int a[] : graph)
            System.out.println(Arrays.toString(a));
        
        return graph;
    }

    public static void main(String[] args) {
        int start = 0;
        AdjacencyList adjWeight = new AdjacencyList(6, true, true);

        adjWeight.addEdge(0, 1, 5);
        adjWeight.addEdge(0, 2, 3);

        adjWeight.addEdge(1, 3, 6);
        adjWeight.addEdge(1, 2, 2);

        adjWeight.addEdge(2, 4, 4);
        adjWeight.addEdge(2, 5, 2);
        adjWeight.addEdge(2, 3, 7);

        adjWeight.addEdge(3, 4, -1);

        adjWeight.addEdge(4, 5, -2);

        adjWeight.displayAdjList();

        System.out.println("Min distance from source " + start);
        System.out.println(Arrays.toString(viaTopologicalSort(adjWeight.adjWeight, 6, start)));

        AdjacencyList undirectedAdj = new AdjacencyList(9);

        undirectedAdj.addEdge(0, 1);
        undirectedAdj.addEdge(0, 3);
        undirectedAdj.addEdge(1, 2);
        undirectedAdj.addEdge(2, 6);
        undirectedAdj.addEdge(3, 4);
        undirectedAdj.addEdge(4, 5);
        undirectedAdj.addEdge(5, 6);
        undirectedAdj.addEdge(6, 7);
        undirectedAdj.addEdge(6, 8);
        undirectedAdj.addEdge(7, 8);

        System.out.println("Undirected unit weight graph shortest path from source " + start);
        System.err.println(Arrays.toString(shortestPathInUndirectedUnitWeightGraph(undirectedAdj.adj, 9, start)));
        
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

        adjUndirectedWeighted.displayAdjList();

        System.out.println("Using Dikstra's algorithm: Starting from "  + start);
        dikstraAlog(adjUndirectedWeighted.adjWeight, 9, start);

        System.out.println("Using Bellman Ford's algorithm: Starting from "  + start);
        bellmanFordAlgo(adjUndirectedWeighted.adjWeight, 9, start);


        AdjacencyMatrix adjMatrix = new AdjacencyMatrix(5, true, true);
        adjMatrix.addEdge(0, 1, 4);
        adjMatrix.addEdge(0, 3, 5);
        adjMatrix.addEdge(1, 2, 1);
        adjMatrix.addEdge(1, 4, 6);
        adjMatrix.addEdge(2, 0, 2);
        adjMatrix.addEdge(2, 3, 3);
        adjMatrix.addEdge(3, 2, 1);
        adjMatrix.addEdge(3, 4, 2);
        adjMatrix.addEdge(4, 0, 1);
        adjMatrix.addEdge(4, 3, 4);
        adjMatrix.displayMatrix();
        System.out.println("Using Floyd Warshall Algorithm");
        floydWarshallAlgo(adjMatrix.matrix);
    }
}
