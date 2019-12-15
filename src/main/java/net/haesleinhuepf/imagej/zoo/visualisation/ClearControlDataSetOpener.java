package net.haesleinhuepf.imagej.zoo.visualisation;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.plugin.PlugIn;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;

public class ClearControlDataSetOpener implements PlugIn {
    private static String path = Prefs.getDefaultDirectory();
    private static String datasetName = "C0opticsprefused";

    @Override
    public void run(String arg) {
        GenericDialogPlus gd = new GenericDialogPlus("Open ClearControl data set");
        gd.addDirectoryField("Folder", path);
        gd.addStringField("Dataset", datasetName);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        path = gd.getNextString();
        datasetName = gd.getNextString();

        show(open(path, datasetName));
    }

    public static ClearControlDataSet open(String path, String datasetName) {
        return new ClearControlDataSet(path, datasetName);
    }

    public static void show(ClearControlDataSet ccds) {
        ImagePlus imp = ccds.getImageData();
        imp.setZ(imp.getNSlices() / 2);
        IJ.run(imp, "Enhance Contrast", "saturated=0.35");
        imp.show();

        ccds.getThumbnails().show();
        ccds.getImageData().show();

        double[] fpm = ccds.getFramesPerMinute();
        double[] time = ClearControlDataSet.ramp(fpm.length);

        new ClearControlInteractivePlot(ccds, "Frames per minute", time, fpm).show();

        for (String measurementFilename : ccds.getMeasurementFiles()) {
            System.out.println("Measurement: " + measurementFilename);
            MeasurementTable mt = new MeasurementTable(ccds.getPath() + measurementFilename);
            //System.out.println(Arrays.toString(mt.getColumnNames()));

            for (String column : mt.getColumnNames()) {
                if (!shouldShow(measurementFilename, column)) {
                    continue;
                }

                System.out.println("Column: " + column);
                double[] yData = mt.getColumn(column);
                double[] xTimeData = ccds.getTimesInMinutes();

                new ClearControlInteractivePlot(ccds, column, xTimeData, yData).show();
            }
        }
    }

    private static boolean shouldShow(String filename, String columnName) {
        if (columnName.trim().length() < 3) {
            return false;
        }
        try {
            Integer.parseInt(columnName);
            return false; // if numeric: don't show
        } catch (Exception e) { }
        return true;
    }


    public static void main(String[] args) {
        new ImageJ();

        String dataSetRootFolder = "C:/structure/data/2018-05-23-16-18-13-89-Florence_multisample/";
        String dataSetName = "opticsprefused";

        ClearControlDataSet ccds = open(dataSetRootFolder, dataSetName);
        show(ccds);
    }
}
