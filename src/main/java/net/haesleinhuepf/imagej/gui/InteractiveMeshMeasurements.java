package net.haesleinhuepf.imagej.gui;

import ij.*;
import ij.gui.*;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.HyperStackConverter;
import ij.plugin.RoiEnlarger;
import ij.plugin.frame.RoiManager;
import ij.plugin.tool.PlugInTool;
import ij.process.*;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.weka.CLIJxWeka2;
import net.haesleinhuepf.clijx.weka.gui.CLIJxWekaObjectClassification;
import net.haesleinhuepf.clijx.weka.gui.InteractivePanelPlugin;
import net.haesleinhuepf.imagej.gui.spimcat.viewer.VirtualMeshMeasurementStack;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;
import net.haesleinhuepf.imagej.zoo.measurement.MeasurementTable;
import net.haesleinhuepf.imagej.zoo.measurement.MeshMeasurements;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;

public class InteractiveMeshMeasurements extends InteractivePanelPlugin{
    VirtualMeshMeasurementStack stack;
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

        mm.setTransposeXY(true);
        mm.setStoreMeasurements(true);
        mm.setProjectionVisualisationOnScreen(false);
        mm.setThreshold(300);
        mm.setShowTableOnScreen(false);
        mm.setProjectionVisualisationToDisc(false);
        mm.setExportMesh(false);

        int frame = 100;

        synchronized (mm) {
            mm.processFrameForRequestedResult(null, null, frame, "");
        }

        String[] channels = configureResultIDs(mm.getResultIDs());
        if (channels == null) {
            return;
        }
        mm.setResultIDs(channels);

        String defaultChannel = mm.getResultIDs()[0];
        for (String id : mm.getResultIDs()) {
            if (id.startsWith("VOL_")) {
                defaultChannel = id;
                break;
            }
        }
        ClearCLImageInterface defaultView = mm.getResult(frame, defaultChannel);

        ImagePlus imp = clijx.pull(defaultView);
        IJ.run(imp, "32-bit", "");

        stack = new VirtualMeshMeasurementStack(clijx, mm, imp.getNSlices(), dataSet.getTimesInSeconds().length);
                //new ImageStack(imp.getWidth(), imp.getHeight());
        //for (int i = 0; i < imp.getNSlices() * dataSet.getTimesInSeconds().length; i++) {
          //  stack.addSlice(imp.getProcessor());
        //}
        viewer = new ImagePlus("SPIMcat " + dataSet.getName(), stack);
        viewer = HyperStackConverter.toHyperStack(viewer, mm.getResultIDs().length, imp.getNSlices(), dataSet.getTimesInSeconds().length);
        if (mm.getResultIDs().length <= CompositeImage.MAX_CHANNELS) {
            viewer = new CompositeImage(viewer, CompositeImage.COMPOSITE);
        }
        viewer.show();

        attach(viewer.getWindow());
        imp.setDisplayMode(IJ.GRAYSCALE);

        Toolbar.addPlugInTool(new MouseHandler());

        viewer.getWindow().getCanvas().disablePopupMenu(true);

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

    private String[] configureResultIDs(String[] resultIDs) {
        ArrayList<String> list = new ArrayList<String>();
        GenericDialog gd = new GenericDialog("Configure channels");
        for (String channel : resultIDs) {
            gd.addCheckbox(channel, true);
        }

        gd.showDialog();
        if (gd.wasCanceled()) {
            return null;
        }

        for (String channel : resultIDs) {
            if(gd.getNextBoolean()) {
                list.add(channel);
            }
        }
        String[] array = new String[list.size()];
        list.toArray(array);
        return array;
    }

    private class MouseHandler extends PlugInTool {

        public MouseHandler(){}

        double mouseStartX;
        double mouseStartY;
        double angleStartX;
        double angleStartY;
        double translationStartX;
        double translationStartY;

        @Override
        public void mousePressed(ImagePlus imp, MouseEvent e) {
            if (imp == viewer) {
                angleStartX = mm.getRotationY();
                angleStartY = mm.getRotationX();
                translationStartX = mm.getTranslationX();
                translationStartY = mm.getTranslationY();
                mouseStartX = e.getX();
                mouseStartY = e.getY();
            }
        }

