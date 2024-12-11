# Graph 

Graph Data Structure is a non-linear data structure consisting of vertices and edges. A Graph is composed of a set of vertices( V ) and a set of edges( E ).

## Terminology:
- **Degree of a vertex:** The degree of a vertex in a graph is the number of edges incident to that vertex. In a directed graph, the indegree of a vertex is the number of incoming edges, and the outdegree is the number of outgoing edges.

- **Spanning Trees and Minimum Spanning Trees:**
A spanning tree of a graph is a subgraph that is a tree and includes all the vertices of the original graph. A minimum spanning tree (MST) is a spanning tree with the minimum possible sum of edge weights.

## Types Of Graphs in Data Structure and Algorithms
1. **Null Graph:**
A graph is known as a null graph if there are no edges in the graph.

2. **Trivial Graph:**
Graph having only a single vertex, it is also the smallest graph possible. 

3. **Undirected Graph:**
A graph in which edges do not have any direction. That is the nodes are unordered pairs in the definition of every edge. 

4. **Directed Graph:**
A graph in which edge has direction. That is the nodes are ordered pairs in the definition of every edge.

5. **Connected Graph:**
The graph in which from one node we can visit any other node in the graph is known as a connected graph. 

6. **Disconnected Graph:**
The graph in which at least one node is not reachable from a node is known as a disconnected graph.

7. **Regular Graph:**
The graph in which the degree of every vertex is equal to K is called K regular graph.

8. **Complete Graph:**
The graph in which from each node there is an edge to each other node.

9. **Cycle Graph:**
The graph in which the graph is a cycle in itself, the minimum value of degree of each vertex is 2. 

10. **Cyclic Graph:**
A graph containing at least one cycle is known as a Cyclic graph.

11. **Directed Acyclic Graph:**
A Directed Graph that does not contain any cycle. 

12. **Bipartite Graph:**
A graph in which vertex can be divided into two sets such that vertex in each set does not contain any edge between them.

13. **Weighted Graph:**
    - A graph in which the edges are already specified with suitable weight is known as a weighted graph. 
    - Weighted graphs can be further classified as directed weighted graphs and undirected weighted graphs. 

## Representation of Graph Data Structure:
There are multiple ways to store a graph: The following are the most common representations.

- [Adjacency Matrix](./AdjacencyMatrix.java)
- [Adjacency List](./AdjacencyList.java)

## Traversal:

1. **Breadth First Search or BFS**

    It begins with a node, then first traverses all its adjacent. Once all adjacent are visited, then their adjacent are traversed. This is different from DFS in a way that closest vertices are visited before others.

    **Initialization**: Enqueue the given source vertex into a queue and mark it as visited.

    **Exploration**: 
    While the queue is not empty:  
    - Dequeue a node from the queue and visit it (e.g., print its value).  
    - For each unvisited neighbor of the dequeued node:
        - Enqueue the neighbor into the queue.  
        - Mark the neighbor as visited.  

    **Termination**: Repeat step 2 until the queue is empty.
    This algorithm ensures that all nodes in the graph are visited in a breadth-first manner, starting from the starting node.


## Bipartite Graph

[↗︎](./Bipartite.java)

A bipartite graph can be colored with two colors such that **no two adjacent vertices share the same color**. This means we can divide the graph’s vertices into two distinct sets where:

- All edges connect vertices from one set to vertices in the other set.
- No edges exist between vertices within the same set.
- No odd-length cycles: A bipartite graph cannot contain any odd-length cycles, as this would require vertices from the same set to be connected by an edge. So, graphs with even length cycle are bipartite.
- Linear graphs are bipartite
- Maximum degree is bounded by the size of the smaller set: The maximum degree of a vertex in a bipartite graph is equal to the size of the smaller set.
- Coloring with two colors: A bipartite graph can be colored with two colors,, such that no adjacent vertices have the same color.

### How to identify Bipartite Graph?
To identify whether a given graph is bipartite, you can use the following algorithm:

