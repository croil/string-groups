package org.example;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public class Main {
    private final static Path root = Path.of("").toAbsolutePath();

    private static boolean invalidArgs(String[] args) {
        if (args == null) {
            System.err.println("Input arguments are null array");
            return true;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("There is null in input arguments");
            return true;
        }
        if (args.length < 1) {
            System.err.printf("Lack of arguments. %nExpected at least: 1%nActual: %s%n", args.length);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        if (invalidArgs(args)) return;
        try {
            Path in = root.resolve(args[0]);
            Path out = root.resolve("out.txt");
            if (Files.notExists(in)) {
                System.err.printf("Input file by path %s does not exist", in);
            } else {
                StringAggregator aggregator = new StringAggregator(in, out);
                long start = System.currentTimeMillis();
                aggregator.aggregate(GroupType.UNION);
                long end = System.currentTimeMillis();
                System.out.printf("Algorithm running time: %s seconds%n", (end - start) / 1000d);
            }
        } catch (InvalidPathException ex) {
            System.err.printf("Invalid path in file %s on position %s", ex.getInput(), ex.getIndex());
        }
    }
}