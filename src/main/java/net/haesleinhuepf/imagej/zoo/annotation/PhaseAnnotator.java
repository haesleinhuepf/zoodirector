package net.haesleinhuepf.imagej.zoo.annotation;

import autopilot.measures.FocusMeasures;
import ij.IJ;
import ij.ImageJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.YesNoCancelDialog;
import ij.io.OpenDialog;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.weka.ApplyOCLWekaModel;
import net.haesleinhuepf.clijx.weka.CLIJxWeka;
import net.haesleinhuepf.clijx.weka.gui.InteractivePanelPlugin;
import net.haesleinhuepf.imagej.zoo.ZooExplorerPlugin;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSetOpener;
import net.haesleinhuepf.imagej.zoo.data.classification.Phase;
import net.haesleinhuepf.imagej.zoo.measurement.MeasurementTable;
import net.haesleinhuepf.imagej.zoo.measurement.SliceAnalyser;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;

public class PhaseAnnotator extends InteractivePanelPlugin implements PlugInFilter, ImageListener {

    FocusMeasures.FocusMeasure[] focusMeasures = new FocusMeasures.FocusMeasure[]{
            FocusMeasures.FocusMeasure.StatisticMax,
            FocusMeasures.FocusMeasure.StatisticMean,
            FocusMeasures.FocusMeasure.StatisticVariance,
            FocusMeasures.FocusMeasure.StatisticNormalizedVariance,
            FocusMeasures.FocusMeasure.SpectralNormDCTEntropyShannon,
            FocusMeasures.FocusMeasure.DifferentialTotalVariation,
            FocusMeasures.FocusMeasure.DifferentialTenengrad
    };

    class Entry{
        // key
        ClearControlDataSet dataSet;
        // meta data
        String path;
        int frame;
        String name;

        // temporary stuff
        double[] spot_counts;
        ImagePlus thumbnails;
        ResultsTable table;

        // measurements
        int spot_count;
    }

    Entry current = null;


    ResultsTable table = new ResultsTable();
    private Choice phaseChoice;

    ImagePlus imp;

    @Override
    public int setup(String arg, ImagePlus imp) {
        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor ip) {

        imp = IJ.getImage();

        setupGUI();

        ImagePlus.addImageListener(this);
        imageUpdated(imp);


    }

