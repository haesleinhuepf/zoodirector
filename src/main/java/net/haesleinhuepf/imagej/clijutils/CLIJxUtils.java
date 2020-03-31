package net.haesleinhuepf.imagej.clijutils;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clijx.CLIJx;

public class CLIJxUtils {
    public static CLIJx clijx = null;

    public static ClearCLBuffer measureAverageDistanceOfTouchingNeighbors(ClearCLBuffer touch_matrix, ClearCLBuffer distance_matrix) {
        ClearCLBuffer distanceVector = clijx.create(new long[]{touch_matrix.getWidth(), 1, 1}, clijx.Float);
        clijx.averageDistanceOfTouchingNeighbors(distance_matrix, touch_matrix, distanceVector);
        return distanceVector;
    }

    public static ClearCLBuffer measureAverageDistanceOfNClosestNeighbors(ClearCLBuffer distance_matrix, int n) {
        ClearCLBuffer distanceVector = clijx.create(new long[]{distance_matrix.getWidth(), 1, 1}, clijx.Float);
        clijx.averageDistanceOfNClosestPoints(distance_matrix, distanceVector, n);
        return distanceVector;
    }

    public static ClearCLBuffer measureAverageSurfaceAngle(ClearCLBuffer pointlist, ClearCLBuffer touch_matrix) {
        ClearCLBuffer distanceVector = clijx.create(new long[]{touch_matrix.getWidth(), 1, 1}, clijx.Float);
        clijx.averageAngleBetweenAdjacentTriangles(pointlist, touch_matrix, distanceVector);
        return distanceVector;

        /*
                ClearCLBuffer touch_matrix2 = fillTouchMatrixCompletely(touch_matrix);

        clijx.show(touch_matrix, "full_matrix");


        ClearCLBuffer matrix2 = clijx.create(touch_matrix2);
        ClearCLBuffer matrix3 = clijx.create(touch_matrix2);

        clijx.multiplyMatrix(touch_matrix2, touch_matrix2, matrix2);

        clijx.show(matrix2, "matrix2");

        clijx.multiplyMatrix(matrix2, touch_matrix2, matrix3);
        //clijx.show(matrix3, "matrix3");

        clijx.show(matrix3, "cubed");


        clijx.setWhereXgreaterThanY(matrix3, 0);
        clijx.setWhereXsmallerThanY(matrix3, 0);

        clijx.show(matrix3, "diagonale");

        clijx.multiplyImageAndScalar(matrix3, matrix2, 1.0 / 6.0);

        ClearCLBuffer distanceVector = clijx.create(new long[]{touch_matrix.getWidth(), 1, 1}, clijx.Float);
        clijx.maximumYProjection(matrix2, distanceVector);
        //clijx.averageAngleBetweenAdjacentTriangles(pointlist, touch_matrix, distanceVector);

        clijx.show(distanceVector, "dist v");

        new WaitForUserDialog("ha").show();


        clijx.release(matrix2);
        clijx.release(matrix3);
        //clijx.release(touch_matrix2);

        clijx.setWhereXequalsY(touch_matrix, 0);
        clijx.setWhereXgreaterThanY(touch_matrix, 0);

        return distanceVector;

        */
    }

    public static ClearCLBuffer distanceMatrixToMesh(ClearCLBuffer inputImage, ClearCLBuffer pointlist, ClearCLBuffer distance_matrix) {

        ClearCLBuffer mesh = clijx.create(inputImage);
        mesh.setName("mesh");

        ClearCLBuffer closestSpotIndices = clijx.create(new long[]{pointlist.getWidth(), 10});
        clijx.nClosestPoints(distance_matrix,closestSpotIndices);
        clijx.pointIndexListToMesh(pointlist,closestSpotIndices,mesh);
        closestSpotIndices.close();
        return mesh;
    }


