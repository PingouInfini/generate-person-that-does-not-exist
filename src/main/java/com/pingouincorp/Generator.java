package com.pingouincorp;

import org.apache.commons.cli.*;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Generator {

    public static void main(String[] args) throws Exception {
        HelpFormatter formatter = new HelpFormatter();
        Options options = new Options();
        options.addOption("help", false, "Affichage de l'aide.")
                .addOption("n", "number", true, "Nombre de photos a generer")
                .addOption("o", "output", true, "[REQUIRED] Spécifier un répertoire de sortie");

        final CommandLineParser parser = new BasicParser();
        final CommandLine line;

        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
            formatter.printHelp("Generator", options);
            return;
        }

        if (line.hasOption("help") || !line.hasOption("output")) {
            formatter.printHelp("Generator", options);
            return;
        }

        String destinationDirectory = line.getOptionValue("output");
        try {
            File f = new File(destinationDirectory);
            if (!f.exists() || !f.isDirectory())
                f.mkdirs();
        } catch (Exception e) {
            System.out.println("destinationDirectory : " + destinationDirectory + " n'est pas un répertoire correct");
            formatter.printHelp("Generator", options);
            return;
        }

        int numberOfPhotos = 1;
        if (line.hasOption("number"))
            numberOfPhotos = Integer.valueOf(line.getOptionValue("number"));

        String imageUrl = "https://thispersondoesnotexist.com/image";
        String destinationFile = "image.jpg";

        Set<String> md5SetOfFiles = new HashSet<>();
        for (int i = 0; i < numberOfPhotos; i++) {
            String md5 = saveImage(imageUrl, destinationDirectory, i, md5SetOfFiles);
            if (md5 == null)
                i--;
            else
                md5SetOfFiles.add(md5);
            TimeUnit.MILLISECONDS.sleep(500);
        }
    }

    public static String saveImage(String imageUrl, String destinationFile, int i, Set<String> md5SetOfFiles) throws IOException {
        URL url = new URL(imageUrl);
        InputStream is = url.openStream();
        InputStream is2 = url.openStream();

        String md5 = DigestUtils.md5Hex(is2.readAllBytes());
        if (md5SetOfFiles.contains(md5))
            return null;

        OutputStream os = new FileOutputStream(destinationFile + "/" + i + ".png");

        byte[] b = new byte[2048];
        int length;

        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }

        is.close();
        os.close();
        return md5;
    }
}
