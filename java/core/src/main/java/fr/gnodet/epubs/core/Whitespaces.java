package fr.gnodet.epubs.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Whitespaces {

    public static String fixWhitespaces(String document) {
        Matcher paragraph = Pattern.compile("<p[\\s\\S]*?</p>").matcher(document);
        StringBuilder newDoc = new StringBuilder();
        int start = 0;
        while (paragraph.find(start)) {
            newDoc.append(document.substring(start, paragraph.start()));
            newDoc.append(fixWhitespacesInParagraph(paragraph.group()));
            start = paragraph.end();
        }
        newDoc.append(document.substring(start, document.length()));
        document = newDoc.toString();
        // Clean hrefs
        int len;
        do {
            len = document.length();
            document = document.replaceAll("(href=\"[^\"\\s\u00a0]*)[\\s\u00a0]*", "$1");
        } while (document.length() != len);
        return document;
    }

    public static String fixWhitespacesInParagraph(String text) {
        text = text.replaceAll("\n", " ");
        text = text.replaceAll("(\\s+)(</[^>]*>)", "$2$1");
        text = text.replaceAll("«\\s*", "«\u00A0");
        text = text.replaceAll("\\s*»", "\u00A0»");
        text = text.replaceAll("“\\s*", "“");
        text = text.replaceAll("\\s*”", "”");
        text = text.replaceAll("\\s*:\\s*", "\u00A0: ");
        text = text.replaceAll("\\s*;\\s*", "\u00A0; ");
        text = text.replaceAll("\\s*!\\s*", "\u00A0! ");
        text = text.replaceAll("\\s*\\?\\s*", "\u00A0? ");
        text = text.replaceAll("\\(\\s*", "(");
        text = text.replaceAll("\\s*\\)", ")");
        text = text.replaceAll("\\s*\\.([^<])", ". $1");
        text = text.replaceAll("\\s*,\\s*", ", ");
        text = text.replaceAll("( *\u00A0 *)( *\u00A0 *)*", "\u00A0");
        text = text.replaceAll(" +", " ");
        text = text.replaceAll("([\\s\u00A0])-([\\s\u00a0,])", "$1\u2014$2");

        // Fix back broken entities
        text = text.replaceAll("&([A-Za-z]+)\u00a0;", "&$1;");
        return text;
    }

}