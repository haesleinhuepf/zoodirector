package net.haesleinhuepf.imagej.gui.spimcat.viewer;

import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.plugin.frame.Channels;
import ij.process.ImageProcessor;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.imagej.zoo.data.VirtualTifStack;
import net.haesleinhuepf.imagej.zoo.measurement.MeshMeasurements;

public class VirtualMeshMeasurementStack extends ij.VirtualStack {

    private final CLIJx clijx;
    private final MeshMeasurements mm;
    private final int channels;
    private String[] availableChannels;
    private final int depth;
    private final int frames;
    private ImagePlus viewer;


    public VirtualMeshMeasurementStack(CLIJx clijx, MeshMeasurements mm, int depth, int frames) {
        this.clijx = clijx;
        this.mm = mm;
        availableChannels = mm.getResultIDs();
        this.channels = availableChannels.length;
        this.depth = depth;
        this.frames = frames;
    }

    @Override
    public ImageProcessor getProcessor(int n) {
        n--;

        //System.out.println("Retrieving " + n);

        //VirtualStack

        int channelIndex = n % channels;
        int slice = (n / channels) % depth;
        int frame = (n / channels / depth) % frames;

        //System.out.println("Retrieving " + channelIndex + "/" + slice + "/" + frame);

        synchronized (mm) {
            ClearCLImageInterface image = mm.getResult(frame, availableChannels[channelIndex]);

            ClearCLBuffer imageSlice = clijx.create(image.getWidth(), image.getHeight());

            if (image.getDepth() > 0 && image.getDimension() > 2) {
                clijx.copySlice(image, imageSlice, slice);
            } else {
                clijx.copy(image, imageSlice);
            }

            ImagePlus imp = clijx.pull(imageSlice);
            imageSlice.close();
            return imp.getProcessor();
        }
        //setDisplayMode("grayscale");


        //IJ.run(imp, viewer.getBitDepth() + "-bit", "");

        //double displayMin = viewer.getDisplayRangeMin();
        //double displayMax = viewer.getDisplayRangeMax();

        //System.out.println("Viewer " + viewer.getBitDepth());
        //System.out.println("Imp " + imp.getBitDepth());

        //try {
        //    viewer.setProcessor(imp.getProcessor());
        //} catch (Exception e) {
        //    System.out.println(e.getStackTrace());
        //}
        /*if (channel.compareTo(formerChannel) != 0) {
            if (channel.toLowerCase().contains("label")) {
               IJ.resetMinAndMax(viewer);
                try {
                    IJ.run(viewer, "glasbey", "");
                } catch (Exception e) { // thrown from ImageJ / IDE
                    IJ.run(viewer, "Fire", "");
                }
            } else {
                IJ.run(imp, "Enhance Contrast", "saturated=0.35");
            }
        } else if (formerFrame != frame || formerSlice != slice) {
            viewer.setDisplayRange(displayMin, displayMax);
        }*/
    }


    @Override
    public int size() {
        return channels * frames * depth;
    }

    @Override
    public int getSize() {
        return channels * frames * depth;
    }
}
