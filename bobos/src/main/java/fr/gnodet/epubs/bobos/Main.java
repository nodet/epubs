package fr.gnodet.epubs.bobos;

import java.awt.*;

import fr.gnodet.epubs.core.Cover;

import static fr.gnodet.epubs.core.IOUtil.writeToFile;

public class Main {

    public static void main(String[] args) throws Exception {

        String title = "Les Bobos en Vérité";
        String creator = "Philippe Ariño";

            byte[] coverPng = Cover.generateCoverPng(Math.random(),
                    title,
                    new Object[]{
                            new Cover.Text(creator, new Font(Font.SERIF, Font.PLAIN, 58), 1.0, 0.0),
                            new Cover.Break(),
                            new Cover.Text(title, new Font(Font.SERIF, Font.PLAIN, 118), 1.2, 0.25),
                            new Cover.Break(),
                    },
                    null);
            System.out.println("Copying cover to: " + System.getProperty("basedir") + "/target/docbkx/epub3/");
            writeToFile(coverPng, System.getProperty("basedir") + "/target/docbkx/epub3/cover.png");
    }

}
