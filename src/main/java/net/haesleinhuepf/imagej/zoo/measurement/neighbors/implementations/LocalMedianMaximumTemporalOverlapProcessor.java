package net.haesleinhuepf.imagej.zoo.measurement.neighbors.implementations;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.imagej.zoo.measurement.neighbors.NeighborProcessor;
import net.haesleinhuepf.imagej.zoo.measurement.neighbors.TakesFormerLabelMap;
import net.haesleinhuepf.imagej.zoo.measurement.neighbors.TakesFormerPointlist;

public class LocalMedianMaximumTemporalOverlapProcessor implements NeighborProcessor, TakesFormerLabelMap, TakesFormerPointlist {
    private ClearCLBuffer former_labelMap;
    private ClearCLBuffer former_pointlist;

    @Override
    public ClearCLBuffer process(CLIJ2 clij2, ClearCLBuffer input, ClearCLBuffer pointlist, ClearCLBuffer label_map, ClearCLBuffer touch_matrix, ClearCLBuffer distance_matrix) {
        ClearCLBuffer result = clij2.create(input.getDimensions(), NativeTypeEnum.Float);
        if (former_labelMap == null || former_pointlist == null) {
            clij2.set(result, 0);
            return result;
        }

        long number_of_labels = pointlist.getWidth();
        long number_of_former_labels = former_pointlist.getWidth();
        ClearCLBuffer jaccard_matrix = clij2.create(number_of_labels + 1, number_of_former_labels + 1);
        clij2.generateJaccardIndexMatrix(label_map, former_labelMap, jaccard_matrix);


        //clij2.show(displacement_matrix, "ds");
        //new WaitForUserDialog("dd").show();

        ClearCLBuffer maxmimum_jaccard_vector = clij2.create(number_of_labels, 1, 1 );
        clij2.maximumYProjection(jaccard_matrix, maxmimum_jaccard_vector);


        ClearCLBuffer local_median_maxmimum_jaccard_vector = clij2.create(number_of_labels, 1, 1 );
        clij2.medianOfTouchingNeighbors(maxmimum_jaccard_vector, touch_matrix, local_median_maxmimum_jaccard_vector);


        clij2.replaceIntensities(label_map, local_median_maxmimum_jaccard_vector, result);
        return result;
    }

    @Override
    public String getLUTName() {
        return "Fire";
    }

    @Override
    public String getName() {
        return "Local median maximum temporal overlap";
    }

    @Override
    public boolean getDefaultActivated() {
        return false;
    }

    @Override
    public void setFormerLabelMap(ClearCLBuffer labelMap) {
        this.former_labelMap = labelMap;
    }

    @Override
    public void setFormerPointlist(ClearCLBuffer pointlist) {
        this.former_pointlist = pointlist;
    }
}
