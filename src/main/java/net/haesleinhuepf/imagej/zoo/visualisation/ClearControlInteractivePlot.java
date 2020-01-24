package net.haesleinhuepf.imagej.zoo.visualisation;

import ij.IJ;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.measurement.MeasurementTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ClearControlInteractivePlot {
    private ClearControlDataSet dataSet;
    private String title;
    private double[] yValues;
    private double[] xTimesInMinutes;
    private MeasurementTable table;

    public ClearControlInteractivePlot(ClearControlDataSet dataSet, String title, double[] xTimesInMinutes, double[] yValues) {
        this.dataSet = dataSet;
        this.title = title;
        this.yValues = yValues;
        this.xTimesInMinutes = xTimesInMinutes;
    }

    public ClearControlInteractivePlot(ClearControlDataSet dataSet, String column, MeasurementTable table) {
        this.dataSet = dataSet;
        this.title = column;
        this.table = table;

        yValues = table.getColumn(column);
        xTimesInMinutes = dataSet.getTimesInMinutes();
    }

    public void show() {
        getPlotWindow().show();
    }

    Plot plot = null;
    PlotWindow plotWindow = null;
    public PlotWindow getPlotWindow() {
        if (plot == null) {
            plot = new Plot(title, "Time / minutes", title);
            //generateThumbnails.getImagePlus().hide();
            plot.add("-", xTimesInMinutes, yValues);

            plotWindow = plot.show();
            plotWindow.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosed(WindowEvent e) {
                    dataSet.removePlot(plot);
                    plotWindow = null;
                    plot = null;
                }
            });
            plotWindow.getCanvas().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    //System.out.println(e.getX() + "/" + e.getY());
                    //System.out.println(generateThumbnails.descaleX(e.getX()) + "/" + generateThumbnails.descaleY(e.getY()));

                    super.mouseClicked(e);

                    // SwingUtilities.invokeLater(() -> {
                        Roi roi = plot.getImagePlus().getRoi();
                        if (roi != null) {
                            Rectangle bounds = roi.getBounds();

                            double timeStartInMinues = plot.descaleX(bounds.x);
                            double timeEndInMinues = plot.descaleX(bounds.x + bounds.width);

                            int frameStart = dataSet.getFirstFrameAfterTimeInSeconds(timeStartInMinues * 60);
                            int frameEnd = dataSet.getFirstFrameAfterTimeInSeconds(timeEndInMinues * 60);
                            dataSet.setCurrentFrameRange(frameStart, frameEnd);
                        } else {
                            double timeInMinues = plot.descaleX(e.getX());
                            int frame = dataSet.getFirstFrameAfterTimeInSeconds(timeInMinues * 60);
                            dataSet.setCurrentFrameRange(frame, frame);
                        }
                    //});

                }
            });
            dataSet.addPlot(plot);
        }
        return plotWindow;
    }

    public String getName() {
        return title;
    }

    public ClearControlDataSet getDataSet() {
        return dataSet;
    }

    public MeasurementTable getMeasurementTable() {
        return table;
    }
}
