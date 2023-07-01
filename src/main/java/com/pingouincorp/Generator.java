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
import java.sql.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Generator {

    public static void main(String[] args) throws Exception {
        HelpFormatter formatter = new HelpFormatter();
        Options options = new Options();
        options.addOption("help", false, "Affichage de l'aide.")
                .addOption("n", "number", true, "Nombre de photos a generer [defaut:1]")
                .addOption("q", "quality", true, "Qualité entre 0.0 et 1.0 (impact la taille du fichier) [defaut:0.5]")
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

        String imageUrl = "https://thispersondoesnotexist.com";

        Set<String> md5SetOfFiles = new HashSet<>();
        char[] animationChars = new char[]{'|', '/', '-', '\\'};

        for (int i = 0; i <= numberOfPhotos; i++) {
            System.out.print("Processing: " + (i*100)/numberOfPhotos + "% " + animationChars[i % 4] + "\r");

            String md5 = saveImage(imageUrl, destinationDirectory + "/" + i + ".png", quality, md5SetOfFiles);
            if (md5 == null)
                i--;
            else
                md5SetOfFiles.add(md5);
            TimeUnit.MILLISECONDS.sleep(500);
        }

        System.out.println("Processing: Done!");
    }


    private static void writeFileFromDatabase() {
        String url = "jdbc:postgresql://localhost:5432/facereco";
        String user = "facereco";
        String password = "facereco";

        try (Connection con = DriverManager.getConnection(url, user, password);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM photos")) {
            int i = 0;

            while (rs.next()) {
                i++;
                byte[] myByteArray = rs.getBytes(2);
                String basefilename = "c:\\temp\\photos\\test-";

                InputStream is = new ByteArrayInputStream(myByteArray);
                try {
                    BufferedImage bi = ImageIO.read(is);
                    writeAsImage(bi, basefilename + "A" + i + ".png", 0.5f);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (SQLException ex) {

            Logger lgr = Logger.getLogger(Generator.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }


    private static void insertPhotoInDatabase(byte[] imageToByteARray) {
        String url = "jdbc:postgresql://localhost:5432/facereco";
        String user = "facereco";
        String password = "facereco";

        try {
            Connection conn = DriverManager.getConnection(url, user, password);


            PreparedStatement ps = conn.prepareStatement("INSERT INTO photos (photo) VALUES (?)");
            ps.setBytes(1, imageToByteARray);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(Generator.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private static String saveImage(String imageUrl, String output, float quality, Set<String> md5SetOfFiles) throws IOException {
        URL url = new URL(imageUrl);

        InputStream is = url.openStream();

        OutputStream os = new FileOutputStream(output);
        BufferedImage image = ImageIO.read(is);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bos);
        byte[] imageToByteArray = bos.toByteArray();

        /*
         TODO INSERT in database...
         insertPhotoInDatabase(imageToByteArray);
         writeFileFromDatabase();
        */

        String md5 = DigestUtils.md5Hex(imageToByteArray);
        if (md5SetOfFiles.contains(md5))
            return null;

        writeAsImage(image, output, quality);
        is.close();

        return md5;
    }

    private static void writeAsImage(BufferedImage image, String output, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext())
            throw new IllegalStateException("No writers found");

        ImageWriter writer = writers.next();
        OutputStream os = new FileOutputStream(output);
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
        os.close();
        ios.close();
        writer.dispose();
    }
}

