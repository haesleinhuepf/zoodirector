package net.haesleinhuepf.imagej.zoo;


import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.explorer.DataExplorer;
import net.haesleinhuepf.imagej.zoo.data.ClearControlSession;
import net.haesleinhuepf.imagej.zoo.data.ZooIndex;
import net.haesleinhuepf.imagej.zoo.data.tree.*;

public class ZooExplorerPlugin implements PlugIn {
    @Override
    public void run(String arg) {
        String rootfolder = IJ.getDirectory("Root folder");
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

        CLIJx.getInstance("2060");
        ZooExplorerPlugin.open("D:/");

        //ZooExplorerPlugin.open("C:/structure/data/");
    }

}

