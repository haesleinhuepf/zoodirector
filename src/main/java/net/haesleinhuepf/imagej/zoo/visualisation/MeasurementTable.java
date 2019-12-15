package net.haesleinhuepf.imagej.zoo.visualisation;

import ij.measure.ResultsTable;

import java.io.IOException;

public class MeasurementTable {
    ResultsTable table;
    private String filename;

    public MeasurementTable(String filename) {
        this.filename = filename;
        try {
            table = ResultsTable.open(filename);

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    public String[] getColumnNames() {
        return table.getHeadings();
    }

    public double[] getColumn(String name) {
        String[] names = getColumnNames();
        for (int i = 0; i < names.length; i++ ) {
            if (names[i].compareTo(name) == 0) {
                return table.getColumnAsDoubles(i);
            }
        }
        return null;
    }
}
