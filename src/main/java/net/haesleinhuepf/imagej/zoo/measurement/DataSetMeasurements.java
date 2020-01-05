package net.haesleinhuepf.imagej.zoo.measurement;

import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;

public abstract class DataSetMeasurements  implements Runnable {
    protected ClearControlDataSet dataSet;

    protected int firstFrame = 0;
    protected int lastFrame = 0;
    protected int frameStep = 1;

    public DataSetMeasurements(ClearControlDataSet dataSet) {
        this.dataSet = dataSet;
        lastFrame = dataSet.getFramesPerMinute().length;
    }

    public DataSetMeasurements setFirstFrame(int firstFrame) {
        this.firstFrame = firstFrame;
        return this;
    }

    public DataSetMeasurements setLastFrame(int lastFrame) {
        this.lastFrame = lastFrame;
        return this;
    }

    protected DataSetMeasurements setFrameStep(int frameStep) {
        this.frameStep = frameStep;
        return this;
    }
}
