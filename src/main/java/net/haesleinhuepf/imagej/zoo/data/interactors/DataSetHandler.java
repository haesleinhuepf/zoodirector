package net.haesleinhuepf.imagej.zoo.data.interactors;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.HyperStackConverter;
import net.haesleinhuepf.explorer.tree.manipulators.AbstractManipulator;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;

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

        int formLine = newFormLine();
        JLabel lblC = new JLabel("Extract thumnails over time");
        add(lblC, "2, " + formLine);

        JButton btnColor = new JButton("Extract...");
        btnColor.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                generateThumbnails(dataSet);
            }

        });
        add(btnColor, "4, " + formLine);

    }

    private void generateThumbnails(ClearControlDataSet dataSet) {
        GenericDialog gd = new GenericDialog("Plot over time");
        gd.addNumericField("Start", Plotter.startTime, 2);
        gd.addNumericField("End", Plotter.endTime, 2);
        gd.addChoice("Time unit for x-axis", new String[]{"Seconds", "Minutes", "Hours"}, Plotter.timeUnit);
        gd.addCheckbox("Save images to processed folder", Plotter.saveImages);

        gd.showDialog();

        if (gd.wasCanceled()) {
            return;
        }

        Plotter.startTime = gd.getNextNumber();
        Plotter.endTime = gd.getNextNumber();
        Plotter.timeUnit = gd.getNextChoice();
        Plotter.saveImages = gd.getNextBoolean();

        Plotter.writePrefs();

        ImagePlus imp = generateThumbnails(dataSet,
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

    public static ImagePlus generateThumbnails(ClearControlDataSet dataSet, double startTime, double endTime, String timeUnit, int numberOfImages, String title, boolean saveImages) {

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


            ImagePlus thumbnail = dataSet.getThumbnails();
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