    Button saveButton;
    Button predictButton;
    private void setupGUI() {
        attach(imp.getWindow());

        {
            Button loadButton = new Button("Load");
            loadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (table.size() > 0) {
                        YesNoCancelDialog yncd = new YesNoCancelDialog(imp.getWindow(), "Sure?", "Your current annotations will be lost. Are you sure?");
                        if (!yncd.yesPressed()) {
                            return;
                        }
                    }
                    try {
                        table = ResultsTable.open(null);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    if (table.size() > 0) {
                        table.show("Annotations");
                        saveButton.setEnabled(true);
                        predictButton.setEnabled(true);
                    }
                }
            });
            guiPanel.add(loadButton);
        }

        {
            saveButton = new Button("Save");
            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    table.save(null);
                }
            });
            saveButton.setEnabled(false);
            guiPanel.add(saveButton);
        }

        {
            predictButton = new Button("Predict");
            predictButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    predictClicked();
                }
            });
            predictButton.setEnabled(false);
            guiPanel.add(predictButton);
        }


        {
            phaseChoice = new Choice();
            for (Phase phase : Phase.all) {
                //names[i] = phase.toString();
                phaseChoice.addItem(phase.toString());
            }
            guiPanel.add(phaseChoice);
        }

        {
            Button annotateButton = new Button("Annotate");
            annotateButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateClicked();
                }
            });
            guiPanel.add(annotateButton);
        }


        {
            Button doneButton = new Button("Done");
            doneButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doneClicked();
                }
            });
            guiPanel.add(doneButton);
        }
    }

    HashMap<Integer, Integer> indexToClassID;
    HashMap<Integer, Integer> classIDTOIndex;
    private CLIJxWeka train() {
        ResultsTable sendToGPUTable = new ResultsTable();
        indexToClassID = new HashMap<>();
        classIDTOIndex = new HashMap<>();

        int index = 1;
        for (int i = 0; i < table.size(); i++) {
            int class_id = (int) table.getValue("Phase_index", i);
            int current_phase_index;
            if (!classIDTOIndex.containsKey(class_id)) {
                indexToClassID.put(index, class_id);
                classIDTOIndex.put(class_id, index);
                current_phase_index = index;
                index++;
            } else {
                current_phase_index = classIDTOIndex.get(class_id);
            }

            sendToGPUTable.incrementCounter();
            sendToGPUTable.addValue("GROUND_TRUTH", current_phase_index);

            sendToGPUTable.addValue("SPOT_COUT", table.getValue("Spot_count", i));

            for (FocusMeasures.FocusMeasure focusMeasure : focusMeasures) {
                sendToGPUTable.addValue(focusMeasure.getLongName(), table.getValue(focusMeasure.getLongName(), i));
            }
        }


        //sendToGPUTable.show("Send to GPU");

        CLIJx clijx = CLIJx.getInstance();

        ClearCLBuffer tableOnGPU = clijx.create(sendToGPUTable.getHeadings().length, sendToGPUTable.size());

        clijx.resultsTableToImage2D(tableOnGPU, sendToGPUTable);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //table.show("My Results");

        ClearCLBuffer transposed1 = clijx.create(tableOnGPU.getHeight(), tableOnGPU.getWidth());
        ClearCLBuffer transposed2 = clijx.create(tableOnGPU.getHeight(), 1, tableOnGPU.getWidth());
        clijx.transposeXY(tableOnGPU, transposed1);
        clijx.transposeYZ(transposed1, transposed2);

        ClearCLBuffer ground_truth = clijx.create(transposed2.getWidth(), transposed2.getHeight(), 1);
        ClearCLBuffer featureStack = clijx.create(transposed2.getWidth(), transposed2.getHeight(), transposed2.getDepth() - 1);

        clijx.crop3D(transposed2, featureStack, 0, 0, 1);
        clijx.crop3D(transposed2, ground_truth, 0, 0, 0);

        //System.out.println("feature stack");
        //clijx.print(featureStack);
        //System.out.println("ground_truth");
        //clijx.print(ground_truth);

        //System.out.println("Ground truth:");
        //clijx.print(ground_truth);

        CLIJxWeka clijxweka = new CLIJxWeka(clijx, featureStack, ground_truth);

        clijxweka.getClassifier();


        clijx.release(ground_truth);
        clijx.release(featureStack);
        clijx.release(transposed1);
        clijx.release(transposed2);
        clijx.release(tableOnGPU);

        return clijxweka;
    }


    private void predictClicked() {
        CLIJx clijx = CLIJx.getInstance();

        CLIJxWeka clijxweka = train();

        ResultsTable sendToGPUTable = new ResultsTable();
        sendToGPUTable.incrementCounter();
        sendToGPUTable.addValue("SPOT_COUT", current.spot_count);
        for (FocusMeasures.FocusMeasure focusMeasure : focusMeasures) {
            sendToGPUTable.addValue(focusMeasure.getLongName(), current.table.getValue(focusMeasure.getLongName(), 0));
        }

        float[] values = new float[focusMeasures.length + 1];
        values[0] = current.spot_count;
        int i = 1;
        for (FocusMeasures.FocusMeasure focusMeasure : focusMeasures) {
            values[i] = (float) current.table.getValue(focusMeasure.getLongName(), 0);
            i++;
        }

        FloatBuffer featureBuffer = FloatBuffer.wrap(values);

        ClearCLBuffer featureStack = clijx.create(1, 1, sendToGPUTable.getHeadings().length);
        featureStack.readFrom(featureBuffer, true);

        ClearCLBuffer result = clijx.create(1, 1);

        ApplyOCLWekaModel.applyOCL(clijx, featureStack, result, clijxweka.getOCL());

        float[] resultArray = new float[(int) result.getWidth()];
        FloatBuffer buffer = FloatBuffer.wrap(resultArray);

        result.writeTo(buffer, true);

        int predictedIndex = (int) resultArray[resultArray.length - 1];
        int predictedClass = indexToClassID.get(predictedIndex + 1);

        IJ.log("Prediction: " + Phase.all[predictedClass]);

        clijx.release(result);
        clijx.release(featureStack);
    }

    private void doneClicked() {
        dismantle();
    }

    private void annotateClicked() {
        //System.out.println("Choice: " + phaseChoice.getSelectedItem());
        //System.out.println("Choice index: " + phaseChoice.getSelectedIndex());

        table.incrementCounter();
        table.addValue("Dataset", current.name);
        table.addValue("Frame", current.frame);
        table.addValue("Phase_index", phaseChoice.getSelectedIndex());
        table.addValue("Phase_name", phaseChoice.getSelectedItem());
        table.addValue("Path", current.path);
        table.addValue("Spot_count", current.spot_count);

        copyTableRow(current.table, table);
        table.show("Annotations");
        table.save("backup.csv");

        saveButton.setEnabled(true);
        predictButton.setEnabled(true);
    }


    private void copyTableRow(ResultsTable table, ResultsTable table1) {
        for(String heading : table.getHeadings()) {
            int columnIndex = table.getColumnIndex(heading);
            float value = table.getColumn(columnIndex)[0];
            table1.addValue(heading, value);
        }
    }

    @Override
    public void imageOpened(ImagePlus imp) {

    }

    @Override
    public void imageClosed(ImagePlus imp) {

    }

    @Override
    public void imageUpdated(ImagePlus imp) {
        ClearControlDataSet dataSet = ClearControlDataSet.getDataSetOfImagePlus(imp);
        if (dataSet != null) {
            //IJ.log(imp.getTitle() + ": " + dataSet.getPath());
            current = generateEntry(dataSet);
        }
    }

    private Entry generateEntry(ClearControlDataSet dataSet) {
        Entry entry = new Entry();
        if (current != null && current.dataSet == dataSet) {
            entry = current;
        }

        entry.dataSet = dataSet;
        entry.name = dataSet.getShortName();
        entry.path = dataSet.getPath();
        entry.frame = dataSet.getFrameRangeStart();

        if (entry.spot_counts == null)
        {
            MeasurementTable measurement = dataSet.getMeasurement("spotcount.tsv");
            if (measurement != null) {
                entry.spot_counts = measurement.getColumn("Number of spots");
            } else {
                IJ.log("No spotcount.tsv found.");
            }
        }
        if (entry.spot_counts != null) {
            entry.spot_count = (int) entry.spot_counts[entry.frame];
        }

        if (entry.thumbnails == null) {
            entry.thumbnails = dataSet.getThumbnailsFromFolder("stacks/thumbnails_sb", false);
        }
        if (entry.thumbnails != null) {
            CLIJx clijx = CLIJx.getInstance();
            entry.thumbnails.setT(entry.frame + 1);
            ClearCLBuffer buffer = clijx.pushCurrentSlice(entry.thumbnails);

            //ImagePlus imp = clijx.pull(buffer);

            entry.table = new ResultsTable();

            entry.table.incrementCounter();
            new SliceAnalyser(buffer, focusMeasures, entry.table).run();

            clijx.release(buffer);
        }
        return entry;
    }

    public static void main(String[] args) {
        new ImageJ();
        ZooExplorerPlugin.open("C:/structure/data/");


        String sourceFolder = "C:/structure/data/2019-12-17-16-54-37-81-Lund_Tribolium_nGFP_TMR/";
        String datasetFolder = "C0opticsprefused";
        ClearControlDataSet dataSet = ClearControlDataSetOpener.open(sourceFolder, datasetFolder);

        dataSet.getThumbnailsFromFolder("stacks/thumbnails_sb_text").show();

        new PhaseAnnotator().run(null);
    }
}
