package net.haesleinhuepf.imagej.gui;

import ij.*;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.plugin.HyperStackConverter;
import ij.plugin.frame.RoiManager;
import ij.plugin.tool.PlugInTool;
import ij.process.*;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.gui.Utilities;
import net.haesleinhuepf.clijx.weka.gui.CLIJxWekaObjectClassification;
import net.haesleinhuepf.clijx.weka.gui.InteractivePanelPlugin;
import net.haesleinhuepf.imagej.gui.spimcat.viewer.VirtualMeshMeasurementStack;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;
import net.haesleinhuepf.imagej.zoo.measurement.MeshMeasurements;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class InteractiveMeshMeasurements extends InteractivePanelPlugin{
    // Backend
    VirtualMeshMeasurementStack stack;
    CLIJx clijx;
    MeshMeasurements mm;

    // Frontend
    ImagePlus viewer;
    String[] annotatableClasses = new String[]{"Serosa","Embryo"};
    Choice channelPullDown;
    private Choice annotationPulldown;

    public InteractiveMeshMeasurements(ClearControlDataSet dataSet) {
        clijx = CLIJx.getInstance();
        clijx.setWaitForKernelFinish(true);

        mm = new MeshMeasurements(dataSet, clijx);


        mm.setZoomFactor(1);
        mm.setBlurSigma(2);
        mm.setNumberDoubleErosionsForPseudoCellSegmentation(4);
        mm.setNumberDoubleDilationsForPseudoCellSegmentation(12);

        mm.setTransposeXY(false);
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

        String tool = IJ.getToolName();
        Toolbar.addPlugInTool(new AnnotationMouseHandler());
        Toolbar.addPlugInTool(new TipTiltMouseHandler());
        IJ.setTool(tool);

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

        forceRedraw();
        initializedChannels.clear();
    }


    private String[] configureResultIDs(String[] resultIDs) {
        ArrayList<String> list = new ArrayList<String>();
        GenericDialog gd = new GenericDialog("Configure channels");
        boolean sameRow = true;
        for (String channel : resultIDs) {
            gd.addCheckbox(channel, (!channel.startsWith("VOL_")) ||
                            channel.compareTo("VOL_03_TRANSFORMED_INPUT") == 0 ||
                            channel.compareTo("VOL_06_LABELLED_CELLS") == 0
                   );
            if (sameRow) {
                gd.addToSameRow();
            }
            sameRow = !sameRow;
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

    private class AnnotationMouseHandler extends PlugInTool {
        PolygonRoi line = null;

        @Override
        public void mousePressed(ImagePlus imp, MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            line = new PolygonRoi(new float[]{x}, new float[]{y}, Roi.FREELINE);
            imp.setRoi(line);
        }

        @Override
        public void mouseDragged(ImagePlus imp, MouseEvent e) {
            FloatPolygon floatPolygon = line.getFloatPolygon();

            float[] xes = new float[floatPolygon.xpoints.length + 1];
            float[] yes = new float[floatPolygon.ypoints.length + 1];

            System.arraycopy(floatPolygon.xpoints, 0, xes, 0, floatPolygon.xpoints.length);
            System.arraycopy(floatPolygon.ypoints, 0, yes, 0, floatPolygon.ypoints.length);

            xes[xes.length - 1] = e.getX();
            yes[yes.length - 1] = e.getY();

            FloatPolygon newPolygon = new FloatPolygon(xes, yes);
            line = new PolygonRoi(newPolygon, Roi.FREELINE);
            imp.setRoi(line);
        }

        @Override
        public void mouseReleased(ImagePlus imp, MouseEvent e) {
            if (line == null) {
                return;
            }
            int classID = annotationPulldown.getSelectedIndex() + 1;
            line.setName("" + classID + " " + annotationPulldown.getSelectedItem());
            line.setStrokeColor(CLIJxWekaObjectClassification.getColor(classID));

            RoiManager rm = RoiManager.getInstance();
            if (rm == null) {
                rm = new RoiManager();
            }
            rm.addRoi(line);
            line = null;
        }


        @Override
        public String getToolName() {
            return "SPIMcat Annotations";
        }

        @Override
        public String getToolIcon()
        {
            return Utilities.generateIconCodeString(
                    getToolIconString()
            );

        }

        public String getToolIconString()
        {
            return
                    //        0123456789ABCDEF
                    /*0*/	 "#        #####  " +
                    /*1*/	 " #      #     # " +
                    /*2*/	 "  ##     #     #" +
                    /*3*/	 "    #####      #" +
                    /*4*/	 "              # " +
                    /*5*/	 "        ######  " +
                    /*6*/	 "       #        " +
                    /*7*/	 "      #    ##   " +
                    /*8*/	 "     #    #  #  " +
                    /*9*/	 "     #    #  #  " +
                    /*A*/	 "     #   #    # " +
                    /*B*/	 "         ###### " +
                    /*C*/	 "         #    # " +
                    /*D*/	 "        #      #" +
                    /*E*/	 "        #      #" +
                    /*F*/	 "        #      #" ;
        }

    }

    private class TipTiltMouseHandler extends PlugInTool {

        public TipTiltMouseHandler(){}

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

                if (SwingUtilities.isLeftMouseButton(e)) {
                    mm.setTranslationX(translationStartX - deltaX);
                    mm.setTranslationY(translationStartY - deltaY);
                } else {
                    mm.setRotationY(angleStartX - deltaX / 5);
                    mm.setRotationX(angleStartY + deltaY / 5);
                }
                mm.invalidateTransformed();
                formerFrame = -1;
                forceRedraw();
                //System.out.println("Refreshing...");
            }
        }

        @Override
        public String getToolName() {
            return "SPIMcat Tip/tilt";
        }

        @Override
        public String getToolIcon()
        {
            return Utilities.generateIconCodeString(
                    getToolIconString()
            );

        }

        public String getToolIconString()
        {
            return
                    //        0123456789ABCDEF
                    /*0*/	 "####  ####      " +
                    /*1*/	 "##      ##      " +
                    /*2*/	 "# #    # #      " +
                    /*3*/	 "#  #  #  #      " +
                    /*4*/	 "    ##          " +
                    /*5*/	 "    ##          " +
                    /*6*/	 "#  #  #  #      " +
                    /*7*/	 "# #    # #      " +
                    /*8*/	 "##      ##  ####" +
                    /*9*/	 "####  ####  ##  " +
                    /*A*/	 "            # # " +
                    /*B*/	 "            #  #" +
                    /*C*/	 "        ####   #" +
                    /*D*/	 "        ##     #" +
                    /*E*/	 "        # #   # " +
                    /*F*/	 "        #  ###  " ;
        }
    }

    private void buildGUI() {
        panelHeight = 60;

        //Panel panel = new Panel();
        guiPanel.setLayout(new FlowLayout());



        guiPanel.add(new Label("C"));

        channelPullDown = new Choice();
        for (String channelName : mm.getResultIDs()) {
            channelPullDown.add(channelName);
        }
        channelPullDown.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                viewer.setC(channelPullDown.getSelectedIndex() + 1);

                //refresh();
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
        channelPullDown.setSize(200, channelPullDown.getHeight());

        {
            Button btnConfig = new Button("Config");
            btnConfig.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("CONFIG");
                    config();
                    //formerFrame = -1;
                    mm.invalidate();
                    mm.invalidateTraining();
                    forceRedraw();
                }
            });
            guiPanel.add(btnConfig);
        }

        annotationPulldown = new Choice();
        annotationPulldown.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                Toolbar.addPlugInTool(new AnnotationMouseHandler());
            }
        });
        fillPulldown(annotationPulldown, annotatableClasses);
        guiPanel.add(annotationPulldown);

        {
            Button btnTrain = new Button("Train");
            btnTrain.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("TRAIN");
                    train();
                    //formerFrame = -1;
                    mm.invalidate();
                    forceRedraw();
                }
            });
            guiPanel.add(btnTrain);
        }

        {
            Button btnReset = new Button("Reset");
            btnReset.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("RESET");
                    //formerFrame = -1;
                    mm.invalidate();
                    mm.invalidateTraining();
                    forceRedraw();
                }
            });
            guiPanel.add(btnReset);
        }

    }

    private void fillPulldown(Choice pulldown, String[] content) {
        pulldown.removeAll();
        for (String entry : content) {
            pulldown.add(entry);
        }
    }

    private void config() {
        GenericDialog gd = new GenericDialog("SPIMcat viewer");

        gd.addNumericField("Threshold", mm.getThreshold(), 2);
        gd.addNumericField("Blur sigma", mm.getBlurSigma(), 2);
        gd.addNumericField("Remove background sigma", mm.getBackgroundBlurSigma(), 2);
        gd.addCheckbox("Remove surface cells", mm.isEliminateOnSurfaceCells());
        gd.addCheckbox("Remove sub sufrac cell", mm.isEliminateSubSurfaceCells());
        //gd.addCheckbox("Auto context classification", mm.isAutoContextClassification());
        gd.addCheckbox("Draw text", mm.isDrawText());
        gd.addCheckbox("Draw classification outlines", stack.isDrawOutlines());
//        gd.addCheckbox("Auto brightness contrast", autoBrightNessContrast);

        gd.addSlider("Rotation X", -90, 90, mm.getRotationX(), 5);
        gd.addSlider("Rotation Y", -90, 90, mm.getRotationY(), 5);
        gd.addSlider("Rotation Z", -90, 90, mm.getRotationZ(), 5);
        gd.addSlider("Translation X", -256, 256, mm.getTranslationX(), 32);
        gd.addSlider("Translation Y", -256, 256, mm.getTranslationY(), 32);
        gd.addSlider("Translation Z", -256, 256, mm.getTranslationZ(), 32);


        gd.addStringField("Annotations (comma separated):", String.join(", ", annotatableClasses));

        gd.addCheckbox("Invalidate everything, when clicking Ok.", true);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        mm.setThreshold(gd.getNextNumber());
        mm.setBlurSigma(gd.getNextNumber());
        mm.setBackgroundSubtractionSigma(gd.getNextNumber());
        mm.setEliminateOnSurfaceCells(gd.getNextBoolean());
        mm.setEliminateSubSurfaceCells(gd.getNextBoolean());
        //mm.setAutoContextClassification(gd.getNextBoolean());
        mm.setDrawText(gd.getNextBoolean());
        stack.setDrawOutlines(gd.getNextBoolean());
  //      autoBrightNessContrast = gd.getNextBoolean();

        mm.setRotationX(gd.getNextNumber());
        mm.setRotationY(gd.getNextNumber());
        mm.setRotationZ(gd.getNextNumber());
        mm.setTranslationX(gd.getNextNumber());
        mm.setTranslationY(gd.getNextNumber());
        mm.setTranslationZ(gd.getNextNumber());

        String enteredClasses = gd.getNextString();
        annotatableClasses = enteredClasses.split(",");
        for (int i = 0; i < annotatableClasses.length; i++) {
            annotatableClasses[i] = annotatableClasses[i].trim();
        }
        fillPulldown(annotationPulldown, annotatableClasses);

        if (gd.getNextBoolean()) {
            mm.invalidate();
            mm.invalidateTraining();
        }
        forceRedraw();
    }

    private void train() {
        //Roi roi = viewer.getRoi();
        RoiManager rm = RoiManager.getInstance();
        if (rm == null) {
            System.out.println("No ROI Manager; exiting");
        }

        synchronized (mm) {
            //mm.setStoreMeasurements(true);
            //mm.invalidate(); // actually not necessary
            //mm.processFrameForRequestedResult(null, null, viewer.getFrame(), "");

            //ClearCLBuffer clLabelMap = (ClearCLBuffer) mm.getResult(viewer.getFrame(), "07_max_labelled_cells");
            //ClearCLBuffer measurements = (ClearCLBuffer) mm.getResult(viewer.getFrame(), "07_measurements");
            //mm.setStoreMeasurements(false); //

            ResultsTable collection = new ResultsTable();
            //clijx.image2DToResultsTable(measurements, table);
            String[] availableChannels = mm.getResultIDs();

            int default_label_volume_channel = -1;
            int default_label_projection_channel = -1;
            for (int i = 0; i < availableChannels.length; i++) {
                if (availableChannels[i].compareTo("VOL_06_LABELLED_CELLS") == 0) {
                    default_label_volume_channel = i;
                } else if (availableChannels[i].compareTo("07_max_labelled_cells") == 0) {
                    default_label_projection_channel =  i;
                }
            }

            for (int i = 0; i < rm.getCount(); i++) {
                Roi roi = rm.getRoi(i);

                int channel = roi.getCPosition() - 1;
                String selected_channel = availableChannels[channel];
                System.out.println("Annotation was drawn on " + selected_channel);
                if (!selected_channel.toLowerCase().contains("labelled")) {
                    // the selected channel doesn't allow training, let's replace it
                    if (selected_channel.startsWith("VOL_")) {
                        // replace it by a volume
                        channel = default_label_volume_channel;
                    } else {
                        channel = default_label_projection_channel;
                    }
                }
                System.out.println("Deriving labels from " + availableChannels[channel]);

                //System.out.println("=>CZT " + roi.getCPosition() + " " + roi.getZPosition() + " " + roi.getTPosition());
                int position = (channel + 1) +
                               roi.getZPosition() * viewer.getNChannels()+
                               roi.getTPosition() * viewer.getNChannels() * viewer.getNSlices();
                mm.invalidate();
                mm.setStoreMeasurements(true);
                mm.processFrameForRequestedResult(null, null, roi.getTPosition(), "");
                ClearCLBuffer clLabelMap = clijx.push(new ImagePlus("imp", stack.getProcessor(position)));
                ResultsTable table = mm.getAllMeasurements(); //new ResultsTable();
                //clijx.show(clLabelMap, "map");
                ArrayList<Integer> labels = getLabelsFromRoi(clijx.pull(clLabelMap), roi);
                clLabelMap.close();

                int klass = Integer.parseInt(roi.getName().split(" ")[0]);

                for (int label : labels) {
                    System.out.println("Label " + label + " / " + table.size());
                    table.setValue("CLASS", label, klass);
                }

                for (int j = 0; j < table.size(); j++) {
                    if (table.getValue("CLASS", j) > 0) {
                        collection.incrementCounter();
                        for (String header : table.getHeadings()) {
                            collection.addValue(header, table.getValue(header, j));
                        }
                    }
                }
            }

            //collection.show("Collection");
            mm.train(collection, collection.getColumn(collection.getColumnIndex("CLASS")));

        }
        forceRedraw();
    }

    private ArrayList<Integer> getLabelsFromRoi(ImagePlus imp, Roi roi) {
        ArrayList<Integer> labels = new ArrayList<Integer>();

        FloatPolygon fp = roi.getInterpolatedPolygon(1, false);
        float[] xpoints = new float[fp.xpoints.length];
        System.arraycopy(fp.xpoints, 0, xpoints, 0, fp.xpoints.length);
        float[] ypoints = new float[fp.ypoints.length];
        System.arraycopy(fp.ypoints, 0, ypoints, 0, fp.ypoints.length);

        ImageProcessor ip = imp.getProcessor();
        synchronized (fp) {
            for (int i = 0; i < xpoints.length && i < ypoints.length; i++) {
                Integer label = (int) ip.getf((int) fp.xpoints[i], (int) fp.ypoints[i]);
                labels.add(label);
            }
        }
        return labels;
    }


    boolean refreshing = false;
    int formerFrame = -1;
    int formerChannel = -1;
    int formerSlice = -1;

    ArrayList<Integer> initializedChannels = new ArrayList<Integer>();

    private void forceRedraw() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                double displayMin = viewer.getDisplayRangeMin();
                double displayMax = viewer.getDisplayRangeMax();
                LUT lut = viewer.getProcessor().getLut();
                viewer.setProcessor(stack.getProcessor(viewer.getC() +
                        viewer.getSlice() *  viewer.getNChannels() +
                        viewer.getFrame() * viewer.getNSlices() * viewer.getNChannels()
                ));
                viewer.getProcessor().setLut(lut);
                if (!initializedChannels.contains(viewer.getC())) {
                    initializedChannels.add(viewer.getC());
                    IJ.run(viewer, "Enhance Contrast", "saturated=0.35");
                } else {
                    viewer.setDisplayRange(displayMin, displayMax);
                }
                viewer.setOverlay(stack.getOverlay());
                viewer.updateAndDraw();
            }
        });
    }

    @Override
    protected synchronized void refresh() {
  /*      if (refreshing) {
            return;
        }
        refreshing = true;
        int frame = viewer.getFrame(); //Integer.parseInt(frameTextField.getText());
        int channel = viewer.getChannel();
        int slice = viewer.getSlice() - 1;

        System.out.println("Refreshing...");

        if (channel != formerChannel || frame != formerFrame || formerSlice != slice) {

            //stack.setChannel(channel);
            //if (channel.compareTo(formerChannel) != 0) {
            //    viewer.setC(channelPullDown.getSelectedIndex() + 1);
            System.out.println("rot: " + mm.getRotationX());
            //viewer.setC(viewer.getC());
            //viewer.updateImage();
            //viewer.updateAndDraw();
            //}

            formerChannel = channel;
            formerFrame = frame;
            formerSlice = slice;
        } else {
            System.out.println("" + System.currentTimeMillis() + " nothing changed");
        }



*/
        super.refresh();
        //refreshing = false;
    }

    protected void mouseUp(MouseEvent e) {
        forceRedraw();
    }

    boolean repeat = false;
    protected void imageChanged() {
        if (repeat) {
            return;
        }
        repeat = true;
        if (!initializedChannels.contains(viewer.getC())) {
            initializedChannels.add(viewer.getC());
            IJ.run(viewer, "Enhance Contrast", "saturated=0.35");
        }
        //if (autoBrightNessContrast) {
        //    IJ.run(viewer, "Enhance Contrast", "saturated=0.35");
        //}

        viewer.setOverlay(stack.getOverlay());
        repeat = false;
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
