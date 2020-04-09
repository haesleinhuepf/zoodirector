package net.haesleinhuepf.imagej.zoo.measurement;

import ij.ImageJ;
import ij.measure.ResultsTable;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.converters.helptypes.Double1;
import net.haesleinhuepf.clij2.converters.helptypes.Double3;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.weka.CLIJxWeka2;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;

import java.nio.FloatBuffer;


public class WekaTest {
    public static void main(String... args) {

        String sourceFolder = "C:/structure/data/2019-12-17-16-54-37-81-Lund_Tribolium_nGFP_TMR/";
                //"C:/structure/data/2019-10-28-17-22-59-23-Finsterwalde_Tribolium_nGFP/";
        String datasetFolder = "C0opticsprefused";

        ClearControlDataSet dataSet = ClearControlDataSetOpener.open(sourceFolder, datasetFolder);


        CLIJx clijx = CLIJx.getInstance();

        ClearCLBuffer featureStack = makeDefaultFeatureStack(clijx, dataSet);

        double[] truth = dataSet.getPhases();
                //new double[(int)featureStack.getWidth()];

        ClearCLBuffer clTruth_temp = doubleArrayToClearCLBuffer(clijx, truth);
        ClearCLBuffer clTruth = clijx.create(clTruth_temp);
        clijx.closeIndexGapsInLabelMap(clTruth_temp, clTruth);


        printFirst(clijx, clTruth_temp, 50);
        printFirst(clijx, clTruth, 50);
        System.out.println("maxt: " + clijx.maximumOfAllPixels(clTruth_temp));
        System.out.println("max: " + clijx.maximumOfAllPixels(clTruth));

        new ImageJ();
        clijx.show(featureStack, "fs");
        clijx.show(clTruth, "truth");




        CLIJxWeka2 cw = new CLIJxWeka2(clijx, featureStack, clTruth);
        System.out.println("" + cw.getClassifier());
    }

    private static void printFirst(CLIJx clijx, ClearCLBuffer input, int length) {
        ClearCLBuffer temp = clijx.create(new long[]{length, input.getHeight()}, NativeTypeEnum.Float);
        clijx.crop(input, temp, 0, 0);
        clijx.print(temp);
        clijx.release(temp);
    }

    static ClearCLBuffer makeDefaultFeatureStack(CLIJx clijx, ClearControlDataSet dataSet)
    {

        MeasurementTable spotCountTable = dataSet.getMeasurement("spotcount.tsv");
        MeasurementTable focusMeasuresTable = dataSet.getMeasurement("processed/autopilotFocusMeasures_maxProjection.csv");

        for (String column : focusMeasuresTable.getColumnNames()) {
            System.out.println(column);
        }


        ClearCLBuffer numberOfSpots = doubleArrayToClearCLBuffer(clijx, spotCountTable.getColumn("Number of spots"));
        System.out.println(numberOfSpots);

        String[] focusFeatures = {
                "Maximum",
                "Mean",
                "Variance",
                "Lp Sparsity of DCT",
                "Normalized Haar Wavelet Transform Shannon Entropy",
                "Normalized DFT Shannon Entropy",
                "High/low freq. DCT power Ratio",
                "High/low freq. DFT power Ratio"
        };

        int numberOfSigmas = 5;
        int numberOfFeatures = (focusFeatures.length + 1) * numberOfSigmas * 2;
        ClearCLBuffer featureStack = clijx.create(new long[]{numberOfSpots.getWidth(), 1, numberOfFeatures}, NativeTypeEnum.Float);


        int featureCount = 0;
        featureCount = collectFeatures(clijx, numberOfSpots, featureStack, numberOfSigmas, featureCount);
        for (String anotherFeature : focusFeatures) {
            ClearCLBuffer buffer = doubleArrayToClearCLBuffer(clijx, focusMeasuresTable.getColumn(anotherFeature));
            featureCount = collectFeatures(clijx, buffer, featureStack, numberOfSigmas, featureCount);
            clijx.release(buffer);
        }

        clijx.release(numberOfSpots);

        return featureStack;
    }

    private static int collectFeatures(CLIJx clijx, ClearCLBuffer input, ClearCLBuffer featureStack, int numberOfSigmas, int featureCount) {
        ClearCLBuffer temp1 = clijx.create(input);
        ClearCLBuffer temp2 = clijx.create(input);
        for (int i = 0; i < numberOfSigmas; i ++) {
            double sigma = i * 2;
            if (i > 0) {
                clijx.copySlice(input, featureStack, featureCount);
                featureCount++;

                clijx.gradientX(input, temp1);
                clijx.copySlice(temp1, featureStack, featureCount);
                featureCount++;
            } else {

                clijx.blur(input, temp2, sigma, sigma);
                clijx.copySlice(temp2, featureStack, featureCount);
                featureCount++;

                clijx.gradientX(temp2, temp1);
                clijx.copySlice(temp1, featureStack, featureCount);
                featureCount++;
            }
        }
        clijx.release(temp1);
        clijx.release(temp2);
        return featureCount;
    }


    static ClearCLBuffer doubleArrayToClearCLBuffer(CLIJx clijx, double[] input) {
        return clijx.push(new Double1(input));
        /*
        float[] temp = new float[input.length];
        for (int i = 0; i < temp.length; i++) {
            temp[i] = (float) input[i];
        }

        long[] dims = new long[dimensions];
        for (int d = 0; d < dims.length; d++) {
            dims[d] = 1;
        }
        dims[targetDimension] = temp.length;

        FloatBuffer floatBuffer = FloatBuffer.wrap(temp);
        ClearCLBuffer clbuffer = clijx.create(dims, NativeTypeEnum.Float);

        clbuffer.readFrom(floatBuffer, true);
        return clbuffer;*/
    }

}
