package net.haesleinhuepf.imagej.zoo.measurement;

import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.filter.MaximumFinder;
import ij.process.ByteProcessor;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.imagej.zoo.ZooUtilities;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;
import org.scijava.util.VersionUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HalfCylinderMaxProjectionMeasurements extends DataSetMeasurements{

    CLIJx clijx;

    int numberOfAngles = 360;

    double threshold = 7;
    double noiseThreshold = 50;

    double center_x = 0.5;
    double center_z = 0.0;

    boolean saveProjectionResults = true;
    boolean showProjectionsOnScreen = true;
    private double backgroundSubtractionSigmaXY = 15;
    private double backgroundSubtractionSigmaZ = 0;

    public HalfCylinderMaxProjectionMeasurements(ClearControlDataSet dataSet){
        super(dataSet);
        clijx = CLIJx.getInstance();
    }

    public HalfCylinderMaxProjectionMeasurements setNoiseThreshold(double noiseThreshold) {
        this.noiseThreshold = noiseThreshold;
        return this;
    }

    public HalfCylinderMaxProjectionMeasurements setNumberOfAngles(int numberOfAngles) {
        this.numberOfAngles = numberOfAngles;
        return this;
    }

    public HalfCylinderMaxProjectionMeasurements setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }

    public HalfCylinderMaxProjectionMeasurements setSaveProjectionResults(boolean saveProjectionResults) {
        this.saveProjectionResults = saveProjectionResults;
        return this;
    }

    public HalfCylinderMaxProjectionMeasurements setShowProjectionsOnScreen(boolean showProjectionsOnScreen) {
        this.showProjectionsOnScreen = showProjectionsOnScreen;
        return this;
    }

    private ClearCLBuffer halfStackCylinderMaximumProjection(ClearCLBuffer input) {

        long width = input.getWidth();
        long height = input.getHeight();
        long depth = input.getDepth();

        double pixel_size_x = dataSet.getVoxelSizeX(0);
        double pixel_size_y = dataSet.getVoxelSizeY(0);
        double pixel_size_z = dataSet.getVoxelSizeZ(0);

        //System.out.println("px: " + pixel_size_x);
        //System.out.println("py: " + pixel_size_y);
        //System.out.println("pz: " + pixel_size_z);

        long radius = width;

        double deltaAngle = 180.0 / numberOfAngles;

        ClearCLBuffer reslicedFromTop = clijx.create(new long[]{input.getWidth(), input.getDepth(), input.getHeight()}, input.getNativeType());
        ClearCLBuffer halfCylinderProjection = clijx.create(new long[]{(long)(radius), input.getHeight(), (long)(180.0 / deltaAngle)}, input.getNativeType());
        ClearCLBuffer projected = clijx.create(new long[]{halfCylinderProjection.getDepth(), halfCylinderProjection.getHeight()}, input.getNativeType());
        //ClearCLBuffer projectedStack = clijx.create(new long[]{halfCylinderProjection.getDepth(), halfCylinderProjection.getHeight()}, input.getNativeType());

        clijx.resliceTop(input, reslicedFromTop);
        clijx.resliceRadial(reslicedFromTop, halfCylinderProjection, deltaAngle, -90, center_x * input.getWidth(), center_z * input.getDepth(), 1.0 / pixel_size_x, 1.0 / pixel_size_z);
        clijx.maximumXProjection(halfCylinderProjection, projected);
        //clijx.copySlice(projected, projectedStack, 0);

        //ImagePlus resultStack = clijx.convert(projectedStack, ImagePlus.class);

        //System.out.println("\n\n\n\n\n\n\n\n\n");
        //System.out.println("width " + resultStack.getWidth());
        //System.out.println("height " + resultStack.getHeight());
        //System.out.println("depth " + resultStack.getNSlices());

        //clijx.release(input);
        clijx.release(reslicedFromTop);
        //clijx.release(projected);
        //clijx.release(projectedStack);
        clijx.release(halfCylinderProjection);

        return projected;
    }

    private ClearCLBuffer findSpots(ClearCLBuffer input) {
        ImagePlus buffer = clijx.pull(input);
        MaximumFinder mf = new MaximumFinder();
        ByteProcessor bp = mf.findMaxima(buffer.getProcessor(), noiseThreshold, threshold, MaximumFinder.SINGLE_POINTS, true, false);

        ImagePlus result = new ImagePlus("points", bp);
        ClearCLBuffer ijSpotDetection = clijx.push(result);
        ClearCLBuffer spotDetection = clijx.create(ijSpotDetection);

        clijx.greaterConstant(ijSpotDetection, spotDetection, 0);

        clijx.release(ijSpotDetection);

        return spotDetection;
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

                "HalfCylinderMaxProjectionMeasurements version: " + VersionUtils.getVersion(HalfCylinderMaxProjectionMeasurements.class) + "\n" +
                "noiseThreshold: " + noiseThreshold + "\n" +
                "threshold: " + threshold  + "\n" +
                "numberOfAngles: " + numberOfAngles  + "\n" +
                "showProjectionsOnScreen: " + showProjectionsOnScreen  + "\n"+
                "saveProjectionResults: " + saveProjectionResults  + "\n";

        try {
            Files.write(Paths.get(outputFolder + now + "_hscmp_measurement_log.txt"), analysisLog.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        ResultsTable resultsTable = new ResultsTable();

        GenericDialog cancelDialog = new GenericDialog("Analysis running...");
        cancelDialog.addMessage("Close cancel.");
        cancelDialog.setModal(false);
        cancelDialog.show();

        for (int f = firstFrame; f <= lastFrame; f+=frameStep) {
            if (cancelDialog.wasCanceled() || cancelDialog.wasOKed()) {
                break;
            }

            System.out.println("f " + f);

            resultsTable.incrementCounter();
            resultsTable.addValue("Frame", f);

            long timestamp = System.currentTimeMillis();

            // IJ.run("Close All");
            clijx.stopWatch("");

            String filename = "0000000" + f;
            filename = filename.substring(filename.length() - 6) + ".raw";

            ImagePlus timePointStack = dataSet.getImageData(f);
            if (timePointStack == null || timePointStack.getNSlices() < 2) {
                System.out.println("Error reading stack from time point: " + f + ". Ignoring.");
                continue;
            }

            // conversion
            ClearCLBuffer input = clijx.convert(timePointStack, ClearCLBuffer.class);

            // downsampling
            ClearCLBuffer downsampled = clijx.create(new long[]{input.getWidth() / 2, input.getHeight() / 2, input.getDepth()}, input.getNativeType());
            clijx.downsampleSliceBySliceHalfMedian(input, downsampled);

            // background removal
            ClearCLBuffer backgroundSubtracted = clijx.create(downsampled);
            clijx.subtractBackground3D(downsampled, backgroundSubtracted, backgroundSubtractionSigmaXY, backgroundSubtractionSigmaXY, backgroundSubtractionSigmaZ);

            ClearCLBuffer hscmp = halfStackCylinderMaximumProjection(backgroundSubtracted);

            //clijx.show(hscmp, "hscmpp");

            ClearCLBuffer spotDetection = findSpots(hscmp);

            double numberOfSpots = clijx.countNonZeroPixels(spotDetection);
            double meanIntensity = clijx.meanOfAllPixels(hscmp);
            double varianceIntensity = clijx.varianceOfAllPixels(hscmp, meanIntensity);

            double meanSpotIntensity = clijx.meanOfMaskedPixels(hscmp, spotDetection);
            double varianceSpotIntensity = clijx.varianceOfMaskedPixels(hscmp, spotDetection, meanSpotIntensity);

            System.out.println("num spots "+ numberOfSpots);


            resultsTable.addValue("Number of spots", numberOfSpots);
            resultsTable.addValue("Mean intensity", meanIntensity);
            resultsTable.addValue("Variance intensity", varianceIntensity);
            resultsTable.addValue("Mean spot intensity", meanSpotIntensity);
            resultsTable.addValue("Variance spot intensity", varianceSpotIntensity);

            if (showProjectionsOnScreen) {
                clijx.showGrey(hscmp, "Half stack cylinder maximum projection (HSCMP)");
                clijx.showGrey(spotDetection, "HSCMP spot detection");
            }

            if (saveProjectionResults) {
                clijx.saveAsTIF(hscmp, outputFolder + "_hscmp/" + filename + ".tif");
                clijx.saveAsTIF(spotDetection, outputFolder + "_hscmp_spots/" + filename + ".tif");
            }

            clijx.release(input);
            clijx.release(downsampled);
            clijx.release(backgroundSubtracted);
            clijx.release(hscmp);
            clijx.release(spotDetection);

            System.out.println("Whole analysis took " + (System.currentTimeMillis() - timestamp) + " ms");

            dataSet.saveMeasurementTable(resultsTable, "processed/hscmpMeasurements.csv");
        }
        cancelDialog.setVisible(false);
        cancelDialog.dispose();
    }

    public static void main(String[] args) {
        new ImageJ();
        CLIJx.getInstance("2060");

        String sourceFolder = "C:/structure/data/2019-12-17-16-54-37-81-Lund_Tribolium_nGFP_TMR/";
        String datasetFolder = "C0opticsprefused";

        ClearControlDataSet dataSet = ClearControlDataSetOpener.open(sourceFolder, datasetFolder);
        new HalfCylinderMaxProjectionMeasurements(dataSet).run(); //setFirstFrame(1000).setLastFrame(1010).run();
    }

}
