package net.haesleinhuepf.imagej.zoo.measurement;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.ClearCLImage;
import net.haesleinhuepf.clij.clearcl.ClearCLKernel;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.converters.helptypes.Float1;
import net.haesleinhuepf.clij2.plugins.OnlyzeroOverwriteMaximumBox;
import net.haesleinhuepf.clij2.plugins.OnlyzeroOverwriteMaximumDiamond;
import net.haesleinhuepf.clij2.plugins.StatisticsOfLabelledPixels;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.utilities.CLIJUtilities;
import net.haesleinhuepf.clijx.weka.ApplyWekaToTable;
import net.haesleinhuepf.clijx.weka.CLIJxWeka2;
import net.haesleinhuepf.clijx.weka.TrainWekaFromTable;
import net.haesleinhuepf.imagej.clijutils.CLIJxUtils;
import net.haesleinhuepf.imagej.zoo.ZooUtilities;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;
import net.haesleinhuepf.imagej.zoo.measurement.classification.GenerateClassifiedTouchMatrix;
import net.haesleinhuepf.imagej.zoo.measurement.classification.MostPopularValueOfTouchingNeighbors;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.util.VersionUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.List;

import static net.haesleinhuepf.imagej.clijutils.CLIJxUtils.*;

public class MeshMeasurements extends DataSetMeasurements {
    CLIJx clijx;

    double zoomFactor = 1.5; // -> each analysed voxel is 1.5x1.5x1.5 microns large`


    double blurSigma = 3;
    double backgroundBlurSigma = 0;

    boolean show_table_visualisation_on_screen = false;
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
    private boolean storeProjections = true;

    private boolean transposeXY = false;
    private Integer spot_count = null;
    private boolean autoContextClassification = true;

    public MeshMeasurements setTransposeXY(boolean transposeXY) {
        this.transposeXY = transposeXY;
        return this;
    }

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

    public MeshMeasurements(ClearControlDataSet dataSet, CLIJx clijx) {
        super(dataSet);
        this.clijx = clijx;
    }

    public MeshMeasurements setZoomFactor(double zoomFactor) {
        this.zoomFactor = zoomFactor;
        return this;
    }

    public MeshMeasurements setStoreMeasurements(boolean storeMeasurements) {
        this.storeMeasurements = storeMeasurements;
        return this;
    }

    public MeshMeasurements setAutoContextClassification(boolean autoContextClassification) {
        this.autoContextClassification = autoContextClassification;
        return this;
    }

    public boolean isAutoContextClassification() {
        return autoContextClassification;
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

        storeMeasurement(key, array);
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

        storeMeasurement(key, array);
    }

    private void storeMeasurement(String key, float[] measurement) {
        if (!storeMeasurements) {
            return;
        }
        if (measurements.containsKey(key)) {
            measurements.remove(key);
        }
        measurements.put(key, measurement);

        ClearCLBuffer labelMap = (ClearCLBuffer) resultImages.get("07_max_labelled_cells");
        System.out.println("Labelmap " + labelMap);
        if (labelMap != null) {
            ClearCLBuffer vector = clijx.push(new Float1(measurement));
            resultImages.put("07_map_" + key, generateParametricImage(vector, labelMap));
            vector.close();
        }
    }

