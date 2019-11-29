package sc.fiji.io;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.io.SaveDialog;

/**
 * My_Gif_Stack_Writer
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 11 2019
 */
public class My_Gif_Stack_Writer {
    static String type = "gif";

    public static void writeGif(ImagePlus imp, String filename) {
        if (imp == null) {
            IJ.showMessage("Save As ", "No images are open.");
        } else {
            String name = imp.getTitle();
            int dotIndex = name.lastIndexOf(".");
            if (dotIndex >= 0) {
                name = name.substring(0, dotIndex);
            }

            AnimatedGifEncoder2 fr = new AnimatedGifEncoder2();
            fr.delay = 100;
            fr.repeat = 1000;
            fr.name = name;
            fr.setFrameRate(10);
            IJ.register(Gif_Stack_Writer.class);
            ImageStack stack = imp.getStack();
            ImagePlus tmp = new ImagePlus();
            int nSlices = stack.getSize();
            fr.start(filename);

            for(int i = 1; i <= nSlices; ++i) {
                IJ.showStatus("writing: " + i + "/" + nSlices);
                IJ.showProgress((double)i / (double)nSlices);
                tmp.setProcessor((String)null, stack.getProcessor(i));

                try {
                    fr.addFrame(tmp);
                } catch (Exception var14) {
                    IJ.showMessage("Save as " + type, "" + var14);
                }

                System.gc();
            }

            fr.finish();
            IJ.showStatus("");
            IJ.showProgress(1.0D);
        }
    }
}
