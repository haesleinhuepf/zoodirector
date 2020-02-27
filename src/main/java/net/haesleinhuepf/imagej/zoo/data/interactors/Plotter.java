package net.haesleinhuepf.imagej.zoo.data.interactors;

import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.plotting.PlotTableOverTime;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.plugin.HyperStackConverter;
import ij.plugin.HyperStackMaker;
import ij.plugin.RGBStackMerge;
import ij.process.ImageProcessor;
import net.haesleinhuepf.explorer.tree.manipulators.AbstractManipulator;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;
import net.haesleinhuepf.imagej.zoo.measurement.MeasurementTable;
import net.haesleinhuepf.imagej.zoo.visualisation.ClearControlInteractivePlot;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.File;

/**
 * Plotter
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */
public class Plotter extends AbstractManipulator {

    static double startTime = 0;
    static double endTime = 60;

    static String timeUnit = "Minutes";

    static int plotWidth = 640;
    static int plotHeight = 480;

    static int numberOfImages = 100;
    static boolean saveImages = false;

    public static Double minY = null;
    public static Double maxY = null;
    public static boolean rgbPlot = true;


    public Plotter(ClearControlInteractivePlot plot) {

        readPrefs();

        int formLine = newFormLine();
        JLabel lblC = new JLabel("Plot over time");
        add(lblC, "2, " + formLine);

        JButton btnColor = new JButton("Plot...");
        btnColor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generatePlot(plot);
            }

        });
        add(btnColor, "4, " + formLine);

    }


    private void generatePlot(ClearControlInteractivePlot plot) {
        ClearControlDataSet dataSet = plot.getDataSet();
        int frameStart = dataSet.getFrameRangeStart();
        int frameEnd = dataSet.getFrameRangeEnd();
        double startTime = dataSet.getTimesInMinutes()[frameStart];
        double endTime = dataSet.getTimesInMinutes()[frameEnd];

        GenericDialog gd = new GenericDialog("Plot over time");
        gd.addNumericField("Start", startTime, 2);
        gd.addNumericField("End", endTime, 2);
        gd.addChoice("Time unit for x-axis", new String[]{"Seconds", "Minutes", "Hours"}, "Minutes");
        gd.addNumericField("Number of images", Plotter.numberOfImages, 0);
        gd.addNumericField("Plot size x", plotWidth, 0);
        gd.addNumericField("Plot size y", plotHeight, 0);
        gd.addCheckbox("Save images to processed folder", saveImages);
        gd.addCheckbox("RGB plot", rgbPlot);

        gd.showDialog();

        if (gd.wasCanceled()) {
            return;
        }

        startTime = gd.getNextNumber();
        endTime = gd.getNextNumber();
        timeUnit = gd.getNextChoice();
        numberOfImages = (int) gd.getNextNumber();
        plotWidth = (int) gd.getNextNumber();
        plotHeight = (int) gd.getNextNumber();
        saveImages = gd.getNextBoolean();
        rgbPlot = gd.getNextBoolean();

        writePrefs();


        ImagePlus imp = plot(plot.getDataSet(),
                startTime,
                endTime,
                timeUnit,
                numberOfImages,
                plot.getMeasurementTable(),
                plot.getName(),
                plotWidth,
                plotHeight,
                saveImages,
                rgbPlot);


        imp = HyperStackConverter.toHyperStack(imp, imp.getNChannels(), 1, imp.getNSlices());
        imp.setT(imp.getNFrames());
        imp.show();
    }

    @Deprecated
    public static ImagePlus plot(ClearControlDataSet dataSet, double startTime, double endTime, String timeUnit, int numberOfImages, MeasurementTable table, String title, int widthInPixels, int heightInPixels, boolean saveImages) {
        return plot(dataSet, startTime, endTime, timeUnit, numberOfImages, table, title, widthInPixels, heightInPixels, saveImages);
    }

    public static ImagePlus plot(ClearControlDataSet dataSet, double startTime, double endTime, String timeUnit, int numberOfImages, MeasurementTable table, String title, int widthInPixels, int heightInPixels, boolean saveImages, boolean rgbPlot) {

        double startTimeInMinutes = startTime;
        double endTimeInMinutes = endTime;
        if (timeUnit == "Seconds") {
            startTimeInMinutes = startTime / 60;
            endTimeInMinutes = endTime / 60;
        }
        if (timeUnit == "Hours") {
            startTimeInMinutes = startTime * 60;
            endTimeInMinutes = endTime * 60;
        }


        String outputFolder = dataSet.getPath() + "processed/" +
                title.
                replace(" ", "_").
                replace("/", "_").
                replace("\\", "_")
                + startTimeInMinutes + "-" + endTimeInMinutes + "min/";
        
        if (saveImages) {
            new File(outputFolder).mkdirs();
        }
        //int numberOfImages = 500;
        //int numberOfMinutes = 5  * 60 * 24;

        double numberOfMinutes = endTimeInMinutes - startTimeInMinutes;

        double timeStepInMinutes = 1.0 * numberOfMinutes / (numberOfImages - 1);

        int firstFrame = dataSet.getFirstFrameAfterTimeInSeconds(startTimeInMinutes * 60 );
        int lastFrame = dataSet.getFirstFrameAfterTimeInSeconds(endTimeInMinutes * 60);

        int numberOfFrames = lastFrame - firstFrame + 1;
                //dataSet.getFirstFrameAfterTimeInSeconds(timeStepInMinutes * 60 * numberOfImages);

        System.out.println("Number of frames: " + numberOfFrames);

        double[] times = new double[numberOfFrames];
        if (timeUnit.compareTo("Seconds") == 0) {
            System.arraycopy(dataSet.getTimesInSeconds(), firstFrame, times, 0, numberOfFrames);
        } else if (timeUnit.compareTo("Minutes") == 0) {
            System.arraycopy(dataSet.getTimesInMinutes(), firstFrame, times, 0, numberOfFrames);
        } else if (timeUnit.compareTo("Hours") == 0) {
            System.arraycopy(dataSet.getTimesInHours(), firstFrame, times, 0, numberOfFrames);
        }

        double[] measurements = new double[numberOfFrames];
        System.arraycopy(table.getColumn(title), firstFrame, measurements, 0, numberOfFrames);

        double minX = new Min().evaluate(times);
        double maxX = new Max().evaluate(times);

        double minMeasurement = minY != null?minY:new Min().evaluate(measurements);
        double maxMeasurement = maxY != null?maxY:new Max().evaluate(measurements);

        PlotTableOverTime.plotXTitle = "Time / " + timeUnit;
        PlotTableOverTime.width = widthInPixels;
        PlotTableOverTime.height = heightInPixels;

        //ImagePlus[] images = new ImagePlus[numberOfFrames];
        ImageStack stack = new ImageStack(widthInPixels, heightInPixels);
        for (int i = 0; i < numberOfImages; i++) {
            //System.out.println();
            double time = startTimeInMinutes + i * timeStepInMinutes;
            int frame = dataSet.getFirstFrameAfterTimeInSeconds(time * 60);
            System.out.println("Frame " + frame);

            String timepoint = "000000" + i;
            timepoint = timepoint.substring(timepoint.length() - 6, timepoint.length());

            PlotTableOverTime.plotTitle = title;
            PlotTableOverTime.plotYTitle = title;
            Plot plot = PlotTableOverTime.getPlot(measurements, times, minMeasurement, maxMeasurement, minX, maxX, frame - firstFrame);
            
            ImagePlus image = plot.getImagePlus();
            if (saveImages) {
                IJ.saveAs(image, "tif", outputFolder + timepoint + ".tif");
            }
            //images[i] = image;
            stack.addSlice(image.getProcessor());
            //if (i > 5 ) break;
        }

        ImagePlus result = new ImagePlus(title, stack);

        if (rgbPlot) {
            for (int i = 0; i < result.getNSlices(); i++) {
                result.setZ(i + 1);
                result.getProcessor().invert();
            }

            result.setZ(result.getNSlices() - 1);
            ImageProcessor lastSlice = result.getProcessor();
            ImageStack background = new ImageStack(lastSlice.getWidth(), lastSlice.getHeight());
            for (int i = 0; i < result.getNSlices(); i++) {
                background.addSlice(lastSlice);
            }

            result = RGBStackMerge.mergeChannels(new ImagePlus[]{result, new ImagePlus("title", background)}, false);
            result.setC(1);
            IJ.run(result,"Green", "");
            result.setC(2);
            IJ.run(result, "Magenta", "");
        }


        return result;

    }



    static void readPrefs() {
        startTime = Prefs.get("net.haesleinhuepf.zoo.plotter.startTime", startTime);
        endTime = Prefs.get("net.haesleinhuepf.zoo.plotter.endTime", endTime);
        timeUnit = Prefs.getString("net.haesleinhuepf.zoo.plotter.timeUnit", timeUnit);
        plotWidth = (int)Prefs.get("net.haesleinhuepf.zoo.plotter.plotWidth", plotWidth);
        plotHeight = (int)Prefs.get("net.haesleinhuepf.zoo.plotter.plotHeight", plotHeight);
        numberOfImages = (int)Prefs.get("net.haesleinhuepf.zoo.plotter.numberOfImages", numberOfImages);
        saveImages = Prefs.get("net.haesleinhuepf.zoo.plotter.saveImages", saveImages?1:0) > 0;
    }

    static void writePrefs() {
        Prefs.set("net.haesleinhuepf.zoo.plotter.startTimeInMinutes", startTime);
        Prefs.set("net.haesleinhuepf.zoo.plotter.endTimeInMinutes", endTime);
        Prefs.set("net.haesleinhuepf.zoo.plotter.timeUnit", timeUnit);
        Prefs.set("net.haesleinhuepf.zoo.plotter.plotWidth", plotWidth);
        Prefs.set("net.haesleinhuepf.zoo.plotter.plotHeight", plotHeight);
        Prefs.set("net.haesleinhuepf.zoo.plotter.saveImages", saveImages?1:0);
    }
}
