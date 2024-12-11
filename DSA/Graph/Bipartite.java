package Graph;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Bipartite {

    static boolean isBipartite(List<List<Integer>> adj) {
        Queue<int[]> q = new LinkedList<>();
        HashSet<Integer> blue = new HashSet<>(), visited = new HashSet<>();
        visited.add(0);
        blue.add(0);
        q.add(new int[]{0, 0}); // 0 : blue, 1 : red
        while(!q.isEmpty()) {
            int curr[] = q.remove();
            for(int el : adj.get(curr[0])) {
                if(visited.contains(el)) {
                    if(!((curr[1] == 0 && !blue.contains(el)) || (curr[1] == 1 && blue.contains(el)))) {
                        return false;
                    }
                } else {
                    visited.add(el);
                    q.add(new int[]{el, 1-curr[1]});
                    if(curr[1] == 1) {
                        blue.add(el);
                    }
                }
            }
        }

        return true;
    }
    public static void main(String[] args) {
        AdjacencyList adj = new AdjacencyList(6);
        adj.addEdge(0, 1);
        adj.addEdge(1, 2);
        adj.addEdge(2, 3);
        adj.addEdge(0, 3);
        adj.addEdge(3, 4);
        adj.addEdge(4, 5);
        // adj.addEdge(1, 3);
        // {0 B, 2 B, 4 B}
        // {1 R, 3 R, 5}
        System.out.println(isBipartite(adj.adj));
    }
}