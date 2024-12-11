package Graph;

import java.util.Arrays;

public class UnionFindByRank {
    int rank[], parent[];

    UnionFindByRank(int size) {
        rank = new int[size];
        parent = new int[size];
        for(int i=0; i<size; i++)
            parent[i] = i;
    }

    int find(int i) {
        if(parent[i] == i)
            return i;

        parent[i] = find(parent[i]);
        return parent[i];
    }

    void unionByRank(int i, int j) {
        int iPnt = find(i);
        int jPnt = find(j);

        if(iPnt == jPnt)
            return;

        int iRank = rank[i], jRank = rank[j];

        if(iRank < jRank) {
            parent[iPnt] = jPnt; 
        } else if(jRank < iRank) {
            parent[jPnt] = iPnt;
        } else {
            parent[iPnt] = jPnt;
            rank[jPnt]++;
        }
    }

    public static void main(String[] args) {
        UnionFindByRank uf = new UnionFindByRank(5);
        uf.unionByRank(0, 1);
        uf.unionByRank(2, 3);
        uf.unionByRank(1, 3);

        System.out.println(Arrays.toString(uf.parent));
        System.out.println(uf.find(0));
        System.out.println(Arrays.toString(uf.parent));

    }
}