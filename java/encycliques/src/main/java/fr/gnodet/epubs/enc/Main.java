package fr.gnodet.epubs.enc;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.gnodet.epubs.core.EPub.createEpub;
import static fr.gnodet.epubs.core.IOUtil.loadTextContent;
import static fr.gnodet.epubs.core.IOUtil.writeToFile;
import static fr.gnodet.epubs.core.Quotes.fixQuotes;
import static fr.gnodet.epubs.core.Quotes.fixQuotesInParagraph;
import static fr.gnodet.epubs.core.Tidy.tidyHtml;
import static fr.gnodet.epubs.core.Tidy.translateEntities;
import static fr.gnodet.epubs.core.Whitespaces.fixWhitespaces;

public class Main {

    static final javax.xml.parsers.DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    public static void main(String[] args) throws Exception {

        dbf.setNamespaceAware(true);

        Document enclist = dbf.newDocumentBuilder().parse(Main.class.getResourceAsStream("enc-list.xml"));
        NodeList books = enclist.getDocumentElement().getElementsByTagName("book");
        for (int i = 0; i < books.getLength(); i++) {
//        for (int i = 17; i < books.getLength(); i++) {
//        int i = 19; {
            Element book = (Element) books.item(i);
            String file = book.getAttribute("file");
            String title = book.getAttribute("title");
            String titlefr = book.getAttribute("title-fr");
            String creator = book.getAttribute("creator");
            String date = book.getAttribute("date");
            String full = getFull(creator);

            URL url = new URL("http://www.vatican.va/holy_father/" + full + "/encyclicals/documents/" + file);
            String output = "enc_" + date + "_hf_" + full + "_" + title.toLowerCase().replaceAll("\\s", "-").replaceAll("æ", "ae");
            try {
                process(url, "target/cache/" + file, "target/html/" + output + ".html", full);
                Map<String, byte[]> resources = new HashMap<String, byte[]>();
                resources.put("OEBPS/img/" + full + "-bw.svg", readFully(Main.class.getResource("coa/" + full + "-bw.svg")));
                resources.put("OEBPS/img/cover.png", generateCoverPng((i * 1.0 / books.getLength()), creator, title, titlefr, full));
                resources.put("OEBPS/cover.html", generateCoverHtml(creator, titlefr, title, full).getBytes());
                createEpub(new File[] { new File("target/html/" + output + ".html") },
                           resources,
                           new File("target/epub/" + output + ".epub"),
                           title, creator, null);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static String generateCoverHtml(String creator, String titlefr, String title, String full) {
        return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "  <head>\n" +
                "    <title>Cover</title>\n" +
                "    <style type=\"text/css\"> img { max-width: 100%; } </style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <div id=\"cover-image\">\n" +
                "      <img src=\"img/cover.png\" alt=\"" + titlefr + "\"/>\n" +
                "    </div>\n" +
                "  </body>\n" +
                "</html>";
    }

    private static byte[] generateCoverPng(double hue, String creator, String title, String subtitle, String full) throws IOException, TranscoderException {

        int width = 711;
        int height = 1084;

        String backColor =   hsl2rgb(hue, 0.9, 0.5);
        String borderColor = hsl2rgb(hue, 0.9, 0.85);
        String titleColor =  hsl2rgb(hue, 0.9, 0.25);

        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" preserveAspectRatio=\"xMidYMid meet\" viewBox=\"0 0 711 1084\" id=\"svg2\">\n");
        sb.append("<rect width=\"711\" height=\"1084\" style=\"fill:").append(backColor).append(";stroke-width:0;stroke:rgb(0,0,0)\"/>\n");
        sb.append("<rect x=\"50\" y=\"50\" width=\"611\" height=\"984\" style=\"fill:white;stroke-width:10;stroke:").append(borderColor).append("\"/>\n");

        sb.append("<text fill=\"black\" font-family=\"serif\" font-size=\"58\" y=\"118\" x=\"355.5\" style=\"text-anchor: middle;\">")
                .append(creator.toUpperCase())
                .append("</text>\n");

        List<String> titleParts = minLines(title.toUpperCase(), title.length() / 10 + 1);
        int top = 108 + 75 + ((608 - 108) - 100 * titleParts.size()) / 2;
        sb.append("<g transform=\"translate(0, -100) scale(0.8, 1.1) translate(444.375, ").append(313 + 50*(3 - titleParts.size())).append(")\">\n");
        for (int i = 0; i < titleParts.size(); i++) {
            sb.append("<text fill=\"").append(titleColor).append("\" font-family=\"serif\" font-size=\"96\" y=\"").append(i*100).append("\" x=\"0\" style=\"text-anchor: middle\">")
                    .append(titleParts.get(i))
                    .append("</text>\n");
        }
        sb.append("</g>\n");

        List<String> subtitleParts = minLines(subtitle, subtitle.length() / 15 + 1);
        top = 558 + (100 - 50 * subtitleParts.size()) / 2;
        for (int i = 0; i < subtitleParts.size(); i++) {
            sb.append("<text fill=\"black\" font-family=\"serif\" font-size=\"58\" font-style=\"italic\" y=\"").append(top + i*60).append("\" x=\"355.5\" style=\"text-anchor: middle\">")
                    .append(subtitleParts.get(i))
                    .append("</text>");
        }

        String innerSvg = new String(readFully(Main.class.getResource("coa/" + full + ".svg")));
        if (innerSvg.startsWith("<?")) {
            innerSvg = innerSvg.substring(innerSvg.indexOf("?>") + 2);
        }
        int vbi = innerSvg.indexOf("viewBox=");
        String[] viewBox = innerSvg.substring(vbi + 9, innerSvg.indexOf("\"", vbi+9)).split(" ");
        double iw = new Double(viewBox[2]);
        double ih = new Double(viewBox[3]);

        double rw = 324.0 * iw / ih;
        double x = (width - rw) / 2;


        sb.append("<svg y=\"658\" x=\"").append((int) x).append("\" height=\"324\" >")
                .append(innerSvg)
                .append("</svg>");

        sb.append("</svg>");


        new File("target/svgs").mkdirs();
        writeToFile(sb.toString(), "target/svgs/" + pad(Long.toString((long) (hue * 100)), 2, "0") + " - " + title + ".svg");

        // Create a JPEG transcoder
        PNGTranscoder t = new PNGTranscoder();
        // Set the transcoding hints.
        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(height));
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(width));
        t.addTranscodingHint(PNGTranscoder.KEY_AOI, new Rectangle(width, height));
        // Create the transcoder input.
        TranscoderInput input = new TranscoderInput(new StringReader(sb.toString()));
        // Create the transcoder output.
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        TranscoderOutput output = new TranscoderOutput(ostream);

        // Save the image.
        t.transcode(input, output);

        // Flush and close the stream.
        ostream.flush();
        ostream.close();

        writeToFile(ostream.toByteArray(), "target/pngs/" + pad(Long.toString((long) (hue * 100)), 2, "0") + " - " + title + ".png");

        return ostream.toByteArray();
    }

