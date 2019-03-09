package net.haesleinhuepf.imagej.zoo;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import sc.fiji.io.Gif_Stack_Writer;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

public class ZooDirector {
    public static void main(String... args) throws IOException {
        StringBuilder index = new StringBuilder();
        index.append("<html><body>");
        index.append("<table>");
        File rootFolder = new File("C:/structure/mpicloud/zoo");

        for (File subFolder : rootFolder.listFiles()) {
            if (subFolder.isDirectory()) {
                String folderName = subFolder.getName();

                StringBuilder images = new StringBuilder();
                StringBuilder imageNames = new StringBuilder();
                boolean imageFound = false;

                HashMap<String, String> propertyMap = new HashMap<>();

                ArrayList<String> additionalFiles = new ArrayList<>();

                for (File file : subFolder.listFiles()) {
                    String filename = file.getName();

                    //
                    //File file = new File(rootFolder.getAbsolutePath() + "/" + folderName + "/" + filename);

                    if (filename.endsWith(".txt") &&
                            !filename.endsWith("log.txt") &&
                            !filename.endsWith("index.txt") &&
                            !filename.endsWith("metadata.txt") &&
                            !filename.endsWith("program.txt") &&
                            !filename.endsWith("scheduleLog.txt") &&
                            !filename.endsWith("DataClean.txt") &&
                            !filename.endsWith("DataRaw.txt") &&
                            !filename.endsWith("Florence.txt") &&
                            !filename.endsWith("Jarakta3.txt") &&
                            !filename.endsWith("Seoul.txt") &&
                            !filename.endsWith("Manila.txt") &&
                            !filename.endsWith("Karatschi.txt") &&
                            !filename.endsWith("Kairo.txt") &&
                            file.length() > 0) {
                        propertyMap.put(filename, readFile(file.getAbsolutePath()).replace("\n", "<br/>"));
                    } else if (filename.endsWith(".gif")) {

                        // // -Dplugins.dir=C:\Programs\fiji-win64\Fiji.app\plugins\
                        //ImagePlus video = IJ.openImage(file.getAbsolutePath());
                        //video.show();
                        //IJ.run(video,"Animated Gif ... ", "name=[" + filename + "] set_global_lookup_table_options=[Do not use] optional= image=[No Disposal] set=100 number=1000 transparency=[No Transparency] red=0 green=0 blue=0 index=0 filename=[" + file.getAbsolutePath() + "]");

                        imageNames.append("<td><b>" + filename + "</b></td><br/>\n");
                        images.append("<td><img src=\"" + folderName + "/" + filename + "\" height=\"150\"/></td>\n");
                        imageFound = true;
                    } else if (checkProperty("frames", filename, propertyMap)) {
                    } else if (checkProperty("samples", filename, propertyMap)) {
                    } else if (checkProperty("timepoints", filename, propertyMap)) {
                    } else if (checkProperty("framedelay", filename, propertyMap)) {
                    } else if (checkProperty("dead", filename, propertyMap)) {
                    } else if (checkProperty("not healthy", filename, propertyMap)) {
                    } else if (checkProperty("hyperdrive experiment", filename, propertyMap)) {
                    } else if (checkProperty("fused", filename, propertyMap)) {
                    } else if (checkProperty("unfused", filename, propertyMap)) {
                    } else if (checkProperty("samplesource", filename, propertyMap)) {
                    } else if (checkProperty("sampleauthor", filename, propertyMap)) {
                    } else if (checkProperty("rake experiment", filename, propertyMap)) {
                    } else if (checkProperty("defocus experiment", filename, propertyMap)) {
                    } else if (checkProperty("defocus_experiment", filename, propertyMap)) {
                    } else if (checkProperty("dual-channel", filename, propertyMap)) {
                    } else if (checkProperty("author", filename, propertyMap)) {
                    } else if (checkProperty("wunderbar", filename, propertyMap)) {
                    } else if (checkProperty("ARCHIVE", filename, propertyMap)) {
                    } else if (checkProperty("DELETE", filename, propertyMap)) {
                    } else if (checkProperty("experiment", filename, propertyMap)) {
                    } else if (checkProperty("microscope", filename, propertyMap)) {
                    } else if (checkProperty("KEEP", filename, propertyMap)) {
                    } else if (checkProperty("make_video", filename, propertyMap)) {
                    } else if (checkProperty("488", filename, propertyMap)) {
                    } else if (checkProperty("594", filename, propertyMap)) {
                    } else if (checkProperty("wavelength", filename, propertyMap)) {
                    } else if (checkProperty("size", filename, propertyMap)) {
                    } else if (checkProperty("okish", filename, propertyMap)) {
                    } else if (checkProperty("action", filename, propertyMap)) {
                    } else if (checkProperty("sample", filename, propertyMap)) {
                    } else if (checkProperty("internal use only", filename, propertyMap)) {
                    } else {
                        additionalFiles.add(filename);
                    }
                }



                index.append("<tr><td colspan=\"3\"><b>" + folderName + "</b></td></tr>\n");

                index.append("<tr><td>");
                if (propertyMap.keySet().size() > 0) {
                    index.append("<table border=\"1\"><tr>");
                    for (String key : propertyMap.keySet()) {
                        index.append("<tr><td>" + key + "</td><td>" + propertyMap.get(key) + "</td></tr>");
                    }
                    index.append("</table>");
                }
                index.append("</td>\n");


                index.append("<td>");
                if (imageFound) {
                    index.append("<table><tr>");
                    index.append(imageNames);
                    index.append("</tr><tr>");
                    index.append(images);
                    index.append("</tr></table>");
                } else {
                    index.append("&nbsp;");
                }
                index.append("</td>\n");

                index.append("<td>");
                for (String filename : additionalFiles) {
                    String description = "";
                    if (filename.endsWith(".txt")) {
                        BufferedReader reader = new BufferedReader(new FileReader(rootFolder.getAbsolutePath() + "/" + folderName + "/" + filename));
                        int lines = 0;
                        while (reader.readLine() != null) lines++;
                        reader.close();

                        description = " (" + lines + " lines)";
                    }
                    index.append("<li><a href=\"" + folderName + "/" + filename + "\"/>" + filename + description + "</a></li>\n");
                }
                index.append("</td></tr>");

            }
        }
        index.append("</table>");
        index.append("</body></html>");

        PrintWriter writer = new PrintWriter(rootFolder.getAbsolutePath() + "/index.html", "UTF-8");
        writer.println(index);
        writer.close();
    }

    private static boolean checkProperty(String property, String filename, HashMap<String, String> propertyMap) {
        if (filename.toLowerCase().startsWith(property.toLowerCase())) {



            filename = filename.substring(property.length());
            filename = filename.substring(0, filename.length() - 4);
            filename = filename.replace("_", " ");
            filename = filename.trim();

            propertyMap.put(property, filename);
            return true;
        }
        return false;
    }

    private static String readFile(String filename) {
        System.out.println("Reading " + filename);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
