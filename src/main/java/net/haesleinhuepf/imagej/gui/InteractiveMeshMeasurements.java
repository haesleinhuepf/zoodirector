package net.haesleinhuepf.imagej.gui;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Toolbar;
import ij.plugin.HyperStackConverter;
import ij.plugin.tool.PlugInTool;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.weka.gui.InteractivePanelPlugin;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;
import net.haesleinhuepf.imagej.zoo.measurement.MeshMeasurements;

import java.awt.*;
import java.awt.event.*;

public class InteractiveMeshMeasurements extends InteractivePanelPlugin{

    MeshMeasurements mm;
    ImagePlus viewer;

    CLIJx clijx;

    public InteractiveMeshMeasurements(ClearControlDataSet dataSet) {
        clijx = CLIJx.getInstance();
        clijx.setWaitForKernelFinish(true);

        mm = new MeshMeasurements(dataSet, clijx);


        mm.setZoomFactor(1);
        mm.setBlurSigma(2);
        mm.setNumberDoubleErosionsForPseudoCellSegmentation(4);
        mm.setNumberDoubleDilationsForPseudoCellSegmentation(12);


        mm.setStoreMeasurements(false);
        mm.setProjectionVisualisationOnScreen(false);
        mm.setThreshold(300);
        mm.setShowTableOnScreen(false);
        mm.setProjectionVisualisationToDisc(false);
        mm.setExportMesh(false);


        mm.processFrameForRequestedResult(null, null, 0, "");

        int frame = 0;

        String defaultResult = mm.getResultIDs()[0];
        for (String id : mm.getResultIDs()) {
            if (id.startsWith("VOL_")) {
                defaultResult = id;
                break;
            }
        }
        ClearCLImageInterface defaultView = mm.getResult(frame, defaultResult);

        ImagePlus imp = clijx.pull(defaultView);
        IJ.run(imp, "32-bit", "");

        ImageStack stack = new ImageStack(imp.getWidth(), imp.getHeight());
        for (int i = 0; i < imp.getNSlices() * dataSet.getTimesInSeconds().length; i++) {
            stack.addSlice(imp.getProcessor());
        }
        viewer = new ImagePlus("SPIMcat " + dataSet.getName(), stack);
        viewer = HyperStackConverter.toHyperStack(viewer, 1, imp.getNSlices(), dataSet.getTimesInSeconds().length);
        viewer.show();

        attach(viewer.getWindow());

        Toolbar.addPlugInTool(new MouseHandler());


        buildGUI();
        //        new MeshMeasurements(dataSet).
        //                //setCLIJx(CLIJx.getInstance("2070")).
        //                        setProjectionVisualisationToDisc(false).
        //                setProjectionVisualisationOnScreen(true).
        //                setExportMesh(false).
        //                setThreshold(300).
        //                setStoreMeasurements(true).
        //                //setEliminateOnSurfaceCells(true).
        //                //setBlurSigma(1).
        //                //setEliminateSubSurfaceCells(true).
        //                /*setCut(
        //                        98,
        //                        482,
        //                        10,
        //                        170,
        //                        30,
        //                        275,
        //                        50,
        //                        dataSet.getTimesInSeconds()[1050],
        //                        0.9
        //                ).*/
        //                        setFirstFrame(startFrame).
        //                //setLastFrame(endFrame).
        ////                setFirstFrame(startFrame).
        //                //              setFrameStep(100).
        //                //            setLastFrame(endFrame).
        //                        run();


        dataSet.registerImagePlus(viewer);
    }

    private class MouseHandler extends PlugInTool {

        public MouseHandler(){}

        double mouseStartX;
        double mouseStartY;
        double angleStartX;
        double angleStartY;

        @Override
        public void mousePressed(ImagePlus imp, MouseEvent e) {
            if (imp == viewer) {
                angleStartX = mm.getRotationX();
                angleStartY = mm.getRotationY();
                mouseStartX = e.getX();
                mouseStartY = e.getY();
            }
        }

        @Override
        public void mouseDragged(ImagePlus imp, MouseEvent e) {
            if (imp == viewer) {

                double deltaX = e.getX() - mouseStartX;
                double deltaY = e.getY() - mouseStartY;

                mm.setRotationX(angleStartY - deltaX / 5);
                mm.setRotationY(angleStartX + deltaY / 5);

                mm.invalidateTransformed();
                formerFrame = -1;
                refresh();
                //System.out.println("Refreshing...");
            }
        }

    }

    Choice channelPullDown;
    TextField frameTextField;
    private void buildGUI() {

        guiPanel.add(new Label("Channel"));

        channelPullDown = new Choice();
        for (String channelName : mm.getResultIDs()) {
            channelPullDown.add(channelName);
        }
        channelPullDown.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                refresh();
            }
        });
/*        channelPullDown.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                refresh();
            }
        });

 */
        guiPanel.add(channelPullDown);

        guiPanel.add(new Label("Frame"));


        frameTextField = new TextField();
        frameTextField.setText("0");
        frameTextField.addTextListener(new TextListener() {
            @Override
            public void textValueChanged(TextEvent e) {
                if (frameTextField.getText().length() > 0) {
                    refresh();
                }
            }
        });
        guiPanel.add(frameTextField);

    }


    boolean refreshing = false;
    int formerFrame = -1;
    String formerChannel = "";
    int formerSlice = -1;

    @Override
    protected synchronized void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        int frame = viewer.getFrame(); //Integer.parseInt(frameTextField.getText());
        String channel = channelPullDown.getSelectedItem();
        int slice = viewer.getSlice() - 1;

        System.out.println("Refreshing...");

        if (channel.compareTo(formerChannel) != 0 || frame != formerFrame || formerSlice != slice) {
            ClearCLImageInterface image = mm.getResult(frame, channel);
            ClearCLBuffer imageSlice = clijx.create(image.getWidth(), image.getHeight());

            if (image.getDepth() > 0 && image.getDimension() > 2) {
                clijx.copySlice(image, imageSlice, slice);
            } else {
                clijx.copy(image, imageSlice);
            }

            ImagePlus imp = clijx.pull(imageSlice);
            imageSlice.close();
            //IJ.run(imp, viewer.getBitDepth() + "-bit", "");

            double displayMin = viewer.getDisplayRangeMin();
            double displayMax = viewer.getDisplayRangeMax();

            System.out.println("Viewer " + viewer.getBitDepth());
            System.out.println("Imp " + imp.getBitDepth());

            try {
                viewer.setProcessor(imp.getProcessor());
            } catch (Exception e) {
                System.out.println(e.getStackTrace());
            }
            if (channel.compareTo(formerChannel) != 0) {
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
            }
            viewer.updateAndDraw();

            formerChannel = channel;
            formerFrame = frame;
            formerSlice = slice;
        } else {
            System.out.println("" + System.currentTimeMillis() + " nothing changed");
        }




        super.refresh();
        refreshing = false;
    }

    protected void mouseUp(MouseEvent e) {
        refresh();
    }

    protected void imageChanged() {
        refresh();
    }

    public static void main(String[] args) {
        new ImageJ();

        String sourceFolder = "C:/structure/data/2019-12-17-16-54-37-81-Lund_Tribolium_nGFP_TMR/";
        //String sourceFolder = "C:/structure/data/2019-10-28-17-22-59-23-Finsterwalde_Tribolium_nGFP/";
        //String datasetFolder = "opticsprefused";
        String datasetFolder = "C0opticsprefused";

        ClearControlDataSet dataSet = ClearControlDataSetOpener.open(sourceFolder, datasetFolder);

        new InteractiveMeshMeasurements(dataSet);
    }

}
