package Graph;

public class AdjacencyMatrix {
    int[][] matrix;
    boolean isDirected, isWeighted;

    AdjacencyMatrix(int V) {
        this(V, false, false);
    }

    AdjacencyMatrix(int V, boolean isDirected) {
        this(V, isDirected, false);
    }

    AdjacencyMatrix(int V, boolean isDirected, boolean isWeighted) {
        matrix = new int[V][V];
        this.isDirected = isDirected;
        this.isWeighted = isWeighted;
    }

    void addEdge(int i, int j) {
        ensureUnweighted();
        matrix[i][j] = 1;
        if(!isDirected) {
            addEdgeHelper(j, i, 1);
        }
    }

    void addEdge(int i, int j, int weight) {
        ensureWeighted();
        matrix[i][j] = weight;
        if(!isDirected) {
            addEdgeHelper(j, i, weight);
        }
    }

    // Ensure the graph is unweighted
    private void ensureUnweighted() {
        if(isWeighted) {
            throw new UnsupportedOperationException("This graph is weighted. Use addEdge(i, j, weight) instead.");
        }
    }

    // Ensure the graph is weighted
    private void ensureWeighted() {
        if(!isWeighted) {
            throw new UnsupportedOperationException("This graph is unweighted. Use addEdge(i, j) instead.");
        }
    }

    private void addEdgeHelper(int source, int dest, int weight) {
        matrix[source][dest] = weight;
    }

    public void displayMatrix() {
        for (int[] row : matrix) {
            for (int val : row) {
                System.out.print(val + " ");
            }
            System.out.println();
        }
    }
    public static void main(String[] args) {
        // Create a graph with 4 vertices and no edges
        // Note that all values are initialized as 0
        AdjacencyMatrix adj = new AdjacencyMatrix(4);

        // Now add edges one by one
        adj.addEdge(0, 1);
        adj.addEdge(0, 2);
        adj.addEdge(1, 2);
        adj.addEdge(2, 3);

        /* Alternatively we can also create using below
           code if we know all edges in advance

         int[][] mat = {{ 0, 1, 0, 0 },
                        { 1, 0, 1, 0 },
                        { 0, 1, 0, 1 },
                        { 0, 0, 1, 0 } }; */

        System.out.println("Adjacency Matrix Representation");
        adj.displayMatrix();
    }
}
