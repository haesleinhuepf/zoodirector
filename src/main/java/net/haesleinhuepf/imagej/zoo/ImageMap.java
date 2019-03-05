package net.haesleinhuepf.imagej.zoo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class ImageMap {
    public static void main(String... args) throws FileNotFoundException, UnsupportedEncodingException {
        StringBuilder index = new StringBuilder();
        index.append("<html><body>");

        File rootFolder = new File("C:/structure/mpicloud/zoo");

        for (File subFolder : rootFolder.listFiles()) {
            if (subFolder.isDirectory()) {
                String folderName = subFolder.getName();
                for (File file : subFolder.listFiles()) {
                    String filename = file.getName();
                    if (filename.endsWith(".gif")) {
                        index.append("<img src=\"" + folderName + "/" + filename + "\" height=\"300\"/>");
                    }
                }
            }
        }

        index.append("</body></html>");

        PrintWriter writer = new PrintWriter(rootFolder.getAbsolutePath() + "/images.html", "UTF-8");
        writer.println(index);
        writer.close();
    }
}
