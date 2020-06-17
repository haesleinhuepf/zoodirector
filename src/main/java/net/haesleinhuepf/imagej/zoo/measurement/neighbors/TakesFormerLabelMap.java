package net.haesleinhuepf.imagej.zoo.measurement.neighbors;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;

public interface TakesFormerLabelMap {
    void setFormerLabelMap(ClearCLBuffer labelMap);
}
