package Graph;

import java.util.Arrays;

public class UnionFindSimple {
    int parent[];

    UnionFindSimple(int V) {
        parent = new int[V];
        for(int i=0; i<V; i++)
            parent[i] = i;
    }

    public int find(int i) {
        if(parent[i] == i)
            return i;

        return find(parent[i]);
    }

    public void union(int i, int j) {
        int iPnt = find(i);
        int jPnt = find(j);
        parent[jPnt] = iPnt;
    }

    public static void main(String[] args) {
        int size = 5;
        UnionFindSimple uf = new UnionFindSimple(size);
        uf.union(1, 2);
        uf.union(3, 4);
        uf.union(4, 1);

        System.out.println(Arrays.toString(uf.parent));
        System.out.println(uf.find(2));
    }
}