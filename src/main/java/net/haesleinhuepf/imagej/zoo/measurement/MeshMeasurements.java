package net.haesleinhuepf.imagej.zoo.measurement;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import javafx.beans.property.ReadOnlySetProperty;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.macro.modules.Clear;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.advancedfilters.VarianceOfMaskedPixels;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;

import javax.swing.*;

public class MeshMeasurements extends DataSetMeasurements {
    CLIJx clijx;

    double zoomFactor = 1.5; // -> each analysed voxel is 1.5x1.5x1.5 microns large`


    double blurSigma = 3;

    boolean projection_visualisation_on_screen = true;
    boolean projection_visualisation_to_disc = false;

    boolean do_pseudo_cell_segmentation = true;

    int maximumDistanceToMeshPoints = 100;

    int numberDoubleErosionsForPseudoCellSegmentation = 7;
    int numberDoubleDilationsForPseudoCellSegmentation = 17;
    //private String thresholdAlgorithm = "Triangle";
    private double threshold = 300;

    public MeshMeasurements(ClearControlDataSet dataSet) {
        super(dataSet);
        clijx = CLIJx.getInstance();
    }

    public MeshMeasurements setZoomFactor(double zoomFactor) {
        this.zoomFactor = zoomFactor;
        return this;
    }

    public MeshMeasurements setNumberDoubleDilationsForPseudoCellSegmentation(int numberDoubleDilationsForPseudoCellSegmentation) {
        this.numberDoubleDilationsForPseudoCellSegmentation = numberDoubleDilationsForPseudoCellSegmentation;
        return this;
    }

    public MeshMeasurements setNumberDoubleErosionsForPseudoCellSegmentation(int numberDoubleErosionsForPseudoCellSegmentation) {
        this.numberDoubleErosionsForPseudoCellSegmentation = numberDoubleErosionsForPseudoCellSegmentation;
        return this;
    }

    public MeshMeasurements setProjectionVisualisationOnScreen(boolean projection_visualisation_on_screen) {
        this.projection_visualisation_on_screen = projection_visualisation_on_screen;
        return this;
    }

    public MeshMeasurements setProjectionVisualisationToDisc(boolean projection_visualisation_to_disc) {
        this.projection_visualisation_to_disc = projection_visualisation_to_disc;
        return this;
    }

    public MeshMeasurements setDoPseudoCellSegmentation(boolean do_pseudo_cell_segmentation) {
        this.do_pseudo_cell_segmentation = do_pseudo_cell_segmentation;
        return this;
    }

    public MeshMeasurements setCLIJx(CLIJx clijx) {
        this.clijx = clijx;
        return this;
    }

    public MeshMeasurements setBlurSigma(double blurSigma) {
        this.blurSigma = blurSigma;
        return this;
    }

    //public MeshMeasurements setThresholdAlgorithm(String thresholdAlgorithm) {
    //    this.thresholdAlgorithm = thresholdAlgorithm;
    //    return this;
    //}

