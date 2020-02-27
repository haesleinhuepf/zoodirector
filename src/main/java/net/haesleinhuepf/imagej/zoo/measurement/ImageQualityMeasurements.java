package net.haesleinhuepf.imagej.zoo.measurement;

import autopilot.measures.FocusMeasures;
import ij.ImageJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;

public class ImageQualityMeasurements extends DataSetMeasurements {


    public ImageQualityMeasurements(ClearControlDataSet dataSet) {
        super(dataSet);
    }

    @Override
    public void run()
    {
        ResultsTable maximumProjectionAnalysisResults = new ResultsTable();
        ResultsTable meanProjectionAnalysisResults = new ResultsTable();

        CLIJx clijx = CLIJx.getInstance();

        ClearCLBuffer projection = null;

        for (int f = firstFrame; f <= lastFrame; f+=frameStep) {
            System.out.println("f " + f);
            maximumProjectionAnalysisResults.incrementCounter();
            meanProjectionAnalysisResults.incrementCounter();

            maximumProjectionAnalysisResults.addValue("Frame", f);
            meanProjectionAnalysisResults.addValue("Frame", f);

            ImagePlus timePointStack = dataSet.getImageData(f);


            ClearCLBuffer input = clijx.push(timePointStack);
            projection = clijx.create(new long[]{input.getWidth(), input.getHeight()}, NativeTypeEnum.Float);

            clijx.maximumZProjection(input, projection);
            new SliceAnalyser(projection, FocusMeasures.getFocusMeasuresArray(), maximumProjectionAnalysisResults).run();

            clijx.meanZProjection(input, projection);
            new SliceAnalyser(projection, FocusMeasures.getFocusMeasuresArray(), meanProjectionAnalysisResults).run();

            input.close();

            dataSet.saveMeasurementTable(maximumProjectionAnalysisResults, "processed/autopilotFocusMeasures_maxProjection.csv");
            dataSet.saveMeasurementTable(meanProjectionAnalysisResults, "processed/autopilotFocusMeasures_meanProjection.csv");

            clijx.clear();
        }
        projection.close();
        maximumProjectionAnalysisResults.show("Max projection analysis results");
        meanProjectionAnalysisResults.show("Mean projection analysis Results");


    }

    public static void main(String ... arg) {
        new ImageJ();

        String sourceFolder = "C:/structure/data/2019-12-17-16-54-37-81-Lund_Tribolium_nGFP_TMR/";
        String datasetFolder = "C0opticsprefused";

        ClearControlDataSet dataSet = ClearControlDataSetOpener.open(sourceFolder, datasetFolder);

        new ImageQualityMeasurements(dataSet).run();
    }

}
