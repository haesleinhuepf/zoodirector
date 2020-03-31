package net.haesleinhuepf.imagej.zoo.measurement;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import net.haesleinhuepf.clij.clearcl.ClearCL;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.ClearCLImage;
import net.haesleinhuepf.clij2.converters.helptypes.Float1;
import net.haesleinhuepf.clij2.plugins.StatisticsOfLabelledPixels;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.utilities.CLIJUtilities;
import net.haesleinhuepf.imagej.clijutils.CLIJxUtils;
import net.haesleinhuepf.imagej.zoo.ZooUtilities;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.util.VersionUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import static net.haesleinhuepf.imagej.clijutils.CLIJxUtils.*;

public class MeshMeasurements extends DataSetMeasurements {
    CLIJx clijx;

    double zoomFactor = 1.5; // -> each analysed voxel is 1.5x1.5x1.5 microns large`


    double blurSigma = 3;
    double backgroundBlurSigma = 0;

    boolean projection_visualisation_on_screen = true;
    boolean projection_visualisation_to_disc = true;

    int maximumDistanceToMeshPoints = 100;

    int numberDoubleErosionsForPseudoCellSegmentation = 7;
    int numberDoubleDilationsForPseudoCellSegmentation = 17;
    //private String thresholdAlgorithm = "Triangle";
    private double threshold = 250;
    private boolean exportMesh = false;

    // default values from Finsterwalde
//    private double translationX = -39;
//    private double translationY = -30;
//    private double translationZ = -20;
//
//    private double rotationX = 5;
//    private double rotationY = -41;
//    private double rotationZ = -1;

    // default values from Lund
    private double translationX = 0;
    private double translationY = 0;
    private double translationZ = 0;

    private double rotationX = 0;
    private double rotationY = 0;
    private double rotationZ = 0;

    private boolean doCut = false;
    private double gapX = 0;
    private double gapY = 0;
    private double gapWidth = 0;
    private double gapHeight = 0;
    private double cutBlurSigma = 0;
    private double cutBackgroundIntensity = 0;
    private double cutShiftDistance = 0;
    private double cutShiftSpeed = 0;
    private double cutTimeInSeconds = 0;
    private double cutDeceleration = 0.9;


    private boolean eliminateSubSurfaceCells = false;
    private boolean eliminateOnSurfaceCells = false;
    private boolean drawText = false;

    private boolean storeMeasurements = false;

    private boolean measure_distances_in_detail = false;

    public MeshMeasurements setCut(
            double gapX,
            double gapY,
            double gapWidth,
            double gapHeight,
            double cutBlurSigma,
            double cutBackgroundIntensity,
            double cutInitialShiftSpeed,
            double cutTimeInSeconds,
            double cutDeceleration
    ) {
        this.cutShiftDistance = 0;
        this.cutShiftSpeed = cutInitialShiftSpeed;
        this.doCut = true;
        this.gapX = gapX;
        this.gapY = gapY;
        this.gapWidth = gapWidth;
        this.gapHeight = gapHeight;
        this.cutBlurSigma = cutBlurSigma;
        this.cutBackgroundIntensity = cutBackgroundIntensity;
        this.cutDeceleration = cutDeceleration;

        this.cutTimeInSeconds = cutTimeInSeconds;

        return this;
    }

    public MeshMeasurements(ClearControlDataSet dataSet) {
        super(dataSet);
        clijx = CLIJx.getInstance();
    }

    public MeshMeasurements setZoomFactor(double zoomFactor) {
        this.zoomFactor = zoomFactor;
        return this;
    }

    public MeshMeasurements setStoreMeasurements(boolean storeMeasurements) {
        this.storeMeasurements = storeMeasurements;
        return this;
    }
    HashMap<String, float[]> measurements = new HashMap<>();
    public float[] getMeasurement(String key) {
        return measurements.get(key);
    }

    private void storeMeasurement(String key, ClearCLBuffer measurement) {
        if (!storeMeasurements) {
            return;
        }
        if (measurements.containsKey(key)) {
            measurements.remove(key);
        }
        float[] array = new float[(int) measurement.getWidth()];
        FloatBuffer buffer = FloatBuffer.wrap(array);

        measurement.writeTo(buffer, true);

        measurements.put(key, array);
    }

    private void storeMeasurement(String key, double[]measurement) {
        if (!storeMeasurements) {
            return;
        }
        if (measurements.containsKey(key)) {
            measurements.remove(key);
        }
        float[] array = new float[measurement.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = (float) measurement[i];
        }

        measurements.put(key, array);
    }

    private void storeMeasurement(String key, float[] measurement) {
        if (!storeMeasurements) {
            return;
        }
        if (measurements.containsKey(key)) {
            measurements.remove(key);
        }
        measurements.put(key, measurement);
    }

    public ResultsTable getAllMeasurements() {
        ArrayList<String> keys = new ArrayList<>();
        keys.addAll(measurements.keySet());
        Collections.sort(keys);

        ResultsTable table = new ResultsTable();
        if (measurements.size() == 0) {
            return table;
        }

        float[] anyMeasuremnt = measurements.get(0);
        for (int i = 0; i < anyMeasuremnt.length; i++) {
            table.incrementCounter();
        }

        for (String key : keys) {
            float[] measuremnt = measurements.get(key);
            for (int i = 0; i < anyMeasuremnt.length; i++) {
                table.setValue(key, i, measuremnt[i]);
            }
        }
        return table;
    }

    public MeshMeasurements setNumberDoubleDilationsForPseudoCellSegmentation(int numberDoubleDilationsForPseudoCellSegmentation) {
        this.numberDoubleDilationsForPseudoCellSegmentation = numberDoubleDilationsForPseudoCellSegmentation;
        return this;
    }

    public MeshMeasurements setNumberDoubleErosionsForPseudoCellSegmentation(int numberDoubleErosionsForPseudoCellSegmentation) {
        this.numberDoubleErosionsForPseudoCellSegmentation = numberDoubleErosionsForPseudoCellSegmentation;
        return this;
    }

    public MeshMeasurements setProjectionVisualisationOnScreen(boolean projection_visualisation_on_screen) {
        this.projection_visualisation_on_screen = projection_visualisation_on_screen;
        return this;
    }

    public MeshMeasurements setProjectionVisualisationToDisc(boolean projection_visualisation_to_disc) {
        this.projection_visualisation_to_disc = projection_visualisation_to_disc;
        return this;
    }

    public MeshMeasurements setCLIJx(CLIJx clijx) {
        this.clijx = clijx;
        return this;
    }

    public MeshMeasurements setBlurSigma(double blurSigma) {
        this.blurSigma = blurSigma;
        return this;
    }

