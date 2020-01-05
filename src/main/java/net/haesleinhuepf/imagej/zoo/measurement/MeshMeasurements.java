package net.haesleinhuepf.imagej.zoo.measurement;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.advancedmath.EqualConstant;
import net.haesleinhuepf.imagej.zoo.ZooUtilities;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;
import org.scijava.util.VersionUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MeshMeasurements extends DataSetMeasurements {
    CLIJx clijx;

    double zoomFactor = 1.5; // -> each analysed voxel is 1.5x1.5x1.5 microns large`


    double blurSigma = 3;

    boolean projection_visualisation_on_screen = true;
    boolean projection_visualisation_to_disc = true;

    int maximumDistanceToMeshPoints = 100;

    int numberDoubleErosionsForPseudoCellSegmentation = 7;
    int numberDoubleDilationsForPseudoCellSegmentation = 17;
    //private String thresholdAlgorithm = "Triangle";
    private double threshold = 250;
    private boolean exportMesh = false;

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

        String now = ZooUtilities.now();

        String analysisLog = "Analysis log \n" +
                now + "\n" +
                "Dataset path: " + dataSet.getPath() + "\n" +
                "Dataset name: " + dataSet.getName() + "\n" +
                "Dataset shortname: " + dataSet.getShortName() + "\n" +

                "CLIJx GPU name: " + clijx.getGPUName() + "\n" +
                "CLIJx OpenCL version: " + clijx.getOpenCLVersion() + "\n" +
                "CLIJx mvn version: " + VersionUtils.getVersion(CLIJx.class) + "\n" +

                "MeshMeasurements version: " + VersionUtils.getVersion(MeshMeasurements.class) + "\n" +
                "zoomFactor: " + zoomFactor + "\n" +
                "blurSigma: " + blurSigma  + "\n" +
                "projection_visualisation_on_screen: " + projection_visualisation_on_screen  + "\n" +
                "projection_visualisation_to_disc: " + projection_visualisation_to_disc  + "\n" +
                "maximumDistanceToMeshPoints: " + maximumDistanceToMeshPoints  + "\n" +
                "numberDoubleErosionsForPseudoCellSegmentation: " + numberDoubleErosionsForPseudoCellSegmentation  + "\n" +
                "numberDoubleDilationsForPseudoCellSegmentation: " + numberDoubleDilationsForPseudoCellSegmentation  + "\n" +
                "threshold: " + threshold  + "\n";

        try {
            Files.write(Paths.get(outputFolder + now + "_meshmeasurementlog.txt"), analysisLog.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }


        ResultsTable meshMeasurementTable = new ResultsTable();

        GenericDialog cancelDialog = new GenericDialog("Analysis running...");
        cancelDialog.addMessage("CLick on cancel to cancel.");
        cancelDialog.setModal(false);
        cancelDialog.show();

        for (int f = firstFrame; f <= lastFrame; f+=frameStep) {
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

            // label spots
            ClearCLBuffer labelled_spots = labelSpots(detected_spots);
            clijx.stopWatch("spot labelling (CCA)");

		    // cell segmentation
            ClearCLBuffer segmented_cells = pseudo_cell_segmentation(labelled_spots);
            clijx.stopWatch("cell segmentation");

            ClearCLBuffer max_membranes = null;
            ClearCLBuffer mean_membranes = null;
            ClearCLBuffer nonzero_min_membranes = null;

            if (projection_visualisation_on_screen || projection_visualisation_to_disc) {
                // cell outlines
                ClearCLBuffer cell_outlines = label_outlines(segmented_cells);
                clijx.stopWatch("outline labels");

                max_membranes = max_z_projection(cell_outlines);
                mean_membranes = mean_projection(cell_outlines);
                nonzero_min_membranes = nonzero_min_projection(cell_outlines);

                clijx.release(cell_outlines);
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
            //clijx.spotsToPointList(detected_spots, pointlist);
            //clijx.stopWatch("pointlist");
            clijx.labelledSpotsToPointList(labelled_spots, pointlist);
            clijx.stopWatch("labels to pointlist");
            //clijx.show(pointlist, "pli");

            // -----------------------------------------------------------------------
            // neighborhood analysis
            ClearCLBuffer distance_matrix = generateDistanceMatrix(pointlist, number_of_spots);

            //clijx.show(distance_matrix, "dist");

            clijx.stopWatch("distance map");



            ClearCLBuffer[] result;

            ClearCLBuffer touch_matrix = generateTouchMatrix(pointlist, number_of_spots, segmented_cells);
            clijx.setColumn(touch_matrix, 0, 0);
            //clijx.show(touch_matrix, "touch");

            ClearCLBuffer distance_vector = measureAverageDistanceOfTouchingNeighbors(touch_matrix, distance_matrix);

            ClearCLBuffer average_distance_of_touching_neighbors = visualiseAverageDistanceOfTouchingNeighbors(distance_vector, segmented_cells);
            //clijx.show(averag_distance_of_touching_neighbors, "averag_distance_of_touching_neighbors");

            //clijx.showGrey(touch_matrix, "touch_matrix");
            //clijx.showGrey(distance_matrix, "distance_matrix");
            clijx.stopWatch("touch map");
            ClearCLBuffer relevantDistances = clijx.create(distance_matrix.getDimensions(), clijx.Float);
            clijx.multiplyImages(distance_matrix, touch_matrix, relevantDistances);

            double meanDistance = clijx.meanOfPixelsAboveThreshold(relevantDistances, 0);
            double varianceDistance = clijx.varianceOfMaskedPixels(relevantDistances, touch_matrix, meanDistance);
            meshMeasurementTable.addValue("mean_neighbor_distance", meanDistance);
            meshMeasurementTable.addValue("variance_neighbor_distance", varianceDistance);
            double numberOfTouches = clijx.sumOfAllPixels(touch_matrix);
            meshMeasurementTable.addValue("number_of_touches", numberOfTouches);

            ClearCLBuffer mesh = clijx.create(inputImage);
            mesh.setName("mesh");
            clijx.touchMatrixToMesh(pointlist, touch_matrix, mesh);
            clijx.stopWatch("mesh");



            double meanDistance2 = clijx.meanOfMaskedPixels(average_distance_of_touching_neighbors, detected_spots);
            double varianceDistance2 = clijx.varianceOfMaskedPixels(average_distance_of_touching_neighbors, detected_spots, meanDistance);
            meshMeasurementTable.addValue("mean_neighbor_distance2", meanDistance2);
            meshMeasurementTable.addValue("variance_neighbor_distance2", varianceDistance2);

            ClearCLBuffer max_avg_dist = null;
            ClearCLBuffer mean_avg_dist = null;
            ClearCLBuffer nonzero_min_avg_dist = null;
            if (projection_visualisation_on_screen || projection_visualisation_to_disc) {
                max_avg_dist = max_z_projection(average_distance_of_touching_neighbors);
                mean_avg_dist = mean_projection(average_distance_of_touching_neighbors);
                nonzero_min_avg_dist = nonzero_min_projection(average_distance_of_touching_neighbors);
            }
            clijx.release(average_distance_of_touching_neighbors);

            //
//            ClearCLBuffer minimum_distance_of_touching_neighbors = measureMinimumDistanceOfTouchingNeighbors(touch_matrix, distance_matrix, segmented_cells);
//
//
//            double meanMinDistance = clijx.meanOfMaskedPixels(minimum_distance_of_touching_neighbors, detected_spots);
//            double varianceMinDistance = clijx.varianceOfMaskedPixels(minimum_distance_of_touching_neighbors, detected_spots, meanDistance);
//            meshMeasurementTable.addValue("mean_min_neighbor_distance2", meanDistance2);
//            meshMeasurementTable.addValue("variance_neighbor_distance2", varianceDistance2);
//
//            ClearCLBuffer max_min_dist = null;
//            ClearCLBuffer mean_min_dist = null;
//            ClearCLBuffer nonzero_min_min_dist = null;
//            if (projection_visualisation_on_screen || projection_visualisation_to_disc) {
//                max_min_dist = max_z_projection(minimum_distance_of_touching_neighbors);
//                mean_min_dist = mean_projection(minimum_distance_of_touching_neighbors);
//                nonzero_min_min_dist = nonzero_min_projection(minimum_distance_of_touching_neighbors);
//            }


            // -----------------------------------------------------------------------
            // Visualisation / output
            if (exportMesh) {
                if (number_of_spots > 1 && numberOfTouches > 0) {
                    clijx.writeVTKLineListToDisc(pointlist, touch_matrix, outputFolder + "_vtk_mesh/" + filename.replace(".raw", "") + ".vtk");
                    clijx.writeXYZPointListToDisc(pointlist, outputFolder + "_vtk_mesh/" + filename.replace(".raw", "") + ".xyz");
                }
            }

            if (projection_visualisation_on_screen || projection_visualisation_to_disc) {

                // save maximum and average projections to disc
                result = arg_max_projection(inputImage);
                ClearCLBuffer max_image = result[0];
                ClearCLBuffer arg_max_image = result[1];

                ClearCLBuffer max_spots = max_z_projection(detected_spots);

                result = arg_max_projection(segmented_cells);
                ClearCLBuffer max_cells = result[0];
                ClearCLBuffer arg_max_cells = result[1];


                ClearCLBuffer max_mesh_x = max_x_projection(mesh);
                ClearCLBuffer max_mesh_y = max_y_projection(mesh);
                ClearCLBuffer max_mesh_z = max_z_projection(mesh);


                if (projection_visualisation_to_disc) {
                    clijx.saveAsTIF(max_image, outputFolder + "_max_image/" + filename + ".tif");
                    clijx.saveAsTIF(arg_max_image, outputFolder + "_arg_max_image/" + filename + ".tif");
                    clijx.saveAsTIF(max_spots, outputFolder + "_max_spots/" + filename + ".tif");
                    clijx.saveAsTIF(max_cells, outputFolder + "_max_cells/" + filename + ".tif");
                    clijx.saveAsTIF(arg_max_cells, outputFolder + "_arg_max_cells/" + filename + ".tif");
                    clijx.saveAsTIF(mean_membranes, outputFolder + "_mean_membranes/" + filename + ".tif");
                    clijx.saveAsTIF(max_membranes, outputFolder + "_max_membranes/" + filename + ".tif");
                    clijx.saveAsTIF(max_avg_dist, outputFolder + "_max_avg_dist/" + filename + ".tif");
                    clijx.saveAsTIF(mean_avg_dist, outputFolder + "_mean_avg_dist/" + filename + ".tif");
                    clijx.saveAsTIF(max_mesh_x, outputFolder + "_max_mesh_x/" + filename + ".tif");
                    clijx.saveAsTIF(max_mesh_y, outputFolder + "_max_mesh_y/" + filename + ".tif");
                    clijx.saveAsTIF(max_mesh_z, outputFolder + "_max_mesh_z/" + filename + ".tif");
                    clijx.saveAsTIF(nonzero_min_membranes, outputFolder + "_nonzero_min_membranes/" + filename + ".tif");
                    clijx.saveAsTIF(nonzero_min_avg_dist, outputFolder + "_nonzero_min_avg_dist/" + filename + ".tif");

//                    clijx.saveAsTIF(max_min_dist, outputFolder + "_max_min_dist/" + filename + ".tif");
//                    clijx.saveAsTIF(mean_min_dist, outputFolder + "_mean_min_dist/" + filename + ".tif");
//                    clijx.saveAsTIF(nonzero_min_min_dist, outputFolder + "_nonzero_min_min_dist/" + filename + ".tif");

                }
                clijx.stopWatch("writing to disc");

                if (projection_visualisation_on_screen) {
                    clijx.showGrey(max_image, "_max_image");
                    clijx.showGrey(arg_max_image, "_arg_max_image");
                    clijx.showGrey(max_spots, "_max_spots");
                    clijx.showGrey(max_cells, "_max_cells");
                    clijx.showGrey(arg_max_cells, "_arg_max_cells");
                    clijx.showGrey(mean_membranes, "_mean_membranes");
                    clijx.showGrey(max_membranes, "_max_membranes");

                    clijx.showGrey(max_avg_dist, "_max_avg_dist");
                    clijx.showGrey(mean_avg_dist, "_mean_avg_dist");

                    clijx.showGrey(max_mesh_x, "_max_mesh_x");
                    clijx.showGrey(max_mesh_y, "_max_mesh_y");
                    clijx.showGrey(max_mesh_z, "_max_mesh_z");

                    clijx.showGrey(nonzero_min_membranes, "_nonzero_min_membranes");
                    clijx.showGrey(nonzero_min_avg_dist, "_nonzero_min_avg_dist");


//                    clijx.showGrey(max_min_dist, "_max_min_dist");
//                    clijx.showGrey(mean_min_dist, "_mean_min_dist");
//                    clijx.showGrey(nonzero_min_min_dist, "_nonzero_min_min_dist");

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
            meshMeasurementTable.show("Mesh measurements results");
            dataSet.saveMeasurementTable(meshMeasurementTable, "processed/meshMeasurements.csv");
        }
        cancelDialog.hide();

    }
/*
    public double meanOfMaskedPixels(ClearCLBuffer clImage, ClearCLBuffer mask) {
        ClearCLBuffer tempBinary = clijx.create(clImage);
        // todo: if we can be sure that the mask has really only 0 and 1 pixel values, we can skip this first step:
        ClearCLBuffer tempMultiplied = clijx.create(clImage);
        EqualConstant.equalConstant(clijx, mask, tempBinary, 1f);
        clijx.mask(clImage, tempBinary, tempMultiplied);
        double sum = clijx.sumPixels(tempMultiplied);
        double count = clijx.sumPixels(tempBinary);
        System.out.println("sum " + sum);
        System.out.println("count " + count);
        clijx.release(tempBinary);
        clijx.release(tempMultiplied);
        return sum / count;
    }
*/
    private ClearCLBuffer measureAverageDistanceOfTouchingNeighbors(ClearCLBuffer touch_matrix, ClearCLBuffer distance_matrix) {
        ClearCLBuffer distanceVector = clijx.create(new long[]{touch_matrix.getWidth(), 1, 1}, clijx.Float);
        clijx.averageDistanceOfTouchingNeighbors(distance_matrix, touch_matrix, distanceVector);
        return distanceVector;
    }

    private ClearCLBuffer visualiseAverageDistanceOfTouchingNeighbors(ClearCLBuffer distanceVector,  ClearCLBuffer label_map) {


        //clijx.show(distanceVector, "disvec");


        ClearCLBuffer parametricDistanceImage = clijx.create(label_map.getDimensions(), clijx.Float);
        clijx.replaceIntensities(label_map, distanceVector, parametricDistanceImage);

        //clijx.release(distanceVector);

        return parametricDistanceImage;
    }

    private ClearCLBuffer measureMinimumDistanceOfTouchingNeighbors(ClearCLBuffer touch_matrix, ClearCLBuffer distance_matrix, ClearCLBuffer label_map) {
        ClearCLBuffer distanceVector = clijx.create(new long[]{touch_matrix.getWidth(), 1, 1}, clijx.Float);

        clijx.minimumDistanceOfTouchingNeighbors(distance_matrix, touch_matrix, distanceVector);



        //clijx.show(distanceVector, "disvec");

        ClearCLBuffer parametricDistanceImage = clijx.create(label_map.getDimensions(), clijx.Float);
        clijx.replaceIntensities(label_map, distanceVector, parametricDistanceImage);

        clijx.release(distanceVector);

        return parametricDistanceImage;
    }


    private ClearCLBuffer spot_detection (ClearCLBuffer inputImage) {
        // blur a bit and detect maxima
        ClearCLBuffer blurred = clijx.create(inputImage);
        blurred.setName("blurred");
        ClearCLBuffer thresholded = clijx.create(inputImage);
        thresholded.setName("thresholded");
        ClearCLBuffer detected_spots = clijx.create(inputImage.getDimensions(), clijx.UnsignedByte);
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

    private ClearCLBuffer labelSpots(ClearCLBuffer detected_spots) {
        ClearCLBuffer cca_result = clijx.create(detected_spots.getDimensions(), clijx.Float);
        clijx.stopWatch("");
        //clijx.connectedComponentsLabeling(detected_spots, cca_result);
        //clijx.stopWatch("CCA");
        clijx.labelSpots(detected_spots, cca_result);
        clijx.stopWatch("LS");
        return cca_result;
    }

    private ClearCLBuffer pseudo_cell_segmentation(ClearCLBuffer labelled_spots) {

        ClearCLBuffer tempSpots1 = clijx.create(labelled_spots);
    	tempSpots1.setName("Segmented cells");
    	clijx.copy(labelled_spots, tempSpots1);

        ClearCLBuffer tempSpots2 = clijx.create(labelled_spots);
	    tempSpots2.setName("tempSpots2");
        ClearCLBuffer flag =clijx.create(new long[]{1, 1, 1});
	    flag.setName("flag");

    	for (int j = 0; j < numberDoubleDilationsForPseudoCellSegmentation; j++) {
            clijx.onlyzeroOverwriteMaximumDiamond(tempSpots1, flag, tempSpots2);
            clijx.onlyzeroOverwriteMaximumBox(tempSpots2, flag, tempSpots1);
        }
        clijx.release(flag);

        ClearCLBuffer tempSpots3 = clijx.create(labelled_spots);
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

    private ClearCLBuffer generateDistanceMatrix(ClearCLBuffer pointlist, int number_of_spots) {

        ClearCLBuffer distance_matrix = clijx.create(new long[]{number_of_spots + 1, number_of_spots + 1});
        ClearCLBuffer temp = clijx.create(new long[]{number_of_spots + 1, number_of_spots + 1});
        distance_matrix.setName("distance_matrix");
        clijx.generateDistanceMatrix(pointlist, pointlist, temp);


        // correct measurement to have it in microns
        clijx.activateSizeIndependentKernelCompilation();
        clijx.multiplyImageAndScalar(temp, distance_matrix, 1.0 / zoomFactor);

        //ClearCLBuffer neighbor_avg_distance_vector = clijx.create(new long[]{distance_matrix.getWidth(), 1, 1});
        //neighbor_avg_distance_vector.setName("neighbor_avg_distance_vector");
        //clijx.averageDistanceOfClosestPoints(distance_matrix, neighbor_avg_distance_vector, 5);
        //ClearCLBuffer neighbor_avg_distance_map = clijx.create(labelmap);
        //neighbor_avg_distance_map.setName("neighbor_avg_distance_map");
        //clijx.replaceIntensities(labelmap, neighbor_avg_distance_vector, neighbor_avg_distance_map);
        //clijx.release(neighbor_avg_distance_vector);

        return distance_matrix;
    }


    private ClearCLBuffer generateTouchMatrix(ClearCLBuffer pointlist, int number_of_spots, ClearCLBuffer labelmap) {
        // determine which labels touch
        ClearCLBuffer touch_matrix = clijx.create(new long[]{number_of_spots + 1, number_of_spots + 1});
        touch_matrix.setName("touch_matrix");
        clijx.generateTouchMatrix(labelmap, touch_matrix);
        return touch_matrix;
    }

    private ClearCLBuffer nonzero_min_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[]{stack.getWidth(), stack.getHeight()});
        image2D.setName("NONZERO_MIN_"+stack.getName());
        clijx.projectMinimumThresholdedZBounded(stack,image2D,0,0,stack.getDepth());
        return image2D;
    }

    private ClearCLBuffer max_x_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[]{stack.getDepth(), stack.getHeight()});
        image2D.setName("MAX_x_" + stack.getName());
        clijx.maximumXProjection(stack,image2D);
        return image2D;
    }

    private ClearCLBuffer max_y_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[]{stack.getWidth(), stack.getDepth()});
        image2D.setName("MAX_y_" + stack.getName());
        clijx.maximumYProjection(stack,image2D);
        return image2D;
    }

    private ClearCLBuffer max_z_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[]{stack.getWidth(), stack.getHeight()});
        image2D.setName("MAX_z_" + stack.getName());
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

        CLIJx.getInstance("2060");

        String sourceFolder = "C:/structure/data/2019-12-17-16-54-37-81-Lund_Tribolium_nGFP_TMR/";
        String datasetFolder = "C0opticsprefused";

        ClearControlDataSet dataSet = ClearControlDataSetOpener.open(sourceFolder, datasetFolder);

        int startFrame = 800;
        int endFrame = startFrame + 1000;

        new MeshMeasurements(dataSet).
                setProjectionVisualisationToDisc(false).
                setProjectionVisualisationOnScreen(false).
                setExportMesh(true).
                setThreshold(300).
                setFirstFrame(37).
//                setFirstFrame(startFrame).
  //              setFrameStep(100).
    //            setLastFrame(endFrame).
                run();
    }

    private MeshMeasurements setExportMesh(boolean exportMesh) {
        this.exportMesh = exportMesh;
        return this;
    }
}
