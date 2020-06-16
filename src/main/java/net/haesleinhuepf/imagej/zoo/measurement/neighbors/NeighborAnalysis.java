package net.haesleinhuepf.imagej.zoo.measurement.neighbors;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.EllipseRoi;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.HyperStackConverter;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.StatisticsOfLabelledPixels;
import net.haesleinhuepf.clij2.plugins.VoronoiLabeling;
import net.haesleinhuepf.imagej.zoo.measurement.neighbors.implementations.*;

import java.util.HashMap;

public class NeighborAnalysis implements PlugInFilter {

    static int background_subtraction_radius = 5;
    static double spot_detectection_blur_sigma = 3;
    static double threshold = 0;

    NeighborProcessor[] processors = new NeighborProcessor[] {
        new AverageDistanceOfTouchingNeighborsProcessor(),
            new SpotDetectionProcessor(),
            new LabelMapProcessor(),
            new TouchDistanceMeshProcessor(),
            new TouchMeshProcessor(),
            new TouchCountMeshProcessor(),
            new NumberOfTouchingNeighborsProcessor(),
            new EstimatedNumberOfTouchingNeighborsProcessor(),
            new TouchPortionMeshProcessor(),

            new AverageDistanceOfNClosestPointsProcessor(1),
            new AverageDistanceOfNClosestPointsProcessor(2),
            new AverageDistanceOfNClosestPointsProcessor(3),
            new AverageDistanceOfNClosestPointsProcessor(6),
            new AverageDistanceOfNClosestPointsProcessor(8),
            new AverageDistanceOfNClosestPointsProcessor(10),
            new AverageDistanceOfNClosestPointsProcessor(20),

            new ParametricImageProcessor(StatisticsOfLabelledPixels.STATISTICS_ENTRY.MEAN_INTENSITY.toString()),
            new ParametricImageProcessor(StatisticsOfLabelledPixels.STATISTICS_ENTRY.MINIMUM_INTENSITY.toString()),
            new ParametricImageProcessor(StatisticsOfLabelledPixels.STATISTICS_ENTRY.MAXIMUM_INTENSITY.toString()),
            new ParametricImageProcessor(StatisticsOfLabelledPixels.STATISTICS_ENTRY.STANDARD_DEVIATION_INTENSITY.toString()),
            new ParametricImageProcessor(StatisticsOfLabelledPixels.STATISTICS_ENTRY.PIXEL_COUNT.toString()),
            new ParametricImageProcessor(StatisticsOfLabelledPixels.STATISTICS_ENTRY.MEAN_DISTANCE_TO_CENTROID.toString()),
            new ParametricImageProcessor(StatisticsOfLabelledPixels.STATISTICS_ENTRY.MAX_DISTANCE_TO_CENTROID.toString()),
            new ParametricImageProcessor(StatisticsOfLabelledPixels.STATISTICS_ENTRY.MAX_MEAN_DISTANCE_TO_CENTROID_RATIO.toString()),
            new ParametricImageProcessor(StatisticsOfLabelledPixels.STATISTICS_ENTRY.MEAN_DISTANCE_TO_MASS_CENTER.toString()),
            new ParametricImageProcessor(StatisticsOfLabelledPixels.STATISTICS_ENTRY.MAX_DISTANCE_TO_MASS_CENTER.toString()),
            new ParametricImageProcessor(StatisticsOfLabelledPixels.STATISTICS_ENTRY.MAX_MEAN_DISTANCE_TO_MASS_CENTER_RATIO.toString())

    };

