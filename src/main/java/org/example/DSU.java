package org.example;

import java.util.*;
import java.util.stream.IntStream;

public class DSU {
    private final int[] parent;
    private final int[] sizes;
    private final int capacity = 1000000;

    private final Map<Integer, Set<Integer>> sets;

    private final Map<Integer, Set<Integer>> hashes;

    private int size = 0;

    public DSU() {
        this.parent = new int[capacity];
        this.sizes = new int[capacity];
        this.sets = new TreeMap<>((x, y) -> {
            int sizeComparison = Integer.compare(sizes[y], sizes[x]);
            if (sizeComparison == 0) {
                return Integer.compare(x, y);
            }
            return sizeComparison;
        });
        this.hashes = new HashMap<>();
    }

    public void makeSet(int a) {
        parent[a] = a;
        sizes[a] = 1;
        size++;
    }

    public int root(int a) {
        if (a == parent[a]) {
            return a;
        }
        parent[a] = root(parent[a]);
        return parent[a];
    }


    public void union(int a, int b) {
        a = root(a);
        b = root(b);
        if (a != b) {
            if (sizes[a] < sizes[a]) {
                int temp = a;
                a = b;
                b = temp;
            }
            parent[b] = a;
            sizes[a] += sizes[b];
        }
    }

    public Collection<Set<Integer>> unions(Map<Integer, Row> rows, GroupType type) {
        int border = switch (type) {
            case ALL -> 0;
            case UNION -> 1;
        };
        IntStream.range(0, size).forEach(i -> {
            int root = root(i);
            if (sizes[root] > border) {
                sets.putIfAbsent(root, new HashSet<>());
                hashes.putIfAbsent(root, new HashSet<>());
                int hash = rows.get(i).hash();
                if (!hashes.get(root).contains(hash)) {
                    sets.get(root).add(i);
                    hashes.get(root).add(hash);
                }
            }
        });
        return sets.values();
    }
}
