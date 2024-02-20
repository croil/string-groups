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
import java.util.stream.Collectors;

public class StringAggregator {
    private final Path in;
    private final Path out;
    private final Lexer lexer;
    private final DSU dsu;
    private final Map<Integer, Row> rows;
    private final List<Map<Long, Integer>> columnData;


    public StringAggregator(Path in, Path out) {
        this.in = in;
        this.out = out;
        this.lexer = new Lexer();
        this.dsu = new DSU();
        this.rows = new HashMap<>();
        this.columnData = new ArrayList<>(16);
    }

    public void aggregate(GroupType type) {
        try (
                BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8);
                BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8);
        ) {

            String strLine;
            int row = 0;
            while ((strLine = reader.readLine()) != null) {
                if (lexer.load(strLine)) {
                    processLine(row);
                    row++;
                }
            }
            var unions = dsu.unions(rows, type);
            writeGroupsNumber(writer, unions.size());
            unions.forEach(withCounter((i, group) -> writeGroup(writer, i + 1, group)));
        } catch (IOException e) {
            System.err.printf("I/O exception occurs: %s%n", e.getMessage());
        }
    }

    private void writeGroupsNumber(BufferedWriter writer, int size) throws IOException {
        writer.write(String.format("%s", size));
        writer.newLine();
        writer.newLine();
    }

    private void processLine(int row) {
        List<Long> line = new ArrayList<>();
        dsu.makeSet(row);
        String strWord;
        int column = 0;
        while ((strWord = lexer.nextWord()) != null) {
            if (column == columnData.size()) {
                columnData.add(new HashMap<>());
            }
            var map = columnData.get(column);
            long word = strWord.isEmpty() ? 0 : Long.parseLong(strWord);
            line.add(word);
            if (word != 0) {
                if (map.containsKey(word)) {
                    dsu.union(row, map.get(word));
                } else {
                    map.put(word, row);
                }
            }
            column++;
        }
        rows.put(row, new Row(line, line.hashCode()));
    }


    private void writeGroup(BufferedWriter writer, int n, Set<Integer> lines) {
        try {
            writer.write(String.format("Group %s%n", n));
            for (int line : lines) {
                String str = rows.get(line)
                        .hashedRow()
                        .stream()
                        .map(i -> "\"%s\"".formatted(i == 0 ? "" : Long.toString(i)))
                        .collect(Collectors.joining(";"));
                writer.write(str.isEmpty() ? "\"\"" : str);
                writer.newLine();
            }
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static <T> Consumer<T> withCounter(BiConsumer<Integer, T> consumer) {
        AtomicInteger counter = new AtomicInteger(0);
        return value -> consumer.accept(counter.getAndIncrement(), value);
    }


}
