package net.haesleinhuepf.imagej.zoo;

import ij.IJ;
import ij.ImagePlus;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clijx.CLIJx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import static net.haesleinhuepf.imagej.zoo.ZooDirector.readFile;

public class Thumbnailer {
    public static void main(String... args) {
        String[] folders = {
                "\\\\fileserver\\myersspimdata\\Robert\\"
        };

        for (String folder : folders) {

            int i = 0;
            for (File subfolder : new File(folder).listFiles()) {
                i++;
                System.out.println("i " + i);
                if (subfolder.isDirectory()) {
                    boolean thumbnail_exists = false;
                    for (File file : subfolder.listFiles()) {
                        if (file.toString().endsWith(".gif")) {
                            thumbnail_exists = true;
                            break;
                        }
                    }
                    if (!thumbnail_exists) {
                        makeThumbnail(subfolder.toString());
                    }
                }
            }
        }
    }

    private static void makeThumbnail(String folderName) {
        File folder = new File(folderName + "/stacks");


        System.out.println("Scanning " + folder);

        if (!folder.exists()) {
            return;
        }

        File thumbnailFolder = null;
        File imageFolder = null;
        for (File subFolder : folder.listFiles()) {
            if (subFolder.toString().contains("fused") ||
                subFolder.toString().contains("\\C") ||
                subFolder.toString().contains("default") ||
                subFolder.toString().contains("sequential") ||
                subFolder.toString().contains("interleaved")
            ) {
                imageFolder = subFolder;
            }
            if (subFolder.toString().contains("thumb")) {
                thumbnailFolder = subFolder;
            }
        }

        if (imageFolder != null) {
            System.out.println(imageFolder);
            long sumSize = 0;
            for (File file : imageFolder.listFiles()) {
                sumSize += file.length();
            }
            writeFile(folderName + "/scanframes.txt", "" + imageFolder.listFiles().length);
            writeFile(folderName + "/scanframessize.txt", "" + (sumSize / 1024 / 1024 / 1024) + " GB");
        }

        if (thumbnailFolder != null) {
            makeThumbnails(folderName, thumbnailFolder);
        } else if (imageFolder != null) {
            makeThumbnails(folderName, imageFolder);
        }
        // ImagePlus imp = "\\fileserver\myersspimdata\Dani\2019-06-13-16-43-32-23-Kalmar_Tribolium_nGFP_2nd\stacks\C0opticsprefused";



    }

    private static void makeThumbnails(String targetFolder, File sourceFolder) {
        int numFiles = sourceFolder.listFiles().length;
        for (int i = 0; i < numFiles; i++) {
            File file = sourceFolder.listFiles()[i];
            if (file.toString().endsWith(".tif") ||
                file.toString().endsWith(".raw")) {
                CLIJx clijx = CLIJx.getInstance();

                ClearCLBuffer buffer = null;
                if (file.toString().endsWith(".tif")) {
                    buffer = clijx.op.readImageFromDisc(file.toString());
                } else {
                    String metaFilename = targetFolder + "/" + sourceFolder.getName() + ".index.txt";
                    if (new File(metaFilename).exists()) {
                        String meta = readFile(metaFilename);
                        String[] line = meta.split("\n")[0].split("\t")[2].split(",");
                        long width = Integer.parseInt(line[0].trim());
                        long height = Integer.parseInt(line[1].trim());
                        long depth = Integer.parseInt(line[2].trim());
                        if (depth < 200) {
                            buffer = clijx.op.readRawImageFromDisc(file.toString(), (int)width, (int)height, (int)depth, 16);
                        }
                    }
                }

                if (buffer != null) {
                    System.out.println("w " + buffer.getWidth());
                    System.out.println("h " + buffer.getHeight());
                    System.out.println("d " + buffer.getDepth());

                    ClearCLBuffer maxProjection = buffer;
                    if (buffer.getDepth() > 1) {
                        maxProjection = clijx.create(new long[]{buffer.getWidth(), buffer.getHeight()}, buffer.getNativeType());
                        clijx.op.maximumZProjection(buffer, maxProjection);
                    }
                    ImagePlus imp = clijx.pull(maxProjection);

                    IJ.run(imp, "Enhance Contrast", "saturated=0.35");
                    IJ.run(imp, "8-bit", "");

                    IJ.save(imp, targetFolder + "/scan_thumb_" + i + ".gif");

                    if (i > numFiles / 2) {
                        if (i < numFiles - 1) {
                            i = numFiles - 2;
                        } else {
                            break;
                        }
                    } else {
                        i = numFiles / 2;
                    }
                    if (numFiles < 5) {
                        break;
                    }
                }
            }
        }
    }

    private static void writeFile(String filename, String content) {
        try {
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            writer.println(content);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
