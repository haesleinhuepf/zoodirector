package net.haesleinhuepf.imagej.gui.spimcat.viewer;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.plugin.RoiEnlarger;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.weka.gui.CLIJxWekaObjectClassification;
import net.haesleinhuepf.imagej.zoo.measurement.MeshMeasurements;

import java.awt.*;
import java.util.ArrayList;

public class VirtualMeshMeasurementStack extends ij.VirtualStack {

    private final CLIJx clijx;
    private final MeshMeasurements mm;
    private final int channels;
    private String[] availableChannels;
    private final int depth;
    private final int frames;
    private boolean drawOutlines = true;

    public boolean isDrawOutlines() {
        return drawOutlines;
    }

    public void setDrawOutlines(boolean drawOutlines) {
        this.drawOutlines = drawOutlines;
    }

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

        System.out.println("<=CZT " + channelIndex + " " + slice + " " + frame);

        //System.out.println("Retrieving " + channelIndex + "/" + slice + "/" + frame);

        synchronized (mm) {
            overlay = new Overlay();
            TextRoi text = new TextRoi(0, 0,
                    mm.getDataSetName() + "\n" +
                            availableChannels[channelIndex] + "\n" +
                            mm.getHumanReadableTime(frame) + "(Frame " + frame + ")\n" +
                            "Count: " + mm.getSpotCount(),
                    new Font("Arial", 0, 12)
            );

            text.setStrokeColor(Color.WHITE);
            overlay.add(text);

            double textpos = text.getFloatHeight();

            if (mm.isTrained()) {

                RoiManager rm = RoiManager.getInstance();

                mm.processFrameForRequestedResult(null, null, mm.getProcessedFrame(), "");
                ClearCLBuffer labelmap = null;
                if (!mm.isAutoContextClassification()) {
                    labelmap = (ClearCLBuffer) mm.getResult(mm.getProcessedFrame(), "07_max_labelled_cells_classification");
                } else {
                    labelmap = (ClearCLBuffer) mm.getResult(mm.getProcessedFrame(), "07_max_labelled_cells_classification_autocontext");
                }
                if (labelmap == null) {
                    System.out.println("Error: No labelmap found!");
                } else {
                    ArrayList<Roi> rois = new ArrayList<Roi>();
                    System.out.println("Labelmap " + labelmap);
                    System.out.println("rois " + rois);
                    clijx.pullLabelsToROIList(labelmap, rois);

                    for (int i = 0; i < rois.size(); i++) {
                        Roi roi = rois.get(i);
                        roi = RoiEnlarger.enlarge(roi, -1);
                        ImageStatistics stats = roi.getStatistics();
                        int roiX = (int) stats.xCentroid;
                        int roiY = (int) stats.yCentroid;
                        Color color = CLIJxWekaObjectClassification.getColor(i + 1);


                        if (rm != null) {
                            Roi rmRoi = null;
                            for (int j = 0; j  <rm.getCount(); j++) {
                                Roi aRoi = rm.getRoi(j);
                                String[] temp = aRoi.getName().split(" ");
                                if (temp.length > 1 && Integer.parseInt(temp[0]) == i + 1) {
                                    rmRoi = aRoi;
                                    break;
                                }
                            }

                            if (rmRoi != null) {
                                color = rmRoi.getStrokeColor();
                                //roi.setName(rm.getRoi(i).getName());
                                TextRoi textRoi = new TextRoi(roiX, roiY, rmRoi.getName().split(" ")[1]);
                                textRoi.setStrokeColor(color);
                                overlay.add(textRoi);

                                text = new TextRoi(0, textpos,
                                        rmRoi.getName().split(" ")[1],
                                        new Font("Arial", 0, 12)
                                );
                                text.setStrokeColor(color);
                                overlay.add(text);

                                textpos += text.getFloatHeight();
                            }
                        }


                        roi.setStrokeColor(color);
                        if (drawOutlines) {
                            overlay.add(roi);
                        }

                        //stats.
                        //EllipseFitter ef = new EllipseFitter();
                        //ef.fit(viewer.getProcessor(), null);
                        //stats.drawEllipse();

                        //int roiWidth = (int) Math.sqrt(stats.area);
                        //int roiHeight = (int) Math.sqrt(stats.area);

                        //roi = new OvalRoi(roiX - roiWidth / 2, roiY - roiHeight / 2, roiWidth, roiHeight);
                        //roi.setStrokeColor(CLIJxWekaObjectClassification.getColor(i + 1));
                        //overlay.add(roi);





                    }
                    //viewer.killRoi();
                }
            }




            ClearCLImageInterface image = mm.getResult(frame, availableChannels[channelIndex]);
            if (image == null) {
                return new FloatProcessor(getWidth(), getHeight());
            }
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

    Overlay overlay;
    public Overlay getOverlay() {
        return overlay;
    }
}
