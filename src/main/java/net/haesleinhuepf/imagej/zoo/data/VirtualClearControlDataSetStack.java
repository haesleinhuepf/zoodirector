package net.haesleinhuepf.imagej.zoo.data;

import ij.ImagePlus;
import ij.process.ImageProcessor;

public class VirtualClearControlDataSetStack extends ij.VirtualStack{

    private final double time_range_seconds;
    ClearControlDataSet dataSet;
    int depth;
    private final double start_time_seconds;
    private final double end_time_seoncds;
    private final int number_of_frames;

    public VirtualClearControlDataSetStack(ClearControlDataSet dataSet, double start_time_seconds, double end_time_seoncds, int number_of_frames) {
        this.dataSet = dataSet;
        depth = dataSet.getImageData(0).getNSlices();
        this.start_time_seconds = start_time_seconds;
        this.end_time_seoncds = end_time_seoncds;
        this.number_of_frames = number_of_frames;
        time_range_seconds = end_time_seoncds - start_time_seconds;
    }

    @Override
    public ImageProcessor getProcessor(int n) {
        int stackNumber = n / depth;
        int sliceNumber = n % depth;

        double frame_time_in_seconds = start_time_seconds + stackNumber * time_range_seconds / number_of_frames;
        int frame = dataSet.getFirstFrameAfterTimeInSeconds(frame_time_in_seconds);

        ImagePlus imp = dataSet.getImageData(frame);
        if (sliceNumber <= imp.getNSlices()) {
            imp.setZ(sliceNumber);
        }
        return imp.getProcessor();
    }


    @Override
    public int size() {
        return number_of_frames * depth;
    }

    @Override
    public int getSize() {
        return number_of_frames * depth;
    }
}