    public ResultsTable getAllMeasurements() {
        ArrayList<String> keys = new ArrayList<>();
        keys.addAll(measurements.keySet());
        Collections.sort(keys);

        ResultsTable table = new ResultsTable();
        if (measurements.size() == 0) {
            return table;
        }

        //float[] anyMeasuremnt = measurements.get(measurements.keySet().iterator());
        //for (int i = 0; i < anyMeasuremnt.length; i++) {
        //    table.incrementCounter();
        //}

        for (String key : keys) {
            float[] measurement = measurements.get(key);
            for (int i = 0; i < measurement.length; i++) {
                table.setValue(key, i, measurement[i]);
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

    public MeshMeasurements setStoreProjections(boolean storeProjections) {
        this.storeProjections = storeProjections;
        return this;
    }


    public MeshMeasurements setShowTableOnScreen(boolean show_table_visualisation_on_screen) {
        this.show_table_visualisation_on_screen = show_table_visualisation_on_screen;
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

    public double getTranslationX() {
        return translationX;
    }

    public double getTranslationY() {
        return translationY;
    }

    public double getTranslationZ() {
        return translationZ;
    }

    public double getRotationX() {
        return rotationX;
    }

    public double getRotationY() {
        return rotationY;
    }

    public double getRotationZ() {
        return rotationZ;
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

    public double getBlurSigma() {
        return blurSigma;
    }

    public double getBackgroundBlurSigma() {
        return backgroundBlurSigma;
    }

    public double getThreshold() {
        return threshold;
    }

    public boolean isEliminateSubSurfaceCells() {
        return eliminateSubSurfaceCells;
    }

    public boolean isEliminateOnSurfaceCells() {
        return eliminateOnSurfaceCells;
    }

    public boolean isDrawText() {
        return drawText;
    }

    public MeshMeasurements setDrawText(boolean drawText) {
        this.drawText = drawText;
        return this;
    }

    public Integer getSpotCount() {
        return spot_count;
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

            invalidate();
            processFrame(outputFolder, meshMeasurementTable, f);

            //clijx.reportMemory();
        }
    }

    public ClearCLImageInterface processFrameForRequestedResult(String outputFolder, ResultsTable meshMeasurementTable, int frame, String requested_result) {
        if (requested_result.length() == 0 || !resultImages.containsKey(requested_result)) {
            String targetFilename = "0000000" + frame;
            targetFilename = targetFilename.substring(targetFilename.length() - 6) + ".raw";

            processFrame(outputFolder, meshMeasurementTable, frame, targetFilename, requested_result);
        }
        return resultImages.get(requested_result);
    }

    @Deprecated
    public void processFrame(String outputFolder, ResultsTable meshMeasurementTable, int frame)
    {
        processFrameForRequestedResult(outputFolder, meshMeasurementTable, frame, "");
    }

    public void invalidate() {
        processed_frame = -1;
        spot_count = null;
        clijx.clear();
        resultImages.clear();
        measurements.clear();
    }

    public void invalidateTransformed() {
        String keyToKeep = "VOL_01_RESAMPLED_INPUT";
        ClearCLImageInterface image = resultImages.get(keyToKeep);
        for (String key : resultImages.keySet()) {
            if (key.compareTo(keyToKeep) != 0) {
                resultImages.get(key).close();
            }
        }
        resultImages.clear();
        if (image != null) {
            resultImages.put(keyToKeep, image);
        }
    }


    public ClearCLImageInterface getResult(int frame, String requested_result) {
        if (processed_frame != frame) {
            invalidate();
        }

        if (!resultImages.containsKey(requested_result)) {
            processFrameForRequestedResult(null, null, frame, requested_result);
        }
        if (!resultImages.containsKey(requested_result)) {
            System.out.println("WAAAAA Couldn't make " + requested_result);
        }
        return resultImages.get(requested_result);
    }

    private String[] resultIDs = null;
    public void setResultIDs(String[] resultIDs) {
        this.resultIDs = resultIDs;
    }

    public String[] getResultIDs() {
        if (resultIDs != null ) {
            return resultIDs;
        }
        Set<String> keys = resultImages.keySet();
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(keys);
        Collections.sort(list);

        ArrayList<String> secondList = new ArrayList<>();

        ClearCLImageInterface defaultChannel = getResult(processed_frame, list.get(0));
        secondList.add(list.get(0));

        for (int i = 1; i < list.size(); i++) {
            ClearCLImageInterface channel = getResult(processed_frame, list.get(i));
            if (channel.getWidth() == defaultChannel.getWidth() && channel.getHeight() == defaultChannel.getHeight()) {
                secondList.add(list.get(i));
            }
        }

        String[] array = new String[secondList.size()];
        secondList.toArray(array);
        return array;
    }

    HashMap<String, ClearCLImageInterface> resultImages = new HashMap<String, ClearCLImageInterface>();
    int processed_frame = - 1;

    public void processFrame(String outputFolder, ResultsTable meshMeasurementTable, int frame, String filename) {
        processFrame(outputFolder, meshMeasurementTable, frame, filename, "");
    }

    public void processFrame(String outputFolder, ResultsTable meshMeasurementTable, int frame, String filename, String requested_result) {
        // --------------------------------------------------------------------
        // HANDLE Table
        if (meshMeasurementTable != null) {
            meshMeasurementTable.incrementCounter();
            meshMeasurementTable.addValue("Frame", frame);
        }

        processed_frame = frame;

        long timestamp = System.currentTimeMillis();

        CLIJxUtils.clijx = clijx;

        // IJ.run("Close All");
        clijx.stopWatch("");

        System.out.println("Trying to read " + dataSet.getPath() + "/.../" + filename);
        ImagePlus timePointStack = dataSet.getImageData(frame);

        //System.out.println(foldername + filename)
        // # break;

        // -------------------------------------------------------------------------------------------------------------
        // loading and resampling
        String key_resampled = "VOL_01_RESAMPLED_INPUT";

        ClearCLImage resampledImage = (ClearCLImage) resultImages.get(key_resampled);
        if (resampledImage == null) {

            ClearCLBuffer pushedImage = clijx.push(timePointStack);
            if (transposeXY) {
                pushedImage = transposeImageXY(pushedImage);
            }
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


            long w = (long) (pushedImage.getWidth() * factorX);
            long h = (long) (pushedImage.getHeight() * factorY);
            long d = (long) (pushedImage.getDepth() * factorZ);

            System.out.println(new long[]{w, h, d});

            long[] size = new long[]{w, h, d};
            //size = padSize(size);
            resampledImage = clijx.create(size, CLIJUtilities.nativeToChannelType(pushedImage.getNativeType()));
            resampledImage.setName("resampledImage");

            System.out.println("PushedImage: " + pushedImage);

            clijx.resample(pushedImage, resampledImage, 1.0 / factorX, 1.0 / factorY, 1.0 / factorZ, true);

            clijx.stopWatch("resample");
            resultImages.put(key_resampled, resampledImage);



            clijx.release(pushedImage);
        }

        //////////////////////////////////////////////////////
        // if we have what we were looking for, leave       //
        if (resultImages.containsKey(requested_result)) {   //
            return;                                         //
        }                                                   //
        //////////////////////////////////////////////////////

        // -------------------------------------------------------------------------------------------------------------
        // background subtraction (optional)
        String key_background_subtracted = "VOL_02_BACKGROUND_SUBTRACTED";
        ClearCLImage backgroundSubtractedImage = (ClearCLImage) resultImages.get(key_background_subtracted);
        if (backgroundSubtractedImage == null) {
            backgroundSubtractedImage = clijx.create(resampledImage);
            if (backgroundBlurSigma > 0) {
                clijx.subtractBackground3D(resampledImage, backgroundSubtractedImage, backgroundBlurSigma, backgroundBlurSigma, backgroundBlurSigma);
                resultImages.put(key_background_subtracted, resampledImage);
            } else {
                backgroundSubtractedImage = resampledImage;
            }
        }
        clijx.stopWatch("background subtraction");

        //////////////////////////////////////////////////////
        // if we have what we were looking for, leave       //
        if (resultImages.containsKey(requested_result)) {   //
            return;                                         //
        }                                                   //
        //////////////////////////////////////////////////////

        // -------------------------------------------------------------------------------------------------------------
        // affine transform
        String key_transformed = "VOL_03_TRANSFORMED_INPUT";
        ClearCLBuffer inputImage = (ClearCLBuffer) resultImages.get(key_transformed);
        if (inputImage == null) {
            inputImage = clijx.create(resampledImage.getDimensions(), resampledImage.getNativeType());

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
            resultImages.put(key_transformed, inputImage);

            if (projection_visualisation_on_screen || projection_visualisation_to_disc || storeProjections) {

                // save maximum and average projections to disc
                ClearCLBuffer[] result = arg_max_projection(inputImage);
                resultImages.put("01_max_image", result[0]);
                resultImages.put("02_arg_max_image", result[1]);
            }
        }

        System.out.println("SEARCHING FOR " + requested_result);
        //////////////////////////////////////////////////////
        // if we have what we were looking for, leave       //
        if (resultImages.containsKey(requested_result)) {   //
            return;                                         //
        }                                                   //
        //////////////////////////////////////////////////////

        //clijx.release(backgroundSubtractedImage);

        clijx.stopWatch("affine transform");

        //clijx.show(inputImage, "inputImage");
        //new WaitForUserDialog("hhhh").show();
        //if (single_stack_visualisation) {
        //    ImagePlus imp_inputImage = clijx.pull(inputImage);
        //}
        // clijx.saveAsTIF( inputImage,                outputFolder + "_input/" + filename + ".tif");

        // -------------------------------------------------------------------------------------------------------------
        // spot detection
        String key_det_spots = "VOL_04_DETECTED_SPOTS";
        ClearCLBuffer detected_spots = (ClearCLBuffer) resultImages.get(key_det_spots);
        if (detected_spots == null) {
            detected_spots = spot_detection(inputImage);

            //clijx.show(detected_spots, "detected_spots");
            //if (true) return;
            clijx.stopWatch("spot detection");
            resultImages.put(key_det_spots, detected_spots);

            if (projection_visualisation_on_screen || projection_visualisation_to_disc || storeProjections) {

                resultImages.put("03_max_spots", max_z_projection(detected_spots));
            }

        }


        //////////////////////////////////////////////////////
        // if we have what we were looking for, leave       //
        if (resultImages.containsKey(requested_result)) {   //
            return;                                         //
        }                                                   //
        //////////////////////////////////////////////////////

        // -------------------------------------------------------------------------------------------------------------
        // Spot labelling, cell segmentation, ray tracing
        boolean hasToBeRegenerated = false;
        // label spots
        String key_spots = "VOL_05_LABELLED_SPOTS";

        ClearCLBuffer labelled_spots = (ClearCLBuffer) resultImages.get(key_spots);

        if (labelled_spots == null) {
            labelled_spots = labelSpots(detected_spots);
            clijx.stopWatch("spot labelling (CCA)");
            hasToBeRegenerated = true;
        }

        String key_cells = "VOL_06_LABELLED_CELLS";

        // cell segmentation
        ClearCLBuffer segmented_cells = (ClearCLBuffer) resultImages.get(key_cells);

        if (segmented_cells == null) {
            segmented_cells = pseudo_cell_segmentation(labelled_spots);
            clijx.stopWatch("cell segmentation");
            hasToBeRegenerated = true;
        }


        if (eliminateSubSurfaceCells && hasToBeRegenerated) {

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
        if (eliminateOnSurfaceCells && hasToBeRegenerated) {

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
        resultImages.put(key_spots, labelled_spots);
        resultImages.put(key_cells, segmented_cells);

        {
            ClearCLBuffer[] result = arg_max_projection(segmented_cells);
            resultImages.put("07_max_labelled_cells", result[0]);
            resultImages.put("08_arg_max_labelled_cells", result[1]);
        }

        //////////////////////////////////////////////////////
        // if we have what we were looking for, leave       //
        if (resultImages.containsKey(requested_result)) {   //
            return;                                         //
        }                                                   //
        //////////////////////////////////////////////////////

        // -------------------------------------------------------------------------------------------------------------
        // Statistics of labelled objects
        System.out.println("HELLO WORLD");
        if (storeMeasurements) {
            System.out.println("YES");

            ResultsTable table = new ResultsTable();
            clijx.statisticsOfBackgroundAndLabelledPixels(inputImage, segmented_cells, table);


            String[] columnsToKeep = new String[]{

                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.MEAN_INTENSITY.toString(),

                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.STANDARD_DEVIATION_INTENSITY.toString(),

                    //StatisticsOfLabelledPixels.STATISTICS_ENTRY.BOUNDING_BOX_WIDTH.toString(),
                    //StatisticsOfLabelledPixels.STATISTICS_ENTRY.BOUNDING_BOX_HEIGHT.toString(),
                    //StatisticsOfLabelledPixels.STATISTICS_ENTRY.BOUNDING_BOX_DEPTH.toString(),
                    //StatisticsOfLabelledPixels.STATISTICS_ENTRY.MASS_CENTER_X.toString(),
                    //StatisticsOfLabelledPixels.STATISTICS_ENTRY.MASS_CENTER_Y.toString(),
                    //StatisticsOfLabelledPixels.STATISTICS_ENTRY.MASS_CENTER_Z.toString(),
                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.PIXEL_COUNT.toString(),
                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.MINIMUM_INTENSITY.toString(),
                    StatisticsOfLabelledPixels.STATISTICS_ENTRY.MAXIMUM_INTENSITY.toString()
            };

            if (table.size() > 0) {
                System.out.println("YES2");

                for (String key : columnsToKeep) {
                    System.out.println("YES3 " + key);

                    storeMeasurement(key, table.getColumn(table.getColumnIndex(key)));
                }
            } else {
                for (String key : columnsToKeep) {
                    System.out.println("YES3 " + key);
                    storeMeasurement(key, new float[1]);
                }
            }

        }
        clijx.stopWatch("measurements");

        //ClearCLBuffer max_membranes = null;
        //ClearCLBuffer mean_membranes = null;
        //ClearCLBuffer nonzero_min_membranes = null;

        // -------------------------------------------------------------------------------------------------------------
        // cell outlines
        if (projection_visualisation_on_screen || projection_visualisation_to_disc || storeProjections) {
            // cell outlines
            String key_cell_outlines = "VOL_07_CELL_OUTLINES";
            ClearCLBuffer cell_outlines = (ClearCLBuffer) resultImages.get(key_cell_outlines);
            if (cell_outlines == null) {
                cell_outlines = label_outlines(segmented_cells);
                resultImages.put(key_cell_outlines, cell_outlines);
                clijx.stopWatch("outline labels");
            }
            resultImages.put("04_max_membranes", max_z_projection(cell_outlines));
            resultImages.put("05_mean_membranes", mean_projection(cell_outlines));
            resultImages.put("06_nonzero_min_membranes", nonzero_min_projection(cell_outlines));

            //clijx.release(cell_outlines);
        }


        //////////////////////////////////////////////////////
        // if we have what we were looking for, leave       //
        if (resultImages.containsKey(requested_result)) {   //
            return;                                         //
        }                                                   //
        //////////////////////////////////////////////////////

        // -------------------------------------------------------------------------------------------------------------
        // touching neighbors and distances

        // -----------------------------------------------------------------------
        // convert spots image to spot list
        int number_of_spots = (int) clijx.sumPixels(detected_spots);
        if (meshMeasurementTable != null) {
            meshMeasurementTable.addValue("spot_count", number_of_spots);
        }

        this.spot_count = number_of_spots;

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
        System.out.println("Pointlist " + pointlist);
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

        System.out.println("Touch matrix: " + touch_matrix);
        System.out.println("Distance matrix: " + distance_matrix);

        ClearCLBuffer distance_vector = measureAverageDistanceOfTouchingNeighbors(touch_matrix, distance_matrix);
        clijx.stopWatch("average distance");
        //ClearCLBuffer surface_angle_vector = measureAverageSurfaceAngle(pointlist, touch_matrix);
        //clijx.stopWatch("average angle");
        ClearCLBuffer neighbor_count_vector = countNeighbors(touch_matrix);
        //

        //ClearCLBuffer max_avg_surf_ang = null;
        //ClearCLBuffer mean_avg_surf_ang = null;
        //ClearCLBuffer nonzero_min_avg_surf_ang = null;
        {
            //ClearCLBuffer average_surface_angle = generateParametricImage(surface_angle_vector, segmented_cells);
            //storeMeasurement("surface_angle", surface_angle_vector);
//            if (projection_visualisation_on_screen || projection_visualisation_to_disc) {
//                resultImages.put("22_max_avg_surf_ang", max_z_projection(average_surface_angle));
//                resultImages.put("23_mean_avg_surf_ang", mean_projection(average_surface_angle));
//                resultImages.put("24_nonzero_min_avg_surf_ang", nonzero_min_projection(average_surface_angle));
//            }
//            clijx.release(average_surface_angle);
        }

        //ClearCLBuffer max_neigh_touch = null;
        //ClearCLBuffer mean_neigh_touch = null;
        //ClearCLBuffer nonzero_min_neigh_touch = null;

        // -------------------------------------------------------------------------------------------------------------
        // count neighbors
        {
            String key_neighbor_count = "VOL_08_NEIGHBOR_COUNT";
            ClearCLBuffer neighbor_count = (ClearCLBuffer) resultImages.get(key_neighbor_count);
            if (neighbor_count == null) {
                neighbor_count = generateParametricImage(neighbor_count_vector, segmented_cells);
                if (requested_result.compareTo(key_neighbor_count) == 0) {
                    resultImages.put(key_neighbor_count, neighbor_count);
                }
            }
            storeMeasurement("neighbor_count", neighbor_count_vector);
            ClearCLBuffer averageNeighborCountOfNeighboringNeighbors1 = meanNeighbours(neighbor_count_vector, touch_matrix);
            storeMeasurement("average_neighbor_count_of_neighbors1", averageNeighborCountOfNeighboringNeighbors1);

            ClearCLBuffer averageNeighborCountOfNeighboringNeighbors2 = meanNeighbours(averageNeighborCountOfNeighboringNeighbors1, touch_matrix);
            storeMeasurement("average_neighbor_count_of_neighbors2", averageNeighborCountOfNeighboringNeighbors2);

            ClearCLBuffer averageNeighborCountOfNeighboringNeighbors3 = meanNeighbours(averageNeighborCountOfNeighboringNeighbors2, touch_matrix);
            storeMeasurement("average_neighbor_count_of_neighbors3", averageNeighborCountOfNeighboringNeighbors3);







            if (projection_visualisation_on_screen || projection_visualisation_to_disc) {
                resultImages.put("19_max_neigh_touch", max_z_projection(neighbor_count));
                resultImages.put("20_mean_neigh_touch", mean_projection(neighbor_count));
                resultImages.put("21_nonzero_min_neigh_touch", nonzero_min_projection(neighbor_count));
            }
            // don't keep it if we didn't ask for it
            if (requested_result.compareTo(key_neighbor_count) != 0) {
                clijx.release(neighbor_count);
            }
        }

        //////////////////////////////////////////////////////
        // if we have what we were looking for, leave       //
        if (resultImages.containsKey(requested_result)) {   //
            return;                                         //
        }                                                   //
        //////////////////////////////////////////////////////

        //clijx.show(averag_distance_of_touching_neighbors, "averag_distance_of_touching_neighbors");

        //clijx.showGrey(touch_matrix, "touch_matrix");
        //clijx.showGrey(distance_matrix, "distance_matrix");
        clijx.stopWatch("touch map");
        ClearCLBuffer relevantDistances = clijx.create(distance_matrix.getDimensions(), clijx.Float);
        clijx.multiplyImages(distance_matrix, touch_matrix, relevantDistances);

        System.out.println("Dist mat " + Arrays.toString(distance_matrix.getDimensions()));
        //System.out.println(clijx.reportMemory());
        double numberOfTouches = clijx.sumOfAllPixels(touch_matrix);
        if (meshMeasurementTable != null) {
            double meanDistance = clijx.meanOfPixelsAboveThreshold(relevantDistances, 0);
            double varianceDistance = clijx.varianceOfMaskedPixels(relevantDistances, touch_matrix, meanDistance);
            meshMeasurementTable.addValue("mean_neighbor_distance", meanDistance);
            meshMeasurementTable.addValue("variance_neighbor_distance", varianceDistance);
            meshMeasurementTable.addValue("number_of_touches", numberOfTouches);
        }
        relevantDistances.close();


        // -------------------------------------------------------------------------------------------------------------
        String key_avg_neighbor_distance = "VOL_08_AVG_DISTANCE_TO_NEIGHBORS";

        ClearCLBuffer average_distance_of_touching_neighbors = (ClearCLBuffer) resultImages.get(key_avg_neighbor_distance);
        if (average_distance_of_touching_neighbors == null) {
            average_distance_of_touching_neighbors = generateParametricImage(distance_vector, segmented_cells);
            storeMeasurement("average_distance", distance_vector);

            ClearCLBuffer averageDistanceOfNeihboringNeighbors1 = meanNeighbours(distance_vector, touch_matrix);
            storeMeasurement("average_distance_of_neighbors1", averageDistanceOfNeihboringNeighbors1);

            ClearCLBuffer averageDistanceOfNeihboringNeighbors2 = meanNeighbours(averageDistanceOfNeihboringNeighbors1, touch_matrix);
            storeMeasurement("average_distance_of_neighbors2", averageDistanceOfNeihboringNeighbors2);

            ClearCLBuffer averageDistanceOfNeihboringNeighbors3 = meanNeighbours(averageDistanceOfNeihboringNeighbors2, touch_matrix);
            storeMeasurement("average_distance_of_neighbors3", averageDistanceOfNeihboringNeighbors3);


        }
        resultImages.put(key_avg_neighbor_distance, average_distance_of_touching_neighbors);


        if (meshMeasurementTable != null) {
            double meanDistance2 = clijx.meanOfMaskedPixels(average_distance_of_touching_neighbors, detected_spots);
            double varianceDistance2 = clijx.varianceOfMaskedPixels(average_distance_of_touching_neighbors, detected_spots, meanDistance2);
            meshMeasurementTable.addValue("mean_neighbor_distance2", meanDistance2);
            meshMeasurementTable.addValue("variance_neighbor_distance2", varianceDistance2);
        }

        //ClearCLBuffer max_avg_dist = null;
        //ClearCLBuffer mean_avg_dist = null;
        //ClearCLBuffer nonzero_min_avg_dist = null;
        if (projection_visualisation_on_screen || projection_visualisation_to_disc || storeProjections) {
            resultImages.put("16_max_avg_dist", max_z_projection(average_distance_of_touching_neighbors));
            resultImages.put("17_mean_avg_dist", mean_projection(average_distance_of_touching_neighbors));
            resultImages.put("18_nonzero_min_avg_dist", nonzero_min_projection(average_distance_of_touching_neighbors));
        }
        //clijx.release(average_distance_of_touching_neighbors);

        //////////////////////////////////////////////////////
        // if we have what we were looking for, leave       //
        if (resultImages.containsKey(requested_result)) {   //
            return;                                         //
        }                                                   //
        //////////////////////////////////////////////////////

        // -------------------------------------------------------------------------------------------------------------

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

        //////////////////////////////////////////////////////
        // if we have what we were looking for, leave       //
        if (resultImages.containsKey(requested_result)) {   //
            return;                                         //
        }                                                   //
        //////////////////////////////////////////////////////

        // -----------------------------------------------------------------------
        // Visualisation / output
        if (exportMesh) {
            if (number_of_spots > 1 && numberOfTouches > 0) {
                clijx.stopWatch("export vtk");
                if (outputFolder != null) {
                    clijx.writeVTKLineListToDisc(pointlist, touch_matrix, outputFolder + "_vtk_mesh/" + filename.replace(".raw", "") + ".vtk");
                }
                //clijx.writeXYZPointListToDisc(pointlist, outputFolder + "_vtk_mesh/" + filename.replace(".raw", "") + ".xyz");
                clijx.stopWatch("export vtk");
            }
        }

        // -------------------------------------------------------------------------------------------------------------
        // measurements
        String key_measurements = "07_measurements";
        if (storeMeasurements) {
            ClearCLBuffer measurementsImage = (ClearCLBuffer) resultImages.get(key_measurements);
            if (measurementsImage == null) {
                ResultsTable table = getAllMeasurements();
                System.out.println("Measurements: " + Arrays.toString(table.getHeadings()));
                measurementsImage = clijx.create(table.getHeadings().length, table.size());
                clijx.resultsTableToImage2D(measurementsImage, table);
                if (outputFolder != null) {
                    clijx.saveAsTIF(measurementsImage, outputFolder + "_measurements/" + filename + ".tif");
                    table.save(outputFolder + "_measurements/" + filename + ".csv");
                }
                resultImages.put(key_measurements, measurementsImage);
            }
            //measurementsImage.close();
        }

        // -------------------------------------------------------------------------------------------------------------
        // classification
        String key_label_classification = "VOL_02_label_classification";

        ClearCLBuffer classification = null;
        ClearCLBuffer classification_auto_context = null;

        ClearCLBuffer label_classification = (ClearCLBuffer) resultImages.get(key_label_classification);
        if (label_classification == null) {
            if (clijxweka != null && storeMeasurements) {
                System.out.println("PREDICTING");

//                ClearCLBuffer measurementsImage = (ClearCLBuffer) resultImages.get(key_measurements);
  //              ResultsTable table = new ResultsTable();
       //         clijx.image2DToResultsTable(measurementsImage, table);
                ResultsTable table = getAllMeasurements();

                ApplyWekaToTable.applyWekaToTable(clijx, table, "CLASS", clijxweka);
                float[] classes = table.getColumn(table.getColumnIndex("CLASS"));
                storeMeasurement("classes", classes);

                classification = clijx.push(new Float1(classes));
                //clijx.print(classification);
                label_classification = generateParametricImage(classification, segmented_cells);
                //clijx.show(label_classification, "label_classification");

                ClearCLBuffer labels_max = (ClearCLBuffer) resultImages.get("07_max_labelled_cells");
                if (labels_max != null) {
                    ClearCLBuffer labels_max_classification = generateParametricImage(classification, labels_max);
                    resultImages.put("07_max_labelled_cells_classification", labels_max_classification);
                } else {
                    resultImages.put("07_max_labelled_cells_classification", max_z_projection(label_classification));
                }

                if (autoContextClassification) {

                    ClearCLBuffer popularClassificationOfNeighbors = clijx.create(classification);
                    MostPopularValueOfTouchingNeighbors.mostPopularValueOfTouchingNeighbors(clijx, classification, touch_matrix, popularClassificationOfNeighbors);

                    // store intermediate results
                    storeMeasurement("popular_neighbor_classes", popularClassificationOfNeighbors);
                    resultImages.put("07_max_popular_labels_among_neighbors", generateParametricImage(popularClassificationOfNeighbors, labels_max));

                    if (clijxwekaAutoContext != null) {
                        String key_classes_autocontext = "07_classification_autocontext";
                        ClearCLBuffer label_classification_auto_context = (ClearCLBuffer) resultImages.get(key_classes_autocontext);
                        if (label_classification_auto_context == null) {

                            ResultsTable table2 = getAllMeasurements();
                            ApplyWekaToTable.applyWekaToTable(clijx, table2, "CLASS", clijxwekaAutoContext);
                            float[] classesAutoContext = table2.getColumn(table2.getColumnIndex("CLASS"));
                            storeMeasurement("classes_auto_context", classesAutoContext);

                            classification_auto_context = clijx.push(new Float1(classesAutoContext));
                            //clijx.print(classification);
                            label_classification_auto_context = generateParametricImage(classification_auto_context, segmented_cells);
                            resultImages.put(key_classes_autocontext, label_classification_auto_context);
                            //clijx.show(label_classification, "label_classification");

                            if (labels_max != null) {
                                ClearCLBuffer labels_max_classification = generateParametricImage(classification_auto_context, labels_max);
                                resultImages.put("07_max_labelled_cells_classification_autocontext", labels_max_classification);
                            } else {
                                resultImages.put("07_max_labelled_cells_classification_autocontext", max_z_projection(label_classification));
                            }
                        }
                    }
                }
            } else {
                label_classification = clijx.create(segmented_cells.getDimensions(), clijx.UnsignedByte);
                clijx.set(label_classification, 0);
                resultImages.put("07_max_labelled_cells_classification", max_z_projection(label_classification));
                if (autoContextClassification) {
                    resultImages.put("07_max_popular_labels_among_neighbors", max_z_projection(label_classification));
                    resultImages.put("07_max_labelled_cells_classification_autocontext", max_z_projection(label_classification));
                }
            }
            resultImages.put(key_label_classification, label_classification);
            resultImages.put("02_nonzero_min_label_classification", nonzero_min_projection(label_classification));
            resultImages.put("02_mean_label_classification", mean_projection(label_classification));
            resultImages.put("02_max_label_classification", max_z_projection(label_classification));
        }
        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------
        // draw mesh
        String key_mesh = "VOL_12_MESH";
        ClearCLBuffer mesh = (ClearCLBuffer) resultImages.get(key_mesh);
        if (mesh == null) {
            mesh = clijx.create(inputImage);
            mesh.setName("mesh");

            if (classification_auto_context != null && autoContextClassification) {
                ClearCLBuffer classifiedTouchMatrix = clijx.create(touch_matrix);
                GenerateClassifiedTouchMatrix.generateClassifiedTouchMatrix(clijx, segmented_cells, classification_auto_context, classifiedTouchMatrix);
                clijx.touchMatrixToMesh(pointlist, classifiedTouchMatrix, mesh);
                classifiedTouchMatrix.close();
            } else if (classification != null) {
                ClearCLBuffer classifiedTouchMatrix = clijx.create(touch_matrix);
                GenerateClassifiedTouchMatrix.generateClassifiedTouchMatrix(clijx, segmented_cells, classification, classifiedTouchMatrix);
                clijx.touchMatrixToMesh(pointlist, classifiedTouchMatrix, mesh);
                classifiedTouchMatrix.close();
            } else {
                clijx.touchMatrixToMesh(pointlist, touch_matrix, mesh);
            }
            resultImages.put(key_mesh, mesh);
            clijx.stopWatch("mesh");

            // distance mesh
            ClearCLBuffer distanceMesh = clijx.create(mesh);
            ClearCLBuffer touch_distance_matrix = clijx.create(touch_matrix);
            clijx.multiplyImages(touch_matrix, distance_matrix, touch_distance_matrix);
            clijx.touchMatrixToMesh(pointlist, touch_distance_matrix, distanceMesh);
            touch_distance_matrix.close();
            resultImages.put("VOL_12_DISTANCE_MESH", distanceMesh);
            resultImages.put("12_max_DISTANCE_MESH", max_z_projection(distanceMesh));
        }
        clijx.release(classification_auto_context);
        clijx.release(classification);

        //////////////////////////////////////////////////////
        // if we have what we were looking for, leave       //
        if (resultImages.containsKey(requested_result)) {   //
            return;                                         //
        }                                                   //
        //////////////////////////////////////////////////////

        if (projection_visualisation_on_screen || projection_visualisation_to_disc || storeProjections) {

            resultImages.put("09_pixel1", clijx.create(1,1,1));

            resultImages.put("10_max_mesh_x", max_x_projection(mesh));
            resultImages.put("11_max_mesh_y", max_y_projection(mesh));
            resultImages.put("12_max_mesh_z", max_z_projection(mesh));


            List<String> resultKeys = new ArrayList<String>(resultImages.keySet());

            Collections.sort(resultKeys);

            if (projection_visualisation_to_disc) {
                for (String key : resultKeys) {
                    String saveKey = key; //.substring(2);
                    ClearCLImageInterface buffer1 = resultImages.get(key);
                    if (buffer1 instanceof ClearCLBuffer && buffer1.getDepth() == 1) {
                        ClearCLBuffer buffer = (ClearCLBuffer) buffer1;
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
                        if (outputFolder != null) {
                            clijx.saveAsTIF(buffer, outputFolder + saveKey + "/" + filename + ".tif");
                        }
                    }
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
                    ClearCLImageInterface buffer1 = resultImages.get(key);
                    if (buffer1 instanceof ClearCLBuffer && buffer1.getDepth() == 1) {
                        clijx.showGrey((ClearCLBuffer) buffer1, saveKey);
                    }
                }

                clijx.organiseWindows(0, 0, 8, 3, 200, 350);
                //clijx.organiseWindows(500, -1300, 5, 3, 630, 420);

            }


            clijx.stopWatch("visualisation");
        }
        clijx.release(pointlist);

        System.out.println("Whole analysis took " + (System.currentTimeMillis() - timestamp) + " ms");

        //IJ.log(clijx.reportMemory());
        //break;

        if (show_table_visualisation_on_screen && meshMeasurementTable != null) {
            meshMeasurementTable.show("Mesh measurements results");
        }
        if (projection_visualisation_to_disc && meshMeasurementTable != null) {
            dataSet.saveMeasurementTable(meshMeasurementTable, "processed/meshMeasurements.csv");
        }
    }

    private ClearCLBuffer meanNeighbours(ClearCLBuffer distance_vector, ClearCLBuffer touch_matrix) {
        ClearCLBuffer result = clijx.create(distance_vector);
        clijx.meanOfTouchingNeighbors(distance_vector, touch_matrix, result);
        return result;
    }

    private ClearCLBuffer transposeImageXY(ClearCLBuffer pushedImage) {
        ClearCLBuffer result = clijx.create(new long[]{pushedImage.getHeight(), pushedImage.getWidth(), pushedImage.getDepth()}, pushedImage.getNativeType());
        clijx.transposeXY(pushedImage, result);
        pushedImage.close();
        return result;
    }

    CLIJxWeka2 clijxweka = null;
    CLIJxWeka2 clijxwekaAutoContext = null;
    public void train(float[] ground_truth) {

        {
            ResultsTable table = getAllMeasurements();
            System.out.println("Use for training: " + Arrays.toString(table.getHeadings()));
            for (int i = 0; i < table.size(); i++) {
                table.setValue("CLASS", i, ground_truth[i]);
            }
            //table.show("TRAINING");
            clijxweka = TrainWekaFromTable.trainWekaFromTable(clijx, table, "CLASS", 200, 2, 3);
        }

        storeMeasurements = true;
        if (autoContextClassification) {
            int frame = processed_frame;
            invalidate();
            processFrameForRequestedResult(null, null, frame, "");

            //float[] prediced_classes = getMeasurement("classes");
            //float[] popular_neighbor_classes = getMeasurement("popular_neighbor_classes");


            ResultsTable table2 = getAllMeasurements();
            System.out.println("Use for autocontext training: " + Arrays.toString(table2.getHeadings()));
            for (int i = 0; i < table2.size(); i++) {
              //  table.setValue("prediced_classes", i, ground_truth[i]);
                //table.setValue("popular_neighbor_classes", i, ground_truth[i]);
                table2.setValue("CLASS", i, ground_truth[i]);
            }
            //table2.show("TRAINING Autocontext");
            clijxwekaAutoContext = TrainWekaFromTable.trainWekaFromTable(clijx, table2, "CLASS", 200, 2, 3);
        }
    }

    public void invalidateTraining(){
        clijxweka = null;
        clijxwekaAutoContext = null;
        invalidate();
    }

    public boolean isTrained() {
        return clijxweka != null;
    }

    public int getProcessedFrame() {
        return processed_frame;
    }


    /*private long[] padSize(long[] size) {
        long[] result = new long[size.length];
        for (int d = 0; d < result.length; d++) {
            if (size[d] % 4 == 0) {
                result[d] = size[d];
            } else {
                result[d] = size[d] + (4 - (size[d] % 4));
            }
        }
        return result;
    }*/

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
        //long time = System.currentTimeMillis();
        clijx.blur(inputImage, blurred, blurSigma, blurSigma, blurSigma);
        //System.out.println("blur: " + (System.currentTimeMillis() - time));
        //}
        //clijx.show(blurred, "blurred");
        // ----------------------------------------------------------------------
        // spot detection
        clijx.detectMaximaBox(blurred, detected_spots, 1);

        // remove spots in background
        //clijx.automaticThreshold(blurred, thresholded, thresholdAlgorithm);
        clijx.stopWatch("before threshold");
        CLIJ.debug = true;
        clijx.threshold(blurred, thresholded, threshold);
        CLIJ.debug = false;
        clijx.stopWatch("after threshold");
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

        clijx.stopWatch("cell segmentation start");

        ClearCLBuffer tempSpots1 = clijx.create(labelled_spots);
    	tempSpots1.setName("Segmented cells");
    	clijx.copy(labelled_spots, tempSpots1);

        ClearCLBuffer tempSpots2 = clijx.create(labelled_spots);
	    tempSpots2.setName("tempSpots2");


        ClearCLBuffer tempSpots3 = clijx.create(labelled_spots);
        tempSpots3.setName("tempSpots3");

        ClearCLBuffer flag =clijx.create(new long[]{1, 1, 1});
	    flag.setName("flag");

        clijx.stopWatch("cell segmentation alloc");

        ClearCLKernel diamondKernel = null;
        ClearCLKernel boxKernel = null;
        for (int j = 0; j < numberDoubleDilationsForPseudoCellSegmentation; j++) {
            diamondKernel = OnlyzeroOverwriteMaximumDiamond.onlyzeroOverwriteMaximumDiamond(clijx, tempSpots1, flag, tempSpots2, diamondKernel);
            boxKernel = OnlyzeroOverwriteMaximumBox.onlyzeroOverwriteMaximumBox(clijx, tempSpots2, flag, tempSpots1, boxKernel);
        }
        if (diamondKernel != null) {
            diamondKernel.close();
        }
        if (boxKernel != null) {
            boxKernel.close();
        }

        clijx.stopWatch("cell segmentation box diamond");


        clijx.release(flag);

        CLIJ.debug = true;
/*
        String openCL = "float value = READ_IMAGE(input, sampler, POS_input_INSTANCE(x, y, z, 0)).x;" +
                        "if (value > 0) {value = 1;}" +
                        "WRITE_IMAGE(output, POS_output_INSTANCE(x, y, z, 0), CONVERT_output_PIXEL_TYPE(value));";

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("input", tempSpots1);
        map.put("output", tempSpots2);
	    clijx.customOperation(openCL, "", map);*/
        clijx.threshold(tempSpots1,tempSpots2,1);
        CLIJ.debug = false;

        clijx.stopWatch("cell segmentation threshold");


        for (int j = 0; j < numberDoubleErosionsForPseudoCellSegmentation; j++) {
            clijx.erodeBox(tempSpots2, tempSpots3);
            clijx.erodeBox(tempSpots3, tempSpots2);
        }

        clijx.stopWatch("cell segmentation erode");


        clijx.erodeBox(tempSpots2,tempSpots3);
	    clijx.copy(tempSpots1,tempSpots2);
	    clijx.mask(tempSpots2,tempSpots3,tempSpots1);
	    clijx.release(tempSpots3);

    	// clijx.show(tempSpots1,"tempSpots1");
	    clijx.release(tempSpots2);

        clijx.stopWatch("cell segmentation rest");

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

            int startFrame = 0;
            //int endFrame = startFrame + 20;

            new MeshMeasurements(dataSet).
                    //setCLIJx(CLIJx.getInstance("2070")).
                    setProjectionVisualisationToDisc(false).
                    setProjectionVisualisationOnScreen(true).
                    setExportMesh(false).
                    setThreshold(300).
                    setStoreMeasurements(true).
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
                    //setLastFrame(endFrame).
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

    public String getHumanReadableTime(int frame) {
        Duration duration = Duration.ofSeconds((int)dataSet.getTimesInSeconds()
                [frame]); // in milliseconds
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }

    public String getDataSetName() {
        return dataSet.getShortName() + " " + dataSet.getName();
    }
}
