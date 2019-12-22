package net.haesleinhuepf.imagej.zoo.measurement;

import autopilot.image.DoubleArrayImage;
import autopilot.measures.FocusMeasures;
import de.mpicbg.rhaase.utils.DoubleArrayImageImgConverter;
import ij.measure.ResultsTable;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;

public class SliceAnalyser implements Runnable {
    private ClearCLBuffer image;
    private FocusMeasures.FocusMeasure[] features;
    private ResultsTable table;

    public SliceAnalyser(ClearCLBuffer image, FocusMeasures.FocusMeasure[] features, ResultsTable table) {
        this.image = image;
        this.features = features;
        this.table = table;
    }

    @Override
    public void run() {
        RandomAccessibleInterval rai = CLIJ.getInstance().pullRAI(image);

        DoubleArrayImage image = new DoubleArrayImageImgConverter(Views.iterable(rai)).getDoubleArrayImage();

        for (FocusMeasures.FocusMeasure focusMeasure : features) {
            //System.out.println("Determining " + focusMeasure.getLongName());
            double focusMeasureValue = FocusMeasures.computeFocusMeasure(focusMeasure, image);
            table.addValue(focusMeasure.getLongName(), focusMeasureValue);
        }
    }
}
