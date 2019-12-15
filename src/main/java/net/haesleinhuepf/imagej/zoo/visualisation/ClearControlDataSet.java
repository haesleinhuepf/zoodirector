package net.haesleinhuepf.imagej.zoo.visualisation;

import ij.ImageJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.gui.NewImage;
import ij.io.Opener;
import ij.plugin.FolderOpener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * ClearControlDataSet
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */
public class ClearControlDataSet {

    private int[] indizes;
    private double[] timesInSeconds;
    private double[] timesInMinutes;
    private double maxTimeInSeconds;
    private long[] widths;
    private long[] heights;
    private long[] depths;
    private double[] framesPerMinute = null;

    private double[] voxelDimXs;
    private double[] voxelDimYs;
    private double[] voxelDimZs;
    private String path;
    private String dataset;

    public ClearControlDataSet(String path, String dataset) {
        this.path = path;
        this.path = this.path.replace("\\","/");
        if (!this.path.endsWith("/")){
            this.path = this.path + "/";
        }


        this.dataset = dataset;

        File folder = new File(path);
        if (!folder.exists()) {
            throw new IllegalArgumentException("Folder " + path + " doesn't exist.");
        }

        // ---------------------------------------------------------------------------
        // read index
        List<String> index;
        try {
            index = Files.readAllLines(Paths.get(path + dataset + ".index.txt"));
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
            //e.printStackTrace();
        }

        indizes = new int[index.size()];
        timesInSeconds = new double[index.size()];
        timesInMinutes = new double[index.size()];
        widths = new long[index.size()];
        heights = new long[index.size()];
        depths = new long[index.size()];


        voxelDimXs = new double[index.size()];
        voxelDimYs = new double[index.size()];
        voxelDimZs = new double[index.size()];

        int count = 0;
        for (String line : index) {
            if (line.length()==0) {
                continue;
            }
            String[] tabSeparated = line.split("\t");
            indizes[count] = Integer.parseInt(tabSeparated[0].trim());
            timesInSeconds[count] = Double.parseDouble(tabSeparated[1].trim());
            timesInMinutes[count] = timesInSeconds[count] / 60;
            maxTimeInSeconds = timesInSeconds[count];

            String sizes = tabSeparated[2];
            String[] commaSeparatedSizes = sizes.split(",");
            widths[count] = Long.parseLong(commaSeparatedSizes[0].trim());
            heights[count] = Long.parseLong(commaSeparatedSizes[1].trim());
            depths[count] = Long.parseLong(commaSeparatedSizes[2].trim());

            count++;
        }

        // ---------------------------------------------------------------------------
        // read meta data
        try {
            index = Files.readAllLines(Paths.get(path + dataset + ".metadata.txt"));
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
            //e.printStackTrace();
        }
        count = 0;
        for (String line : index) {
            if (line.length() == 0) {
                continue;
            }
            line = line.replace("{", "").replace("}", "");

            String[] keyValuePairs = line.split(",");
            for (String keyValuePair : keyValuePairs) {
                String[] temp = keyValuePair.split(":");
                String key = temp[0];
                String value = temp[1];
                if (key.contains("VoxelDimX")) {
                    voxelDimXs[count] = Double.parseDouble(value.trim());
                } else if (key.contains("VoxelDimY")) {
                    voxelDimYs[count] = Double.parseDouble(value.trim());
                } else if (key.contains("VoxelDimZ")) {
                    voxelDimZs[count] = Double.parseDouble(value.trim());
                }
            }
            count++;
        }
    }

    private ImagePlus data = null;
    public ImagePlus getImageData(){
        if (data == null) {
            data = VirtualRawStackOpener.open(
                    path + "stacks/" + dataset + "/",
                    (int) widths[0], (int) heights[0], (int) depths[0],
                    timesInSeconds.length, 16, true,
                    voxelDimXs[0], voxelDimYs[1], voxelDimZs[0], "micron"
            );
        }
        return data;
    }

    private ImagePlus thumbnails = null;
    public ImagePlus getThumbnails() {
        if (thumbnails == null) {
            String[] potentialThumbnailsFolders = {
                    path + "stacks/thumbnails_sb_text",
                    path + "stacks/thumbnails",
                    path + "stacks/thumbnails_max",
                    path + "stacks/thumbnails_back",
                    path + "stacks/max_proj",
                    path + "stacks/C0opticsprefused_cylproj"
            };

            for (String thumbnailfolder : potentialThumbnailsFolders) {
                if (new File(thumbnailfolder).exists() && new File(thumbnailfolder).listFiles().length > 1) {
                    thumbnails = FolderOpener.open(thumbnailfolder, "virtual");
                    break;
                }
            }
        }
        if (thumbnails == null) {
            thumbnails = NewImage.createByteImage("no thumbnails found", 1, 1,1, NewImage.FILL_BLACK);
        }
        return thumbnails;
    }

    public double[] getTimesInSeconds() {
        return timesInSeconds;
    }
    public double[] getTimesInMinutes() {
        return timesInMinutes;
    }


    public double[] getFramesPerMinute() {
        if (framesPerMinute == null) {
            framesPerMinute = new double[((int) (maxTimeInSeconds / 60)) + 1];

            int lastIndex = 0;
            int frameCount = 0;
            for (int i = 0; i < timesInSeconds.length; i++) {
                int currentIndex = (int) (timesInSeconds[i] / 60);
                frameCount++;
                if (currentIndex != lastIndex) {
                    framesPerMinute[currentIndex] = frameCount;
                    frameCount = 0;
                }
                lastIndex = currentIndex;
            }
        }
        return framesPerMinute;
    }

    public static double[] ramp(int numEntries) {
        double[] array = new double[numEntries];
        for (int i = 0; i < numEntries; i++) {
            array[i] = i;
        }
        return array;
    }

    public int getFirstFrameAfterTime(double timeInSeconds) {
        for (int i = 0; i < timesInSeconds.length; i++) {
            if (timesInSeconds[i] > timeInSeconds) {
                return i;
            }
        }
        return 0;
    }

    public String[] getMeasurementFiles() {
        ArrayList<String> list = new ArrayList<>();
        File folder = new File(path);
        for (File file : folder.listFiles()) {
            String filename = file.getName().toLowerCase();
            if (filename.endsWith(".tsv") || filename.endsWith(".tsv")) {
                list.add(file.getName());
            }
        }

        String[] filenames = new String[list.size()];
        list.toArray(filenames);
        return filenames;
    }

    public String getPath() {
        return path;
    }

    public static void main(String... args) {
        new ImageJ();

        ClearControlDataSet ccds = new ClearControlDataSet("C:/structure/data/2019-10-28-17-22-59-23-Finsterwalde_Tribolium_nGFP/", "C0opticsprefused");

        ccds.getImageData().show();

        System.out.println("time0: " + ccds.getTimesInSeconds()[0]);
        System.out.println("time1: " + ccds.getTimesInSeconds()[1]);
    }

}
