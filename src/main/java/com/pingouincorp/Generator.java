package com.pingouincorp;

import org.apache.commons.cli.*;
import org.apache.commons.codec.digest.DigestUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Generator {

    public static void main(String[] args) throws Exception {
        HelpFormatter formatter = new HelpFormatter();
        Options options = new Options();
        options.addOption("help", false, "Affichage de l'aide.")
                .addOption("n", "number", true, "Nombre de photos a generer")
                .addOption("q", "quality", true, "Qualité entre 0.0 et 1.0")
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

        float quality = 0.5f;
        if (line.hasOption("quality"))
            quality = Float.valueOf(line.getOptionValue("quality"));

        String destinationDirectory = line.getOptionValue("output");
        try {
            File f = new File(destinationDirectory);
            if (!f.exists()) {
                boolean done = f.mkdirs();
                if (!done || !f.isDirectory())
                    return;
            }
        } catch (Exception e) {
            System.out.println("destinationDirectory : " + destinationDirectory + " n'est pas un répertoire correct");
            formatter.printHelp("Generator", options);
            return;
        }

        int numberOfPhotos = 1;
        if (line.hasOption("number"))
            numberOfPhotos = Integer.valueOf(line.getOptionValue("number"));

        String imageUrl = "https://thispersondoesnotexist.com/image";

        Set<String> md5SetOfFiles = new HashSet<>();
        for (int i = 0; i < numberOfPhotos; i++) {
            String md5 = saveImage(imageUrl, destinationDirectory + "/" + i + ".png", quality, md5SetOfFiles);
            if (md5 == null)
                i--;
            else
                md5SetOfFiles.add(md5);
            TimeUnit.MILLISECONDS.sleep(500);
        }
    }

    private static String saveImage(String imageUrl, String output, float quality, Set<String> md5SetOfFiles) throws IOException {
        URL url = new URL(imageUrl);

        InputStream is = url.openStream();

        OutputStream os = new FileOutputStream(output);
        BufferedImage image = ImageIO.read(is);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bos );
        String md5 = DigestUtils.md5Hex(bos.toByteArray());
        if (md5SetOfFiles.contains(md5))
            return null;


        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext())
            throw new IllegalStateException("No writers found");

        ImageWriter writer = writers.next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(os);
        writer.setOutput(ios);
        ImageWriteParam param = writer.getDefaultWriteParam();

        // compress to a given quality
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        // appends a complete image stream containing a single image and
        //associated stream and image metadata and thumbnails to the output
        writer.write(null, new IIOImage(image, null, null), param);

        // close all streams
        is.close();
        os.close();
        ios.close();
        writer.dispose();

        return md5;
    }
}