- Choose any vertex in the graph and assign it to one of the two sets, say X.
- Assign all of its neighbors to the other set, say Y.
- For each vertex in set Y, assign all their unassigned neighbors to set X, and for each vertex in set X, assign all their unassigned neighbors to set Y.
- Check if any two adjacent vertices are in the same set. If yes, then the graph is not bipartite. Otherwise, it is bipartite.

## Topological Sort

[↗︎](./TopologicalSort.java)

- It is a linear ordering of vertices such that for every directed edge u-v, vertex u comes before v in the ordering.  
- Can only be implemented for Directed Acyclic Graph.
- Makes use of stack


### Kahn's Algorithm

Kahn’s Algorithm for Topological Sorting is a method used to order the vertices of a directed graph in a linear order such that for every directed edge from vertex A to vertex B, A comes before B in the order. The algorithm works by repeatedly finding vertices with no incoming edges, removing them from the graph, and updating the incoming edges of the remaining vertices. This process continues until all vertices have been ordered.

**Algorithm:**
1. Calculate in-degree of all vertices.
2. Add all vertices for which in-degree is 0.
3. While queue is not empty, remove vertice, add to result, for each child decrement in-degree and if in-degree is 0 then add to queue.
4. If result index and total vertices are not equal then cycle exists, hence topological sort is not possible.

## Shortest Path
[↗︎](./ShortestPath.java)

### 1. Shortest path in DAG

If the graph is directed and acyclic then shortest path can be calculated using topological sort.

### 2. [Dikstra's Algorithm](https://www.geeksforgeeks.org/dijkstras-shortest-path-algorithm-greedy-algo-7/)

Finds the shortest path from the source to all other vertices, but doesnot work if edge weights are negative.
- Maintain a set of included vertices into shorted path.  
- Use priority queue to get the nearest vertext which is not included in the shortest path.

**Why Dijkstra’s Algorithms fails for the Graphs having Negative Edges ?**  
The problem with negative weights arises from the fact that Dijkstra’s algorithm assumes that once a node is added to the set of visited nodes, its distance is finalized and will not change. However, in the presence of negative weights, this assumption can lead to incorrect results.

### 3. [Bellman Ford Algorithm](https://www.geeksforgeeks.org/bellman-ford-algorithm-dp-23/?ref=next_article)

- Finds the shortest path from the source to all other vertices, even with negative edge weights.  
- It detects negative weight cycle. A negative weight cycle is a cycle in a graph, whose sum of edge weights is negative. If you traverse the cycle, the total weight accumulated would be less than zero. In the presence of negative weight cycle in the graph, the shortest path doesn’t exist because with each traversal of the cycle shortest path keeps decreasing.

### 4. [Flyod Warshall Algorithm](https://www.geeksforgeeks.org/floyd-warshall-algorithm-dp-16/?ref=next_article)

- Multi source shortest path findgin algorithm
- Detects negative weight cycles

## [Spanning Tree](https://www.geeksforgeeks.org/spanning-tree/)

A spanning tree is a subset of Graph G, such that all the vertices are connected using minimum possible number of edges. Hence, a spanning tree does not have cycles and a graph may have more than one spanning tree.

- For a connected graph having N vertices then the number of edges in the spanning tree for that graph will be N-1.
- A Spanning tree does not have any cycle.
- **Cayley's Formula**: It states that the number of spanning trees in a complete graph with N vertices is N^{N-2}.

## Minimum Spanning Tree

The weight of a spanning tree is determined by the sum of weight of all the edge involved in it.

> A minimum spanning tree (MST) is defined as a spanning tree that has the minimum weight among all the possible spanning trees.

### 1. [Prim's MST Algorithm](https://www.geeksforgeeks.org/prims-minimum-spanning-tree-mst-greedy-algo-5/)

