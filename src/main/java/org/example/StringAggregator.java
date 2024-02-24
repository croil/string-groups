package org.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StringAggregator {
    private final Path in;
    private final Path out;
    private final Lexer lexer;
    private final DSU dsu;
    private final List<Map<Double, Integer>> columnData;


    public StringAggregator(Path in, Path out) {
        this.in = in;
        this.out = out;
        this.lexer = new Lexer();
        this.dsu = new DSU();
        this.columnData = new ArrayList<>(16);
    }

    public void aggregate() {
        try (BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8)) {
            forEachLine(reader, this::processLine);
            columnData.clear();
        } catch (IOException e) {
            System.err.printf("I/O exception occurs: %s%n", e.getMessage());
        }

        try (
                BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8);
                BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)
        ) {
            forEachLine(reader, dsu::setUnion);
            var unions = dsu.unions();
            writeGroupsNumber(writer, unions);
            unions.forEach(withCounter((i, group) -> writeGroup(writer, i + 1, group)));
        } catch (IOException e) {
            System.err.printf("I/O exception occurs: %s%n", e.getMessage());
        }


    }

    private void writeGroupsNumber(BufferedWriter writer, Collection<Set<String>> unions) throws IOException {
        long size = unions.stream().filter(s -> s.size() > 1).count();
        writer.write(String.format("%s", size));
        writer.newLine();
        writer.newLine();
    }

    private void processLine(int row, String line) {
        if (lexer.load(line)) {
            dsu.makeSet(row);
            String strWord;
            int column = 0;
            while ((strWord = lexer.next()) != null) {
                if (column == columnData.size()) {
                    columnData.add(new HashMap<>());
                }
                var map = columnData.get(column);
                if (!lexer.isBlankWord(strWord)) {
                    double hash = Double.parseDouble(strWord);
                    if (map.containsKey(hash)) {
                        dsu.union(row, map.get(hash));
                    } else {
                        map.put(hash, row);
                    }
                }
                column++;
            }
        }
    }

    private void writeGroup(BufferedWriter writer, int n, Set<String> lines) {
        try {
            writer.write(String.format("Group %s%n", n));
            lines.forEach(line -> {
                try {
                    writer.write(line);
                    writer.newLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static <T> Consumer<T> withCounter(BiConsumer<Integer, T> consumer) {
        AtomicInteger counter = new AtomicInteger(0);
        return value -> consumer.accept(counter.getAndIncrement(), value);
    }

    private static void forEachLine(BufferedReader reader, BiConsumer<Integer, String> consumer) throws
            IOException {
        String line;
        int row = 0;
        while ((line = reader.readLine()) != null) {
            consumer.accept(row, line);
            row++;
        }
    }


}
