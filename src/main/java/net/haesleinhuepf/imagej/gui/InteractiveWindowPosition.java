package net.haesleinhuepf.imagej.gui;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Toolbar;
import ij.plugin.tool.PlugInTool;

import java.awt.event.MouseEvent;


public class InteractiveWindowPosition extends PlugInTool {

    Integer startX = null;
    Integer startY = null;

    Integer windowStartX = null;
    Integer windowStartY = null;


    @Override
    public void mousePressed(ImagePlus imp, MouseEvent e) {
        //super.mousePressed(imp, e);
        startX = e.getXOnScreen();
        startY = e.getYOnScreen();

        windowStartX = imp.getWindow().getX();
        windowStartY = imp.getWindow().getY();
    }

    @Override
    public void mouseReleased(ImagePlus imp, MouseEvent e) {
        //super.mouseReleased(imp, e);

        startX = null;
        startY = null;

        windowStartX = null;
        windowStartY = null;
    }

    @Override
    public void mouseDragged(ImagePlus imp, MouseEvent e) {
        //super.mouseDragged(imp, e);
        if (startX != null && startY != null & windowStartX != null & windowStartY != null) {
            imp.getWindow().setLocation(
                    windowStartX - startX + e.getXOnScreen(),
                    windowStartY - startY + e.getYOnScreen());
        }
    }


    public static void main(String[] args) {
        new ImageJ();
        IJ.openImage("src/test/resources/blobs.tif").show();
        IJ.openImage("src/test/resources/blobs.tif").show();


        Toolbar.addPlugInTool(new InteractiveWindowPosition());
        //new InteractiveWindowPosition().run("");

    }


    @Override
    public String getToolName() {
        return "Window Position";
    }

    @Override
    public String 	getToolIcon()
    {
        return Utilities.generateIconCodeString(
                getToolIconString()
        );

    }

    public static String getToolIconString()
    {
        return
                //        0123456789ABCDEF
                /*0*/	 "       ##       " +
                /*1*/	 "      ####      " +
                /*2*/	 "     ######     " +
                /*3*/	 "       ##       " +
                /*4*/	 "   #   ##   #   " +
                /*5*/	 "  ##   ##   ##  " +
                /*6*/	 " ###   ##   ### " +
                /*7*/	 "################" +
                /*8*/	 "################" +
                /*9*/	 " ###   ##   ### " +
                /*A*/	 "  ##   ##   ##  " +
                /*B*/	 "   #   ##   #   " +
                /*C*/	 "       ##       " +
                /*D*/	 "     ######     " +
                /*E*/	 "      ####      " +
                /*F*/	 "       ##       " ;
    }


}
