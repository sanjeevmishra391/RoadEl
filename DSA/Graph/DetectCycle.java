package Graph;

import java.util.HashSet;
import java.util.List;

public class DetectCycle {
    static boolean isCyclicUtil(int v, List<List<Integer>> adj, boolean[] visited, int parent) {
        // Mark the current node as visited
        visited[v] = true;
        // Recur for all the vertices
        // adjacent to this vertex
        for (int i : adj.get(v)) {
          
            // If an adjacent vertex is not visited,
            // then recur for that adjacent
            if (!visited[i]) {
                if (isCyclicUtil(i, adj, visited, v))
                    return true;
            }
          
            // If an adjacent vertex is visited and
            // is not parent of current vertex,
            // then there exists a cycle in the graph.
            else if (i != parent)
                return true;
        }
        return false;
    }

    static boolean undirectedGraphCycleExists(List<List<Integer>> adj) {
        boolean visited[] = new boolean[adj.size()];
        for(int n = 0; n<adj.size(); n++) {
                if(!visited[n])
                    if(isCyclicUtil(n, adj, visited, -1))
                        return true;
        }

        return false;
    }

    static boolean directedIsCyclicUtil(List<List<Integer>> adj, HashSet<Integer> visited, int v) {
        if(visited.contains(v))
            return true;

        visited.add(v);
        for(int i : adj.get(v)) {
            if(directedIsCyclicUtil(adj, visited, i))
                return true;
        }
        visited.remove(v);

        return false;
    }

    static boolean directedGraphCycleExists(List<List<Integer>> adj) {
        HashSet<Integer> visited = new HashSet<>();
        for(int i=0; i<adj.size(); i++) {
            if(!visited.contains(i)) {
                if(directedIsCyclicUtil(adj, visited, i))
                    return true;
            }
        }

        return false;
    }

    public static void main(String[] args) {
        AdjacencyList adjList = new AdjacencyList(3);
        adjList.addEdge(1, 0);
        adjList.addEdge(0, 1);
        adjList.addEdge(0, 2);
        adjList.addEdge(2, 0);
        adjList.addEdge(1, 2);
        adjList.addEdge(2, 1);

        System.out.println(undirectedGraphCycleExists(adjList.adj));

        AdjacencyList adjListDirected = new AdjacencyList(3, true);
        adjListDirected.addEdge(0, 1);
        adjListDirected.addEdge(1, 2);
        // adjListDirected.addEdge(2, 0);

        System.out.println(directedGraphCycleExists(adjListDirected.adj));


    }
}
