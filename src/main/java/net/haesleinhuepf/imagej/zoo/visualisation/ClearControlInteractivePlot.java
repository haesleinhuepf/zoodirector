package net.haesleinhuepf.imagej.zoo.visualisation;

import ij.IJ;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ClearControlInteractivePlot {
    private ClearControlDataSet dataSet;
    private String title;
    private double[] yValues;
    private double[] xTimesInMinutes;

    public ClearControlInteractivePlot(ClearControlDataSet dataSet, String title, double[] xTimesInMinutes, double[] yValues) {
        this.dataSet = dataSet;
        this.title = title;
        this.yValues = yValues;
        this.xTimesInMinutes = xTimesInMinutes;
    }

    public void show() {
        getPlotWindow().show();
    }

    Plot plot = null;
    PlotWindow plotWindow = null;
    public PlotWindow getPlotWindow() {
        if (plot == null) {
            plot = new Plot(title, "Time / minutes", title);
            //plot.getImagePlus().hide();
            plot.add("-", xTimesInMinutes, yValues);

            plotWindow = plot.show();
            plotWindow.getCanvas().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    //System.out.println(e.getX() + "/" + e.getY());
                    //System.out.println(plot.descaleX(e.getX()) + "/" + plot.descaleY(e.getY()));
                    double timeInMinues = plot.descaleX(e.getX());
                    int frame = dataSet.getFirstFrameAfterTimeInSeconds(timeInMinues * 60);

                    dataSet.setCurrentFrame(frame);

                    super.mouseClicked(e);
                }
            });
            dataSet.addPlot(plot);
        }
        return plotWindow;
    }

    public String getName() {
        return title;
    }
}
