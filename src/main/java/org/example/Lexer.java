package org.example;

import java.util.regex.Pattern;

public class Lexer {
    private final Pattern wordPattern = Pattern.compile("^\"(.*)?\"$");

    private String[] tokens;
    private int index;


    public boolean load(String text) {
        boolean isValid = !text.isEmpty() && matches(text);
        this.tokens = isValid ? text.split(";", Integer.MAX_VALUE) : new String[0];
        this.index = 0;
        return isValid;
    }

    private boolean matches(String text) {
        int quoterCount = 0;
        for (int i = 0; i < text.length(); i++) {
            switch (text.charAt(i)) {
                case ';' -> quoterCount = 0;
                case '"' -> quoterCount++;
                default -> {
                    if (quoterCount == 2) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public String next() {
        if (index == tokens.length) {
            return null;
        }
        var result = tokens[index];
        index++;
        return result.isEmpty() ? result : result.substring(1, result.length() - 1);
    }

    public boolean isBlankWord(String strWord) {
        if (strWord.isEmpty()) {
            return true;
        }
        var matcher = wordPattern.matcher(strWord);
        if (matcher.find()) {
            return matcher.group(1).isEmpty();
        }
        return false;
    }
}
