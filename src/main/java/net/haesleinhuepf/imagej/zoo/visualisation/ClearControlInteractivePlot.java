package net.haesleinhuepf.imagej.zoo.visualisation;

import ij.IJ;
import ij.gui.Plot;
import ij.gui.PlotWindow;

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
        Plot plot = new Plot(title, "Time / minutes", title);
        plot.add("-", xTimesInMinutes, yValues);

        PlotWindow window = plot.show();
        window.getCanvas().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                //System.out.println(e.getX() + "/" + e.getY());
                //System.out.println(plot.descaleX(e.getX()) + "/" + plot.descaleY(e.getY()));
                double timeInMinues = plot.descaleX(e.getX());
                int frame = dataSet.getFirstFrameAfterTime(timeInMinues * 60);

                dataSet.getThumbnails().setZ(frame);

                dataSet.getImageData().setT(frame);
                IJ.run(dataSet.getImageData(), "Enhance Contrast", "saturated=0.35");

                super.mouseClicked(e);
            }
        });
    }
}
