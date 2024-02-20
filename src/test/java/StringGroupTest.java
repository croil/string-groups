import org.example.GroupType;
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
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringGroupTest {

    private static Path root;

    @BeforeAll
    public static void init() {
        try {
            root = Files.createTempDirectory("groups" + System.currentTimeMillis());
        } catch (IOException e) {
            System.err.println("Couldn't run tests. I/O error occurs: " + e.getMessage());
        }
    }

    @Test
    public void emptyFileTest() {
        Path in = createFile("empty.in");
        Path out = createFile("empty.out");
        test(in, out, new String[]{}, new String[]{}, 0);
    }

    @Test
    public void oneGroupOneStringTest() {
        Path in = createFile("oneGroupOneString.in");
        Path out = createFile("oneGroupOneString.out");
        String[] oneGroupOneString = new String[]{
                collect(111, 222, 333)
        };
        test(in, out, oneGroupOneString, oneGroupOneString, 1);
    }

    @Test
    public void oneGroupMultiStringsTest() {
        Path in = createFile("oneGroupMultiStrings.in");
        Path out = createFile("oneGroupMultiStrings.out");
        String[] oneGroupMultiStrings = new String[]{
                collect(111, 222, 333),
                collect(100, 222, 301),
                collect(300, 0, 333),
        };
        test(in, out, oneGroupMultiStrings, oneGroupMultiStrings, 1);
    }

    @Test
    public void manyAloneGroupsTest() {
        Path in = createFile("manyAloneGroups.in");
        Path out = createFile("manyAloneGroups.out");
        String[] manyAloneGroups = new String[]{
                collect(111, 222, 333),
                collect(222, 333, 111),
                collect(123, 345, 456),
                collect(0),
                collect(0, 0, 0, 0)
        };
        test(in, out, manyAloneGroups, manyAloneGroups, 5);
    }

    @Test
    public void emptyStringsTest() {
        Path in = createFile("emptyStrings.in");
        Path out = createFile("emptyStrings.out");
        String[] emptyStrings = new String[]{
                collect(0),
                collect(0, 0, 0),
                collect(0, 0),
                collect(0, 0, 0, 0, 0)
        };
        test(in, out, emptyStrings, emptyStrings, 4);
    }

    @Test
    public void doubleStringTest() {
        Path in = createFile("doubleString.in");
        Path out = createFile("doubleString.out");
        String[] doubleString = new String[]{
                collect(111, 222, 333),
                collect(111, 222, 333),
                collect(222, 333, 444),
                collect(222, 333, 444),
        };
        test(in, out, doubleString, new String[]{
                collect(111, 222, 333), collect(222, 333, 444)
        }, 2);
    }

    @Test
    public void anyGroupsTest() {
        Path in = createFile("anyGroups.in");
        Path out = createFile("anyGroups.out");
        String[] anyGroups = new String[]{
                collect(111, 123, 222),
                collect(200, 123, 100),
                collect(300, 0, 100),
                collect(222, 333, 444),
                collect(222, 0, 0),
                collect(555, 666, 777, 888),
                collect(0, 0, 666, 777, 888)
        };
        test(in, out, anyGroups, anyGroups, 4);
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
        test(in, out, arr, new String[]{
                arr[0], arr[3], arr[1], arr[4], arr[2]
        }, 3);
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
        test(in, out, arr, new String[]{}, 0);
    }

    @Test
    @Timeout(30)
    public void bigDataTest() {
        URL url = getClass().getClassLoader().getResource("Ing.txt");
        if (url == null) {
            System.err.println("Add data file in resource folder");
            return;
        }
        Path in = Path.of(new File(url.getFile()).getAbsolutePath());
        Path out = in.resolveSibling("bigData.out");
        StringAggregator aggregator = new StringAggregator(in, out);
        aggregator.aggregate(GroupType.UNION);
    }

    @AfterAll
    public static void destroy() {
        recursiveDelete(root);
    }


    private void test(Path in, Path out, String[] set, String[] expected, int groupsNumber) {
        writeStrings(in, set);
        StringAggregator aggregator = new StringAggregator(in, out);
        aggregator.aggregate(GroupType.ALL);
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


    private void writeStrings(Path in, String... strings) {
        try (BufferedWriter writer = Files.newBufferedWriter(in, StandardCharsets.UTF_8)) {
            Arrays.stream(strings).forEach(s -> {
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

    private void checkStrings(Path out, int groupNumber, String... strings) {
        Pattern groupNamePattern = Pattern.compile("Group.*");
        try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            Assertions.assertNotNull(line);
            Assertions.assertEquals(groupNumber, Integer.parseInt(line));
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || groupNamePattern.matcher(line).matches()) {
                    continue;
                }
                Assertions.assertTrue(count < strings.length);
                Assertions.assertEquals(line, strings[count]);
                count++;
            }
            Assertions.assertEquals(count, strings.length);
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
