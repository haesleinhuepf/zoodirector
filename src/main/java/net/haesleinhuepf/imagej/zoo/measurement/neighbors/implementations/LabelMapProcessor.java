package net.haesleinhuepf.imagej.zoo.measurement.neighbors.implementations;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.imagej.zoo.measurement.neighbors.NeighborProcessor;

public class LabelMapProcessor implements NeighborProcessor {
    @Override
    public ClearCLBuffer process(CLIJ2 clij2, ClearCLBuffer input, ClearCLBuffer pointlist, ClearCLBuffer label_map, ClearCLBuffer touch_matrix, ClearCLBuffer distance_matrix) {
        ClearCLBuffer result = clij2.create(input.getDimensions(), NativeTypeEnum.Float);
        clij2.copy(label_map, result);
        return result;
    }

    @Override
    public String getLUTName() {
        return "Glasbey";
    }

    @Override
    public String getName() {
        return "Label map";
    }

    @Override
    public boolean getDefaultActivated() {
        return false;
    }
}
