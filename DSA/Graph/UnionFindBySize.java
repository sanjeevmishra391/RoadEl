package Graph;

import java.util.Arrays;

public class UnionFindBySize {
    int size[], parent[];

    UnionFindBySize(int n) {
        size = new int[n];
        parent = new int[n];
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

    void unionBySize(int i, int j) {
        int iPnt = find(i);
        int jPnt = find(j);

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

    public static void main(String[] args) {
        UnionFindBySize uf = new UnionFindBySize(5);
        uf.unionBySize(0, 1);
        uf.unionBySize(2, 3);
        uf.unionBySize(1, 3);

        System.out.println(Arrays.toString(uf.parent));
        System.out.println(uf.find(3));
        System.out.println(Arrays.toString(uf.parent));

    }
}