        @Override
        public void mouseDragged(ImagePlus imp, MouseEvent e) {
            if (imp == viewer) {

                double deltaX = e.getX() - mouseStartX;
                double deltaY = e.getY() - mouseStartY;

                if (e.getButton() == MouseEvent.BUTTON1) {
                    mm.setTranslationX(translationStartX - deltaX / 5);
                    mm.setTranslationY(translationStartY + deltaY / 5);
                } else {
                    mm.setRotationY(angleStartX - deltaX / 5);
                    mm.setRotationX(angleStartY + deltaY / 5);
                }
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
                viewer.setC(channelPullDown.getSelectedIndex() + 1);
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

        {
            Button btnConfig = new Button("Config");
            btnConfig.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    config();
                }
            });
            guiPanel.add(btnConfig);
        }

        {
            Button btnTrain = new Button("Train");
            btnTrain.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    train();
                    formerFrame = -1;
                    mm.invalidate();
                    refresh();
                }
            });
            guiPanel.add(btnTrain);
        }

        {
            Button btnReset = new Button("Reset");
            btnReset.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    formerFrame = -1;
                    mm.invalidateTraining();
                    refresh();
                }
            });
            guiPanel.add(btnReset);
        }

    }

    private void config() {
        GenericDialog gd = new GenericDialog("SPIMcat viewer");

        gd.addNumericField("Threshold", mm.getThreshold(), 2);
        gd.addNumericField("Blur sigma", mm.getBlurSigma(), 2);
        gd.addNumericField("Remove background sigma", mm.getBackgroundBlurSigma(), 2);
        gd.addCheckbox("Remove surface cells", mm.isEliminateOnSurfaceCells());
        gd.addCheckbox("Remove sub sufrac cell", mm.isEliminateSubSurfaceCells());
        gd.addCheckbox("Draw text", mm.isDrawText());

        gd.addSlider("Rotation X", -90, 90, mm.getRotationX(), 5);
        gd.addSlider("Rotation Y", -90, 90, mm.getRotationY(), 5);
        gd.addSlider("Rotation Z", -90, 90, mm.getRotationZ(), 5);
        gd.addSlider("Translation X", -256, 256, mm.getTranslationX(), 32);
        gd.addSlider("Translation Y", -256, 256, mm.getTranslationY(), 32);
        gd.addSlider("Translation Z", -256, 256, mm.getTranslationZ(), 32);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        mm.setThreshold(gd.getNextNumber());
        mm.setBlurSigma(gd.getNextNumber());
        mm.setBackgroundSubtractionSigma(gd.getNextNumber());
        mm.setEliminateOnSurfaceCells(gd.getNextBoolean());
        mm.setEliminateSubSurfaceCells(gd.getNextBoolean());
        mm.setDrawText(gd.getNextBoolean());

        mm.setRotationX(gd.getNextNumber());
        mm.setRotationY(gd.getNextNumber());
        mm.setRotationZ(gd.getNextNumber());
        mm.setTranslationX(gd.getNextNumber());
        mm.setTranslationY(gd.getNextNumber());
        mm.setTranslationZ(gd.getNextNumber());

        mm.invalidate();
        refresh();
    }

    private void train() {
        //Roi roi = viewer.getRoi();
        RoiManager rm = RoiManager.getInstance();
        if (rm == null) {
            System.out.println("No ROI Manager; exiting");
        }

        synchronized (mm) {
            mm.setStoreMeasurements(true);
            ClearCLBuffer clLabelMap = (ClearCLBuffer) mm.getResult(viewer.getFrame(), "07_max_labelled_cells");
            ClearCLBuffer measurements = (ClearCLBuffer) mm.getResult(viewer.getFrame(), "07_measurements");
            //mm.setStoreMeasurements(false); //

            ResultsTable table = new ResultsTable();
            clijx.image2DToResultsTable(measurements, table);

            for (int i = 0; i < rm.getCount(); i++) {
                Roi roi = rm.getRoi(i);
                ArrayList<Integer> labels = getLabelsFromRoi(clijx.pull(clLabelMap), roi);

                for (int label : labels) {
                    table.setValue("CLASS", label, i + 1);
                }
            }

            mm.train(table);

        }
    }

    private ArrayList<Integer> getLabelsFromRoi(ImagePlus imp, Roi roi) {
        ArrayList<Integer> labels = new ArrayList<Integer>();

        FloatPolygon fp = roi.getInterpolatedPolygon(1, false);
        ImageProcessor ip = imp.getProcessor();
        for (int i = 0; i < fp.npoints; i++) {
            if (fp.xpoints.length > i && fp.ypoints.length > i) {

                Integer label = (int) ip.getf((int) fp.xpoints[i], (int) fp.ypoints[i]);
                labels.add(label);

            }
        }

        return labels;
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

            //stack.setChannel(channel);
            //if (channel.compareTo(formerChannel) != 0) {
            //    viewer.setC(channelPullDown.getSelectedIndex() + 1);
            System.out.println("rot: " + mm.getRotationX());
            //viewer.setC(viewer.getC());
            //viewer.updateImage();
            //viewer.updateAndDraw();
            double displayMin = viewer.getDisplayRangeMin();
            double displayMax = viewer.getDisplayRangeMax();
            LUT lut = viewer.getProcessor().getLut();

            viewer.setProcessor(stack.getProcessor(viewer.getC() +
                    viewer.getZ() *  viewer.getNChannels() +
                    viewer.getT() * viewer.getNSlices() * viewer.getNChannels()
                    ));

            viewer.getProcessor().setLut(lut);
            viewer.setDisplayRange(displayMin, displayMax);
            //}

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
        Overlay overlay = new Overlay();
        if (mm.isTrained()) {
            RoiManager rm = RoiManager.getInstance();

            ClearCLBuffer labelmap = (ClearCLBuffer) mm.processFrameForRequestedResult(null, null, mm.getProcessedFrame(), "07_max_labelled_cells_classification");
            // = (ClearCLBuffer) mm.getResult(mm.getProcessedFrame(), "07_max_labelled_cells_classification");
            ArrayList<Roi> rois = new ArrayList<Roi>();
            System.out.println("Labelmap " + labelmap);
            System.out.println("rois " + rois);
            clijx.pullLabelsToROIList(labelmap, rois);

            for (int i = 0; i < rois.size(); i++) {
                Roi roi = rois.get(i);

                roi.setStrokeColor(CLIJxWekaObjectClassification.getColor(i + 1));
                overlay.add(roi);


                //roi = RoiEnlarger.enlarge(roi, -1);
                viewer.setRoi(roi);
                ImageStatistics stats = viewer.getStatistics(Measurements.CENTROID, Measurements.AREA);
                //stats.
                //EllipseFitter ef = new EllipseFitter();
                //ef.fit(viewer.getProcessor(), null);
                //stats.drawEllipse();

                int roiX = (int) stats.xCentroid;
                int roiY = (int) stats.yCentroid;
                int roiWidth = (int) Math.sqrt(stats.area);
                int roiHeight = (int) Math.sqrt(stats.area);

                roi = new OvalRoi(roiX - roiWidth / 2, roiY - roiHeight / 2, roiWidth, roiHeight);

                roi.setStrokeColor(CLIJxWekaObjectClassification.getColor(i + 1));
                overlay.add(roi);

                if (rm != null && rm.getRoi(i) != null) {
                    //roi.setName(rm.getRoi(i).getName());
                    TextRoi textRoi = new TextRoi(roiX, roiY, rm.getRoi(i).getName());
                    textRoi.setStrokeColor(roi.getStrokeColor());
                    overlay.add(textRoi);
                }

            }
            viewer.killRoi();
        }


        TextRoi text = new TextRoi(0,0,
                mm.getDataSetName() + "\n" +
                mm.getResultIDs()[viewer.getChannel() - 1] + "\n" +
                mm.getHumanReadableTime() + "(Frame " + mm.getProcessedFrame() + ")",
                new Font("Arial", 0, 12)
        );
        text.setStrokeColor(Color.WHITE);
        overlay.add(text);
        viewer.setOverlay(overlay);
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
