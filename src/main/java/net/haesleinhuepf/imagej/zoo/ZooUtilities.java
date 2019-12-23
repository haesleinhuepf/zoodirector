package net.haesleinhuepf.imagej.zoo;

import ij.ImagePlus;
import ij.plugin.FolderOpener;
import ij.plugin.HyperStackConverter;

import java.io.File;

/**
 * ZooUtilities
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */
public class ZooUtilities {
    public static ImagePlus openFolderStack(String thumbnailfolder) {

        File file = new File(thumbnailfolder);

        ImagePlus thumbnails = FolderOpener.open(thumbnailfolder, "virtual");
        thumbnails = HyperStackConverter.toHyperStack(thumbnails, 1, 1, thumbnails.getNSlices());
        thumbnails.setTitle(file.getName());
        return thumbnails;
    }
}
