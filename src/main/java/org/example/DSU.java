package org.example;

import java.util.*;

public class DSU {
    private final int[] parent;
    private final int[] sizes;

    private final TreeMap<Integer, Set<String>> sets;

    public DSU() {
        this.parent = new int[15000000];
        this.sizes = new int[15000000];
        this.sets = new TreeMap<>((x, y) -> {
            int sizeComparison = Integer.compare(sizes[y], sizes[x]);
            if (sizeComparison == 0) {
                return Integer.compare(x, y);
            }
            return sizeComparison;
        });
        Arrays.fill(parent, -1);
    }

    public void makeSet(int a) {
        parent[a] = a;
        sizes[a] = 1;
    }

    public int root(int a) {
        if (a == -1 || a == parent[a]) {
            return a;
        }
        parent[a] = root(parent[a]);
        return parent[a];
    }


    public void union(int a, int b) {
        a = root(a);
        b = root(b);
        if (a != b) {
            if (sizes[a] < sizes[b]) {
                int temp = a;
                a = b;
                b = temp;
            }
            parent[b] = a;
            sizes[a] += sizes[b];
        }
    }

    public void setUnion(int row, String line) {
        int root = root(row);
        if (root != -1) {
            sets.putIfAbsent(root, new HashSet<>());
            sets.get(root).add(line);
        }
    }

    public Collection<Set<String>> unions() {
        return sets.values();
    }
}