    private static String pad(String str, int length, String chr) {
        while (str.length() < length) {
            str = chr + str;
        }
        return str;
    }

    private static double hue2rgb(double p, double q, double t) {
        if(t < 0.0) t += 1.0;
        if(t > 1.0) t -= 1.0;
        if(t < 1.0/6.0) return p + (q - p) * 6.0 * t;
        if(t < 1.0/2.0) return q;
        if(t < 2.0/3.0) return p + (q - p) * (2.0/3.0 - t) * 6.0;
        return p;
    }
    private static String hsl2rgb(double h, double s, double l) {
        double r, g, b;
        if (s == 0.0) {
            r = g = b = l; // achromatic
        } else {
            double q = l < 0.5 ? l * (1.0 + s) : l + s - l * s;
            double p = 2.0 * l - q;
            r = hue2rgb(p, q, h + 1.0/3.0);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1.0/3.0);
        }

        return toRGB(r, g, b);
    }

    private static String toRGB(double r, double g, double b) {
        int lr = (int)(r * 255.0);
        int lg = (int)(g * 255.0);
        int lb = (int)(b * 255.0);
        StringBuilder sb = new StringBuilder();
        sb.append("#");
        if (lr < 16) {
            sb.append("0");
        }
        sb.append(Integer.toHexString(lr));
        if (lg < 16) {
            sb.append("0");
        }
        sb.append(Integer.toHexString(lg));
        if (lb < 16) {
            sb.append("0");
        }
        sb.append(Integer.toHexString(lb));
        return sb.toString();
    }

    private static byte[] readFully(URL url) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int l;
        InputStream is = url.openStream();
        try {
            while ((l = is.read(buf)) >= 0) {
                baos.write(buf, 0, l);
            }
        } finally {
            is.close();
        }
        return baos.toByteArray();
    }

    private static void process(URL url, String cache, String output, String creator) throws Exception {
        System.out.println("Processing: " + url);

        //
        // Grab html
        //

        // Load URL text content
        String document = loadTextContent(url, cache);

        // Add doctype if not present
        if (!document.startsWith("<!DOCTYPE")) {
            document = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" + document;
        }
        // Fix bad html
        document = document.replaceAll("<p align=\"left\"<a", "<p align=\"left\"><a");
        // Delete office elements
        document = document.replaceAll("<o:p>[\\S\\s]*?</o:p>", "");
        // Fix encoding
        document = document.replaceAll("<meta[^>]*http-equiv=\"Content-Type\"[^>]*/>", "");
        document = document.replace("<head>", "<head><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />");
        // Delete bad attributes
        document = document.replaceAll("<a title ", "<a ");
        // Fix footnotes with missing paragraph and missing closing div tag
        // Tidy Html
        document = tidyHtml(document);
        // Fix encoding
        document = document.replaceAll("<meta[^>]*http-equiv=\"Content-Type\"[^>]*/>", "");
        document = document.replace("<head>", "<head><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />");
        // Translate entities
        document = translateEntities(document);

        //
        // Html clean up
        //

        // Remove new lines to simplify
        document = document.replaceAll("[\r\n]+", " ");
        // Delete inlined javascript
        document = document.replaceAll("<!\\[if !supportFootnotes\\]>", "");
        document = document.replaceAll("<!\\[endif\\]>", "");
        // Delete scripts tags
        document = document.replaceAll("<script[\\S\\s]*?</script>", "");
        document = document.replaceAll("<SCRIPT[\\S\\s]*?</SCRIPT>", "");
        // Delete image tags
        document = document.replaceAll("<img[^>]*>", "");
        document = document.replaceAll("<IMG[^>]*>", "");
        // Delete unwanted attributes
        document = document.replaceAll(" \\b(width|topmargin|border|marginwidth|marginheight|cellspacing|" +
                "cellpadding|hspacing|lang|xml:lang|alink|vlink|link|background|" +
                "text|height|hspace|alt|bgcolor|rowspan|valign)=[^\\s>]*", "");
        document = document.replaceAll(" style=\"[^\"]*\"", "");
        document = document.replaceAll(" style='[^']*'", "");
        document = document.replaceAll(" clear=\"all\"| align=\"left\"| align=\"justify\"| class=\"[^\"]*\"", "");
        // Remove unwanted meta tags
        document = document.replaceAll("<meta name=\"generator\".*?/>", "");
        // Remove unwanted anchor attributes
        document = document.replaceAll("(<a[^>]*?)\\s*name=\"[^\"]*\"\\s*([^>]*?>)", "$1$2");
        document = document.replaceAll("(<a[^>]*?)\\s*title=\"\"\\s*([^>]*?>)", "$1$2");
        document = document.replaceAll("(<a[^>]*\\s)\\s+([^>]*>)", "$1$2");
        document = document.replaceAll("<a id=\"top\"></a>", "");
        document = document.replaceAll("<a href=\"#top\"></a>", "");
        document = document.replaceAll("<basefont.*?/>", "");
        document = document.replaceAll("<tbody>|</tbody>", "");
        // Remove font styling
        document = document.replaceAll("<font (?:face=\"[^\"]*\" )?size=\"3\">([\\s\\S]*?)</font>", "$1");
        // Clean unneeded span
        document = document.replaceAll("<p><span>([\\s\\S]*?)</span>([\\s\\S]*?)</p>", "<p>$1 $2</p>");
        // Clean paragraphs
        document = document.replaceAll("\\s*<br />\\s*<br />\\s*", "</p><p>");

        // Delete first table
        document = document.replaceFirst("<table[\\s\\S]*?</table>", "");

        // Fix for extraction
        document = document.replaceAll("(<p>NOTES</p>|<p><b><br />\\s*NOTES</b></p>)", "<hr /><p><b>$1</b></p>");
        document = document.replaceAll("Sollicitudo rei socialis</i><br />", "Sollicitudo rei socialis</i> ");
        document = document.replaceAll("n\\.</i> 5 <i>1\\.<br />", "n.</i> 5 <i>1.");
        document = document.replaceAll("<p><b>(<p><b><br /> NOTES</b></p>)</b></p>", "<p><b>NOTES</b></p>");
        // Distribute <center> tags
        document = distribute(document, "center", "p");

        // Fix missing paragraphs
        document = replaceAllFull(document, "(<blockquote>.*?)<p>(.*?</blockquote>)", "$1$2");
        document = replaceAllFull(document, "(<blockquote>.*?)</p>(.*?</blockquote>)", "$1<br />$2");
        document = document.replaceAll("</p>\\s*([^<\\s]+?.*?|\\s*<[^p].*?)\\s*<p", "</p><p>$1</p><p");
        document = document.replaceAll("<p><p>", "<p>");
        document = document.replaceAll("</p></p>", "</p>");

        String head = extract(document, "<head>.*?</head>", 0);
        String title = url.toExternalForm().contains("papa-francesco")
                ? extract(document, "(<p[^>]*>.*?LETTRE ENCYCLIQUE.*?)<p>1\\. ", 1)
                : extract(document, ".*<td[^>]*>(.*?LETTRE ENCYCLIQUE.*?)(<p><i>|<p><em>|<p><b>|<p[^>]*>(<center>)?\u00a0|</td>|<[^>]*>\\s*INTRO)", 1);

        String bened = extractFollowingParaContaining(document, ".*[Bb]énédiction.*", document.indexOf(title) + title.length());
        String footnotes = extract(document, "<hr[^>]*>(.*?<p.*?)(<p[^>]*>[\\s\u00a0]*</p>|<p><br />|</td>)", 1);
        String copyright = extract(document, ">\\s*(©[^<]*?)\\s*<", 1);
        String main = document.substring(document.indexOf(bened != null ? bened : title) + (bened != null ? bened : title).length(),
                                         document.indexOf(footnotes != null ? footnotes : (copyright != null ? copyright : "</body>")));

        document = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"fr\">" +
                cleanHead(head) + "<body>" +
                "<div id=\"title\">" + cleanTitle(title, creator) + "</div>" +
                (bened != null ? "<div id=\"bened\">" + cleanBened(bened) + "</div>" : "") +
                "<div id=\"main\">" + cleanMain(main) + "</div>" +
                (footnotes != null ? "<div id=\"notes\">" + cleanNotes(footnotes) + "</div>" : "") +
                (copyright != null ? "<div id=\"copyright\">" + cleanCopyright(copyright) + "</div>" : "") +
                "</body></html>";

        writeToFile(document, output);
    }

    private static String replaceAllFull(String document, String regexp, String repl) {
        for (;;) {
            String doc = document.replaceAll(regexp, repl);
            if (doc.equals(document)) {
                return doc;
            } else {
                document = doc;
            }
        }
    }

    private static String distribute(String document, String outer, String inner) {
        Matcher paragraph = Pattern.compile("<" + outer + ">([\\s\\S]*?)</" + outer + ">").matcher(document);
        StringBuilder newDoc = new StringBuilder();
        int start = 0;
        while (paragraph.find(start)) {
            newDoc.append(document.substring(start, paragraph.start()));
            newDoc.append(paragraph.group(1).replaceAll("(<" + inner +"[^>]*>)([\\s\\S]*?)(</" + inner + ">)", "$1<" + outer + ">$2</" + outer + ">$3"));
            start = paragraph.end();
        }
        newDoc.append(document.substring(start, document.length()));
        return newDoc.toString();
    }

    private static String cleanHead(String document) {
        // Remove meta tags with spaces
        document = document.replaceAll("<meta[^>]*name=\"[^\"]* [^\"]*\"[^>]*/>", "");
        // Remove style elements
        document = document.replaceAll("<style.*?</style>", "");
        // Add our style
        document = document.replaceAll("</head>",
                "<style type=\"text/css\">\n" +
                        " #title { color: #663300; text-align: center; }\n" +
                        " #title .title { font-style:italic; font-size: larger; font-weight:bold; }\n" +
                        " #title .author { font-weight:bold; }\n" +
                        " #bened { font-style:italic; } \n" +
                        " #main .numpara { font-family: Verdana; font-size: smaller; font-weight: bold; }\n" +
                        " #main .footnote { vertical-align: super; font-size: 70%; line-height: 80%; }\n" +
                        " #main .center { text-align: center; }\n" +
                        " #notes p { margin: 0; padding: 0; font-size: smaller; }\n" +
                        " #notes .ref { font-family: Verdana; font-size: smaller; font-weight: bold; }\n" +
                        " #copyright { color: #663300; text-align: center; font-size: smaller; }\n" +
                        "</style>" +
                        "</head>");
        return document;
    }

    private static String cleanTitle(String document, String creator) {
        document = document.trim();
        document = fixTypos(document);
        document = fixQuotesInParagraph(document);
        document = document.replaceAll("<center>|</center>|<span[^>]*>|<font[^>]*>|</span>|</font>| align=\"center\"|" +
                                       "<strong>|</strong>|<em>|</em>|<b>|</b>|<i>|</i>|" +
                                       "<td>|</td>|<tr>|</tr>|<table>|</table>", "");
        document = document.replaceAll("</p>\\s*<p>", "<br /><br />");
        document = document.replaceAll("<p[^>]*>", "<p>");
        document = document.replaceAll("[\u00a0\\s]*<br />[\u00a0\\s]*", "<br />");
        document = document.replaceAll("<p><br />", "<p>");
        document = document.replaceAll("(<br />)+</p>", "<p>");
        document = document.replaceAll("([^>])<br />SUR", "$1<br /><br />SUR");
        document = document.replaceAll("VOLONTÉ<br />", "VOLONTÉ<br /><br />");
        document = document.replaceAll("<br />À L’OCCASION", "<br /><br />À L’OCCASION");
        document = document.replaceAll("PAUL VI", "<br />PAUL VI");
        document = document.replaceAll("(BENOÎT XVI|JEAN-PAUL II|PAUL VI|FRANÇOIS)", "<span class=\"author\">$1</span><br />");
        if (document.indexOf("LETTRE ENCYCLIQUE") > 4) {
            document = document.replaceAll("<p>(.*?)<br />", "<p><span class=\"title\">$1</span><br />");
        } else {
            document = document.replaceAll("(LETTRE ENCYCLIQUE<br />)(.*?)<br />", "<span class=\"title\">$2</span><br /><br />$1");
        }
        document = document.replaceAll("</p>\\s*</p>", "</p>");
        document = document.replaceAll("<br />\\s*(<br /><br />|</p>)", "$1");
        if (document.endsWith("<p>")) {
            document = document.substring(0, document.length() - 3);
        }
        if (!document.endsWith("</p>")) {
            document = document + "</p>";
        }
        if (!document.startsWith("<p>")) {
            document = "<p>" + document;
        }
        document = "<object width=\"200em\" data=\"img/" + creator + "-bw.svg\" type=\"image/svg+xml\"></object>" + document;
        return document;
    }

    private static String cleanBened(String document) {
        document = document.trim();
        document = document.replaceAll("<i>|</i>", "");
        document = document.replaceAll("<p align=\"center\">", "<p class=\"center\">");
        document = fixTypos(document);
        document = fixQuotesInParagraph(document);
        return document;
    }

    private static String cleanMain(String document) {
        document = document.substring(document.indexOf("<p"), document.lastIndexOf("</p>") + 4);
        document = document.replaceAll("<font[^>]*>(.*?)</font>", "$1");
        document = document.replaceAll("<p[^>]*>[\\s\u00a0]*</p>", "");
        document = document.replaceAll("<p align=\"center\">", "<p class=\"center\">");
        document = document.replaceAll("<p>\\s*<center>(.*?)</center>\\s*</p>", "<p class=\"center\">$1</p>");
        document = document.replaceAll("</p><p></b>", "</b></p><p>");
        document = document.replaceAll("</p></b><p>", "</b></p><p>");
        document = document.replaceAll("<blockquote>\\s*</blockquote>", "");
        document = document.replaceAll("<table[^>]*>|</table>|<tr[^>]*>|</tr>|<td[^>]*>|</td>", "");

        // Simplify tags
        document = document.replaceAll("<strong>(.*?)</strong>", "<b>$1</b>");
        document = document.replaceAll("<em>(.*?)</em>", "<i>$1</i>");

        document = document.replaceAll("\\b([IXV][IXV]*)(e|er|ème)\\b", "<span style=\"font-size: smaller\">$1</span><sup>e</sup>");
        document = document.replaceAll(" 1<sup>er</sup> s\\.", "<span style=\"font-size: smaller\">I</span><sup>er</sup> s.");
        // Fix dashes
        document = document.replaceAll("(\\S)_ ", "$1 - ");
        document = document.replaceAll(" _(\\S)", " - $1");
        document = document.replaceAll(" _ ", " - ");
        document = document.replaceAll(" -- ", " - ");
        document = document.replaceAll(" - </i>", "</i> - ");
        document = document.replaceAll("(</[^>]*>)-", "$1 - ");
        document = document.replaceAll("-un et trine-", "- un et trine -");
        document = document.replaceAll("-et je tiens à le faire remarquer d['’]emblée-", "- et je tiens à le faire remarquer d’emblée -");
        document = document.replaceAll("privées- correspondant", "privées - correspondant");
        document = document.replaceAll("Incarnation- et", "Incarnation - et");
        document = document.replaceAll("Mère- et", "Mère - et");
        document = document.replaceAll("qui- suivant la consigne de l['’]ange-", "qui - suivant la consigne de l’ange -");
        document = document.replaceAll("Pères -comme le rappelle aussi le Concile- ", "Pères - comme le rappelle aussi le Concile - ");
        document = document.replaceAll("hommes -à tous et à chacun- ", "hommes - à tous et à chacun - ");
        document = document.replaceAll("présente -par la volonté du Fils et par l['’]Esprit Saint- dans", "présente - par la volonté du Fils et par l’Esprit Saint - dans");
        document = document.replaceAll("-celle de Jésus Christ- est", "- celle de Jésus Christ - est");
        document = document.replaceAll("expriment- comme je l['’]ai dit- la", "expriment - comme je l’ai dit - la");
        document = document.replaceAll("effet -et cette prière en témoigne- la", "effet - et cette prière en témoigne - la");
        document = document.replaceAll("Christ -accomplissement des prophéties messianiques- rend", "Christ - accomplissement des prophéties messianiques - rend");
        document = document.replaceAll("-«qu['’]il m['’]advienne»-", "- « qu’il m’advienne » -");
        document = document.replaceAll("-le Fils du Très-Haut \\(cf. Lc 1, 32\\)-", "- le Fils du Très-Haut (cf. Lc 1, 32) -");
        document = document.replaceAll("-comme l'enseigne le Concile Vatican II-", "- comme l'enseigne le Concile Vatican II -");
        document = document.replaceAll("- célébration liturgique du mystère de la Rédemption-", " - célébration liturgique du mystère de la Rédemption -");
        // Fix quotes problems
        document = document.replaceAll("Recevez le Saint-Esprit&raquo;.", "Recevez le Saint-Esprit&raquo; 153.");
        document = document.replaceAll("Cyrille déclare: Ne ", "Cyrille déclare: \"Ne ");
        document = document.replaceAll("Seigneur,\" parle", "Seigneur, parle");
        document = document.replaceAll(" plus profonde \".\"", " plus profonde \".");
        document = document.replaceAll("que me manque-t-il encore \\? J", "que me manque-t-il encore ?” J");
        document = document.replaceAll("pesants M M1 Jn", "pesants\" (cf. 1 Jn");
        document = document.replaceAll("âme \".e", "âme \".");
        document = document.replaceAll("coeur", "cœur");
        document = document.replaceAll("<b>:</b>", ":");
        document = document.replaceAll("<b>\" </b>", "&quot; ");
        document = document.replaceAll("transsubstantiation\\.,", "transsubstantiation.");
        document = document.replaceAll("par lui \" (71)", "par lui \" (71).");
        document = document.replace("observe les commandements 1. \"", "observe les commandements. [...] \"");
        document = document.replaceAll("cette formule: \"Tu aimeras", "cette formule : Tu aimeras");
        document = document.replaceAll("à a ceux", "« à ceux");
        document = document.replaceAll("forte avec \"œuvre du", "forte avec l'œuvre du");
        document = document.replaceAll("au fond de sa conscience, l'homme découvre la présence", "au fond de sa conscience, « l'homme découvre la présence");
        document = document.replaceAll("reste de la population\" pauvre", "reste de la population, pauvre");
        document = document.replaceAll("commun de l['’]humanité \\(47\\)", "commun de l’humanité\" (47)");
        document = document.replaceAll("il faut<i>", "il faut <i>");
        document = document.replaceAll("discernementsur", "discernement sur");
        document = document.replaceAll("progrèstechnologique", "progrès technologique");
        document = document.replaceAll("personnelle et sociale de l['’]homme", "personnelle et sociale de l’homme\"");
        document = document.replaceAll("L['’]esprit, rendu ainsi «", "« L’esprit, rendu ainsi \"");
        document = document.replaceAll("ontolo<i>gique\\.</i>", "ontologique.");
        document = document.replaceAll("Abel et le tua.</i></p>\\s*<p><i>Le Seigneur", "Abel et le tua.<br/><br/>Le Seigneur");
        document = document.replaceAll("6, 28\\.35", "6, 28-35");
        document = document.replaceAll("Mettez\\. vous", "Mettez-vous");

        // Clean up spaces / tags
        document = document.replaceAll("<b>[\\s\u00a0]*\"[\\s\u00a0]*</b>", "\"");
        document = document.replaceAll("<i[\\s\u00a0]*\"[\\s\u00a0]*</i>", "\"");
        document = document.replaceAll("<b>[\\s\u00a0]*</b>", "");
        document = document.replaceAll("<i>[\\s\u00a0]*</i>", "");
        document = document.replaceAll("<p>Si<i>", "<p>Si <i>");
        document = document.replaceAll("</i> <i>", " ");
        document = document.replaceAll("<i>(\\s+)", "$1<i>");
        document = document.replaceAll("(\\s+)</i>", "</i>$1");
        document = document.replaceAll("<i><br />", "<br /><i>");
        document = document.replaceAll("<br /></i>", "</i><br />");
        document = document.replaceAll("<i><br /></i>", "<br />");
        document = document.replaceAll("</b> <b>", " ");
        document = document.replaceAll("<b>(\\s+)", "$1<b>");
        document = document.replaceAll("(\\s+)</b>", "</b>$1");
        document = document.replaceAll("<b><br /></b>", "<br />");
        document = document.replaceAll("<br /></b>", "</b><br />");

        // Clean up italics
        document = document.replaceAll("</i> <i>", " ");
        document = document.replaceAll("cf<i>\\.", "cf.<i>");
        document = document.replaceAll("<i>(\\s+)", "$1<i>");
        document = document.replaceAll("(\\s+)</i>", "</i>$1");
        document = document.replaceAll("([.;:,?!])</i>", "</i>$1");


        // Process
        document = fixTypos(document);
        document = fixQuotes(document);
        document = fixBibleRefs(document);
        document = fixFootNotes(document);
        document = fixNumberedParagraphs(document);
        document = fixWhitespaces(document);

        // Clean end paras
        document = document.replaceAll("</p>\\s*</p>", "</p>");
        document = document.replaceAll("<b>\\s*</p>\\s*<p>\\s*</b>", "</p><p>");
        return document;
    }

    private static String cleanNotes(String document) {
        document = document.replaceAll("Acta Leonis XIII, t\\. XI\"", "Acta Leonis XIII, t. XI");
        document = document.replaceAll("Paoline, 1966\" p", "Paoline, 1966, p");
        document = document.replaceAll(" ler novembre 1885\"", " ler novembre 1885,");
        document = document.replaceAll("423\\. \\(31\\) Cf", "423.<br />(31) Cf");

        document = document.trim();
        document = document.replaceAll("<div[^>]*>|</div>|<span[^>]*>|</span>|<font[^>]*>|</font>", "");
        document = document.replaceAll("<table[^>]*>|</table>|<tr[^>]*>|</tr>|<td[^>]*>|</td>", "");
        document = document.replaceAll("<p>\\s*<b>\\s*<p>(.*?)</p>\\s*</b>\\s*</p>", "<p><b>$1</b></p>");
        document = "<p>" + document + "</p>";
        document = document.replaceAll("<br />", "</p><p>");
        document = document.replaceAll("<p[^>]*>©.*?</p>", "");
        document = document.replaceAll("<p>", "</p><p>");
        document = document.replaceAll("</p>\\s*<a", "</p><p><a");
        document = document.replaceAll("<p>\\s*<p>", "<p>");
        document = document.replaceAll("</p>\\s*</p>", "</p>");
        document = document.replaceAll("<p>\\s*</p>", "");
        if (document.startsWith("</p>")) {
            document = document.substring(4);
        }
        if (document.endsWith("<p>")) {
            document = document.substring(3);
        }
        document = document.replaceAll("<p>[\\s\u00a0]+", "<p>");
        document = document.replaceAll("<p><b><a[^>]*>([0-9]+)</a></b>", "<p><a>$1</a>");
        document = document.replaceAll("(<p><a[^>]*>)([0-9]+)(</a>)", "$1[$2]$3");
        document = document.replaceAll("<p>\\((<a[^>]*>)([0-9]+)(</a>)\\)", "<p>$1[$2]$3");
        document = document.replaceAll("<p>\\s*<a[^>]*>\\(</a><a[^>]*>([0-9]+)</a>\\)", "<p><a>[$1]</a>");
        document = document.replaceAll("<p>\\s*([0-9]+)\\. ", "<p><a>[$1]</a> ");
        document = document.replaceAll("(<p><a[^>]*>\\[[0-9]+\\]</a>)(\\S)", "$1 $2");
        document = document.replaceAll("\\.\\.\\.", "…");
        document = fixTypos(document);
        document = fixQuotes(document);
        document = fixBibleRefs(document);
        document = fixWhitespaces(document);
        document = document.replaceAll("[\\s\u00a0]+</p>", "</p>");
        document = document.replaceAll("<p>\\s*</p>", "");
        document = document.replaceAll("<p>\\(([0-9]+)\\) ", "<p><a>[$1]</a> ");
        document = document.replaceAll("<p><a[^>]*>\\[([0-9]+)\\]</a>", "<p id=\"fn$1\"><a class=\"ref\" href=\"#fnr$1\">[$1]</a>");
        document = document.replaceAll("(href=\"[^\"\\s]*)\\s*([^\"\\s]*)\\s*([^\"\\s]*)\\s*", "$1$2$3");
        document = document.replaceAll("(href=\"[^\"\\s]*)\\s*([^\"\\s]*)\\s*", "$1$2");
        document = document.replaceAll("(href=\"[^\"\\s]*)\\s*", "$1");
        document = document.replaceAll("http\u00a0://", "http://");
        document = document.replaceAll("<i>loc. cit.</i> ,", "<i>loc. cit.</i>,");
        document = document.replaceAll("<i>ibid.\\s+</i>,", "<i>ibid.</i>,");
        document = document.replaceAll("<i>Ibid.\\s+</i>,", "<i>Ibid.</i>,");
        document = document.replaceAll("<i>Ibid</i>.,", "<i>Ibid.</i>,");
        document = document.replaceAll("\\bIbid\\.,", "<i>Ibid.</i>,");
        document = document.replaceAll("\\bibid\\.,", "<i>ibid.</i>,");
        document = document.replaceAll("\\bibid,", "<i>ibid.</i>,");
        document = document.replaceAll("\\bIbid,", "<i>Ibid.</i>,");
        document = document.replaceAll("\\bIbid n", "<i>Ibid.</i>, n");
        document = document.replaceAll("<i>(\\s+)", "$1<i>");
        document = document.replaceAll("(\\s+)</i>", "</i>$1");
        document = document.replaceAll("<i>(<a[^>]*?>)ibid\\s*</a></i>\\s*<a[^>]*?>\\.\\s*</a>", "<i>$1ibid.</a></i>");
        document = document.replaceAll("( *\u00A0 *)( *\u00A0 *)*", "\u00A0");
        document = document.replaceAll("<p[^>]*>NOTES</p>", "");
        document = document.replaceAll(" +", " ");
        document = document.replaceAll("pp\\. 959- 960", "pp. 959-960");
        document = document.replaceAll("p\\. 674- 675", "p. 674-675");
        return document.trim();
    }

    private static String cleanCopyright(String document) {
        document = "<p>" + document + "</p>";
        return document;
    }

    private static String extract(String document, String pattern, int group) {
        return extract(document, pattern, group, 0);
    }

    private static String extract(String document, String pattern, int group, int start) {
        Matcher matcher = Pattern.compile(pattern).matcher(document);
        return matcher.find(start) ? matcher.group(group) : null;
    }

    private static String extractFollowingParaContaining(String document, String containing, int begin) {
        Matcher paragraph = Pattern.compile("<p[\\s\\S]*?</p>").matcher(document);
        int start = begin;
        int first = 0;
        int last = start;
        while (paragraph.find(start)) {
            start = paragraph.end();
            if (paragraph.group().matches("<p[^>]*>[\\s\u00a0]*</p>")) {
                continue;
            }
            if (!paragraph.group().matches(containing)) {
                break;
            }
            if (first == 0) {
                first = paragraph.start();
            }
            last = paragraph.end();
        }
        return first != 0 ? document.substring(first, last) : null;

    }

    private static boolean isInside(Matcher matcher, int pos) {
        int start = Math.max(0, pos - 100);
        while (start < pos) {
            if (!matcher.find(start)) {
                return false;
            }
            if (matcher.start() <= pos && pos < matcher.end()) {
                return true;
            }
            start = matcher.end();
        }
        return false;
    }


    private static String fixBibleRefs(String document) {
        document = document.replaceAll("\\(cf\\. 1 Pierre,", "(cf. <i>1 Pierre</i>,");
        document = document.replaceAll("\\((cf\\. )?(([12] )?[A-Za-z]+\\.?)((\\s*[1-9][0-9]*[,.;-]?)+)\\s*\\)", "($1<i>$2</i>$4)");
        document = document.replaceAll("<i>(([12] )?[A-Za-zé]+)</i>\\.", "<i>$1.</i>");
        document = document.replaceAll("([12] )<i>([A-Za-zé]+\\.?)</i>", "<i>$1$2</i>");
        document = document.replaceAll("([12])<i>([A-Za-zé]+\\.?)</i>", "<i>$1 $2</i>");
        document = document.replaceAll("(\\((cf\\. )?<i>[^<]*?</i>,? [0-9]+, [0-9]+)\\.\\)", "$1).");
        return document;
    }

    private static String fixFootNotes(String document) {
        // Check the kind of foot notes we have so far
        boolean parenthesis = document.indexOf("(1)") >= 0 && document.indexOf("(2)") >= 0 && document.indexOf("(3)") >= 0;
        boolean brackets    = document.indexOf("[1]") >= 0 && document.indexOf("[2]") >= 0 && document.indexOf("[3]") >= 0;
        boolean link        = document.indexOf(">1</a>") >= 0 && document.indexOf(">2</a>") >= 0 && document.indexOf(">3</a>") >= 0;
        boolean linkBracket = document.indexOf(">[1]</a>") >= 0 && document.indexOf(">[2]</a>") >= 0 && document.indexOf(">[3]</a>") >= 0;
        // Add parentheses around plain numbers corresponding to foot notes calls
        // All consecutive numbers are considered excluding those in attributes and bible citations
        if (!parenthesis && !brackets && !link && !linkBracket) {
            StringBuilder newDoc = new StringBuilder();
            Matcher matcher = Pattern.compile("([\\s\u00a0])([1-9][0-9]*)([\\s\u00a0,.:;!<])").matcher(document);
            Matcher biblCit = Pattern.compile("\\((cf\\. )?(<i>[^<]*?</i>,? ([1-9][0-9]*[,.;-]?\\s*(s?s\\.\\s*)?)+)+\\)").matcher(document);
            Matcher attribute = Pattern.compile(" [a-z\\-]+=\".*?\"").matcher(document);
            int body = document.indexOf("<body");
            int hr = document.indexOf("<hr");
            int start = 0;
            int appstart = 0;
            int nb = 1;
            while (matcher.find(start)) {
                if (matcher.start() > body && (matcher.start() < hr || hr == -1)) {
                    boolean isInBibleCit = isInside(biblCit, matcher.start());
                    boolean isInAttribute = isInside(attribute, matcher.start());
                    if (!isInBibleCit && !isInAttribute && Integer.parseInt(matcher.group(2)) == nb) {
                        newDoc.append(document.substring(appstart, matcher.start()));
                        newDoc.append(matcher.group(1));
                        newDoc.append("(");
                        newDoc.append(matcher.group(2));
                        newDoc.append(")");
                        newDoc.append(matcher.group(3));
                        nb++;
                        appstart = matcher.end();
                    }
                }
                start = matcher.end();
            }
            newDoc.append(document.substring(appstart, document.length()));
            document = newDoc.toString();
        }
        // Fix foot notes
        document = document.replaceAll("\\(([12][0-9][0-9]|[1-9][0-9]|[1-9])\\)(?!([^<]*<br />[^<]*)*</span>)", "<a id=\"fnr$1\" href=\"#fn$1\">$1</a>");
        document = document.replaceAll("(<a [^>]*>)\\[([1-9][0-9]*)\\](</a>)", "$1$2$3");
        document = document.replaceAll("<font[^>]*><sup>(<a[^>]*>[1-9][0-9]*</a>)</sup></font>", "$1");
        document = document.replaceAll("<a[^>]*>\\(</a><a[^>]*>([1-9][0-9]*)</a>\\)", "<a href=\"#fn$1\">$1</a>");
        document = document.replaceAll("[\\[\\(]?<a[^>]*>([1-9][0-9]*)</a>[\\]\\)]?", "<span class=\"footnote\" id=\"fnr$1\"><a href=\"#fn$1\">$1</a></span>");
        document = document.replaceAll("\\s+(<span class=\"footnote\">.*?</span>)", "$1");
        document = document.replaceAll("([\\.,;]\\s*)(<span class=\"footnote\".*?</span>)", "$2$1");
        return document;
    }

    private static String fixNumberedParagraphs(String document) {
        boolean hasDot = contains(document, "<p[^>]*>7\\. ") && contains(document, "<p[^>]*>8\\. ") && contains(document, "<p[^>]*>9\\. ");
        boolean hasDash = contains(document, "<p[^>]*>7 - ") && contains(document, "<p[^>]*>8 - ") && contains(document, "<p[^>]*>9 - ");
        boolean hasLink = contains(document, "<a id=\"7\\.\">") && contains(document, "<a id=\"8\\.\">") && contains(document, "<a id=\"9\\.\">");
        if (hasDash) {
            document = document.replaceAll("<p>([1-9][0-9]*) - ", "<p><a class=\"numpara\" id=\"p$1\">$1.</a> ");
        } else if (hasDot) {
            document = document.replaceAll("(<p[^>]*>)([1-9][0-9]*)\\. ", "$1<a class=\"numpara\" id=\"p$2\">$2.</a> ");
        } else if (hasLink) {
            document = document.replaceAll("<a id=\"([1-9][0-9]*)\\.\">", "<a class=\"numpara\" id=\"p$1\">");
        }
        return document;
    }

    private static boolean contains(String document, String regexp) {
        Matcher matcher = Pattern.compile(regexp).matcher(document);
        return matcher.find();
    }

    private static int indexOf(String document, String regexp) {
        Matcher matcher = Pattern.compile(regexp).matcher(document);
        if (matcher.find()) {
            return matcher.start();
        } else {
            return -1;
        }
    }

    static final Map<Pattern, String> TYPOS;

    static {
        TYPOS = new LinkedHashMap<Pattern, String>();

        TYPOS.put(Pattern.compile("CHRETIEN"), "CHRÉTIEN");
        TYPOS.put(Pattern.compile("EGLISE"), "ÉGLISE");
        TYPOS.put(Pattern.compile("Eglise"), "Église");
        TYPOS.put(Pattern.compile("Ecriture"), "Écriture");
        TYPOS.put(Pattern.compile("Egypte"), "Égypte");
        TYPOS.put(Pattern.compile("Etant"), "Étant");
        TYPOS.put(Pattern.compile("Etat"), "État");
        TYPOS.put(Pattern.compile("Epoux"), "Époux");
        TYPOS.put(Pattern.compile("Eternel"), "Éternel");
        TYPOS.put(Pattern.compile("Etendant"), "Étendant");
        TYPOS.put(Pattern.compile("Evêque"), "Évêque");
        TYPOS.put(Pattern.compile("Elisabeth"), "Élisabeth");
        TYPOS.put(Pattern.compile("EVANGILE"), "ÉVANGILE");
        TYPOS.put(Pattern.compile("Evangile"), "Évangile");
        TYPOS.put(Pattern.compile("EVEQUE"), "ÉVÊQUE");
        TYPOS.put(Pattern.compile("PRETRES"), "PRÊTRES");
        TYPOS.put(Pattern.compile("ONZIEME"), "ONZIÈME");
        TYPOS.put(Pattern.compile("EVANGELISATION"), "ÉVANGÉLISATION");
        TYPOS.put(Pattern.compile("METHODE"), "MÉTHODE");
        TYPOS.put(Pattern.compile("PORTEE"), "PORTÉE");
        TYPOS.put(Pattern.compile("MILLENAIRE"), "MILLÉNAIRE");
        TYPOS.put(Pattern.compile("HERAUTS"), "HÉRAUTS");
        TYPOS.put(Pattern.compile("MERE"), "MÈRE");
        TYPOS.put(Pattern.compile("PERE"), "PÈRE");
        TYPOS.put(Pattern.compile("MEDIATION"), "MÉDIATION");
        TYPOS.put(Pattern.compile("MISERICORDE"), "MISÉRICORDE");
        TYPOS.put(Pattern.compile("HERITAGE"), "HÉRITAGE");
        TYPOS.put(Pattern.compile("MYSTERE"), "MYSTÈRE");
        TYPOS.put(Pattern.compile("VERITE"), "VÉRITÉ");
        TYPOS.put(Pattern.compile("IMPLANTERENT"), "IMPLANTÈRENT");
        TYPOS.put(Pattern.compile("Etre"), "Être");
        TYPOS.put(Pattern.compile("educateurs"), "éducateurs");
        TYPOS.put(Pattern.compile("GENERATION"), "GÉNÉRATION");
        TYPOS.put(Pattern.compile("HUMANAE"), "HUMANÆ");
        TYPOS.put(Pattern.compile("VITAE"), "VITÆ");
        TYPOS.put(Pattern.compile("CAELIBATUS"), "CÆLIBATUS");
        TYPOS.put(Pattern.compile("OEUVRE"), "ŒUVRE");

        TYPOS.put(Pattern.compile("Incarnation ed la"), "Incarnation de la");
        TYPOS.put(Pattern.compile("\\baue\\b"), "que");
        TYPOS.put(Pattern.compile("\\bI'histoire"), "l'histoire");
        TYPOS.put(Pattern.compile("\\bI'Eucharistie"), "l'Eucharistie");
        TYPOS.put(Pattern.compile("\\bI'[e\u00e9]ducation"), "l'éducation");
        TYPOS.put(Pattern.compile("\\bI'[E\u00c9]glise"), "l'Église");
        TYPOS.put(Pattern.compile("\\bA "), "À ");
        TYPOS.put(Pattern.compile("([\\.\u00ab]) A "), "$1 À ");
        TYPOS.put(Pattern.compile("\\bO "), "Ô ");
        TYPOS.put(Pattern.compile("([\\.\u00ab]) O "), "$1 Ô ");
        TYPOS.put(Pattern.compile("suismoi"), "suis-moi");
        TYPOS.put(Pattern.compile("L '"), "L'");

        TYPOS.put(Pattern.compile("p. MANNA"), "P. MANNA");
        TYPOS.put(Pattern.compile("transperc</i>é"), "transpercé</i>");

        TYPOS.put(Pattern.compile("\\.\\.\\."), "…");

        // Split words
        TYPOS.put(Pattern.compile("mul- tiples"), "multiples");
        TYPOS.put(Pattern.compile("té- nèbres"), "ténèbres");
        TYPOS.put(Pattern.compile("sup- primés"), "supprimés");
        TYPOS.put(Pattern.compile("per- sonnes"), "personnes");
        TYPOS.put(Pattern.compile("histo- rique"), "historique");
        TYPOS.put(Pattern.compile("con- formes"), "conformes");
        TYPOS.put(Pattern.compile("con- naître"), "connaître");
        TYPOS.put(Pattern.compile("soi- même"), "soi-même");
        TYPOS.put(Pattern.compile("pro- clament"), "proclament");
        TYPOS.put(Pattern.compile("pers- pectives"), "perspectives");
        TYPOS.put(Pattern.compile("rassasiez- vous"), "rassasiez-vous");
        TYPOS.put(Pattern.compile("re- cherches"), "recherches");
        TYPOS.put(Pattern.compile("fa- milles"), "familles");
        TYPOS.put(Pattern.compile("poli- tiques"), "politiques");
        TYPOS.put(Pattern.compile("déve- loppent"), "développent");
        TYPOS.put(Pattern.compile("incompa- rable"), "incomparable");
        TYPOS.put(Pattern.compile("dia- logue"), "dialogue");
        TYPOS.put(Pattern.compile("affermissez- vous"), "affermissez-vous");
        TYPOS.put(Pattern.compile("non- guerre"), "non-guerre");
        TYPOS.put(Pattern.compile("léni- nisme"), "léninisme");
        TYPOS.put(Pattern.compile("Tiers- Monde"), "Tiers-Monde");
        TYPOS.put(Pattern.compile("Quart- Monde"), "Quart-Monde");
        TYPOS.put(Pattern.compile("lui- même"), "lui-même");
        TYPOS.put(Pattern.compile("bien- être"), "bien-être");
        TYPOS.put(Pattern.compile("est- ce"), "est-ce");
        TYPOS.put(Pattern.compile("par- lantes"), "parlantes");
        TYPOS.put(Pattern.compile("hom- me"), "homme");
        TYPOS.put(Pattern.compile("elle- même"), "elle-même");
        TYPOS.put(Pattern.compile("eux- même"), "eux-même");
        TYPOS.put(Pattern.compile("vigou- reuse"), "vigoureuse");
        TYPOS.put(Pattern.compile("par- ticipant"), "participant");
        TYPOS.put(Pattern.compile("par- tout"), "partout");
        TYPOS.put(Pattern.compile("lui- même"), "lui-même");
        TYPOS.put(Pattern.compile("avant- goût"), "avant-goût");
        TYPOS.put(Pattern.compile("ren- contres"), "rencontres");
        TYPOS.put(Pattern.compile("pourrait- on"), "pourrait-on");
        TYPOS.put(Pattern.compile("Moyen- Âge"), "Moyen-Âge");
        TYPOS.put(Pattern.compile("y a- t[ -]il"), "y a-t-il");
        TYPOS.put(Pattern.compile("subis- sent"), "subissent");
        TYPOS.put(Pattern.compile("tech- niques"), "techniques");
        TYPOS.put(Pattern.compile("dia- logue"), "dialogue");
        TYPOS.put(Pattern.compile("Jésus- Christ"), "Jésus-Christ");
        TYPOS.put(Pattern.compile("ré- flexion"), "réflexion");
        TYPOS.put(Pattern.compile("sup- porteront"), "supporteront");
        TYPOS.put(Pattern.compile("est-à- dire"), "est-à-dire");
        TYPOS.put(Pattern.compile("com- portements"), "comportements");
        TYPOS.put(Pattern.compile("Ephè- se"), "Éphèse");
        TYPOS.put(Pattern.compile("ré- pondre"), "répondre");
        TYPOS.put(Pattern.compile("œcumé- nique"), "œcuménique");

        TYPOS.put(Pattern.compile("10, 16- 17"), "10, 16-17");
    }

    private static String fixTypos(String document) {
        for (Map.Entry<Pattern, String> typo : TYPOS.entrySet()) {
            document = typo.getKey().matcher(document).replaceAll(typo.getValue());
        }
        return document;
    }

    private static String getFull(String name) {
        if ("Benoît XVI".equals(name)) {
            return "benedict_xvi";
        } else if ("Jean-Paul II".equals(name)) {
            return "john_paul_ii";
        } else if ("Paul VI".equals(name)) {
            return "paul_vi";
        } else if ("François".equals(name)) {
            return "francesco";
        }
        throw new IllegalStateException("Unknown: " + name);
    }


    private static List<String> wrapMinWidth(String[] words, int n) {
        List<String> r = new ArrayList<String>();
        String l = "";
        for (String w : words) {
            if (l.length() > 0 && w.length() + l.length() > n) {
                r.add(l);
                l = "";
            }
            if (l.length() > 0) {
                l = l + " " + w;
            } else {
                l = w;
            }
        }
        if (l.length() > 0) {
            r.add(l);
        }
        return r;
    }

    public static List<String> minLines(String phrase, int lines) {
        String[] words = phrase.split(" ");
        int hi = 0;
        int lo = Integer.MAX_VALUE;
        for (String word : words) {
            hi += word.length();
            lo = Math.min(lo, word.length());
        }
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            List<String> v = wrapMinWidth(words, mid);
            if (v.size() > lines) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return wrapMinWidth(words, lo);
    }

}