    public MeshMeasurements setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }

    @Override
    public void run() {

        String outputFolder = dataSet.getPath() + "processed/";

        ResultsTable meshMeasurementTable = new ResultsTable();

        GenericDialog cancelDialog = new GenericDialog("Analysis running...");
        cancelDialog.addMessage("CLick on cancel to cancel.");
        cancelDialog.setModal(false);
        cancelDialog.show();

        for (int f = firstFrame; f <= lastFrame; f++) {
            if (cancelDialog.wasCanceled() || cancelDialog.wasOKed()) {
                break;
            }
            meshMeasurementTable.incrementCounter();
            meshMeasurementTable.addValue("Frame", f);

            long timestamp = System.currentTimeMillis();

		    // IJ.run("Close All");
            clijx.stopWatch("");

            String filename = "0000000" + f;
            filename = filename.substring(filename.length() - 6) + ".raw";

            ImagePlus timePointStack = dataSet.getImageData(f);

            //System.out.println(foldername + filename)
		    // # break;

            ClearCLBuffer pushedImage = clijx.push(timePointStack);
                    //clijx.create([512, 1024, 67], NativeTypeEnum.UnsignedShort);
            pushedImage.setName("pushedImage");
            clijx.stopWatch("load data");

            // IJ.run(imp, "32-bit", "");
		    // IJ.run(imp, "Rotate 90 Degrees Right", "");
		    // imp.show();

            Calibration calibration = timePointStack.getCalibration();
            double factorX = calibration.pixelWidth * zoomFactor;
            double factorY = calibration.pixelHeight * zoomFactor;
            double factorZ = calibration.pixelDepth * zoomFactor;

            // -----------------------------------------------------------------------
            // resampling
            long w = (long)(pushedImage.getWidth() * factorX);
            long h = (long)(pushedImage.getHeight() * factorY);
            long d = (long)(pushedImage.getDepth() * factorZ);

            System.out.println(new long[]{w, h, d});

            ClearCLBuffer resampledImage = clijx.create(new long[]{w, h, d});
            resampledImage.setName("resampledImage");
            clijx.resample(pushedImage, resampledImage, factorX, factorY, factorZ, true);

            clijx.stopWatch("resample");

            ClearCLBuffer inputImage = clijx.create(new long[]{h, w, d});
            inputImage.setName("inputImage");
            clijx.rotateRight(resampledImage, inputImage);
            clijx.release(resampledImage);

            clijx.stopWatch("rotate");

            // clijx.show(inputImage, "inputImage");
            //if (single_stack_visualisation) {
            //    ImagePlus imp_inputImage = clijx.pull(inputImage);
            //}
		    // clijx.saveAsTIF( inputImage,                outputFolder + "_input/" + filename + ".tif");

    		// -----------------------------------------------------------------------
		    // spot detection
            ClearCLBuffer detected_spots = spot_detection(inputImage);
            clijx.stopWatch("spot detection");

		    // cell segmentation
            ClearCLBuffer segmented_cells = null;
            ClearCLBuffer cell_outlines = null;
            if (do_pseudo_cell_segmentation) {
                segmented_cells = pseudo_cell_segmentation(detected_spots);
                clijx.stopWatch("cell segmentation");

                // cell outlines
                cell_outlines = label_outlines(segmented_cells);
                clijx.stopWatch("outline labels");
            }

            // -----------------------------------------------------------------------
            // convert spots image to spot list
            int number_of_spots = (int)clijx.sumPixels(detected_spots);
            meshMeasurementTable.addValue("spot_count", number_of_spots);

            if (number_of_spots < 1) {
                // we have to have a non-zero sized stack in the next step...
                number_of_spots = 1;
            }
            ClearCLBuffer pointlist = clijx.create(new long[]{number_of_spots, 3});
            pointlist.setName("pointlist");
            clijx.stopWatch("intermediate ");
            clijx.spotsToPointList(detected_spots, pointlist);
            clijx.stopWatch("pointlist");

            // -----------------------------------------------------------------------
            // neighborhood analysis
            ClearCLBuffer[] result;
            if (do_pseudo_cell_segmentation) {
                result = measure_average_close_neighbor_distance(pointlist, number_of_spots, segmented_cells);
            } else {
                result = measure_average_close_neighbor_distance(pointlist, number_of_spots, detected_spots);
            }
			ClearCLBuffer average_close_spot_distance = result[0];
            ClearCLBuffer distance_matrix = result[1];

            clijx.stopWatch("distance map");


            ClearCLBuffer number_of_touching_neighbors = null;
            ClearCLBuffer touch_matrix;
            ClearCLBuffer mesh;

            if (do_pseudo_cell_segmentation) {
                result = measure_number_of_neighbors(pointlist, number_of_spots, segmented_cells);
                number_of_touching_neighbors = result[0];
                touch_matrix = result[1];

                //clijx.showGrey(touch_matrix, "touch_matrix");
                //clijx.showGrey(distance_matrix, "distance_matrix");
                clijx.stopWatch("touch map");
                ClearCLBuffer relevantDistances = clijx.create(distance_matrix.getDimensions(), clijx.Float);
                clijx.multiplyImages(distance_matrix, touch_matrix, relevantDistances);

                double meanDistance = clijx.meanOfPixelsAboveThreshold(relevantDistances, 0);
                double varianceDistance = clijx.varianceOfMaskedPixels(relevantDistances, touch_matrix, meanDistance);
                meshMeasurementTable.addValue("mean_neighbor_distance", meanDistance);
                meshMeasurementTable.addValue("variance_neighbor_distance", varianceDistance);


                // clijx.saveAsTIF(touch_matrix,      outputFolder + "_touch_matrix/" + filename + ".tif");
                // clijx.saveAsTIF(pointlist,      outputFolder + "_pointlist/" + filename + ".tif");

                // clijx.clear();
                // continue;

                mesh = clijx.create(inputImage);
                mesh.setName("mesh");
                clijx.touchMatrixToMesh(pointlist, touch_matrix, mesh);
                clijx.stopWatch("mesh");
            } else {
                mesh = distanceMatrixToMesh(inputImage, pointlist, distance_matrix);
                clijx.stopWatch("mesh");
            }

            // -----------------------------------------------------------------------
            // Visualisation

            if (projection_visualisation_on_screen || projection_visualisation_to_disc) {
                // save maximum and average projections to disc
                result = arg_max_projection(inputImage);
                ClearCLBuffer max_image = result[0];
                ClearCLBuffer arg_max_image = result[1];

                ClearCLBuffer max_spots = max_projection(detected_spots);
                ClearCLBuffer[] max_cells = new ClearCLBuffer[1];
                ClearCLBuffer[] arg_max_cells = new ClearCLBuffer[1];

                ClearCLBuffer[] max_membranes = new ClearCLBuffer[1];
                ClearCLBuffer[] mean_membranes = new ClearCLBuffer[1];

                if (do_pseudo_cell_segmentation) {
                    result = arg_max_projection(segmented_cells);
                    max_cells[0] = result[0];
                    arg_max_cells[0] = result[1];
                    max_membranes[0] = max_projection(cell_outlines);
                    mean_membranes[0] = mean_projection(cell_outlines);
                }
                ClearCLBuffer max_avg_dist = max_projection(average_close_spot_distance);
                ClearCLBuffer mean_avg_dist = mean_projection(average_close_spot_distance);

                ClearCLBuffer[] max_num_touch = new ClearCLBuffer[1];
                ClearCLBuffer[] mean_num_touch = new ClearCLBuffer[1];
                if (do_pseudo_cell_segmentation) {
                    max_num_touch[0] = max_projection(number_of_touching_neighbors);
                    mean_num_touch[0] = mean_projection(number_of_touching_neighbors);
                }
                ClearCLBuffer max_mesh = max_projection(mesh);

                ClearCLBuffer[] nonzero_min_membranes = new ClearCLBuffer[1];
                ClearCLBuffer[] nonzero_min_num_touch = new ClearCLBuffer[1];
                if (do_pseudo_cell_segmentation) {
                    nonzero_min_membranes[0] = nonzero_min_projection(cell_outlines);
                    nonzero_min_num_touch[0] = nonzero_min_projection(number_of_touching_neighbors);
                }
                ClearCLBuffer nonzero_min_avg_dist = nonzero_min_projection(average_close_spot_distance);



                if (projection_visualisation_to_disc) {
                    clijx.saveAsTIF(max_image, outputFolder + "_max_image/" + filename + ".tif");
                    clijx.saveAsTIF(arg_max_image, outputFolder + "_arg_max_image/" + filename + ".tif");
                    clijx.saveAsTIF(max_spots, outputFolder + "_max_spots/" + filename + ".tif");
                    if (do_pseudo_cell_segmentation) {
                        clijx.saveAsTIF(max_cells[0], outputFolder + "_max_cells/" + filename + ".tif");
                        clijx.saveAsTIF(arg_max_cells[0], outputFolder + "_arg_max_cells/" + filename + ".tif");
                        clijx.saveAsTIF(mean_membranes[0], outputFolder + "_mean_membranes/" + filename + ".tif");
                        clijx.saveAsTIF(max_membranes[0], outputFolder + "_max_membranes/" + filename + ".tif");
                    }
                    clijx.saveAsTIF(max_avg_dist, outputFolder + "_max_avg_dist/" + filename + ".tif");
                    clijx.saveAsTIF(mean_avg_dist, outputFolder + "_mean_avg_dist/" + filename + ".tif");
                    if (do_pseudo_cell_segmentation) {
                        clijx.saveAsTIF(max_num_touch[0], outputFolder + "_max_num_touch/" + filename + ".tif");
                        clijx.saveAsTIF(mean_num_touch[0], outputFolder + "_mean_num_touch/" + filename + ".tif");
                    }
                    clijx.saveAsTIF(max_mesh, outputFolder + "_max_mesh/" + filename + ".tif");
                    if (do_pseudo_cell_segmentation) {
                        clijx.saveAsTIF(nonzero_min_membranes[0], outputFolder + "_nonzero_min_membranes/" + filename + ".tif");
                        clijx.saveAsTIF(nonzero_min_num_touch[0], outputFolder + "_nonzero_min_num_touch/" + filename + ".tif");
                    }
                    clijx.saveAsTIF(nonzero_min_avg_dist, outputFolder + "_nonzero_min_avg_dist/" + filename + ".tif");
                }

                if (projection_visualisation_on_screen) {
                    clijx.showGrey(max_image, "_max_image");
                    clijx.showGrey(arg_max_image, "_arg_max_image");
                    clijx.showGrey(max_spots, "_max_spots");
                    if (do_pseudo_cell_segmentation) {
                        clijx.showGrey(max_cells[0], "_max_cells");
                        clijx.showGrey(arg_max_cells[0], "_arg_max_cells");
                        clijx.showGrey(mean_membranes[0], "_mean_membranes");
                        clijx.showGrey(max_membranes[0], "_max_membranes");
                    }

                    clijx.showGrey(max_avg_dist, "_max_avg_dist");
                    clijx.showGrey(mean_avg_dist, "_mean_avg_dist");

                    if (do_pseudo_cell_segmentation) {
                        clijx.showGrey(max_num_touch[0], "_max_num_touch");
                        clijx.showGrey(mean_num_touch[0], "_mean_num_touch");
                    }
                    clijx.showGrey(max_mesh, "_max_mesh");

                    if (do_pseudo_cell_segmentation) {
                        clijx.showGrey(nonzero_min_membranes[0], "_nonzero_min_membranes");
                        clijx.showGrey(nonzero_min_num_touch[0], "_nonzero_min_num_touch");
                    }
                    clijx.showGrey(nonzero_min_avg_dist, "_nonzero_min_avg_dist");

                    // clijx.showGrey(distance_matrix, "distance_matrix");
                    // clijx.showGrey(pointlist, "pointlist");
                    clijx.organiseWindows(0, 0, 4, 4, 485, 300);
                    //clijx.organiseWindows(500, -1300, 5, 3, 630, 420);

                }


                clijx.stopWatch("visualisation");
            }

            System.out.println("Whole analysis took " + (System.currentTimeMillis() - timestamp) + " ms");
            clijx.release(pushedImage);

            IJ.log(clijx.reportMemory());

            clijx.clear();
            //break;
        }
        cancelDialog.hide();

        meshMeasurementTable.show("Mesh measurements results");
        dataSet.saveMeasurementTable(meshMeasurementTable, "meshMeasurements.csv");
    }



    private ClearCLBuffer spot_detection (ClearCLBuffer inputImage) {
        // blur a bit and detect maxima
        ClearCLBuffer blurred = clijx.create(inputImage);
        blurred.setName("blurred");
        ClearCLBuffer thresholded = clijx.create(inputImage);
        thresholded.setName("thresholded");
        ClearCLBuffer detected_spots = clijx.create(inputImage);
        detected_spots.setName("detected_spots");
        ClearCLBuffer masked = clijx.create(inputImage);
        masked.setName("masked");

        // -----------------------------------------------------------------------
        // background / noise removal
        // clijx.differenceOfGaussian(inputImage,blurred,3,3,0,15,15,0);
        // clijx.absoluteInplace(blurred);

        clijx.blur(inputImage, blurred, blurSigma, blurSigma, blurSigma);

        // ----------------------------------------------------------------------
        // spot detection
        clijx.detectMaximaBox(blurred, detected_spots, 1);

        // remove spots in background
        //clijx.automaticThreshold(blurred, thresholded, thresholdAlgorithm);
        clijx.threshold(blurred, thresholded, threshold);
        //clijx.show(blurred, "blurred");
        clijx.mask(detected_spots, thresholded, masked);

        clijx.copy(masked, detected_spots);
        // clijx.show(detected_spots,"t");

        // -----------------------------------------------------------------------
        // clean up
        clijx.release(thresholded);
        clijx.release(masked);
        clijx.release(blurred);
        return detected_spots;
    }

    private ImagePlus spot_visualisation(ClearCLBuffer detected_spots) {
        // make spots bigger for visualisation
        ClearCLBuffer temp = clijx.create(detected_spots);
        temp.setName("temp");
        clijx.dilateBox(detected_spots,temp);
        clijx.dilateSphere(temp,detected_spots);
        clijx.dilateBox(detected_spots,temp);
        clijx.dilateSphere(temp,detected_spots);
        clijx.copy(detected_spots,temp);
        clijx.binaryEdgeDetection(temp,detected_spots);
        clijx.release(temp);

        // clijx.show(detected_spots,"detected_spots");
        return clijx.pull(detected_spots);
    }

    private ClearCLBuffer distanceMatrixToMesh(ClearCLBuffer inputImage, ClearCLBuffer pointlist, ClearCLBuffer distance_matrix) {

        ClearCLBuffer mesh = clijx.create(inputImage);
        mesh.setName("mesh");

        ClearCLBuffer closestSpotIndices = clijx.create(new long[]{pointlist.getWidth(), 10});
        clijx.nClosestPoints(distance_matrix,closestSpotIndices);
        clijx.pointIndexListToMesh(pointlist,closestSpotIndices,mesh);
        closestSpotIndices.close();
        return mesh;
    }


    private ClearCLBuffer distanceMatrixToMesh2(ClearCLBuffer inputImage, ClearCLBuffer pointlist, ClearCLBuffer distance_matrix) {
        ClearCLBuffer mesh = clijx.create(inputImage);
        mesh.setName("mesh");
        ClearCLBuffer tempMesh1 = clijx.create(inputImage);
        ClearCLBuffer tempMesh2 = clijx.create(inputImage);
        clijx.set(tempMesh2, 0);
        for (int d = 1; d < maximumDistanceToMeshPoints + 1; d = d + 5) {
            clijx.set(tempMesh1, 0);
            clijx.distanceMatrixToMesh(pointlist, distance_matrix, tempMesh1, d);
            clijx.addImages(tempMesh1, tempMesh2, mesh);
            clijx.copy(mesh, tempMesh2);
        }
        tempMesh1.close();
        tempMesh2.close();
        return mesh;
    }

    private ClearCLBuffer pseudo_cell_segmentation(ClearCLBuffer detected_spots) {

        ClearCLBuffer tempSpots1 = clijx.create(detected_spots);
    	tempSpots1.setName("Segmented cells");
        ClearCLBuffer tempSpots2 = clijx.create(detected_spots);
	    tempSpots2.setName("tempSpots2");
        ClearCLBuffer flag =clijx.create(new long[]{1, 1, 1});
	    flag.setName("flag");

    	clijx.connectedComponentsLabeling(detected_spots,tempSpots1);

    	for (int j = 0; j < numberDoubleDilationsForPseudoCellSegmentation; j++) {
            clijx.onlyzeroOverwriteMaximumDiamond(tempSpots1, flag, tempSpots2);
            clijx.onlyzeroOverwriteMaximumBox(tempSpots2, flag, tempSpots1);
        }
        clijx.release(flag);

        ClearCLBuffer tempSpots3 =clijx.create(detected_spots);
    	tempSpots3.setName("tempSpots3");
	    clijx.threshold(tempSpots1,tempSpots2,1);

        for (int j = 0; j < numberDoubleErosionsForPseudoCellSegmentation; j++) {
            clijx.erodeBox(tempSpots2, tempSpots3);
            clijx.erodeBox(tempSpots3, tempSpots2);
        }
        clijx.erodeBox(tempSpots2,tempSpots3);
	    clijx.copy(tempSpots1,tempSpots2);
	    clijx.mask(tempSpots2,tempSpots3,tempSpots1);
	    clijx.release(tempSpots3);

    	// clijx.show(tempSpots1,"tempSpots1");
	    clijx.release(tempSpots2);
        return tempSpots1;
    }

    private ClearCLBuffer label_outlines(ClearCLBuffer labels) {
        ClearCLBuffer outlines = clijx.create(labels);
        outlines.setName("outlines");
        ClearCLBuffer label_outlines = clijx.create(labels);
        label_outlines.setName("label_outlines");
        clijx.detectLabelEdges(labels,outlines);
        clijx.multiplyImages(labels,outlines,label_outlines);
        clijx.release(outlines);
        return label_outlines;
    }

    private ClearCLBuffer[] measure_average_close_neighbor_distance(ClearCLBuffer pointlist, int number_of_spots, ClearCLBuffer labelmap) {

        ClearCLBuffer distance_matrix = clijx.create(new long[]{number_of_spots + 1, number_of_spots + 1});
        distance_matrix.setName("distance_matrix");
        clijx.generateDistanceMatrix(pointlist, pointlist, distance_matrix);

        ClearCLBuffer neighbor_avg_distance_vector = clijx.create(new long[]{distance_matrix.getWidth(), 1, 1});
        neighbor_avg_distance_vector.setName("neighbor_avg_distance_vector");
        clijx.averageDistanceOfClosestPoints(distance_matrix, neighbor_avg_distance_vector, 5);
        ClearCLBuffer neighbor_avg_distance_map = clijx.create(labelmap);
        neighbor_avg_distance_map.setName("neighbor_avg_distance_map");
        clijx.replaceIntensities(labelmap, neighbor_avg_distance_vector, neighbor_avg_distance_map);
        clijx.release(neighbor_avg_distance_vector);

        return new ClearCLBuffer[]{neighbor_avg_distance_map, distance_matrix};
    }


    private ClearCLBuffer[] measure_number_of_neighbors(ClearCLBuffer pointlist, int number_of_spots, ClearCLBuffer labelmap) {
        // determine which labels touch
        ClearCLBuffer touch_matrix = clijx.create(new long[]{number_of_spots + 1, number_of_spots + 1});
        touch_matrix.setName("touch_matrix");
        clijx.generateTouchMatrix(labelmap, touch_matrix);

        ClearCLBuffer neighbor_count_vector = clijx.create(new long[]{touch_matrix.getWidth(), 1, 1});
        neighbor_count_vector.setName("neighbor_count_vector");
        clijx.countTouchingNeighbors(touch_matrix, neighbor_count_vector);
        ClearCLBuffer neighbor_count_map = clijx.create(labelmap);
        neighbor_count_map.setName("neighbor_count_map");
        clijx.replaceIntensities(labelmap, neighbor_count_vector, neighbor_count_map);
        clijx.release(neighbor_count_vector);

        return new ClearCLBuffer[]{neighbor_count_map, touch_matrix};
    }

    private ClearCLBuffer nonzero_min_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[]{stack.getWidth(), stack.getHeight()});
        image2D.setName("NONZERO_MIN_"+stack.getName());
        clijx.projectMinimumThresholdedZBounded(stack,image2D,0,0,stack.getDepth());
        return image2D;
    }

    private ClearCLBuffer max_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[]{stack.getWidth(), stack.getHeight()});
        image2D.setName("MAX_"+stack.getName());
        clijx.maximumZProjection(stack,image2D);
        return image2D;
    }


    private ClearCLBuffer[] arg_max_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[]{stack.getWidth(), stack.getHeight()});
        ClearCLBuffer argImage2D = clijx.create(new long[]{stack.getWidth(), stack.getHeight()});
        image2D.setName("MAX_"+stack.getName());
        argImage2D.setName("ARGMAX_"+stack.getName());
        clijx.argMaximumZProjection(stack,image2D,argImage2D);
        return new ClearCLBuffer[]{image2D,argImage2D};
    }

    private ClearCLBuffer mean_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[] {stack.getWidth(),stack.getHeight()});
        image2D.setName("MEAN_" + stack.getName());
        clijx.meanZProjection(stack,image2D);
        return image2D;
    }

    public static void main(String ... arg) {
        new ImageJ();

        String sourceFolder = "C:/structure/data/2019-12-17-16-54-37-81-Lund_Tribolium_nGFP_TMR/";
        String datasetFolder = "C0opticsprefused";

        ClearControlDataSet dataSet = ClearControlDataSetOpener.open(sourceFolder, datasetFolder);

        new MeshMeasurements(dataSet).setFirstFrame(1000).setLastFrame(1001).run();
    }

}
