package Graph;

import java.util.Arrays;

public class UnionFind {
    int parent[], rank[], size[];

    UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        size = new int[n];

        for(int i=0; i<n; i++)
            parent[i] = i;

        Arrays.fill(size, 1);
    }

    int find(int i) {
        if(parent[i] == i)
            return i;

        parent[i] = find(parent[i]);
        return parent[i];
    }

    void unionByRank(int i, int j) {
        int iPnt = find(i), jPnt = find(j);

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

    void unionBySize(int i, int j) {
        int iPnt = find(i), jPnt = find(j);

        if(iPnt == jPnt)
            return;

        int iSize = size[i], jSize = size[j];

        if(iSize < jSize) {
            parent[iPnt] = jPnt; 
            size[jPnt] += size[iPnt];
        } else {
            parent[jPnt] = iPnt;
            size[iPnt] += size[jPnt];
        }
    }
}
