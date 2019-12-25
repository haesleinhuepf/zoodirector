package net.haesleinhuepf.imagej.zoo.measurement;

import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.plotting.PlotTableOverTime;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;

/**
 * Measurements
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */
public class Measurements {
    public static void main(String... args) {
        ClearControlDataSet dataSet = ClearControlDataSetOpener.open("C:/structure/data/2019-12-17-16-54-37-81-Lund_Tribolium_nGFP_TMR/", "C0opticsprefused");
        String outputFolder = "C:/structure/data/2019-12-17-16-54-37-81-Lund_Tribolium_nGFP_TMR/processed/";

        int numberOfImages = 700;
        int numberOfDays = 7;

        double timeStepInMinutes = 1.0 * numberOfDays * 60 * 24 / (numberOfImages - 1);
        int numberOfFrames = dataSet.getFirstFrameAfterTimeInSeconds(timeStepInMinutes * 60 * numberOfImages) + 1;

        System.out.println("Number of frames: " + numberOfFrames);

        double[] timesInHours = new double[numberOfFrames];
        System.arraycopy(dataSet.getTimesInHours(), 0, timesInHours, 0, numberOfFrames);

        double[] spotCounts = new double[numberOfFrames];
        MeasurementTable spotCountTable = new MeasurementTable(dataSet.getPath() + "spotcount.tsv");
        System.arraycopy(spotCountTable.getColumn("Number of spots"), 0, spotCounts, 0, numberOfFrames);

        double[] frameDelaysInSeconds = new double[numberOfFrames];
        System.arraycopy(dataSet.getFrameDelayInSeconds(), 0, frameDelaysInSeconds, 0, numberOfFrames);

        double[] framesPerMinute = new double[numberOfFrames];
        System.arraycopy(dataSet.getFramesPerMinute(), 0, framesPerMinute, 0, numberOfFrames);

        double minX = new Min().evaluate(timesInHours);
        double maxX = new Max().evaluate(timesInHours);

        double minSpots = new Min().evaluate(spotCounts);
        double maxSpots = new Max().evaluate(spotCounts);

        double minFrameDelay = new Min().evaluate(frameDelaysInSeconds);
        double maxFrameDelay = new Max().evaluate(frameDelaysInSeconds);

        double minFramesPerMinute = new Min().evaluate(framesPerMinute);
        double maxFramesPerMinute = new Max().evaluate(framesPerMinute);


        PlotTableOverTime.plotXTitle = "Time / h";
        PlotTableOverTime.width = 768;
        PlotTableOverTime.height = 256;


        for (int i = 0; i < numberOfImages; i++) {
            //System.out.println();
            double time = i * timeStepInMinutes;
            int frame = dataSet.getFirstFrameAfterTimeInSeconds(time * 60);
            System.out.println("Frame " + frame);

            String timepoint = "000000" + i;
            timepoint = timepoint.substring(timepoint.length() - 6, timepoint.length());

            PlotTableOverTime.plotTitle = "Spot count over time";
            PlotTableOverTime.plotYTitle = "Spot count";
            Plot plot = PlotTableOverTime.getPlot(spotCounts, timesInHours, minSpots, maxSpots, minX, maxX, frame);
            IJ.saveAs(plot.getImagePlus(), "tif", outputFolder + "spotCountPlot/" + timepoint + ".tif");

            PlotTableOverTime.plotTitle = "Frame delay over time";
            PlotTableOverTime.plotYTitle = "Frame delay / s";
            plot = PlotTableOverTime.getPlot(frameDelaysInSeconds, timesInHours, minFrameDelay, maxFrameDelay, minX, maxX, frame);
            IJ.saveAs(plot.getImagePlus(), "tif", outputFolder + "frameDelayPlot/" + timepoint + ".tif");

            PlotTableOverTime.plotTitle = "Frames per minute over time";
            PlotTableOverTime.plotYTitle = "Frames per minute";
            plot = PlotTableOverTime.getPlot(framesPerMinute, timesInHours, minFramesPerMinute, maxFramesPerMinute, minX, maxX, frame);
            IJ.saveAs(plot.getImagePlus(), "tif", outputFolder + "framesPerMinutePlot/" + timepoint + ".tif");


            ImagePlus thumbnail = dataSet.getThumbnails();
            thumbnail.setT(frame + 1);

            ImagePlus imp = new ImagePlus("", thumbnail.getProcessor());
            IJ.saveAs(imp, "tif", outputFolder + "image/" + timepoint + ".tif");



            //if (i > 5 ) break;
        }

    }
}
