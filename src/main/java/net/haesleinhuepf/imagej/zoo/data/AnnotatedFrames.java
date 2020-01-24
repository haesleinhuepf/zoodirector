package net.haesleinhuepf.imagej.zoo.data;

import ij.measure.ResultsTable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class AnnotatedFrames {
    private final ClearControlDataSet dataSet;
    private String filename;
    private HashMap<Integer, String> annotations;

    public AnnotatedFrames(ClearControlDataSet dataSet) {
        this.dataSet = dataSet;

        filename = dataSet.getPath() + "annotatedFrames.csv";
        if (new File(filename).exists()) {
            load();
        } else {
            annotations = new HashMap<>();
        }
    }

    private void load() {
        annotations = new HashMap<>();
        try {
            ResultsTable table = ResultsTable.open(filename);
            for (int i = 0; i < table.size(); i++) {
                annotations.put((int)table.getValue("Frame", i), table.getStringValue("Annotation", i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void save() {
        ResultsTable table = getTable();
        table.save(filename);
    }

    public ResultsTable getTable() {
        ResultsTable table = new ResultsTable();
        for (Integer key : annotations.keySet()) {
            table.incrementCounter();
            table.setValue("Frame", table.size() - 1, key);
            table.setValue("Annotation", table.size() - 1,  annotations.get(key));
        }
        return table;
    }

    public String getAnnotation(int frame) {
        return annotations.get(frame);
    }

    public void putAnnotation(int frame, String annotation) {
        annotations.put(frame, annotation);
        save();
    }
}
