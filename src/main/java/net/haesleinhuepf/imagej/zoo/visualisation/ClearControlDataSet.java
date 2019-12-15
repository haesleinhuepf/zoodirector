package net.haesleinhuepf.imagej.zoo.visualisation;

import ij.ImageJ;
import ij.ImagePlus;
import jdk.nashorn.internal.parser.JSONParser;
import org.mozilla.javascript.json.JsonParser;

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
    private double[] times;
    private long[] widths;
    private long[] heights;
    private long[] depths;

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
        times = new double[index.size()];
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
            times[count] = Double.parseDouble(tabSeparated[1].trim());

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

    public ImagePlus getImageData(){
        ImagePlus data = VirtualRawStackOpener.open(
                path + "/stacks/" + dataset + "/",
                (int) widths[0], (int) heights[0], (int) depths[0],
                times.length, 16, true,
                voxelDimXs[0], voxelDimYs[1], voxelDimZs[0], "micron"
        );
        return data;
    }

    public double[] getMeasurement(String key) {
        return null;
    }

    public double[] getTimes() {
        return times;
    }

    public static void main(String... args) {
        new ImageJ();

        ClearControlDataSet ccds = new ClearControlDataSet("C:/structure/data/2019-10-28-17-22-59-23-Finsterwalde_Tribolium_nGFP/", "C0opticsprefused");

        ccds.getImageData().show();

        System.out.println("time0: " + ccds.getTimes()[0]);
        System.out.println("time1: " + ccds.getTimes()[1]);


    }
}