- Maintain ```keys``` and ```parents``` array, where ```keys``` will be initialised with infinity and will store the edges from vertex i.
- At each step find the minimal edge which is not in the minimum spanning tree set and include it in the set. 
- Update the neighbour edges of the selected vertex and parent of neighbours.

### 2. [Kruskal's Algorithm](https://www.geeksforgeeks.org/kruskals-minimum-spanning-tree-algorithm-greedy-algo-2/)

- Sort according to edges in non-desc order
- Get the minimum weight edge and if does not creates cycle then add in minimum spannig tree
- Repeat second step for v-1 edges.

## Disjoint sets / Union Find
[↗︎](./UnionFind.java)

Two sets are called disjoint sets if they don't have any element in common, the intersection of sets is a null set.

### Operations on Disjoint Set Data Structures:
1. **Find** : Can be implemented by recursively traversing the parent array until we hit a node that is the parent of itself.
2. **Union** : It takes two elements as input and finds the representatives of their sets using the Find operation, and finally puts either one of the trees (representing the set) under the root node of the other tree.

[Simple Union Find](./UnionFindSimple.java)

### Optimizations (Union by Rank / Size)

The efficiency depends heavily on which tree get attached to the other. There are 2 ways in which it can be done. First is Union by Rank, which considers height of the tree as the factor and Second is Union by Size, which considers size of the tree as the factor while attaching one tree to the other . This method along with Path Compression gives complexity of nearly constant time.

**1. Union by Rank** [↗︎](./UnionFindByRank.java)

- If the rank of one set s1 is less than the rank of other set s2, then it’s best to move s1 under s2, because that won’t change the rank of s2.
- If the ranks are equal, it doesn’t matter which tree goes under the other, but the rank of the result will always be one greater than the rank of the trees.

**2. Union By Size** [↗︎](./UnionFindBySize.java)

- If i is a representative of a set, size[i] is the number of the elements in the tree representing the set. 
- If the size of set s1 is less than the size of set s2, then it’s best to move s1 under s2 and increase size of s2 by size of s1.

## Strongly Connected Components

In a **directed graph**, a Strongly Connected Component is a subset of vertices where every vertex in the subset is reachable from every other vertex in the same subset by traversing the directed edges. 

### [Kosaraju’s Algorithm](./StronglyConnectedComponent.java)

- DFS on Original Graph: Record finish times.
- Transpose the Graph: Reverse all edges.
- DFS on Transposed Graph: Process nodes in order of decreasing finish times to find SCCs.

## Problems:

[Number of islands](https://leetcode.com/problems/number-of-islands/description/)  
[Rotten Oranges](https://leetcode.com/problems/rotting-oranges/)  
[01 Matrix](https://leetcode.com/problems/01-matrix/description/)  
[Surrounded Regions](https://leetcode.com/problems/surrounded-regions/description/)  
[Number of Enclaves](https://leetcode.com/problems/number-of-enclaves/description/)  
[Find Eventual Safe States](https://leetcode.com/problems/find-eventual-safe-states/description/)  
[Course Schedule I](https://leetcode.com/problems/course-schedule/description/)  
[Course Schedule II](https://leetcode.com/problems/course-schedule-ii/description/)  
[Path With Minimum Effort](https://leetcode.com/problems/path-with-minimum-effort/description/)  
[Cheapest Flights Within K Stops](https://leetcode.com/problems/cheapest-flights-within-k-stops/description/)  
[Find the City With the Smallest Number of Neighbors at a Threshold Distance](https://leetcode.com/problems/find-the-city-with-the-smallest-number-of-neighbors-at-a-threshold-distance/description/)  
[Number of provinces](https://leetcode.com/problems/number-of-provinces/description/) : Disjoint set problem  
[Number of Operations to Make Network Connected](https://leetcode.com/problems/number-of-operations-to-make-network-connected/description/)   
[Accounts Merge](https://leetcode.com/problems/accounts-merge/description/)  
[Making a Large Island](https://leetcode.com/problems/making-a-large-island/description/)  
