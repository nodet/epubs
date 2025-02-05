package fr.gnodet.epubs.thonnard.precis;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import fr.gnodet.epubs.core.Cover;
import fr.gnodet.epubs.core.IOUtil;
import fr.gnodet.epubs.core.Processors;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static fr.gnodet.epubs.core.EPub.createEpub;
import static fr.gnodet.epubs.core.EPub.createToc;
import static fr.gnodet.epubs.core.IOUtil.loadTextContent;
import static fr.gnodet.epubs.core.IOUtil.writeToFile;
import static fr.gnodet.epubs.core.Processors.process;
import static fr.gnodet.epubs.core.Quotes.fixQuotes;
import static fr.gnodet.epubs.core.Tidy.tidyHtml;
import static fr.gnodet.epubs.core.Tidy.translateEntities;
import static fr.gnodet.epubs.core.Whitespaces.fixWhitespaces;

public class Main {

    public static void main(String[] args) throws Exception {

        String filename = "precis_de_philosophie";
        String epub = "target/site/epub/" + filename + ".epub";
        String title = "Précis de philosophie";
        String creator = "François-Joseph THONNARD";
        String burl = "http://inquisition.ca/fr/livre/thonnard/philo/";
        String firstFile = "s001_index.htm";

        Set<String> toDownload = new LinkedHashSet<String>();
        Set<String> downloaded = new LinkedHashSet<String>();
        toDownload.add(firstFile);
        while (!toDownload.isEmpty()) {
            String name = toDownload.iterator().next();
            toDownload.remove(name);

            try {
                String document = loadTextContent(new URL(burl + name), "target/cache/" + name);
                downloaded.add(name);
                int index = document.indexOf("<a href=\"");
                while (index >= 0) {
                    int start = index + "<a href=\"".length();
                    int stop = document.indexOf("\"", start + 1);
                    String rel = document.substring(start, stop);

                    // TODO: download images

                    if (!rel.startsWith("../") && !rel.startsWith("http")) {
                        start = rel.indexOf('#');
                        if (start >= 0) {
                            rel = rel.substring(0, start);
                        }
                        if (rel.length() > 0 && !downloaded.contains(rel) && !toDownload.contains(rel)) {
                            toDownload.add(rel);
                        }
                    }
                    index = document.indexOf("<a href=\"", stop);
                }
            } catch (FileNotFoundException e) {
                System.err.println("Ignoring file " + e.getMessage());
            }
        }


        List<File> files = new ArrayList<File>();
        List<String> docs = new ArrayList<String>();

        for (String file : downloaded) {
            String document = loadTextContent(new URL(burl + file), "target/cache/" + file);
            String output = "target/html/" + file;

            // Fix encoding
            document = document.replaceAll("<meta[^>]*http-equiv=\"Content-Type\"[^>]*/>", "");
            document = document.replace("<head>", "<head><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />");
            // Delete scripts tags
            document = document.replaceAll("<script[\\S\\s]*?</script>", "");
            document = document.replaceAll("<SCRIPT[\\S\\s]*?</SCRIPT>", "");
            // Delete image tags
            document = document.replaceAll("<img[^>]*>", "");
            document = document.replaceAll("<IMG[^>]*>", "");
            // Fix id and href attributes
            document = process(document, "href=\"#([^\"]*)\"", 1, new URIProcessor());
            document = process(document, "name=\"([^\"]*)\"", 1, new URIProcessor());
            document = process(document, "href=\"(\\.\\./[^\"]*)\"", 1, new Processors.RelativeURIProcessor(burl));
            // Delete empty elements
            document = document.replaceAll("<font[^>]*>\\s*</font>", "");
            document = document.replaceAll("<b[^>]*>\\s*</b>", "");
            document = document.replaceAll("<p[^>]*>\\s*</p>", "");
            // Fix style
            document = document.replaceAll("<link[^>]*type=\"text/css\"[^>]*>", "");
            document = document.replaceAll("<style>", "<style type=\"text/css\">");

            // Remove comments
            document = document.replaceAll("<!--[\\s\\S]*?-->", "");

            // Tidy html
            document = tidyHtml(document);
            document = document.replaceAll("<a[\\s\n]+(href[^>]*)>\n*", "<a $1>");

            // Use xhtml 1.1
            document = document.replaceAll(
                    "<!DOCTYPE[^>]*>",
                    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");

            // Remove new lines to simplify
            {
                int index = document.indexOf("<pre>");
                while (index > 0) {
                    int stop = document.indexOf("</pre>", index) + "</pre>".length();
                    String pre = document.substring(index, stop);

                    pre = pre.replaceAll("\n", "<br/>");

                    document = document.substring(0, index) + pre + document.substring(stop, document.length());
                    index = document.indexOf("<pre>", stop);
                }
            }
            document = document.replaceAll("[\r\n]+", " ");
            // Fix encoding
            document = document.replaceAll("<meta[^>]*>", "");
            document = document.replace("<head>", "<head><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />");
            // Translate entities
            document = translateEntities(document);

            // Delete header and footer
            document = document.replaceAll("<p class=\"poucethaut\">([^<]|<[^p])*</p>\\s*<p[^>]*>\\s*\\[([^<]|<[^p])*\\]\\s*</p>", "");
            document = document.replaceAll("<p[^>]*>\\s*\\[([^<]|<[^p])*\\]\\s*</p>\\s*<p class=\"poucetbas\">([^<]|<[^p])*</p>", "");
            document = document.replaceAll("<p[^>]*>Note: Si le grec classique.*?</p>", "");
            document = document.replaceAll("<p[^>]*>Table alphabétique.*?</p>\\s*<p[^>]*>Je pense que Google.*?</p>.*?<form.*?</form>.*?-->", "");
            // Delete unwanted attributes
            document = document.replaceAll(" \\b(width|topmargin|border|marginwidth|marginheight|cellspacing|" +
                    "cellpadding|hspacing|lang|xml:lang|alink|vlink|link|background|" +
                    "text|height|hspace|alt|bgcolor|rowspan|valign)=[^\\s>]*", "");
            document = document.replaceAll(" style=\"[^\"]*\"", "");
            document = document.replaceAll(" style='[^']*'", "");
            document = document.replaceAll(" clear=\"all\"| align=\"left\"| align=\"justify\"| class=\"[^\"]*\"| name=\"[^\"]*\"", "");
            // Remove unwanted meta tags
            document = document.replaceAll("<meta name=\"generator\".*?/>", "");

            //
            // Clean things a bit
            //
            document = document.replaceAll("<i><br />", "<br /><i>");
            document = document.replaceAll(" color=\"#663300\"| face=\"Times New Roman\"| size=\"3\"", "");
            document = document.replaceAll("align=\"center\"", "class=\"center\"");
            document = document.replaceAll("align=\"right\"", "class=\"right\"");

            // Fix stuff
            document = document.replaceAll("Donc elle est immortelle »\"", "Donc elle est immortelle »");
            document = document.replaceAll("http://inquisition.ca/fr/livre/thonnard/histo/h27_p0474a0480.htm#s480", "http://inquisition.ca/fr/livre/thonnard/histo/h27_p0474a0480.htm#s480p1");
            document = document.replaceAll("\\.\\.\\.", "…");
            document = document.replaceAll(":\\s*</i>", "</i> : ");
            document = fixQuotes(document);
            document = fixFootNotes(document);
            document = fixWhitespaces(document);

            document = document.replaceAll("<font\\s*>(.*?)</font>", "$1");
            document = document.replaceAll("<a[^>]*></a>", "");

            // Add our style
            document = document.replaceAll("</head>",
                    "<style type=\"text/css\">\n" +
                            " #title { color: #663300; text-align: center; }\n" +
                            " #title .title { font-style:italic; font-size: larger; font-weight:bold; }\n" +
                            " #title .author { font-weight:bold; }\n" +
                            " #bened { font-style:italic; } \n" +
                            " #main .numpara { font-family: Verdana; font-size: smaller; font-weight: bold; }\n" +
                            " .footnote { vertical-align: super; font-size: 70%; line-height: 80%; }\n" +
                            " .center { text-align: center; }\n" +
                            " .right { text-align: right; }\n" +
                            " p .ref { margin: 0; padding: 0; font-size: smaller; }\n" +
                            " p a .ref { font-family: Verdana; font-size: smaller; font-weight: bold; }\n" +
                            "</style>" +
                            "</head>");

            // Write file
            writeToFile(document, output);
            files.add(new File(output));
            docs.add(document);
        }

        // Create epub

        String tocNcx = createToc(title, IOUtil.readUrl(Main.class.getResource("precis-philo.xml"), "UTF-8"));
        byte[] coverPng = Cover.generateCoverPng(Math.random(),
                title,
                new Object[] {
                        new Cover.Text(creator, new Font(Font.SERIF, Font.PLAIN, 58), 1.0, 0.0),
                        new Cover.Break(),
                        new Cover.Text("Précis de", new Font(Font.SERIF, Font.PLAIN, 96), 1.1, 0.25),
                        new Cover.Text("philosophie", new Font(Font.SERIF, Font.PLAIN, 96), 1.1, 0.25),
                        new Cover.Break(),
                },
                null);
        writeToFile(coverPng, "target/site/images/" + filename + ".png");


        Map<String, byte[]> resources = new HashMap<String, byte[]>();
        resources.put("OEBPS/img/cover.png", coverPng);
        resources.put("OEBPS/cover.html",
                Cover.generateCoverHtml(creator, title, "", creator).getBytes());
        createEpub(files.toArray(new File[files.size()]), resources, new File(epub), title, creator, tocNcx);
    }

    private static String toDigits(int nb) {
        if (nb < 10) {
            return "0" + Integer.toString(nb);
        } else {
            return Integer.toString(nb);
        }
    }

    private static String fixFootNotes(String document) {
        document = document.replaceAll("<p><sup>([0-9]+)</sup>", "<p id=\"fn$1\" class=\"ref\"><a href=\"#fnr$1\">[$1]</a> ");
        document = document.replaceAll("<br /></sup>", "</sup><br />");
        document = document.replaceAll("([.,;:?!]\\s*)(<sup>([0-9]+)</sup>)", "$2$1");
        document = document.replaceAll("<sup>([0-9]+)</sup>", "<a id=\"fnr$1\" class=\"footnote\" href=\"#fn$1\">$1</a>");
        return document;
    }

    private static String[] split(String document, String regex, String fileName) {
        int min = 50 * 1024;
        int max = 50 * 1024;
        int bodyStart = document.indexOf("<body>") + "<body>".length();
        int bodyEnd = document.indexOf("</body>");
        List<Integer> breaks = new ArrayList<Integer>();
        Matcher matcher = Pattern.compile(regex).matcher(document);
        int start = bodyStart;
        breaks.add(bodyStart);
        while (matcher.find(start)) {
            int prev = breaks.get(breaks.size() - 1);
            int cur = matcher.start();
            if (cur - prev > min) {
                breaks.add(matcher.start());
            }
            start = matcher.end();
        }
        breaks.add(bodyEnd);
        List<String> docs = new ArrayList<String>();
        int currStart = bodyStart;
        int numBreak = 0;
        List<String> opened = new ArrayList<String>();
        while (numBreak < breaks.size()) {
            int nextBreak = breaks.get(numBreak);
            if (nextBreak - currStart >= max) {
                String doc = document.substring(currStart, nextBreak);
                if (doc.startsWith("<hr />")) {
                    doc = doc.substring("<hr />".length());
                }
                String open = "";
                for (String tag : opened) {
                    open += "<" + tag + ">";
                }
                doc = open + doc;
                opened.clear();
                computeTags(doc, opened);
                List<String> rev = new ArrayList<String>(opened);
                Collections.reverse(rev);
                for (String tag : rev) {
                    doc += "</" + tag + ">";
                }
                doc = document.substring(0, bodyStart) + doc + "</body></html>";
                docs.add(doc);
                currStart = nextBreak;
            }
            numBreak++;
        }
        Map<String, Integer> refs = new HashMap<String, Integer>();
        for (int i = 0; i < docs.size(); i++) {
            String doc = docs.get(i);
            Matcher idMatcher = Pattern.compile(" id=\"([^\"]*)\"").matcher(doc);
            int idStart = 0;
            while (idMatcher.find(idStart)) {
                String name = idMatcher.group(1);
                refs.put(name, i);
                idStart = idMatcher.end();
            }
        }
        String baseName = new File(fileName).getName();
        baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        for (int i = 0; i < docs.size(); i++) {
            String doc = docs.get(i);
            StringBuilder newDoc = new StringBuilder();
            Matcher idMatcher = Pattern.compile(" href=\"(#([^\"]*))\"").matcher(doc);
            int idStart = 0;
            while (idMatcher.find(idStart)) {
                newDoc.append(doc.substring(idStart, idMatcher.start(1)));
                Integer docIndex = refs.get(idMatcher.group(2));
                if (docIndex != null && docIndex.intValue() != i) {
                    newDoc.append(baseName + ".p" + toDigits(docIndex) + ".html#" + idMatcher.group(2));
                } else {
                    newDoc.append("#" + idMatcher.group(2));
                }
                idStart = idMatcher.end(1);
            }
            newDoc.append(doc.substring(idStart, doc.length()));
            docs.set(i, newDoc.toString());
        }

        return docs.toArray(new String[docs.size()]);
    }

    private static void computeTags(String document, List<String> opened) {
        Matcher matcher = Pattern.compile("</?([a-z]+)\\b[^>]*>").matcher(document);
        int start = 0;
        while (matcher.find(start)) {
            String tag = matcher.group();
            String name = matcher.group(1);
            if (tag.startsWith("</")) {
                String old = opened.remove(opened.size() - 1);
                if (!name.equals(old)) {
                    System.err.println("Tag mismatch: found </" + name + "> but expected </" + old + ">");
                }
            } else if (!tag.endsWith("/>")) {
                opened.add(name);
            }
            start = matcher.end();
        }
    }

    static class URIProcessor implements Processors.Processor {
        @Override
        public String process(String text) {
            try {
                text = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                text = text.replaceAll("[^\\p{Alnum}\\p{Space}]", "");
                String encoded = URLEncoder.encode(text, "UTF-8");
                if (encoded.equals("b+eglise+catholique+et+communaute+politique")) {
                    encoded = "eglise+catholique+et+communaute+politique";
                }
                encoded = encoded.replace('+', '-');
                encoded = encoded.replaceAll("--", "-");
                if (encoded.startsWith("-")) {
                    encoded = encoded.substring(1);
                }
                if (encoded.endsWith("-")) {
                    encoded = encoded.substring(0, encoded.length() - 1);
                }
                return encoded;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
