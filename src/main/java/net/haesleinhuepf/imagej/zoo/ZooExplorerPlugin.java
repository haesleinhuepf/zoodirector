package net.haesleinhuepf.imagej.zoo;


import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.explorer.DataExplorer;
import net.haesleinhuepf.imagej.zoo.data.ClearControlSession;
import net.haesleinhuepf.imagej.zoo.data.ZooIndex;
import net.haesleinhuepf.imagej.zoo.data.tree.*;

import java.util.ArrayList;

public class ZooExplorerPlugin implements PlugIn {
    @Override
    public void run(String arg) {
        CLIJ clij = CLIJ.getInstance();
        ArrayList<String> deviceList = CLIJ.getAvailableDeviceNames();
        String[] deviceArray = new String[deviceList.size()];
        deviceList.toArray(deviceArray);
        GenericDialogPlus gd = new GenericDialogPlus("Initialize Zoo");
        gd.addDirectoryField("Root folder", IJ.getDir("current"));
        gd.addChoice("CL_Device", deviceArray, clij.getClearCLContext().getDevice().getName());
        gd.showDialog();
        if (gd.wasCanceled() ) {
           return;
        }

        String rootfolder = gd.getNextString();
        String cl_device = gd.getNextChoice();
        CLIJ2.getInstance(cl_device);
        open(rootfolder);
    }

    public static void open(String rootfolder) {
        if (rootfolder.length() == 0) {
            return;
        }
        ZooIndex index = new ZooIndex(rootfolder);

        System.out.println("hello");
        DataExplorer explorer = new DataExplorer();
        System.out.println("hello2");

        explorer.addFactoryClass(ClearControlDataSetTreeNodeFactory.class);
        explorer.addFactoryClass(ClearControlSessionTreeNodeFactory.class);
        explorer.addFactoryClass(MeasurementTableTreeNodeFactory.class);
        explorer.addFactoryClass(ClearControlPlotTreeNodeFactory.class);

        System.out.println("count : " + index.getSessionNames().length);

        for (String name : index.getSessionNames()) {
            System.out.println("add " + name);
            explorer.addToRootNode(index.getSession(name));
        }

    }

    public static void main(String... args) {

        new ImageJ();

        //CLIJx.getInstance("2060");
        //ZooExplorerPlugin.open("D:/");

        ZooExplorerPlugin.open("C:/structure/data/");


        //ZooExplorerPlugin.open("\\\\fileserver\\myersspimdata\\IMAGING\\archive_data_good/");
    }

}

