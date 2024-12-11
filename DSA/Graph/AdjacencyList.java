package Graph;

import java.util.ArrayList;
import java.util.List;

public class AdjacencyList {
    List<List<Integer>> adj;
    List<List<int[]>> adjWeight;

    private final boolean isDirected;
    private final boolean isWeighted;

    // Constructor for unweighted, undirected graph
    public AdjacencyList(int V) {
        this(V, false, false);
    }

    // Constructor for directed, unweighted graph
    public AdjacencyList(int V, boolean isDirected) {
        this(V, isDirected, false);
    }

    // Constructor for any graph type
    public AdjacencyList(int V, boolean isDirected, boolean isWeighted) {
        this.isDirected = isDirected;
        this.isWeighted = isWeighted;

        if (isWeighted) {
            adjWeight = new ArrayList<>(V);
            for (int i = 0; i < V; i++) {
                adjWeight.add(new ArrayList<>());
            }
        } else {
            adj = new ArrayList<>(V);
            for (int i = 0; i < V; i++) {
                adj.add(new ArrayList<>());
            }
        }
    }

    // Add edge for unweighted graph
    public void addEdge(int i, int j) {
        ensureUnweighted();
        addEdgeHelper(adj.get(i), j);
        if (!isDirected) {
            addEdgeHelper(adj.get(j), i);
        }
    }

    // Add edge for weighted graph
    public void addEdge(int i, int j, int weight) {
        ensureWeighted();
        addEdgeHelper(adjWeight.get(i), j, weight);
        if (!isDirected) {
            addEdgeHelper(adjWeight.get(j), i, weight);
        }
    }

    // Display adjacency list
    public void displayAdjList() {
        if (isWeighted) {
            for (int i = 0; i < adjWeight.size(); i++) {
                System.out.print(i + ": ");
                for (int[] edge : adjWeight.get(i)) {
                    System.out.print(edge[0] + " @ " + edge[1] + ", ");
                }
                System.out.println();
            }
        } else {
            for (int i = 0; i < adj.size(); i++) {
                System.out.print(i + ": ");
                for (int j : adj.get(i)) {
                    System.out.print(j + " ");
                }
                System.out.println();
            }
        }
    }

    // Private helper to add unweighted edges
    private void addEdgeHelper(List<Integer> list, int vertex) {
        list.add(vertex);
    }

    // Private helper to add weighted edges
    private void addEdgeHelper(List<int[]> list, int vertex, int weight) {
        list.add(new int[]{vertex, weight});
    }

    // Ensure the graph is unweighted
    private void ensureUnweighted() {
        if (isWeighted) {
            throw new UnsupportedOperationException("This graph is weighted. Use addEdge(i, j, weight) instead.");
        }
    }

    // Ensure the graph is weighted
    private void ensureWeighted() {
        if (!isWeighted) {
            throw new UnsupportedOperationException("This graph is unweighted. Use addEdge(i, j) instead.");
        }
    }

    public static void main(String[] args) {
        int V = 5;

        // 1. Unweighted Undirected Graph
        AdjacencyList unweightedUndirected = new AdjacencyList(V);
        unweightedUndirected.addEdge(0, 1);
        unweightedUndirected.addEdge(0, 2);
        unweightedUndirected.addEdge(1, 3);
        unweightedUndirected.addEdge(3, 4);
        System.out.println("Unweighted Undirected Graph:");
        unweightedUndirected.displayAdjList();

        // 2. Unweighted Directed Graph
        AdjacencyList unweightedDirected = new AdjacencyList(V, true);
        unweightedDirected.addEdge(0, 1);
        unweightedDirected.addEdge(0, 2);
        unweightedDirected.addEdge(1, 3);
        unweightedDirected.addEdge(3, 4);
        System.out.println("\nUnweighted Directed Graph:");
        unweightedDirected.displayAdjList();

        // 3. Weighted Undirected Graph
        AdjacencyList weightedUndirected = new AdjacencyList(V, false, true);
        weightedUndirected.addEdge(0, 1, 5);
        weightedUndirected.addEdge(0, 2, 10);
        weightedUndirected.addEdge(1, 3, 15);
        weightedUndirected.addEdge(3, 4, 20);
        System.out.println("\nWeighted Undirected Graph:");
        weightedUndirected.displayAdjList();

        // 4. Weighted Directed Graph
        AdjacencyList weightedDirected = new AdjacencyList(V, true, true);
        weightedDirected.addEdge(0, 1, 5);
        weightedDirected.addEdge(0, 2, 10);
        weightedDirected.addEdge(1, 3, 15);
        weightedDirected.addEdge(3, 4, 20);
        System.out.println("\nWeighted Directed Graph:");
        weightedDirected.displayAdjList();
    }
}
