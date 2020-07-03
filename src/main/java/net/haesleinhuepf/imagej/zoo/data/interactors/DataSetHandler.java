package net.haesleinhuepf.imagej.zoo.data.interactors;

import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.plotting.PlotTableOverTime;
import fiji.util.gui.GenericDialogPlus;
import ij.*;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.measure.ResultsTable;
import ij.plugin.HyperStackConverter;
import ij.text.TextWindow;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Clear;
import net.haesleinhuepf.explorer.tree.manipulators.AbstractManipulator;
import net.haesleinhuepf.imagej.gui.InteractiveMeshMeasurements;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;
import net.haesleinhuepf.imagej.zoo.data.VirtualClearControlDataSetStack;
import net.haesleinhuepf.imagej.zoo.data.classification.Phase;
import net.haesleinhuepf.imagej.zoo.measurement.ImageQualityMeasurements;
import net.haesleinhuepf.imagej.zoo.measurement.MeasurementTable;
import net.haesleinhuepf.imagej.zoo.measurement.MeshMeasurements;
import net.haesleinhuepf.imagej.zoo.visualisation.ClearControlInteractivePlot;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

        setLayout(new GridLayout(4, 3));

        Plotter.readPrefs();
        {
            JButton btnViwer = new JButton("Open SPIMcat viewer...");
            btnViwer.setSize(50, 10);
            btnViwer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new InteractiveMeshMeasurements(dataSet);
                }

            });
            add(btnViwer);
        }



        {
            JButton btnColor = new JButton("Extract thumnails over time");
            btnColor.setSize(50, 10);
            btnColor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    generateThumbnails(dataSet);
                }

            });
            add(btnColor);
        }

        {
            JButton btnColor = new JButton("Extract cylinder projections over time");
            btnColor.setSize(50, 10);
            btnColor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    generateCylinderProjections(dataSet);
                }

            });
            add(btnColor);
        }

        {
            JButton btnColor = new JButton("Extract stacks over time");
            btnColor.setSize(50, 10);
            btnColor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    generateStacks(dataSet);
                }

            });
            add(btnColor);
        }


        {
            JButton btnColor = new JButton("Plot frame delay over time");
            btnColor.setSize(50, 10);
            btnColor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    plotFrameDelayOverTime(dataSet);
                }

            });
            add(btnColor);
        }

        {
            JButton btnColor = new JButton("Analyse focus measures over time");
            btnColor.setSize(50, 10);
            btnColor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    analyseMaximumProjection(dataSet);
                }

            });
            add(btnColor);
        }

        {

            JButton btnColor = new JButton("Analyse mesh measures over time");
            btnColor.setSize(50, 10);
            btnColor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    analyseMesh(dataSet);
                }

            });
            add(btnColor);

            JButton btnAnalyseWithGaps = new JButton("Analyse mesh over time with gaps");
            btnAnalyseWithGaps.setSize(50, 10);
            btnAnalyseWithGaps.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    analyseMeshWithGaps(dataSet);
                }

            });
            add(btnAnalyseWithGaps);

        }