    @Override
    public int setup(String s, ImagePlus imagePlus) {
        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        ImagePlus input_imp = IJ.getImage();
        int original_z = input_imp.getZ();
        int original_t = input_imp.getT();

        Roi roi = input_imp.getRoi();
        input_imp.killRoi();

        GenericDialog gd = new GenericDialog("Neighbor analyser");
        gd.addChoice("Input", new String[]{"Raw image", "Spot image", "Label Map"}, "Raw image");

        gd.addNumericField("Background subtraction (top-hat) radius", background_subtraction_radius, 0);
        gd.addNumericField("Spot detection blur sigma", spot_detectection_blur_sigma, 2);
        gd.addNumericField("Threshold", threshold, 2);

        int check_box_count = 0;
        for (NeighborProcessor processor : processors) {
            gd.addCheckbox(processor.getName().replace(" ", "_"), processor.getDefaultActivated());

            if (check_box_count % 2 == 0) {
                gd.addToSameRow();
            }
            check_box_count++;
        }

        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        String input_type = gd.getNextChoice();
        background_subtraction_radius = (int)gd.getNextNumber();
        spot_detectection_blur_sigma = gd.getNextNumber();
        threshold = gd.getNextNumber();

        HashMap<NeighborProcessor, ImageStack> result_stacks = new HashMap<>();

        for (NeighborProcessor processor : processors) {
            if (gd.getNextBoolean()) {
                result_stacks.put(processor, new ImageStack());
            }
        }

        CLIJ2 clij2 = CLIJ2.getInstance();

        for (int t = 0; t < input_imp.getNFrames(); t++) {
            input_imp.setT(t + 1);

            ClearCLBuffer input = clij2.pushCurrentZStack(input_imp);
            ClearCLBuffer label_map = null;
            ClearCLBuffer pointlist = null;

            int number_of_objects = 0;
            ResultsTable table = null;

            if (input_type.compareTo("Raw image") == 0) {
                ClearCLBuffer temp1 = clij2.create(input);
                clij2.topHatBox(input, temp1, background_subtraction_radius, background_subtraction_radius, 0);

                ClearCLBuffer temp2 = clij2.create(input);
                clij2.gaussianBlur(temp1, temp2, spot_detectection_blur_sigma, spot_detectection_blur_sigma, 0);

                ClearCLBuffer temp3 = clij2.create(input.getDimensions(), NativeTypeEnum.Float);
                if (temp3.getDimension() == 2) {
                    clij2.detectMaximaBox(temp2, temp3, 1, 1, 0);
                } else {
                    clij2.detectMaximaBox(temp2, temp3, 1, 1, 1);
                }

                if (threshold > 0) {
                    clij2.greaterConstant(temp2, temp1, threshold);
                    clij2.mask(temp3, temp1, temp2);
                } else {
                    clij2.copy(temp3, temp2);
                }

                number_of_objects = (int)clij2.sumOfAllPixels(temp2);
                pointlist = clij2.create(number_of_objects, input.getDimension());
                clij2.spotsToPointList(temp2, pointlist);

                label_map = temp3;
                VoronoiLabeling.voronoiLabeling(clij2, temp2, label_map);

                temp1.close();
                temp2.close();

            } else if (input_type.compareTo("Spot image") == 0) {
                number_of_objects = (int)clij2.sumOfAllPixels(input);
                pointlist = clij2.create(number_of_objects, input.getDimension());
                clij2.spotsToPointList(input, pointlist);

                label_map = clij2.create(input.getDimensions(), clij2.Float);
                VoronoiLabeling.voronoiLabeling(clij2, input, label_map);
            } else {
                label_map = input;
                number_of_objects = (int)clij2.maximumOfAllPixels(input);
                pointlist = clij2.create(number_of_objects, label_map.getDimension());
                clij2.centroidsOfLabels(input, pointlist);
            }

            System.out.println("Input " + input);
            System.out.println("Pointlist " + pointlist);
            System.out.println("Label map " + label_map);



            ClearCLBuffer touch_matrix = clij2.create(number_of_objects + 1, number_of_objects + 1);
            clij2.generateTouchMatrix(label_map, touch_matrix);

            ClearCLBuffer distance_matrix = clij2.create(number_of_objects + 1, number_of_objects + 1);
            clij2.generateDistanceMatrix(pointlist, pointlist, distance_matrix);

            for (NeighborProcessor processor : processors) {
                 if (result_stacks.keySet().contains(processor)) {
                    ImageStack stack = result_stacks.get(processor);

                    if (processor instanceof TakesPropertyTable) {
                        if (table == null) {
                            table = new ResultsTable();
                            clij2.statisticsOfBackgroundAndLabelledPixels(input, label_map, table);
                        }
                        ((TakesPropertyTable) processor).setTable(table);
                    }

                    ClearCLBuffer result = processor.process(clij2, input, pointlist, label_map, touch_matrix, distance_matrix);
                    ImagePlus imp = clij2.pull(result);
                    result.close();

                    for (int z = 0; z < imp.getNSlices(); z++) {
                        imp.setZ(z + 1);
                        stack.addSlice(imp.getProcessor());
                    }
                }
            }

            input.close();
            pointlist.close();

            if (label_map != input) {
                label_map.close();
            }
            //break;
        }

        for (NeighborProcessor processor : processors) {
            if (result_stacks.keySet().contains(processor)) {
                ImageStack stack = result_stacks.get(processor);
                ImagePlus imp = new ImagePlus(processor.getName(), stack);
                if (input_imp.getNSlices() > 1) {
                    int frames = imp.getNFrames() / input_imp.getNSlices();
                    int slices = input_imp.getNSlices();
                    imp = HyperStackConverter.toHyperStack(imp, 1, slices, frames);
                    imp.setTitle(processor.getName());
                }

                if (roi != null) {
                    imp.setRoi(roi);
                    IJ.run(imp, "Make Inverse", "");

                    for (int t = 0; t < imp.getNFrames(); t++) {
                        imp.setT(t + 1);
                        for (int z = 0; z < imp.getNSlices(); z++) {
                            imp.setZ(z + 1);
                            IJ.run(imp, "Multiply...", "value=0");
                        }
                    }
                    imp.setRoi(roi);
                    imp.changes = false;
                }

                imp.show();

                IJ.run(imp,"Enhance Contrast", "saturated=0.35");
                if (processor.getLUTName() != null) {
                    IJ.run(imp, processor.getLUTName(), "");
                }
            }
        }

        input_imp.setZ(original_z);
        input_imp.setT(original_t);
        input_imp.setRoi(roi);
        System.out.println("Bye");
    }


    public static void main(String[] args) {
        new ImageJ();

        ImagePlus imp = IJ.openImage("C:/structure/data/027632.tif");
                //IJ.openImage("src/main/resources/thumbnails.tif");
        imp.killRoi();
        imp.show();

        NeighborAnalysis.threshold = 0;

        //imp.setRoi(new EllipseRoi(153.0,45.0,101.0,492.0,0.53));

        new NeighborAnalysis().run(null);
    }
}
