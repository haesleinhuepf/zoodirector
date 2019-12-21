package net.haesleinhuepf.imagej.zoo.measurement;

import autopilot.image.DoubleArrayImage;
import autopilot.measures.FocusMeasures;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.quality.MeasureQualityInTilesPlugin;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.quality.MeasureQualityPerSlicePlugin;
import de.mpicbg.rhaase.utils.DoubleArrayImageImgConverter;
import ij.ImageJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import javafx.beans.property.ReadOnlySetProperty;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij.macro.modules.Clear;
import net.haesleinhuepf.imagej.zoo.visualisation.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.visualisation.ClearControlDataSetOpener;
import net.imglib2.view.Views;

public class ImageQualityMeasurements implements Runnable {

    private ClearControlDataSet dataSet;

    private int firstFrame = 0;
    private int lastFrame = 0;

    public ImageQualityMeasurements(ClearControlDataSet dataSet) {
        this.dataSet = dataSet;
        lastFrame = dataSet.getFramesPerMinute().length;
    }

    public ImageQualityMeasurements setFirstFrame(int firstFrame) {
        this.firstFrame = firstFrame;
        return this;
    }

    public ImageQualityMeasurements setLastFrame(int lastFrame) {
        this.lastFrame = lastFrame;
        return this;
    }

    @Override
    public void run()
    {
        ResultsTable rt = new ResultsTable();

        CLIJ clij = CLIJ.getInstance();

        ClearCLBuffer projection = null;

        for (int f = firstFrame; f <= lastFrame; f++) {
            rt.incrementCounter();

            ImagePlus timePointStack = dataSet.getImageData(f);


            ClearCLBuffer input = clij.push(timePointStack);
            projection = clij.create(new long[]{input.getWidth(), input.getHeight()}, NativeTypeEnum.Float);

            clij.op().maximumZProjection(input, projection);

            input.close();

            new SliceAnalyser(projection, FocusMeasures.getFocusMeasuresArray(), rt).run();
        }
        projection.close();
        rt.show("Results");

        dataSet.saveMeasurementTable(rt, "autopilotFocusMeasures_maxProjection.csv");
    }

    public static void main(String ... arg) {
        new ImageJ();

        String sourceFolder = "C:/structure/data/2019-04-26-14-06-58-88-Porto/";
        String datasetFolder = "C0opticsprefused";

        ClearControlDataSet dataSet = ClearControlDataSetOpener.open(sourceFolder, datasetFolder);

        new ImageQualityMeasurements(dataSet).setLastFrame(10).run();
    }

}
