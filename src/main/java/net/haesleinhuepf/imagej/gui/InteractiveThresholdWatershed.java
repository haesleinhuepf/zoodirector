package net.haesleinhuepf.imagej.gui;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Toolbar;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.tool.PlugInTool;
import ij.process.ImageProcessor;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clijx.CLIJx;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class InteractiveThresholdWatershed extends PlugInTool implements PlugInFilter {


    private ClearCLBuffer input;
    private ClearCLBuffer output;
    private ClearCLBuffer temp;
    private ImagePlus imp;
    private Integer startX = null;
    private Integer startY = null;

    private Float sigma = 2f;
    private Float threshold = 0f;

    private Float startSigma;
    private Float startThreshold;
    private CLIJx clijx;
    private String title;

    public void mousePressed(ImagePlus imp, MouseEvent e) {
        if (this.imp == imp) {
            startX = e.getX();
            startY = e.getY();
            startThreshold = threshold;
            startSigma = sigma;
        }
        e.consume();
    }

    public void mouseReleased(ImagePlus imp, MouseEvent e) {
        if (this.imp == imp) {
            startX = null;
            startY = null;
        }
        e.consume();
    }

    public void mouseDragged(ImagePlus imp, MouseEvent e)
    {
        if (this.imp == imp) {
            synchronized (this) {
                if (startX != null && startY != null) {
                    threshold = startThreshold - (startX - e.getX());
                    sigma = startSigma - (startY - e.getY()) * 0.1f;
                    refresh();
                }
            }
        }
        e.consume();
    }

    private void refresh() {
        System.out.println("ch " + threshold);

        try {
            clijx.blur(input, output, sigma, sigma);
            clijx.threshold(output, temp, threshold);
            clijx.binaryEdgeDetection(temp, output);
            clijx.showRGB(output, input, output, title);
        } catch (Exception e) {
            try {
                Files.write(Paths.get("C:/structure/temp/log.txt"), e.getStackTrace().toString().getBytes());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String... args) {
        new ImageJ();

        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        imp.show();

        // new InteractiveThresholdWatershed().run(imp.getProcessor());

        CLIJx clijx = CLIJx.getInstance();
        ClearCLBuffer input = clijx.push(imp);
        ClearCLBuffer temp = clijx.create(input.getDimensions(), NativeTypeEnum.Float);

        for (int i = 0; i < 10000; i++) {
            System.out.println(i);
            //clijx.threshold(input, temp, i % 100 + 100);

            //ClearCLBuffer temp = clijx.create(input.getDimensions(), NativeTypeEnum.Float);
            //clijx.threshold(input, temp, i % 100 + 100);


            clijx.blur(input, temp, i % 5 + 5, i % 5 + 5);
            //clijx.showRGB(input, temp, temp, "A");
            //temp.close();

        }

        // looping this:
        //clijx.blur(input, temp, i % 5 + 5, i % 5 + 5);
        //clijx.showRGB(input, temp, temp, "A");


    }

    @Override
    public int setup(String s, ImagePlus imagePlus) {
        return PlugInFilter.DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        ImagePlus imp = IJ.getImage();
        clijx = CLIJx.getInstance();
        input = clijx.push(imp);

        threshold = (float)clijx.meanOfAllPixels(input);

        temp = clijx.create(input.getDimensions(), NativeTypeEnum.Float);
        output = clijx.create(input.getDimensions(), NativeTypeEnum.Float);

        title = "SEG " + imp.getTitle();
        refresh();
        //clijx.showRGB(input, output, output, title);
        this.imp = IJ.getImage();

        Toolbar.addPlugInTool(this);
        run("");
    }
}
