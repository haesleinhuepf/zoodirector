package net.haesleinhuepf.imagej.zoo.data;

import fiji.util.gui.GenericDialogPlus;
import ij.ImageJ;
import ij.Prefs;
import ij.plugin.PlugIn;

public class ClearControlDataSetOpener implements PlugIn {
    private static String path = Prefs.getDefaultDirectory();
    private static String datasetName = "C0opticsprefused";

    @Override
    public void run(String arg) {
        GenericDialogPlus gd = new GenericDialogPlus("Open ClearControl data set");
        gd.addDirectoryField("Folder", path);
        gd.addStringField("Dataset", datasetName);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        path = gd.getNextString();
        datasetName = gd.getNextString();

        open(path, datasetName).show();
    }

    public static ClearControlDataSet open(String path, String datasetName) {
        return new ClearControlDataSet(path, datasetName);
    }

    public static void main(String[] args) {
        new ImageJ();

        String dataSetRootFolder = "C:/structure/data/2018-05-23-16-18-13-89-Florence_multisample/";
        String dataSetName = "opticsprefused";

        ClearControlDataSet ccds = open(dataSetRootFolder, dataSetName);
        ccds.show();
    }
}
