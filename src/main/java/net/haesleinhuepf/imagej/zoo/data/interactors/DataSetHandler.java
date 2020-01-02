package net.haesleinhuepf.imagej.zoo.data.interactors;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.HyperStackConverter;
import net.haesleinhuepf.explorer.tree.manipulators.AbstractManipulator;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.measurement.ImageQualityMeasurements;
import net.haesleinhuepf.imagej.zoo.measurement.MeshMeasurements;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * DataSetHandler
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */
public class DataSetHandler extends AbstractManipulator {
    public DataSetHandler(ClearControlDataSet dataSet) {

        Plotter.readPrefs();
        {
            int formLine = newFormLine();
            JLabel lblC = new JLabel("Extract thumnails over time");
            add(lblC, "2, " + formLine);

            JButton btnColor = new JButton("Extract...");
            btnColor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    generateThumbnails(dataSet);
                }

            });
            add(btnColor, "4, " + formLine);
        }

        {
            int formLine = newFormLine();
            JLabel lblC = new JLabel("Analyse focus measures over time");
            add(lblC, "2, " + formLine);

            JButton btnColor = new JButton("Analyse...");
            btnColor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    analyseMaximumProjection(dataSet);
                }

            });
            add(btnColor, "4, " + formLine);
        }

        {
            int formLine = newFormLine();
            JLabel lblC = new JLabel("Analyse mesh measures over time");
            add(lblC, "2, " + formLine);

            JButton btnColor = new JButton("Analyse...");
            btnColor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    analyseMesh(dataSet);
                }

            });
            add(btnColor, "4, " + formLine);
        }
    }

    private void analyseMesh(ClearControlDataSet dataSet) {
        GenericDialog gd = new GenericDialog("Analyse mesh");
        gd.addNumericField("Zoom factor (in microns)", 1.5, 2);
        gd.addNumericField("First frame", 0, 0);
        gd.addNumericField("Last frame", dataSet.getNumberOfFrames() - 1, 0 );
        gd.addNumericField("Spot detection blur sigma", 3, 1);
        gd.addNumericField("Spot detection out of sample threshold", 250, 1);
        //gd.addStringField("Spot detection out of sample threshold", "Triangle");
        gd.addCheckbox("Do pseudo cell segmentation", true);
        gd.addNumericField("Pseudo cell segmentation dual dilations", 17, 0);
        gd.addNumericField("Pseudo cell segmentation dual erosions", 7, 0);
        gd.addCheckbox("Save projections to disc", true);
        gd.addCheckbox("Show projections on screen", true);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        double zoom = gd.getNextNumber();
        int firstFrame = (int) gd.getNextNumber();
        int lastFrame = (int) gd.getNextNumber();
        double blurSigma = gd.getNextNumber();
        double threshold = gd.getNextNumber();
        //String thresholdAlgorithm = gd.getNextString();
        boolean doPseudoCellSegmentation = gd.getNextBoolean();
        int numberOfDilations = (int) gd.getNextNumber();
        int numberOfErosions = (int) gd.getNextNumber();
        boolean saveProjections = gd.getNextBoolean();
        boolean showProjections = gd.getNextBoolean();

        new Thread(new MeshMeasurements(dataSet)
                .setZoomFactor(zoom)
                .setBlurSigma(blurSigma)
                .setThreshold(threshold)
                //.setThresholdAlgorithm(thresholdAlgorithm)
                .setNumberDoubleDilationsForPseudoCellSegmentation(numberOfDilations)
                .setNumberDoubleErosionsForPseudoCellSegmentation(numberOfErosions)
                .setDoPseudoCellSegmentation(doPseudoCellSegmentation)
                .setProjectionVisualisationOnScreen(showProjections)
                .setProjectionVisualisationToDisc(saveProjections)
                .setFirstFrame(firstFrame)
                .setLastFrame(lastFrame)
                ).start();




    }

    private void analyseMaximumProjection(ClearControlDataSet dataSet) {
        new ImageQualityMeasurements(dataSet).run();
    }

    private String thumbnailFolder = "";
    private void generateThumbnails(ClearControlDataSet dataSet) {
        GenericDialog gd = new GenericDialog("Plot over time");
        gd.addNumericField("Start", Plotter.startTime, 2);
        gd.addNumericField("End", Plotter.endTime, 2);
        gd.addChoice("Time unit for x-axis", new String[]{"Seconds", "Minutes", "Hours"}, Plotter.timeUnit);
        gd.addNumericField("Number of images", Plotter.numberOfImages, 0);
        gd.addChoice("Thumbnail folder", dataSet.getThumbnailFolderNames(), thumbnailFolder);
        gd.addCheckbox("Save images to processed folder", Plotter.saveImages);

        gd.showDialog();

        if (gd.wasCanceled()) {
            return;
        }

        Plotter.startTime = gd.getNextNumber();
        Plotter.endTime = gd.getNextNumber();
        Plotter.timeUnit = gd.getNextChoice();
        Plotter.numberOfImages = (int) gd.getNextNumber();
        thumbnailFolder = gd.getNextString();
        Plotter.saveImages = gd.getNextBoolean();

        Plotter.writePrefs();

        ImagePlus imp = generateThumbnails(dataSet,
                thumbnailFolder,
                Plotter.startTime,
                Plotter.endTime,
                Plotter.timeUnit,
                Plotter.numberOfImages,
                "thumbnails",
                Plotter.saveImages);
        imp = HyperStackConverter.toHyperStack(imp, 1, 1, imp.getNSlices());
        imp.setT(imp.getNFrames());
        imp.show();
    }

    public static ImagePlus generateThumbnails(ClearControlDataSet dataSet, String thumbnailFolder, double startTime, double endTime, String timeUnit, int numberOfImages, String title, boolean saveImages) {

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

        System.out.println("Number of frames: " + numberOfFrames);

        //ImagePlus[] images = new ImagePlus[numberOfFrames];
        ImageStack stack = null;
        for (int i = 0; i < numberOfImages; i++) {
            //System.out.println();
            double time = startTimeInMinutes + i * timeStepInMinutes;
            int frame = dataSet.getFirstFrameAfterTimeInSeconds(time * 60);
            System.out.println("Frame " + frame);

            String timepoint = "000000" + i;
            timepoint = timepoint.substring(timepoint.length() - 6, timepoint.length());


            ImagePlus thumbnail = dataSet.getThumbnailsFromFolder(thumbnailFolder);
            thumbnail.setT(frame + 1);

            ImagePlus image = new ImagePlus("", thumbnail.getProcessor());
            if (saveImages) {
                IJ.saveAs(image, "tif", outputFolder + timepoint + ".tif");
            }
            //images[i] = image;
            if (stack == null) {
                stack = new ImageStack(image.getWidth(), image.getHeight());
            }
            stack.addSlice(image.getProcessor());
            //if (i > 5 ) break;
        }

        ImagePlus result = new ImagePlus(title, stack);
        return result;

    }
}