    //public MeshMeasurements setThresholdAlgorithm(String thresholdAlgorithm) {
    //    this.thresholdAlgorithm = thresholdAlgorithm;
    //    return this;
    //}

    public MeshMeasurements setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }

    public MeshMeasurements setTranslationX(double translationX) {
        this.translationX = translationX;
        return this;
    }

    public MeshMeasurements setTranslationY(double translationY) {
        this.translationY = translationY;
        return this;
    }

    public MeshMeasurements setTranslationZ(double translationZ) {
        this.translationZ = translationZ;
        return this;
    }

    public MeshMeasurements setRotationX(double rotationX) {
        this.rotationX = rotationX;
        return this;
    }

    public MeshMeasurements setRotationY(double rotationY) {
        this.rotationY = rotationY;
        return this;
    }

    public MeshMeasurements setRotationZ(double rotationZ) {
        this.rotationZ = rotationZ;
        return this;
    }

    public MeshMeasurements setBackgroundSubtractionSigma(double backgroundBlurSigma) {
        this.backgroundBlurSigma = backgroundBlurSigma;
        return this;
    }

    public MeshMeasurements setEliminateSubSurfaceCells(boolean eliminateSubSurfaceCells) {
        this.eliminateSubSurfaceCells = eliminateSubSurfaceCells;
        return this;
    }
    public MeshMeasurements setEliminateOnSurfaceCells(boolean eliminateOnSurfaceCells) {
        this.eliminateOnSurfaceCells = eliminateOnSurfaceCells;
        return this;
    }

    public MeshMeasurements setMeasureDistancesInDetail(boolean measure_distances_in_detail) {
        this.measure_distances_in_detail = measure_distances_in_detail;
        return this;
    }


    @Override
    public void run() {

        String outputFolder = dataSet.getPath() + "processed/";

        String now = ZooUtilities.now();

        String analysisLog = "Analysis log \n" +
                now + "\n" +
                "Dataset path: " + dataSet.getPath() + "\n" +
                "Dataset name: " + dataSet.getName() + "\n" +
                "Dataset shortname: " + dataSet.getShortName() + "\n" +

                "CLIJx GPU name: " + clijx.getGPUName() + "\n" +
                "CLIJx OpenCL version: " + clijx.getOpenCLVersion() + "\n" +
                "CLIJx mvn version: " + VersionUtils.getVersion(CLIJx.class) + "\n" +

                "MeshMeasurements version: " + VersionUtils.getVersion(MeshMeasurements.class) + "\n" +
                "zoomFactor: " + zoomFactor + "\n" +

                "translationX: " + translationX + "\n" +
                "translationY: " + translationY + "\n" +
                "translationZ: " + translationZ + "\n" +

                "rotationX: " + rotationX + "\n" +
                "rotationY: " + rotationY + "\n" +
                "rotationZ: " + rotationZ + "\n" +

                "blurSigma: " + blurSigma + "\n" +
                "backgroundBlurSigma: " + backgroundBlurSigma + "\n" +
                "projection_visualisation_on_screen: " + projection_visualisation_on_screen + "\n" +
                "projection_visualisation_to_disc: " + projection_visualisation_to_disc + "\n" +
                "maximumDistanceToMeshPoints: " + maximumDistanceToMeshPoints + "\n" +
                "numberDoubleErosionsForPseudoCellSegmentation: " + numberDoubleErosionsForPseudoCellSegmentation + "\n" +
                "numberDoubleDilationsForPseudoCellSegmentation: " + numberDoubleDilationsForPseudoCellSegmentation + "\n" +
                "threshold: " + threshold + "\n";

        try {
            Files.write(Paths.get(outputFolder + now + "_meshmeasurementlog.txt"), analysisLog.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }


        ResultsTable meshMeasurementTable = new ResultsTable();

        GenericDialog cancelDialog = new GenericDialog("Analysis running...");
        cancelDialog.addMessage("CLick on cancel to cancel.");
        cancelDialog.setModal(false);
        cancelDialog.show();

        for (int f = firstFrame; f <= lastFrame; f += frameStep) {
            if (cancelDialog.wasCanceled() || cancelDialog.wasOKed()) {
                break;
            }

            processFrame(outputFolder, meshMeasurementTable, f);

            clijx.reportMemory();
        }
    }

    public void processFrame(String outputFolder, ResultsTable meshMeasurementTable, int frame)
    {
        String targetFilename = "0000000" + frame;
        targetFilename = targetFilename.substring(targetFilename.length() - 6) + ".raw";;

        processFrame(outputFolder, meshMeasurementTable, frame, targetFilename);
    }

    public void processFrame(String outputFolder, ResultsTable meshMeasurementTable, int frame, String filename) {
        meshMeasurementTable.incrementCounter();
        meshMeasurementTable.addValue("Frame", frame);

        HashMap<String, ClearCLBuffer> resultImages = new HashMap<String, ClearCLBuffer>();

        long timestamp = System.currentTimeMillis();

        CLIJxUtils.clijx = clijx;

        // IJ.run("Close All");
        clijx.stopWatch("");

        System.out.println("Trying to read " + dataSet.getPath() + "/.../" + filename);
        ImagePlus timePointStack = dataSet.getImageData(frame);

        //System.out.println(foldername + filename)
        // # break;

        ClearCLBuffer pushedImage = clijx.push(timePointStack);
        //clijx.create([512, 1024, 67], NativeTypeEnum.UnsignedShort);
        pushedImage.setName("pushedImage");
        clijx.stopWatch("load data");

        if (doCut && cutTimeInSeconds < dataSet.getTimesInSeconds()[frame]) {
            cut(pushedImage,
                    this.gapX,
                    this.gapY,
                    this.gapWidth,
                    this.gapHeight,
                    this.cutBlurSigma,
                    this.cutBackgroundIntensity,
                    this.cutShiftDistance);

            this.cutShiftDistance = this.cutShiftDistance + cutShiftSpeed;
            cutShiftSpeed = cutShiftSpeed * cutDeceleration;
        }
        ;

        //ClearCLBuffer temp = clijx.create(new long[]{pushedImage.getWidth(), pushedImage.getHeight()}, pushedImage.getNativeType());
        //clijx.maximumZProjection(pushedImage, temp);
        //clijx.showGrey(temp, "Temp");
        //temp.close();
        //if (true) return;


        // IJ.run(imp, "32-bit", "");
        // IJ.run(imp, "Rotate 90 Degrees Right", "");
        // imp.show();

        Calibration calibration = timePointStack.getCalibration();
        double factorX = calibration.pixelWidth * zoomFactor;
        double factorY = calibration.pixelHeight * zoomFactor;
        double factorZ = calibration.pixelDepth * zoomFactor;

        // -----------------------------------------------------------------------
        // resampling
        long w = (long) (pushedImage.getWidth() * factorX);
        long h = (long) (pushedImage.getHeight() * factorY);
        long d = (long) (pushedImage.getDepth() * factorZ);

        System.out.println(new long[]{w, h, d});

        ClearCLImage resampledImage = clijx.create(new long[]{w, h, d}, CLIJUtilities.nativeToChannelType(pushedImage.getNativeType()));
        resampledImage.setName("resampledImage");

        System.out.println("PushedImage: " + pushedImage);

        clijx.resample(pushedImage, resampledImage, 1.0 / factorX, 1.0 / factorY, 1.0 / factorZ, true);


        clijx.stopWatch("resample");

        ClearCLImage backgroundSubtractedImage = clijx.create(resampledImage);
        if (backgroundBlurSigma > 0) {
            clijx.subtractBackground3D(resampledImage, backgroundSubtractedImage, backgroundBlurSigma, backgroundBlurSigma, backgroundBlurSigma);
        } else {
            clijx.copy(resampledImage, backgroundSubtractedImage);
        }
        //clijx.show(pushedImage, "Pus");
        //clijx.show(resampledImage, "Res");
        //clijx.show(backgroundSubtractedImage, "Bac");
        clijx.release(resampledImage);


        clijx.stopWatch("background subtraction");

        ClearCLBuffer inputImage = clijx.create(new long[]{w, h, d}, backgroundSubtractedImage.getNativeType());

        inputImage.setName("inputImage");
        //clijx.rotateRight(resampledImage, inputImage);

        AffineTransform3D at = new AffineTransform3D();
        at.translate(-inputImage.getWidth() / 2, -inputImage.getHeight() / 2, -inputImage.getDepth() / 2);
        at.translate(translationX, translationY, translationZ);
        at.rotate(0, rotationX * Math.PI / 180.0);
        at.rotate(1, rotationY * Math.PI / 180.0);
        at.rotate(2, rotationZ * Math.PI / 180.0);
        at.translate(inputImage.getWidth() / 2, inputImage.getHeight() / 2, inputImage.getDepth() / 2);

        clijx.affineTransform3D(backgroundSubtractedImage, inputImage, at);


        clijx.release(backgroundSubtractedImage);

        clijx.stopWatch("affine transform");

        //clijx.show(inputImage, "inputImage");
        //new WaitForUserDialog("hhhh").show();
        //if (single_stack_visualisation) {
        //    ImagePlus imp_inputImage = clijx.pull(inputImage);
        //}
        // clijx.saveAsTIF( inputImage,                outputFolder + "_input/" + filename + ".tif");

        // -----------------------------------------------------------------------
        // spot detection
        ClearCLBuffer detected_spots = spot_detection(inputImage);
        //clijx.show(detected_spots, "detected_spots");
        //if (true) return;
        clijx.stopWatch("spot detection");

        // label spots
        ClearCLBuffer labelled_spots = labelSpots(detected_spots);
        clijx.stopWatch("spot labelling (CCA)");

        // cell segmentation
        ClearCLBuffer segmented_cells = pseudo_cell_segmentation(labelled_spots);
        clijx.stopWatch("cell segmentation");

        if (eliminateSubSurfaceCells) {

            int number_of_spots = (int) clijx.maximumOfAllPixels(labelled_spots);

            ClearCLBuffer pointlist = clijx.create(new long[]{number_of_spots, 3});
            clijx.spotsToPointList(labelled_spots, pointlist);

            // add another label as back plane to close the surface on that side
            clijx.setPlane(segmented_cells, 0, number_of_spots + 1);

            // eliminate cells inside
            ClearCLBuffer new_segmented_cells = clijx.create(segmented_cells);
            clijx.excludeLabelsSubSurface(pointlist, segmented_cells, new_segmented_cells, segmented_cells.getWidth() / 2, segmented_cells.getHeight() / 2, -200);
            clijx.setPlane(new_segmented_cells, 0, 0);
            clijx.release(segmented_cells);
            segmented_cells = new_segmented_cells;

            // eliminate the same cells in the spot detection
            ClearCLBuffer new_labelled_spots = clijx.create(labelled_spots);
            ClearCLBuffer temp = clijx.create(labelled_spots);
            clijx.binaryAnd(labelled_spots, segmented_cells, temp);
            clijx.multiplyImages(temp, segmented_cells, new_labelled_spots);
            clijx.release(labelled_spots);
            labelled_spots = new_labelled_spots;

            clijx.release(segmented_cells);
            segmented_cells = pseudo_cell_segmentation(labelled_spots);

            //clijx.show(segmented_cells, "cells");
            //new WaitForUserDialog("wa").show();
            clijx.release(pointlist);
            clijx.release(temp);
        }
        if (eliminateOnSurfaceCells) {

            int number_of_spots = (int) clijx.maximumOfAllPixels(labelled_spots);

            ClearCLBuffer pointlist = clijx.create(new long[]{number_of_spots, 3});
            clijx.spotsToPointList(labelled_spots, pointlist);

            // add another label as back plane to close the surface on that side
            clijx.setPlane(segmented_cells, 0, number_of_spots + 1);

            // eliminate cells inside
            ClearCLBuffer new_segmented_cells = clijx.create(segmented_cells);
            clijx.excludeLabelsOnSurface(pointlist, segmented_cells, new_segmented_cells, segmented_cells.getWidth() / 2, segmented_cells.getHeight() / 2, -200);
            clijx.setPlane(new_segmented_cells, 0, 0);
            clijx.release(segmented_cells);
            segmented_cells = new_segmented_cells;

            // eliminate the same cells in the spot detection
            ClearCLBuffer new_labelled_spots = clijx.create(labelled_spots);
            ClearCLBuffer temp = clijx.create(labelled_spots);
            clijx.binaryAnd(labelled_spots, segmented_cells, temp);
            clijx.multiplyImages(temp, segmented_cells, new_labelled_spots);
            clijx.release(labelled_spots);
            labelled_spots = new_labelled_spots;

            clijx.release(segmented_cells);
            segmented_cells = pseudo_cell_segmentation(labelled_spots);

            //clijx.show(segmented_cells, "cells");
            //new WaitForUserDialog("wa").show();
            clijx.release(pointlist);
            clijx.release(temp);
        }

        if (storeMeasurements) {

            ResultsTable table = new ResultsTable();
            clijx.statisticsOfLabelledPixels(inputImage, segmented_cells, table);

            String[] columnsToKeep = new String[]{
                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.MEAN_INTENSITY.toString(),

                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.STANDARD_DEVIATION_INTENSITY.toString(),

                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.BOUNDING_BOX_WIDTH.toString(),
                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.BOUNDING_BOX_HEIGHT.toString(),
                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.BOUNDING_BOX_DEPTH.toString(),
                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.MASS_CENTER_X.toString(),
                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.MASS_CENTER_Y.toString(),
                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.MASS_CENTER_Z.toString(),
                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.PIXEL_COUNT.toString(),
                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.MINIMUM_INTENSITY.toString(),
                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.MAXIMUM_INTENSITY.toString()
            };
            for (String key : columnsToKeep) {
                storeMeasurement(key, table.getColumn(table.getColumnIndex(key)));
            }

        }

        //ClearCLBuffer max_membranes = null;
        //ClearCLBuffer mean_membranes = null;
        //ClearCLBuffer nonzero_min_membranes = null;

        if (projection_visualisation_on_screen || projection_visualisation_to_disc) {
            // cell outlines
            ClearCLBuffer cell_outlines = label_outlines(segmented_cells);
            clijx.stopWatch("outline labels");

            resultImages.put("04_max_membranes", max_z_projection(cell_outlines));
            resultImages.put("05_mean_membranes", mean_projection(cell_outlines));
            resultImages.put("06_nonzero_min_membranes", nonzero_min_projection(cell_outlines));

            clijx.release(cell_outlines);
        }

        // -----------------------------------------------------------------------
        // convert spots image to spot list
        int number_of_spots = (int) clijx.sumPixels(detected_spots);
        meshMeasurementTable.addValue("spot_count", number_of_spots);

        if (number_of_spots < 1) {
            // we have to have a non-zero sized stack in the next step...
            number_of_spots = 1;
        }
        ClearCLBuffer pointlist = clijx.create(new long[]{number_of_spots, 3});
        pointlist.setName("pointlist");
        clijx.stopWatch("intermediate ");
        //clijx.spotsToPointList(detected_spots, pointlist);
        //clijx.stopWatch("pointlist");
        clijx.labelledSpotsToPointList(labelled_spots, pointlist);
        clijx.stopWatch("labels to pointlist");
        //clijx.show(pointlist, "pli");

        // -----------------------------------------------------------------------
        // neighborhood analysis
        ClearCLBuffer distance_matrix = generateDistanceMatrix(pointlist, number_of_spots, zoomFactor);
        resultImages.put("14_distance_matrix", distance_matrix);
        resultImages.put("15_pixel2", clijx.create(1,1,1));

        //clijx.show(distance_matrix, "dist");

        clijx.stopWatch("distance map");


        ClearCLBuffer[] result;

        ClearCLBuffer touch_matrix = generateTouchMatrix(pointlist, number_of_spots, segmented_cells);
        clijx.setColumn(touch_matrix, 0, 0);

        // ------------------------------------------------------------------
        //fillTouchMatrixCompletely(touch_matrix);

        //ClearCLBuffer touch_matrix_squared = clijx.create(touch_matrix);
        //clijx.stopWatch("");
        //clijx.multiplyMatrix(touch_matrix, touch_matrix, touch_matrix_squared);
        //clijx.stopWatch("matrix multiplication");
        //clijx.setWhereXgreaterThanY(touch_matrix_squared, 0);
        //clijx.setWhereXgreaterThanY(touch_matrix, 0);
        //clijx.setWhereXequalsY(touch_matrix_squared, 0);
        //clijx.setWhereXequalsY(touch_matrix, 0);
        resultImages.put("13_touch_matrix", touch_matrix);
        //clijx.showGrey(touch_matrix, "touch");
        //clijx.showGrey(touch_matrix_squared, "touch_squared");
        //clijx.copy(touch_matrix_squared, touch_matrix);
        // ------------------------------------------------------------------


        clijx.stopWatch("");
        ClearCLBuffer distance_vector = measureAverageDistanceOfTouchingNeighbors(touch_matrix, distance_matrix);
        clijx.stopWatch("average distance");
        ClearCLBuffer surface_angle_vector = measureAverageSurfaceAngle(pointlist, touch_matrix);
        clijx.stopWatch("average angle");
        ClearCLBuffer neighbor_count_vector = countNeighbors(touch_matrix);
        //

        //ClearCLBuffer max_avg_surf_ang = null;
        //ClearCLBuffer mean_avg_surf_ang = null;
        //ClearCLBuffer nonzero_min_avg_surf_ang = null;
        {
            ClearCLBuffer average_surface_angle = generateParametricImage(surface_angle_vector, segmented_cells);
            storeMeasurement("surface_angle", surface_angle_vector);
            if (projection_visualisation_on_screen || projection_visualisation_to_disc) {
                resultImages.put("22_max_avg_surf_ang", max_z_projection(average_surface_angle));
                resultImages.put("23_mean_avg_surf_ang", mean_projection(average_surface_angle));
                resultImages.put("24_nonzero_min_avg_surf_ang", nonzero_min_projection(average_surface_angle));
            }
            clijx.release(average_surface_angle);
        }

        //ClearCLBuffer max_neigh_touch = null;
        //ClearCLBuffer mean_neigh_touch = null;
        //ClearCLBuffer nonzero_min_neigh_touch = null;
        {
            ClearCLBuffer neighbor_count = generateParametricImage(neighbor_count_vector, segmented_cells);
            storeMeasurement("neighbor_count", neighbor_count_vector);
            if (projection_visualisation_on_screen || projection_visualisation_to_disc) {
                resultImages.put("19_max_neigh_touch", max_z_projection(neighbor_count));
                resultImages.put("20_mean_neigh_touch", mean_projection(neighbor_count));
                resultImages.put("21_nonzero_min_neigh_touch", nonzero_min_projection(neighbor_count));
            }
            clijx.release(neighbor_count);
        }

        //clijx.show(averag_distance_of_touching_neighbors, "averag_distance_of_touching_neighbors");

        //clijx.showGrey(touch_matrix, "touch_matrix");
        //clijx.showGrey(distance_matrix, "distance_matrix");
        clijx.stopWatch("touch map");
        ClearCLBuffer relevantDistances = clijx.create(distance_matrix.getDimensions(), clijx.Float);
        clijx.multiplyImages(distance_matrix, touch_matrix, relevantDistances);

        System.out.println("Dist mat " + Arrays.toString(distance_matrix.getDimensions()));
        System.out.println(clijx.reportMemory());
        double meanDistance = clijx.meanOfPixelsAboveThreshold(relevantDistances, 0);
        double varianceDistance = clijx.varianceOfMaskedPixels(relevantDistances, touch_matrix, meanDistance);
        meshMeasurementTable.addValue("mean_neighbor_distance", meanDistance);
        meshMeasurementTable.addValue("variance_neighbor_distance", varianceDistance);
        double numberOfTouches = clijx.sumOfAllPixels(touch_matrix);
        meshMeasurementTable.addValue("number_of_touches", numberOfTouches);

        ClearCLBuffer mesh = clijx.create(inputImage);
        mesh.setName("mesh");
        clijx.touchMatrixToMesh(pointlist, touch_matrix, mesh);
        clijx.stopWatch("mesh");

        ClearCLBuffer average_distance_of_touching_neighbors = generateParametricImage(distance_vector, segmented_cells);
        storeMeasurement("average_distance", average_distance_of_touching_neighbors);

        double meanDistance2 = clijx.meanOfMaskedPixels(average_distance_of_touching_neighbors, detected_spots);
        double varianceDistance2 = clijx.varianceOfMaskedPixels(average_distance_of_touching_neighbors, detected_spots, meanDistance);
        meshMeasurementTable.addValue("mean_neighbor_distance2", meanDistance2);
        meshMeasurementTable.addValue("variance_neighbor_distance2", varianceDistance2);

        //ClearCLBuffer max_avg_dist = null;
        //ClearCLBuffer mean_avg_dist = null;
        //ClearCLBuffer nonzero_min_avg_dist = null;
        if (projection_visualisation_on_screen || projection_visualisation_to_disc) {
            resultImages.put("16_max_avg_dist", max_z_projection(average_distance_of_touching_neighbors));
            resultImages.put("17_mean_avg_dist", mean_projection(average_distance_of_touching_neighbors));
            resultImages.put("18_nonzero_min_avg_dist", nonzero_min_projection(average_distance_of_touching_neighbors));
        }
        clijx.release(average_distance_of_touching_neighbors);

        if (measure_distances_in_detail) {
            int[] ns = new int[]{1, 2, 4, 6, 10, 20};
            clijx.setRow(distance_matrix, 0, Float.MAX_VALUE);
            clijx.setColumn(distance_matrix, 0, Float.MAX_VALUE);
            clijx.setWhereXequalsY(distance_matrix, Float.MAX_VALUE);
            clijx.show(distance_matrix, "mod_dist");
            for (int n : ns) {
                ClearCLBuffer averageDistanceOfNclosestNeighbors_vector = measureAverageDistanceOfNClosestNeighbors(distance_matrix, n);
                ClearCLBuffer averageDistanceOfNclosestNeighbors_map = generateParametricImage(averageDistanceOfNclosestNeighbors_vector, segmented_cells);
                //averageDistanceOf1closestNeighbors
                resultImages.put("99_averageDistanceOf_" + n + "_closestNeighbors", nonzero_min_projection(averageDistanceOfNclosestNeighbors_map));
                clijx.release(averageDistanceOfNclosestNeighbors_map);
                clijx.release(averageDistanceOfNclosestNeighbors_vector);
            }
        }








        //
//            ClearCLBuffer minimum_distance_of_touching_neighbors = measureMinimumDistanceOfTouchingNeighbors(touch_matrix, distance_matrix, segmented_cells);
//
//
//            double meanMinDistance = clijx.meanOfMaskedPixels(minimum_distance_of_touching_neighbors, detected_spots);
//            double varianceMinDistance = clijx.varianceOfMaskedPixels(minimum_distance_of_touching_neighbors, detected_spots, meanDistance);
//            meshMeasurementTable.addValue("mean_min_neighbor_distance2", meanDistance2);
//            meshMeasurementTable.addValue("variance_neighbor_distance2", varianceDistance2);
//
//            ClearCLBuffer max_min_dist = null;
//            ClearCLBuffer mean_min_dist = null;
//            ClearCLBuffer nonzero_min_min_dist = null;
//            if (projection_visualisation_on_screen || projection_visualisation_to_disc) {
//                max_min_dist = max_z_projection(minimum_distance_of_touching_neighbors);
//                mean_min_dist = mean_projection(minimum_distance_of_touching_neighbors);
//                nonzero_min_min_dist = nonzero_min_projection(minimum_distance_of_touching_neighbors);
//            }


        // -----------------------------------------------------------------------
        // Visualisation / output
        if (exportMesh) {
            if (number_of_spots > 1 && numberOfTouches > 0) {
                clijx.stopWatch("export vtk");
                clijx.writeVTKLineListToDisc(pointlist, touch_matrix, outputFolder + "_vtk_mesh/" + filename.replace(".raw", "") + ".vtk");
                //clijx.writeXYZPointListToDisc(pointlist, outputFolder + "_vtk_mesh/" + filename.replace(".raw", "") + ".xyz");
                clijx.stopWatch("export vtk");
            }
        }

        if (projection_visualisation_on_screen || projection_visualisation_to_disc) {

            // save maximum and average projections to disc
            result = arg_max_projection(inputImage);
            resultImages.put("01_max_image", result[0]);
            resultImages.put("02_arg_max_image", result[1]);
            resultImages.put("03_max_spots", max_z_projection(detected_spots));

            result = arg_max_projection(segmented_cells);
            resultImages.put("07_max_cells", result[0]);
            resultImages.put("08_arg_max_cells", result[1]);
            resultImages.put("09_pixel1", clijx.create(1,1,1));

            resultImages.put("10_max_mesh_x", max_x_projection(mesh));
            resultImages.put("11_max_mesh_y", max_y_projection(mesh));
            resultImages.put("12_max_mesh_z", max_z_projection(mesh));


            List<String> resultKeys = new ArrayList<String>(resultImages.keySet());

            Collections.sort(resultKeys);

            if (projection_visualisation_to_disc) {
                for (String key : resultKeys) {
                    String saveKey = key; //.substring(2);
                    ClearCLBuffer buffer = resultImages.get(key);
                    if (drawText) {
                        ImagePlus imp = clijx.pull(buffer);
                        if (key.endsWith("image")) {
                            imp.setDisplayRange(0, 1000);
                        } else if (key.endsWith("dist") || key.endsWith("_ang") || key.contains("_neigh_touch")) {
                            imp.setDisplayRange(0, 255);
                        } else if (key.contains("mesh")) {
                            imp.setDisplayRange(0, 1);
                        }
                        IJ.run(imp, "8-bit", "");

                        ImageProcessor ip = imp.getProcessor();
                        ip.setColor(new Color(255, 255, 255));
                        ip.setFont(new Font("SanSerif", Font.PLAIN, 15));
                        ip.drawString(key, 10, 30);
                        ip.setFont(new Font("SanSerif", Font.PLAIN, 25));
                        ip.drawString(humanReadableTime((int) dataSet.getTimesInSeconds()[frame]), 10, 65);

                        imp.getCalibration().pixelWidth = 1.0 / zoomFactor;
                        imp.getCalibration().pixelHeight = 1.0 / zoomFactor;
                        imp.getCalibration().setUnit("um");
                        IJ.run(imp,
                                "Scale Bar...",
                                "width=100 height=5 font=25 color=White background=None location=[Lower Left]");

                        buffer = clijx.push(imp);
                    }
                    clijx.saveAsTIF(buffer, outputFolder + saveKey + "/" + filename + ".tif");
                }
                /*
                clijx.saveAsTIF(max_image, outputFolder + "_max_image/" + filename + ".tif");
                clijx.saveAsTIF(arg_max_image, outputFolder + "_arg_max_image/" + filename + ".tif");
                clijx.saveAsTIF(max_spots, outputFolder + "_max_spots/" + filename + ".tif");
                clijx.saveAsTIF(max_cells, outputFolder + "_max_cells/" + filename + ".tif");
                clijx.saveAsTIF(arg_max_cells, outputFolder + "_arg_max_cells/" + filename + ".tif");
                clijx.saveAsTIF(mean_membranes, outputFolder + "_mean_membranes/" + filename + ".tif");
                clijx.saveAsTIF(max_membranes, outputFolder + "_max_membranes/" + filename + ".tif");
                clijx.saveAsTIF(max_avg_dist, outputFolder + "_max_avg_dist/" + filename + ".tif");
                clijx.saveAsTIF(mean_avg_dist, outputFolder + "_mean_avg_dist/" + filename + ".tif");



                clijx.saveAsTIF(max_mesh_x, outputFolder + "_max_mesh_x/" + filename + ".tif");
                clijx.saveAsTIF(max_mesh_y, outputFolder + "_max_mesh_y/" + filename + ".tif");
                clijx.saveAsTIF(max_mesh_z, outputFolder + "_max_mesh_z/" + filename + ".tif");
                clijx.saveAsTIF(nonzero_min_membranes, outputFolder + "_nonzero_min_membranes/" + filename + ".tif");
                clijx.saveAsTIF(nonzero_min_avg_dist, outputFolder + "_nonzero_min_avg_dist/" + filename + ".tif");

                clijx.saveAsTIF(max_avg_surf_ang, outputFolder + "_max_avg_surf_ang/" + filename + ".tif");
                clijx.saveAsTIF(mean_avg_surf_ang, outputFolder + "_mean_avg_surf_ang/" + filename + ".tif");
                clijx.saveAsTIF(nonzero_min_avg_surf_ang, outputFolder + "_nonzero_min_avg_surf_ang/" + filename + ".tif");

                clijx.saveAsTIF(max_neigh_touch, outputFolder + "_max_neigh_touch/" + filename + ".tif");
                clijx.saveAsTIF(mean_neigh_touch, outputFolder + "_mean_neigh_touch/" + filename + ".tif");
                clijx.saveAsTIF(nonzero_min_neigh_touch, outputFolder + "_nonzero_min_neigh_touch/" + filename + ".tif");
*/

//                    clijx.saveAsTIF(max_min_dist, outputFolder + "_max_min_dist/" + filename + ".tif");
//                    clijx.saveAsTIF(mean_min_dist, outputFolder + "_mean_min_dist/" + filename + ".tif");
//                    clijx.saveAsTIF(nonzero_min_min_dist, outputFolder + "_nonzero_min_min_dist/" + filename + ".tif");

            }
            clijx.stopWatch("writing to disc");

            if (projection_visualisation_on_screen) {
                /*
                clijx.showGrey(max_image, "_max_image");
                clijx.showGrey(arg_max_image, "_arg_max_image");
                clijx.showGrey(max_spots, "_max_spots");
                clijx.showGrey(max_cells, "_max_cells");
                clijx.showGrey(arg_max_cells, "_arg_max_cells");
                clijx.showGrey(mean_membranes, "_mean_membranes");
                clijx.showGrey(max_membranes, "_max_membranes");

                clijx.showGrey(max_avg_dist, "_max_avg_dist");
                clijx.showGrey(mean_avg_dist, "_mean_avg_dist");

                clijx.showGrey(max_mesh_x, "_max_mesh_x");
                clijx.showGrey(max_mesh_y, "_max_mesh_y");
                clijx.showGrey(max_mesh_z, "_max_mesh_z");

                clijx.showGrey(nonzero_min_membranes, "_nonzero_min_membranes");
                clijx.showGrey(nonzero_min_avg_dist, "_nonzero_min_avg_dist");

                clijx.showGrey(max_avg_surf_ang, "_max_avg_surf_ang");
                clijx.showGrey(mean_avg_surf_ang, "_mean_avg_surf_ang");
                clijx.showGrey(nonzero_min_avg_surf_ang, "_nonzero_min_avg_surf_ang");

                clijx.showGrey(max_neigh_touch, "_max_neigh_touch");
                clijx.showGrey(mean_neigh_touch, "_mean_neigh_touch");
                clijx.showGrey(nonzero_min_neigh_touch, "_nonzero_min_neigh_touch");


//                    clijx.showGrey(max_min_dist, "_max_min_dist");
//                    clijx.showGrey(mean_min_dist, "_mean_min_dist");
//                    clijx.showGrey(nonzero_min_min_dist, "_nonzero_min_min_dist");

                // clijx.showGrey(distance_matrix, "distance_matrix");
                // clijx.showGrey(pointlist, "pointlist");
                */
                for (String key : resultKeys) {
                    String saveKey = key.substring(2);
                    clijx.showGrey(resultImages.get(key), saveKey);
                }

                clijx.organiseWindows(0, 0, 8, 3, 200, 350);
                //clijx.organiseWindows(500, -1300, 5, 3, 630, 420);

            }


            clijx.stopWatch("visualisation");
        }

        System.out.println("Whole analysis took " + (System.currentTimeMillis() - timestamp) + " ms");
        clijx.release(pushedImage);

        IJ.log(clijx.reportMemory());

        clijx.clear();
        //break;

        if (projection_visualisation_on_screen) {
            meshMeasurementTable.show("Mesh measurements results");
        }
        if (projection_visualisation_to_disc) {
            dataSet.saveMeasurementTable(meshMeasurementTable, "processed/meshMeasurements.csv");
        }
    }

    private ClearCLBuffer countNeighbors(ClearCLBuffer touch_matrix) {
        ClearCLBuffer neighbor_count = clijx.create(new long[]{touch_matrix.getWidth(), 1, 1}, clijx.Float);
        neighbor_count.setName("neighbor_count");
        clijx.countTouchingNeighbors(touch_matrix, neighbor_count);
        return neighbor_count;
    }

    /*
        public double meanOfMaskedPixels(ClearCLBuffer clImage, ClearCLBuffer mask) {
            ClearCLBuffer tempBinary = clijx.create(clImage);
            // todo: if we can be sure that the mask has really only 0 and 1 pixel values, we can skip this first step:
            ClearCLBuffer tempMultiplied = clijx.create(clImage);
            EqualConstant.equalConstant(clijx, mask, tempBinary, 1f);
            clijx.mask(clImage, tempBinary, tempMultiplied);
            double sum = clijx.sumPixels(tempMultiplied);
            double count = clijx.sumPixels(tempBinary);
            System.out.println("sum " + sum);
            System.out.println("count " + count);
            clijx.release(tempBinary);
            clijx.release(tempMultiplied);
            return sum / count;
        }
    */


    private ClearCLBuffer generateParametricImage(ClearCLBuffer distanceVector, ClearCLBuffer label_map) {


        //clijx.show(distanceVector, "disvec");


        ClearCLBuffer parametricDistanceImage = clijx.create(label_map.getDimensions(), clijx.Float);
        clijx.replaceIntensities(label_map, distanceVector, parametricDistanceImage);

        //clijx.release(distanceVector);

        return parametricDistanceImage;
    }

    private ClearCLBuffer measureMinimumDistanceOfTouchingNeighbors(ClearCLBuffer touch_matrix, ClearCLBuffer distance_matrix, ClearCLBuffer label_map) {
        ClearCLBuffer distanceVector = clijx.create(new long[]{touch_matrix.getWidth(), 1, 1}, clijx.Float);

        clijx.minimumDistanceOfTouchingNeighbors(distance_matrix, touch_matrix, distanceVector);



        //clijx.show(distanceVector, "disvec");

        ClearCLBuffer parametricDistanceImage = clijx.create(label_map.getDimensions(), clijx.Float);
        clijx.replaceIntensities(label_map, distanceVector, parametricDistanceImage);

        clijx.release(distanceVector);

        return parametricDistanceImage;
    }


    private ClearCLBuffer spot_detection (ClearCLBuffer inputImage) {
        // blur a bit and detect maxima
        ClearCLBuffer blurred = clijx.create(inputImage);
        blurred.setName("blurred");
        ClearCLBuffer thresholded = clijx.create(inputImage);
        thresholded.setName("thresholded");
        ClearCLBuffer detected_spots = clijx.create(inputImage.getDimensions(), clijx.UnsignedByte);
        detected_spots.setName("detected_spots");
        ClearCLBuffer masked = clijx.create(inputImage);
        masked.setName("masked");

        // -----------------------------------------------------------------------
        // background / noise removal

        // clijx.differenceOfGaussian(inputImage,blurred,3,3,0,15,15,0);
        // clijx.absoluteInplace(blurred);

        //if (backgroundBlurSigma > 0) {
        //    clijx.differenceOfGaussian(inputImage,blurred, blurSigma, blurSigma, blurSigma, backgroundBlurSigma, backgroundBlurSigma, backgroundBlurSigma);
        //} else {
        clijx.blur(inputImage, blurred, blurSigma, blurSigma, blurSigma);
        //}
        //clijx.show(blurred, "blurred");
        // ----------------------------------------------------------------------
        // spot detection
        clijx.detectMaximaBox(blurred, detected_spots, 1);

        // remove spots in background
        //clijx.automaticThreshold(blurred, thresholded, thresholdAlgorithm);
        clijx.threshold(blurred, thresholded, threshold);
        //clijx.show(blurred, "blurred");
        clijx.mask(detected_spots, thresholded, masked);


        clijx.copy(masked, detected_spots);
        // clijx.show(detected_spots,"t");

        // -----------------------------------------------------------------------
        // clean up
        clijx.release(thresholded);
        clijx.release(masked);
        clijx.release(blurred);
        return detected_spots;
    }

    private ImagePlus spot_visualisation(ClearCLBuffer detected_spots) {
        // make spots bigger for visualisation
        ClearCLBuffer temp = clijx.create(detected_spots);
        temp.setName("temp");
        clijx.dilateBox(detected_spots,temp);
        clijx.dilateSphere(temp,detected_spots);
        clijx.dilateBox(detected_spots,temp);
        clijx.dilateSphere(temp,detected_spots);
        clijx.copy(detected_spots,temp);
        clijx.binaryEdgeDetection(temp,detected_spots);
        clijx.release(temp);

        // clijx.show(detected_spots,"detected_spots");
        return clijx.pull(detected_spots);
    }



    private ClearCLBuffer distanceMatrixToMesh2(ClearCLBuffer inputImage, ClearCLBuffer pointlist, ClearCLBuffer distance_matrix) {
        ClearCLBuffer mesh = clijx.create(inputImage);
        mesh.setName("mesh");
        ClearCLBuffer tempMesh1 = clijx.create(inputImage);
        ClearCLBuffer tempMesh2 = clijx.create(inputImage);
        clijx.set(tempMesh2, 0);
        for (int d = 1; d < maximumDistanceToMeshPoints + 1; d = d + 5) {
            clijx.set(tempMesh1, 0);
            clijx.distanceMatrixToMesh(pointlist, distance_matrix, tempMesh1, d);
            clijx.addImages(tempMesh1, tempMesh2, mesh);
            clijx.copy(mesh, tempMesh2);
        }
        tempMesh1.close();
        tempMesh2.close();
        return mesh;
    }


    private ClearCLBuffer pseudo_cell_segmentation(ClearCLBuffer labelled_spots) {

        ClearCLBuffer tempSpots1 = clijx.create(labelled_spots);
    	tempSpots1.setName("Segmented cells");
    	clijx.copy(labelled_spots, tempSpots1);

        ClearCLBuffer tempSpots2 = clijx.create(labelled_spots);
	    tempSpots2.setName("tempSpots2");
        ClearCLBuffer flag =clijx.create(new long[]{1, 1, 1});
	    flag.setName("flag");

    	for (int j = 0; j < numberDoubleDilationsForPseudoCellSegmentation; j++) {
            clijx.onlyzeroOverwriteMaximumDiamond(tempSpots1, flag, tempSpots2);
            clijx.onlyzeroOverwriteMaximumBox(tempSpots2, flag, tempSpots1);
        }
        clijx.release(flag);

        ClearCLBuffer tempSpots3 = clijx.create(labelled_spots);
    	tempSpots3.setName("tempSpots3");
	    clijx.threshold(tempSpots1,tempSpots2,1);

        for (int j = 0; j < numberDoubleErosionsForPseudoCellSegmentation; j++) {
            clijx.erodeBox(tempSpots2, tempSpots3);
            clijx.erodeBox(tempSpots3, tempSpots2);
        }
        clijx.erodeBox(tempSpots2,tempSpots3);
	    clijx.copy(tempSpots1,tempSpots2);
	    clijx.mask(tempSpots2,tempSpots3,tempSpots1);
	    clijx.release(tempSpots3);

    	// clijx.show(tempSpots1,"tempSpots1");
	    clijx.release(tempSpots2);
        return tempSpots1;
    }



    public MeshMeasurements setExportMesh(boolean exportMesh) {
        this.exportMesh = exportMesh;
        return this;
    }




    private void cut(ClearCLBuffer input,
                     double gapX,
                     double gapY,
                     double gapWidth,
                     double gapHeight,
                     double blurSigma,
                     double backgroundIntensity,
                     double shiftDistance
    ) {
        System.out.println("shiftDistance " + shiftDistance);
        //ClearCLBuffer input = clij2.pushCurrentZStack(stack);
        //ClearCLBuffer maxproj = clij2.create(new long[]{input.getWidth(), input.getHeight()}, clij2.Float);

        ClearCLBuffer shiftX = clijx.create(new long[]{input.getWidth(), input.getHeight()}, clijx.Float);
        ClearCLBuffer shiftY = clijx.create(shiftX);
        ClearCLBuffer temp = clijx.create(shiftX);
        ClearCLBuffer temp2 = clijx.create(shiftX);
        ClearCLBuffer temp3 = clijx.create(shiftX);
        ClearCLImage slice = clijx.create(shiftX.getDimensions(), net.haesleinhuepf.clij2.utilities.CLIJUtilities.nativeToChannelType(shiftX.getNativeType()));

//        double gapX = 300;
//        double gapY = 512;
//        double gapWidth = 10;
//        double gapHeight = 200;
//
//        //double shiftDistance = 100;
//        double blurSigma = 25;
//        double backgroundIntensity = 300;

        //for (double shiftDistance = 0; shiftDistance < 50; shiftDistance += 1 ) {

            clijx.set(temp, 0);
            clijx.drawBox(temp, gapX - gapWidth, gapY, 0, gapWidth, gapHeight, 1, -shiftDistance);
            clijx.gaussianBlur2D(temp, temp3, blurSigma, blurSigma);

            clijx.set(temp, 0);
            clijx.drawBox(temp, gapX + gapWidth, gapY, 0, gapWidth, gapHeight, 1, shiftDistance);
            clijx.gaussianBlur2D(temp, slice, blurSigma, blurSigma);

            clijx.addImages(temp3, slice, temp2);
            clijx.invert(temp2, shiftX);

            //clijx.showGrey(shiftX, "shiftX");

            //clij2.set(temp2, 0);
            //clij2.drawBox(slice, gapX, gapY, 0, gapWidth, gapHeight, 1);
            //clij2.multiplyImageAndScalar(temp2, temp, backgroundIntensity);

            for (int z = 0; z < input.getDepth(); z++) {
                clijx.copySlice(input, slice, z);

                clijx.drawBox(slice, gapX, gapY, 0, gapWidth, gapHeight, 1, backgroundIntensity);

                //clij2.minimumImages(temp, slice, temp2);

                clijx.applyVectorField2D(slice, shiftX, shiftY, temp);
                clijx.copySlice(temp, input, z);
            }


            //clij2.maximumZProjection(input, maxproj);
            //clijx.showGrey(maxproj, "maxproj");
        //}

        //clij2.release(maxproj);
        clijx.release(shiftX);
        clijx.release(shiftY);
        clijx.release(temp);
        clijx.release(temp2);
        clijx.release(temp3);
        clijx.release(slice);
    }

    public static void main(String ... arg) {

        if (true)
        {
            new ImageJ();

            //String sourceFolder = "C:/structure/data/2018-05-23-16-18-13-89-Florence_multisample/";
            String sourceFolder = "C:/structure/data/2019-12-17-16-54-37-81-Lund_Tribolium_nGFP_TMR/";
            //String sourceFolder = "C:/structure/data/2019-10-28-17-22-59-23-Finsterwalde_Tribolium_nGFP/";
            //String datasetFolder = "opticsprefused";
            String datasetFolder = "C0opticsprefused";

            ClearControlDataSet dataSet = ClearControlDataSetOpener.open(sourceFolder, datasetFolder);

            int startFrame = 1000;
            int endFrame = startFrame;

            new MeshMeasurements(dataSet).
                    //setCLIJx(CLIJx.getInstance("2070")).
                    setProjectionVisualisationToDisc(false).
                    setProjectionVisualisationOnScreen(true).
                    setExportMesh(false).
                    setThreshold(300).
                    //setEliminateOnSurfaceCells(true).
                    //setBlurSigma(1).
                    //setEliminateSubSurfaceCells(true).
                    /*setCut(
                            98,
                            482,
                            10,
                            170,
                            30,
                            275,
                            50,
                            dataSet.getTimesInSeconds()[1050],
                            0.9
                    ).*/
                    setFirstFrame(startFrame).
                    setLastFrame(endFrame).
//                setFirstFrame(startFrame).
                    //              setFrameStep(100).
                    //            setLastFrame(endFrame).
                            run();
        }

        if (false)
        {
            new ImageJ();


            String sourceFolder = "C:/structure/data/2019-12-17-16-54-37-81-Lund_Tribolium_nGFP_TMR/";
            //String sourceFolder = "C:/structure/data/2019-10-28-17-22-59-23-Finsterwalde_Tribolium_nGFP/";
            String datasetFolder = "C0opticsprefused";

            ClearControlDataSet dataSet = ClearControlDataSetOpener.open(sourceFolder, datasetFolder);

            int startFrame = 1050;
            int endFrame = startFrame + 100;

            new MeshMeasurements(dataSet).
                    setCLIJx(CLIJx.getInstance("2060")).
                    setProjectionVisualisationToDisc(true).
                    setProjectionVisualisationOnScreen(true).
                    setExportMesh(false).
                    setThreshold(300).
                    //setEliminateOnSurfaceCells(true).
                    setCut(
                          98,
                          482,
                          10,
                          170,
                          30,
                           275,
                            25,
                            dataSet.getTimesInSeconds()[1050],
                            0.9
                    ).
                    setAnnotateImagesWithText(true).
                    setFirstFrame(startFrame).
                    setLastFrame(endFrame).
    //                setFirstFrame(startFrame).
                    //              setFrameStep(100).
                    //            setLastFrame(endFrame).
                            run();
        }
    }

    private MeshMeasurements setAnnotateImagesWithText(boolean drawText) {
        this.drawText = drawText;
        return this;
    }
}
