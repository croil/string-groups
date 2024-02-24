import org.example.StringAggregator;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringGroupTest {

    private static Path root;

    @BeforeAll
    public static void init() throws IOException {
        root = Files.createTempDirectory("groups" + System.currentTimeMillis());
    }

    @Test
    public void emptyFileTest() {
        Path in = createFile("empty.in");
        Path out = createFile("empty.out");
        test(in, out, Set.of(), Set.of(), 0);
    }

    @Test
    public void oneGroupOneStringTest() {
        Path in = createFile("oneGroupOneString.in");
        Path out = createFile("oneGroupOneString.out");
        Set<String> oneGroupOneString = Collections.singleton(collect(111, 222, 333));
        test(in, out, oneGroupOneString, List.of(oneGroupOneString), 0);
    }

    @Test
    public void oneGroupMultiStringsTest() {
        Path in = createFile("oneGroupMultiStrings.in");
        Path out = createFile("oneGroupMultiStrings.out");
        Set<String> oneGroupMultiStrings = newHashSet(
                collect(111, 222, 333),
                collect(100, 222, 301),
                collect(300, 0, 333)
        );
        test(in, out, oneGroupMultiStrings, List.of(oneGroupMultiStrings), 1);
    }

    @Test
    public void manyAloneGroupsTest() {
        Path in = createFile("manyAloneGroups.in");
        Path out = createFile("manyAloneGroups.out");
        Set<String> manyAloneGroups = Stream.of(
                collect(111, 222, 333),
                collect(222, 333, 111),
                collect(123, 345, 456),
                collect(0),
                collect(0, 0, 0, 0)
        ).collect(Collectors.toCollection(HashSet::new));
        test(in, out, manyAloneGroups, List.of(manyAloneGroups), 0);
    }

    @Test
    public void emptyStringsTest() {
        Path in = createFile("emptyStrings.in");
        Path out = createFile("emptyStrings.out");
        Set<String> emptyStrings = Stream.of(
                collect(0),
                collect(0, 0, 0),
                collect(0, 0),
                collect(0, 0, 0, 0, 0)
        ).collect(Collectors.toCollection(HashSet::new));
        test(in, out, emptyStrings, List.of(emptyStrings), 0);
    }

    @Test
    public void doubleStringTest() {
        Path in = createFile("doubleString.in");
        Path out = createFile("doubleString.out");
        Set<String> doubleString = newHashSet(
                collect(111, 222, 333),
                collect(111, 222, 333),
                collect(222, 333, 444),
                collect(222, 333, 444)
        );
        test(in, out, doubleString, List.of(doubleString), 0);
    }

    @Test
    public void anyGroupsTest() {
        Path in = createFile("anyGroups.in");
        Path out = createFile("anyGroups.out");
        String[] any = new String[]{
                collect(111, 123, 222),
                collect(200, 123, 100),
                collect(300, 0, 100),
                collect(222, 333, 444),
                collect(222, 0, 0),
                collect(555, 666, 777, 888),
                collect(0, 0, 666, 777, 888)
        };
        test(in, out, newHashSet(any),
                List.of(
                        newHashSet(any[0], any[1], any[2]),
                        newHashSet(any[3], any[4]),
                        newHashSet(any[5]),
                        newHashSet(any[6])
                ),
                2);
    }

    private Set<String> newHashSet(String... arr) {
        return new HashSet<>(Arrays.asList(arr));
    }

    @Test
    public void stringsMashupTest() {
        Path in = createFile("stringMashup.in");
        Path out = createFile("stringMashup.out");
        String[] arr = new String[]{
                collect(893281),
                collect(26374, 2379423, 0),
                collect(0, 893281, 0),
                collect(893281, 13, 3301),
                collect(343, 2379423, 115)
        };
        test(in, out, newHashSet(arr),
                List.of(
                        newHashSet(arr[0], arr[3]),
                        newHashSet(arr[1], arr[4]),
                        newHashSet(arr[2])
                ), 2);
    }


    @Test
    public void badDataTest() {
        Path in = createFile("badData.in");
        Path out = createFile("badData.out");
        String[] arr = new String[]{
                "\"8383\"200000741652251\"",
                "\"79855053897\"83100000580443402\";\"200000133000191\"",
                "\"123\";\"123\";",
                ";;;"
        };
        test(in, out, newHashSet(arr), List.of(List.of(";;;", "\"123\";\"123\";")), 0);
    }

    @Test
    @Timeout(30)
    public void bigDataTest() {
        Runtime runtime = Runtime.getRuntime();
        long bytes = runtime.totalMemory() - runtime.freeMemory();
        URL url = getClass().getClassLoader().getResource("Ing.txt");
        if (url == null) {
            System.err.println("Add data file in resource folder");
            return;
        }
        Path in = Path.of(new File(url.getFile()).getAbsolutePath());
        Path out = in.resolveSibling("bigData.out");
        StringAggregator aggregator = new StringAggregator(in, out);
        aggregator.aggregate();
        long afterBytes = runtime.totalMemory() - runtime.freeMemory();
        Assertions.assertTrue(afterBytes - bytes < 1 << 30);
    }

    @AfterAll
    public static void destroy() {
        recursiveDelete(root);
    }


    private void test(Path in, Path out, Set<String> set, Collection<Collection<String>> expected, int groupsNumber) {
        writeStrings(in, set);
        StringAggregator aggregator = new StringAggregator(in, out);
        aggregator.aggregate();
        checkStrings(out, groupsNumber, expected);
    }

    private String collect(long... arr) {
        return Arrays.stream(arr)
                .mapToObj(l -> String.format("\"%s\"", l == 0 ? "" : l))
                .collect(Collectors.joining(";"));
    }


    private static void recursiveDelete(Path root) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    Files.delete(path);
                } else {
                    recursiveDelete(path);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            Files.delete(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void writeStrings(Path in, Set<String> strings) {
        try (BufferedWriter writer = Files.newBufferedWriter(in, StandardCharsets.UTF_8)) {
            strings.forEach(s -> {
                try {
                    writer.write(s);
                    writer.newLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkStrings(Path out, int groupNumber, Collection<Collection<String>> strings) {
        Pattern groupNamePattern = Pattern.compile("Group.*");
        try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            Assertions.assertNotNull(line);
            Assertions.assertEquals(groupNumber, Integer.parseInt(line));
            int count = 0;
            List<String> stringList = strings.stream().flatMap(Collection::stream).toList();
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || groupNamePattern.matcher(line).matches()) {
                    continue;
                }
                Assertions.assertTrue(count < stringList.size());
                Assertions.assertEquals(line, stringList.get(count));
                count++;
            }
            Assertions.assertEquals(count, stringList.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private Path createFile(String name) {
        try {
            return Files.createFile(root.resolve(name));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Couldn't create temp file with name: %s", name), e);
        }
    }
}