    public static ClearCLBuffer labelSpots(ClearCLBuffer detected_spots) {
        ClearCLBuffer cca_result = clijx.create(detected_spots.getDimensions(), clijx.Float);
        clijx.stopWatch("");
        //clijx.connectedComponentsLabeling(detected_spots, cca_result);
        //clijx.stopWatch("CCA");
        clijx.labelSpots(detected_spots, cca_result);
        clijx.stopWatch("LS");
        return cca_result;
    }


    public static ClearCLBuffer label_outlines(ClearCLBuffer labels) {
        ClearCLBuffer outlines = clijx.create(labels);
        outlines.setName("outlines");
        ClearCLBuffer label_outlines = clijx.create(labels);
        label_outlines.setName("label_outlines");
        clijx.detectLabelEdges(labels,outlines);
        clijx.multiplyImages(labels,outlines,label_outlines);
        clijx.release(outlines);
        return label_outlines;
    }

    public static ClearCLBuffer generateDistanceMatrix(ClearCLBuffer pointlist, int number_of_spots, double zoomFactor) {

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


    public static ClearCLBuffer generateTouchMatrix(ClearCLBuffer pointlist, int number_of_spots, ClearCLBuffer labelmap) {
        // determine which labels touch
        ClearCLBuffer touch_matrix = clijx.create(new long[]{number_of_spots + 1, number_of_spots + 1}, clijx.UnsignedByte);
        touch_matrix.setName("touch_matrix");
        clijx.generateTouchMatrix(labelmap, touch_matrix);
        return touch_matrix;
    }

    public static ClearCLBuffer fillTouchMatrixCompletely(ClearCLBuffer touch_matrix) {
        ClearCLBuffer matrix1 = clijx.create(touch_matrix);
        ClearCLBuffer matrix2 = clijx.create(touch_matrix);

        clijx.copy(touch_matrix, matrix1);
        clijx.transposeXY(touch_matrix, matrix2);
        clijx.addImages(matrix1, matrix2, touch_matrix);

        clijx.release(matrix1);
        clijx.release(matrix2);

        return touch_matrix;
    }

    public static ClearCLBuffer nonzero_min_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[]{stack.getWidth(), stack.getHeight()});
        image2D.setName("NONZERO_MIN_"+stack.getName());
        clijx.minimumZProjectionThresholdedBounded(stack,image2D,0,0,stack.getDepth());
        return image2D;
    }

    public static ClearCLBuffer max_x_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[]{stack.getDepth(), stack.getHeight()});
        image2D.setName("MAX_x_" + stack.getName());
        clijx.maximumXProjection(stack,image2D);
        return image2D;
    }

    public static ClearCLBuffer max_y_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[]{stack.getWidth(), stack.getDepth()});
        image2D.setName("MAX_y_" + stack.getName());
        clijx.maximumYProjection(stack,image2D);
        return image2D;
    }

    public static ClearCLBuffer max_z_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[]{stack.getWidth(), stack.getHeight()});
        image2D.setName("MAX_z_" + stack.getName());
        clijx.maximumZProjection(stack,image2D);
        return image2D;
    }

    public static ClearCLBuffer[] arg_max_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[]{stack.getWidth(), stack.getHeight()});
        ClearCLBuffer argImage2D = clijx.create(new long[]{stack.getWidth(), stack.getHeight()});
        image2D.setName("MAX_"+stack.getName());
        argImage2D.setName("ARGMAX_"+stack.getName());
        clijx.argMaximumZProjection(stack,image2D,argImage2D);
        return new ClearCLBuffer[]{image2D,argImage2D};
    }

    public static ClearCLBuffer mean_projection(ClearCLBuffer stack) {
        ClearCLBuffer image2D = clijx.create(new long[] {stack.getWidth(),stack.getHeight()});
        image2D.setName("MEAN_" + stack.getName());
        clijx.meanZProjection(stack,image2D);
        return image2D;
    }


    public static String humanReadableTime(int totalSecs) {
        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
