package org.example;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {

    private final Pattern linePattern = Pattern.compile("^(\"\\d*\")(;\"\\d*\")*$");
    private final Pattern wordPattern = Pattern.compile("\"(\\d*?)\"");

    private Matcher matcher;


    public boolean load(String text) {
        if (!linePattern.matcher(text).matches()) {
            return false;
        }
        matcher = wordPattern.matcher(text);
        return true;
    }

    public String nextWord() {
        return matcher.find() ? matcher.group(1) : null;
    }
}