/*
        {
            int formLine = newFormLine();
            JLabel lblC = new JLabel("Annotate events");
            add(lblC, "2, " + formLine);

            JButton btnColor = new JButton("Add");
            btnColor.setSize(50, 10);
            btnColor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addAnnotation(dataSet);
                }

            });
            add(btnColor, "4, " + formLine);


            JButton btnShow = new JButton("Show");
            btnShow.setSize(50, 10);
            btnShow.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAnnotations(dataSet);
                }

            });
            add(btnShow, "4, " + formLine);
        }

        {
            int formLine = newFormLine();
            JLabel lblC = new JLabel("Annotate phases");
            add(lblC, "2, " + formLine);

            JButton btnColor = new JButton("Add");
            btnColor.setSize(50, 10);
            btnColor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addPhaseAnnotation(dataSet);
                }
            });
            add(btnColor, "4, " + formLine);


            JButton btnShow = new JButton("Show");
            btnShow.setSize(50, 10);
            btnShow.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showPhaseAnnotations(dataSet);
                }

            });
            add(btnShow, "4, " + formLine);
        }
*/
    }

    private void plotFrameDelayOverTime(ClearControlDataSet dataSet) {
        int end_time_in_minutes = (int)dataSet.getTimesInMinutes()[dataSet.getTimesInMinutes().length - 1];
        int num_frame_delay_measurement_count = dataSet.getFramesPerMinute().length;
        double[] timesInHours = new double[num_frame_delay_measurement_count];
        System.arraycopy(dataSet.getTimesInHours(), 0, timesInHours, 0, num_frame_delay_measurement_count);



        double[] framesPerMinute = new double[end_time_in_minutes];
        System.arraycopy(dataSet.getFramesPerMinute(), 0, framesPerMinute, 0, num_frame_delay_measurement_count);

        double minFramesPerMinute = new Min().evaluate(framesPerMinute);
        double maxFramesPerMinute = 10; new Max().evaluate(framesPerMinute);

        double minX = new Min().evaluate(timesInHours);
        double maxX = new Max().evaluate(timesInHours);

        PlotTableOverTime.plotTitle = "Frames per minute over time";
        PlotTableOverTime.plotYTitle = "Frames per minute";
        PlotTableOverTime.plotXTitle = "Time / h";
        PlotTableOverTime.width = 768;
        PlotTableOverTime.height = 192;
        Plot plot = PlotTableOverTime.getPlot(framesPerMinute, timesInHours, minFramesPerMinute, maxFramesPerMinute, minX, maxX, timesInHours.length - 1);
        plot.getImagePlus().show();
    }

    public static void main(String[] args) {
        new ImageJ();

        String sourceFolder = "d:/structure/data/2019-12-17-16-54-37-81-Lund_Tribolium_nGFP_TMR/";
        //String sourceFolder = "C:/structure/data/2019-10-28-17-22-59-23-Finsterwalde_Tribolium_nGFP/";
        //String datasetFolder = "opticsprefused";
        String datasetFolder = "C0opticsprefused";

        //ClearControlDataSet.intel_byte_order = false;
        //String sourceFolder = "d:/structure/data/190124_ctrl_31_p6_ByungHoLee/";
        //String datasetFolder = "imported";

        ClearControlDataSet dataSet = ClearControlDataSetOpener.open(sourceFolder, datasetFolder);

        new DataSetHandler(dataSet).plotFrameDelayOverTime(dataSet);
    }

    private void generateStacks(ClearControlDataSet dataSet) {
        int frameStart = dataSet.getFrameRangeStart();
        int frameEnd = dataSet.getFrameRangeEnd();
        Plotter.startTime = dataSet.getTimesInMinutes()[frameStart];
        Plotter.endTime = dataSet.getTimesInMinutes()[frameEnd];
        Plotter.timeUnit = "Minutes";


        GenericDialog gd = new GenericDialog("Generate stacks over time");
        gd.addNumericField("Start", Plotter.startTime, 2);
        gd.addNumericField("End", Plotter.endTime, 2);
        gd.addChoice("Time unit for x-axis", new String[]{"Seconds", "Minutes", "Hours"}, Plotter.timeUnit);
        gd.addNumericField("Number of time points", Plotter.numberOfImages);

        String[] names = new String[Phase.all.length];
        int i = 0;
        for (Phase phase : Phase.all) {
            names[i] = phase.toString();
            i++;
        }
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        Plotter.startTime = gd.getNextNumber();
        Plotter.endTime = gd.getNextNumber();
        Plotter.timeUnit = gd.getNextChoice();
        Plotter.numberOfImages = (int) gd.getNextNumber();

        double startTimeInSeconds = Plotter.startTime;
        double endTimeInSeconds = Plotter.endTime;
        if (Plotter.timeUnit == "Minutes") {
            startTimeInSeconds = Plotter.startTime * 60;
            endTimeInSeconds = Plotter.endTime * 60;
        }
        if (Plotter.timeUnit == "Hours") {
            startTimeInSeconds = Plotter.startTime * 3600;
            endTimeInSeconds = Plotter.endTime * 3600;
        }

        ImageStack stack = new VirtualClearControlDataSetStack(dataSet, startTimeInSeconds, endTimeInSeconds, Plotter.numberOfImages);
        ImagePlus result = new ImagePlus("Cylinder projection [" + Plotter.startTime + " ... " + Plotter.endTime +  " " + Plotter.timeUnit + "]" + dataSet.getShortName(), stack);
        result.show();
    }

    private void addPhaseAnnotation(ClearControlDataSet dataSet) {
        int frameStart = dataSet.getFrameRangeStart();
        int frameEnd = dataSet.getFrameRangeEnd();
        Plotter.startTime = dataSet.getTimesInMinutes()[frameStart];
        Plotter.endTime = dataSet.getTimesInMinutes()[frameEnd];
        Plotter.timeUnit = "Minutes";


        GenericDialog gd = new GenericDialog("Add phase annotation");
        gd.addNumericField("Start", Plotter.startTime, 2);
        gd.addNumericField("End", Plotter.endTime, 2);
        gd.addChoice("Time unit for x-axis", new String[]{"Seconds", "Minutes", "Hours"}, Plotter.timeUnit);

        String[] names = new String[Phase.all.length];
        int i = 0;
        for (Phase phase : Phase.all) {
            names[i] = phase.toString();
            i++;
        }
        gd.addChoice("Phase", names, names[0]);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        Plotter.startTime = gd.getNextNumber();
        Plotter.endTime = gd.getNextNumber();
        Plotter.timeUnit = gd.getNextChoice();

        double startTimeInMinutes = Plotter.startTime;
        double endTimeInMinutes = Plotter.endTime;
        if (Plotter.timeUnit == "Seconds") {
            startTimeInMinutes = Plotter.startTime / 60;
            endTimeInMinutes = Plotter.endTime / 60;
        }
        if (Plotter.timeUnit == "Hours") {
            startTimeInMinutes = Plotter.startTime * 60;
            endTimeInMinutes = Plotter.endTime * 60;
        }

        int firstFrame = dataSet.getFirstFrameAfterTimeInSeconds(startTimeInMinutes * 60 );
        int lastFrame = dataSet.getFirstFrameAfterTimeInSeconds(endTimeInMinutes * 60);

        int phaseAnnotation = gd.getNextChoiceIndex();
        Phase phase = Phase.all[phaseAnnotation];

        double[] phases = dataSet.getPhases();
        for (i = firstFrame; i <= lastFrame; i++) {
            phases[i] = phase.value;
        }
        dataSet.savePhases();

        //dataSet.addAnnotation(annotation);
    }

    private void showPhaseAnnotations(ClearControlDataSet dataSet) {
        MeasurementTable phases = dataSet.getMeasurement("processed/phases.csv");
        ClearControlInteractivePlot plot = new ClearControlInteractivePlot(dataSet, "phase_index", phases);
        dataSet.addPlot(plot.getPlotWindow().getPlot());
    }


    private void showAnnotations(ClearControlDataSet dataSet) {
        ResultsTable table = dataSet.getAnnotationsAsTable();
        table.show(dataSet.getShortName() + " event annotations");

        TextWindow window = (TextWindow)WindowManager.getWindow(dataSet.getShortName() + " annotations");
        window.getTextPanel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                System.out.println("Table clicked");
                int selectedIndex = window.getTextPanel().getSelectionStart();

                System.out.println("selec " + selectedIndex );
                if (selectedIndex >= 0) {
                    int frame = (int) table.getValue("Frame", selectedIndex);
                    System.out.println("frame " + frame );
                    dataSet.setCurrentFrameRange(frame, frame);
                }
                super.mouseReleased(e);
            }
        });
    }

    private void addAnnotation(ClearControlDataSet dataSet) {
        GenericDialog gd = new GenericDialog("Add event annotation");
        gd.addMessage("Add event annotation for frame " + dataSet.getFrameRangeStart());
        gd.addStringField("Annotation", "");
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        String annotation = gd.getNextString();
        dataSet.addAnnotation(annotation);
    }

    private void analyseMesh(ClearControlDataSet dataSet) {
        GenericDialog gd = new GenericDialog("Analyse mesh");
        gd.addNumericField("Zoom factor (in microns)", 1.5, 2);
        gd.addNumericField("First frame", 0, 0);
        gd.addNumericField("Last frame", dataSet.getNumberOfFrames() - 1, 0 );
        gd.addNumericField("Spot detection blur sigma", 3, 1);
        gd.addNumericField("Spot detection out of sample threshold", 250, 1);
        //gd.addStringField("Spot detection out of sample threshold", "Triangle");
        //gd.addCheckbox("Do pseudo cell segmentation", true);
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
        //boolean doPseudoCellSegmentation = gd.getNextBoolean();
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
                .setProjectionVisualisationOnScreen(showProjections)
                .setProjectionVisualisationToDisc(saveProjections)
                .setFirstFrame(firstFrame)
                .setLastFrame(lastFrame)
                ).start();
    }

    private void analyseMeshWithGaps(ClearControlDataSet dataSet) {
        int frameStart = dataSet.getFrameRangeStart();
        int frameEnd = dataSet.getFrameRangeEnd();
        double startTime = dataSet.getTimesInMinutes()[frameStart];
        double endTime = dataSet.getTimesInMinutes()[frameEnd];

        GenericDialogPlus gd = new GenericDialogPlus("Analyse mesh over time");
        gd.addNumericField("Start", startTime, 2);
        gd.addNumericField("End", endTime, 2);
        gd.addChoice("Time unit for x-axis", new String[]{"Seconds", "Minutes", "Hours"}, "Minutes");
        gd.addNumericField("Number of images", Plotter.numberOfImages, 0);

        gd.addNumericField("Zoom factor (in microns)", 1.5, 2);

        gd.addNumericField("Translation X", 0, 2);
        gd.addNumericField("Translation Y", 0, 2);
        gd.addNumericField("Translation Z", 0, 2);

        gd.addNumericField("Rotation X", 0, 2);
        gd.addNumericField("Rotation Y", 0, 2);
        gd.addNumericField("Rotation Z", 0, 2);

        gd.addNumericField("Spot detection blur sigma", 3, 1);
        gd.addNumericField("Background subtraction blur sigma", 0, 1);
        gd.addNumericField("Spot detection out of sample threshold", 250, 1);

        gd.addNumericField("Pseudo cell segmentation dual dilations", 17, 0);
        gd.addNumericField("Pseudo cell segmentation dual erosions", 7, 0);

        gd.addCheckbox("Save projections to disc", true);
        gd.addCheckbox("Show projections on screen", true);

        gd.addDirectoryField("Result folder", dataSet.getPath() + "processed/output/");

        gd.showDialog();

        if (gd.wasCanceled()) {
            return;
        }

        Plotter.startTime = gd.getNextNumber();
        Plotter.endTime = gd.getNextNumber();
        Plotter.timeUnit = gd.getNextChoice();
        Plotter.numberOfImages = (int) gd.getNextNumber();

        double zoomFactor = gd.getNextNumber();

        double translationX = gd.getNextNumber();
        double translationY = gd.getNextNumber();
        double translationZ = gd.getNextNumber();

        double rotationX = gd.getNextNumber();
        double rotationY = gd.getNextNumber();
        double rotationZ = gd.getNextNumber();

        double blurSigma = gd.getNextNumber();
        double backgroundSubtractionBlurSigma = gd.getNextNumber();
        double outOfSampleThreshold = gd.getNextNumber();

        int numberOfDualDilations = (int) gd.getNextNumber();
        int numberOfDualErosions = (int) gd.getNextNumber();

        Plotter.saveImages = gd.getNextBoolean();
        boolean showResultsOnScreen = gd.getNextBoolean();

        String outputFolder = gd.getNextChoice();

        Plotter.writePrefs();

        analyseMeshWithGaps(dataSet,
                outputFolder,
                Plotter.startTime,
                Plotter.endTime,
                Plotter.timeUnit,
                Plotter.numberOfImages,
                zoomFactor,
                translationX,
                translationY,
                translationZ,
                rotationX,
                rotationY,
                rotationZ,
                blurSigma,
                backgroundSubtractionBlurSigma,
                outOfSampleThreshold,
                numberOfDualDilations,
                numberOfDualErosions,
                Plotter.saveImages,
                showResultsOnScreen);
    }


    public static void analyseMeshWithGaps(ClearControlDataSet dataSet, String outputFolder, double startTime, double endTime, String timeUnit, int numberOfImages, double zoomFactor, double translationX, double translationY, double translationZ, double rotationX, double rotationY, double rotationZ,  double blurSigma, double backgroundSubtractionBlurSigma, double outOfSampleThreshold, int numberOfDualDilations, int numberOfDualErosions, boolean saveImages, boolean showResultsOnScreen) {

        //new ImageJ();

        MeshMeasurements mm = new MeshMeasurements(dataSet);
        mm.setZoomFactor(zoomFactor);
        mm.setBlurSigma(blurSigma);
        mm.setBackgroundSubtractionSigma(backgroundSubtractionBlurSigma);
        mm.setThreshold(outOfSampleThreshold);
        mm.setNumberDoubleDilationsForPseudoCellSegmentation(numberOfDualDilations);
        mm.setNumberDoubleErosionsForPseudoCellSegmentation(numberOfDualErosions);
        mm.setProjectionVisualisationOnScreen(showResultsOnScreen);
        mm.setProjectionVisualisationToDisc(saveImages);

        mm.setTranslationX(translationX);
        mm.setTranslationY(translationY);
        mm.setTranslationZ(translationZ);
        mm.setRotationX(rotationX);
        mm.setRotationY(rotationY);
        mm.setRotationZ(rotationZ);

        if (saveImages) {
            new File(outputFolder).mkdirs();
        }
        //int numberOfImages = 500;
        //int numberOfMinutes = 5  * 60 * 24;

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

        double numberOfMinutes = endTimeInMinutes - startTimeInMinutes;

        double timeStepInMinutes = 1.0 * numberOfMinutes / (numberOfImages - 1);

        int firstFrame = dataSet.getFirstFrameAfterTimeInSeconds(startTimeInMinutes * 60 );
        int lastFrame = dataSet.getFirstFrameAfterTimeInSeconds(endTimeInMinutes * 60);

        int numberOfFrames = lastFrame - firstFrame + 1;

        System.out.println("Number of frames: " + numberOfFrames);

        ResultsTable table = new ResultsTable();

        //ImagePlus[] images = new ImagePlus[numberOfFrames];
        ImageStack stack = null;
        for (int i = 0; i < numberOfImages; i++) {
            //System.out.println();
            double time = startTimeInMinutes + i * timeStepInMinutes;
            int frame = dataSet.getFirstFrameAfterTimeInSeconds(time * 60);
            System.out.println("Frame " + frame);

            String timepoint = "000000" + i;
            timepoint = timepoint.substring(timepoint.length() - 6, timepoint.length());

            mm.processFrame(outputFolder, table, frame, timepoint);

            //ImagePlus thumbnail = dataSet.getThumbnailsFromFolder(thumbnailFolder);
            //thumbnail.setT(frame + 1);

        }

        if (showResultsOnScreen) {
            table.show("Mesh measurements");
        }
    }

    private void analyseMaximumProjection(ClearControlDataSet dataSet) {
        new ImageQualityMeasurements(dataSet).run();
    }

    private String thumbnailFolder = "";
    private void generateThumbnails(ClearControlDataSet dataSet) {
        int frameStart = dataSet.getFrameRangeStart();
        int frameEnd = dataSet.getFrameRangeEnd();
        double startTime = dataSet.getTimesInMinutes()[frameStart];
        double endTime = dataSet.getTimesInMinutes()[frameEnd];

        GenericDialog gd = new GenericDialog("Plot over time");
        gd.addNumericField("Start", startTime, 2);
        gd.addNumericField("End", endTime, 2);
        gd.addChoice("Time unit for x-axis", new String[]{"Seconds", "Minutes", "Hours"}, "Minutes");
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
        thumbnailFolder = gd.getNextChoice();
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

    int background_subtraction_radius = 5;

    private void generateCylinderProjections(ClearControlDataSet dataSet) {


        int frameStart = dataSet.getFrameRangeStart();
        int frameEnd = dataSet.getFrameRangeEnd();
        double startTime = dataSet.getTimesInMinutes()[frameStart];
        double endTime = dataSet.getTimesInMinutes()[frameEnd];

        GenericDialog gd = new GenericDialog("Plot over time");
        gd.addNumericField("Start", startTime, 2);
        gd.addNumericField("End", endTime, 2);
        gd.addChoice("Time unit for x-axis", new String[]{"Seconds", "Minutes", "Hours"}, "Minutes");
        gd.addNumericField("Number of images", Plotter.numberOfImages, 0);

        gd.addNumericField("background_subtraction_radius", background_subtraction_radius, 0);

        gd.showDialog();

        if (gd.wasCanceled()) {
            return;
        }

        Plotter.startTime = gd.getNextNumber();
        Plotter.endTime = gd.getNextNumber();
        Plotter.timeUnit = gd.getNextChoice();
        Plotter.numberOfImages = (int) gd.getNextNumber();
        Plotter.saveImages = gd.getNextBoolean();

        background_subtraction_radius = (int)gd.getNextNumber();

        Plotter.writePrefs();

        ImagePlus imp = generateCylinderProjections(dataSet,
                Plotter.startTime,
                Plotter.endTime,
                Plotter.timeUnit,
                Plotter.numberOfImages,
                background_subtraction_radius);
        imp = HyperStackConverter.toHyperStack(imp, 1, 1, imp.getNSlices());
        imp.setT(imp.getNFrames());
        imp.show();
    }

    public static ImagePlus generateCylinderProjections(ClearControlDataSet dataSet, double startTime, double endTime, String timeUnit, int numberOfImages, int background_subtraction_radius) {

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

        double numberOfMinutes = endTimeInMinutes - startTimeInMinutes;

        double timeStepInMinutes = 1.0 * numberOfMinutes / (numberOfImages - 1);

        int firstFrame = dataSet.getFirstFrameAfterTimeInSeconds(startTimeInMinutes * 60 );
        int lastFrame = dataSet.getFirstFrameAfterTimeInSeconds(endTimeInMinutes * 60);

        int numberOfFrames = lastFrame - firstFrame + 1;

        System.out.println("Number of frames: " + numberOfFrames);

        CLIJ2 clij2 = CLIJ2.getInstance();
        ClearCLBuffer input = null;
        ClearCLBuffer background_subtracted = null;
        ClearCLBuffer image2 = null;
        ClearCLBuffer image3 = null;
        ClearCLBuffer maximum_x_projection = null;

        //ImagePlus[] images = new ImagePlus[numberOfFrames];
        ImageStack stack = null;
        for (int i = 0; i < numberOfImages; i++) {
            IJ.showProgress(i, numberOfImages);
            //System.out.println();
            double time = startTimeInMinutes + i * timeStepInMinutes;
            int frame = dataSet.getFirstFrameAfterTimeInSeconds(time * 60);
            System.out.println("Frame " + frame + " (" + i + " / " + numberOfImages + ")");

            String timepoint = "000000" + i;
            timepoint = timepoint.substring(timepoint.length() - 6, timepoint.length());

            ImagePlus input_imp = dataSet.getImageData(frame);

            if (input != null) {
                input.close();
            }
            input = clij2.push(input_imp);

            // --------------------------------------------
            // background subtraction, maximum projection
            if (background_subtracted == null) {
                background_subtracted = clij2.create(input);
            }
            clij2.topHatBox(input, background_subtracted, background_subtraction_radius, background_subtraction_radius, 0);

            // --------------------------------------------
            // cylinder-maximum projection

            // reslice top
            if (image2 == null) {
                image2 = clij2.create(input.getWidth(), input.getDepth(), input.getHeight());
            }
            clij2.resliceTop(background_subtracted, image2);

            // radial projection
            double number_of_angles = 360.0;
            double angle_step_size = 0.25;
            double start_angle_degrees = -90.0;
            double center_x = background_subtracted.getWidth() / 2;
            double center_y = 0.0;
            double scale_factor_x = dataSet.getVoxelSizeZ(frame);
            double scale_factor_y = dataSet.getVoxelSizeX(frame);


            if (image3 == null) {
                int maximumRadius = (int)Math.sqrt(Math.pow(image2.getWidth() / 2, 2) + Math.pow(image2.getHeight() / 2, 2));
                image3 = clij2.create(maximumRadius, background_subtracted.getHeight(), (int)(number_of_angles / angle_step_size));
            }

            clij2.resliceRadial(image2, image3, angle_step_size, start_angle_degrees, center_x, center_y, scale_factor_x, scale_factor_y);

            if (maximum_x_projection == null) {
                maximum_x_projection = clij2.create(image3.getDepth() / 2, image3.getHeight());
            }
            clij2.maximumXProjection(image3, maximum_x_projection);

            ImagePlus thumbnail = clij2.pull(maximum_x_projection);
            thumbnail.setT(frame + 1);

            ImagePlus image = new ImagePlus("", thumbnail.getProcessor());
            //images[i] = image;
            if (stack == null) {
                stack = new ImageStack(image.getWidth(), image.getHeight());
            }
            stack.addSlice(image.getProcessor());
            //if (i > 5 ) break;
        }

        clij2.release(input);
        clij2.release(background_subtracted);
        clij2.release(image2);
        clij2.release(image3);
        clij2.release(maximum_x_projection);


        ImagePlus result = new ImagePlus("Cylinder projection [" + startTime + " ... " + endTime +  " " + timeUnit + "]" + dataSet.getShortName(), stack);
        return result;

    }


}
