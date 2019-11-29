package net.haesleinhuepf.imagej.zoo;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clijx.CLIJx;
import sc.fiji.io.My_Gif_Stack_Writer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;

import static net.haesleinhuepf.imagej.zoo.ZooDirector.readFile;

public class Thumbnailer {

    static int numFramesPerVideo = 50;

    public static void main(String... args) {
        String[] folders = {
                "\\\\fileserver.mpi-cbg.de\\myersspimdata\\Dani\\",
                "\\\\fileserver.mpi-cbg.de\\myersspimdata\\IMAGING\\archive_data_good\\",
                "\\\\fileserver.mpi-cbg.de\\myersspimdata\\Robert\\"
        };

        for (String folder : folders) {

            int i = 0;
            for (File subfolder : new File(folder).listFiles()) {
                i++;
                //System.out.println("i " + i);
                if (subfolder.isDirectory()) {
                    boolean thumbnail_exists = false;
                    for (File file : subfolder.listFiles()) {
                        if (file.toString().endsWith(".gif") && (!file.toString().contains("scan_")) && (!file.toString().contains("_AutoGif"))) {
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


        //System.out.println("Scanning " + folder);

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
            //System.out.println(imageFolder);
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

        if ((sourceFolder + "").contains("thumb") && numFiles > numFramesPerVideo) {
            makeGif(targetFolder, sourceFolder);
        }
/*
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
                    //System.out.println("w " + buffer.getWidth());
                    //System.out.println("h " + buffer.getHeight());
                    //System.out.println("d " + buffer.getDepth());

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
        }*/
    }

    private static void makeGif(String targetFolder, File sourceFolder) {
        File[] files = sourceFolder.listFiles();
        int numFiles = files.length;

        ArrayList<File> fileList = new ArrayList<File>();
        for (int i = 0; i < numFiles; i++) {
            fileList.add(files[i]);
        }

        Collections.sort(fileList);


        CLIJx clijx = CLIJx.getInstance();
        ClearCLBuffer stack = null;
        int sliceCount = 0;
        for (int i = 0; i < numFiles; i += numFiles / numFramesPerVideo) {
            File file = fileList.get(i);
            if (file.toString().endsWith(".tif")) {

                ClearCLBuffer buffer = null;
                if (file.toString().endsWith(".tif")) {
                    buffer = clijx.readImageFromDisc(file.toString());
                } else {
                    continue;
                }

                //System.out.println("w " + buffer.getWidth());
                //System.out.println("h " + buffer.getHeight());
                //System.out.println("d " + buffer.getDepth());

                if (buffer.getDepth() > 1) {
                    buffer.close();
                    continue;
                }

                if (stack == null) {
                    stack = clijx.create(new long[]{buffer.getWidth(), buffer.getHeight(), numFramesPerVideo}, NativeTypeEnum.UnsignedShort);
                }

                clijx.copySlice(buffer, stack, sliceCount);
                buffer.close();
                sliceCount++;
            }
        }

        if (stack != null) {
            ImagePlus imp = clijx.pull(stack);
            imp.setZ(imp.getNSlices() / 2);
            IJ.run(imp, "Enhance Contrast", "saturated=0.35");
            IJ.run(imp, "8-bit", "");
            stack.close();
            //System.out.println("stack " + stack.getNativeType());
            //System.out.println("imp " + imp.getBitDepth());

            String[] temp = targetFolder.replace("\\", "/").split("/");
            String temp2 = temp[temp.length - 1];
            temp = temp2.split("_");
            temp2 = temp[0];
            temp = temp2.split("-");
            temp2 = temp[temp.length - 1];

            System.out.println(targetFolder + "/" + temp2 + "_AutoGif.gif");
            My_Gif_Stack_Writer.writeGif(imp, targetFolder + "/" + temp2 + "_AutoGif.gif");
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
