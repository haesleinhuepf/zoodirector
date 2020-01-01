package net.haesleinhuepf.imagej.gui;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clijx.CLIJx;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

public class CLIJBar extends JFrame implements PlugIn {
    boolean killed = false;

    int maxWidth = 64;

    public CLIJBar() {

        setUndecorated(true);

        HashMap<String, ActionListener> actionmap = new HashMap<>();
        actionmap.put("XY", (e) -> transposeXY());
        actionmap.put("XZ", (e) -> transposeXZ());
        actionmap.put("YZ", (e) -> transposeYZ());
        actionmap.put("x", (e) -> close());

        int buttonSize = 32;

        int x = 0;
        int y = 0;
        for (String key : actionmap.keySet()) {
            JButton button = new JButton(key);
            button.setMargin(new Insets(0,0,0,0));
            button.setSize(buttonSize, buttonSize);
            button.setLocation(x * buttonSize, y * buttonSize);
            x++;
            if (x >= maxWidth) {
                x = 0;
                y++;
            }
            button.addActionListener(actionmap.get(key));
            add(button);
        }
        add(new Label());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                killed = true;
            }
        });

        new HeartBeat().start();
    }

    @Override
    public void run(String arg) {
        new CLIJBar().setVisible(true);
    }

    private class HeartBeat extends Thread {
        public void run() {
            while (!killed) {
                ImageJ instance = IJ.getInstance();

                int newX = (int) (instance.getX() + instance.getWidth());
                int newY = (int) instance.getY();
                SwingUtilities.invokeLater(() -> {
                    setLocation(newX, newY);
                    setSize(maxWidth, instance.getHeight());
                });

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //System.out.println("beat");
            }
        }
    }

    private void transposeXY() {
        CLIJx clijx = CLIJx.getInstance();

        ImagePlus imp = IJ.getImage();
        ClearCLBuffer input = clijx.push(imp);
        ClearCLBuffer output = clijx.create(new long[]{input.getHeight(), input.getWidth(), input.getDepth()}, input.getNativeType());
        clijx.transposeXY(input, output);
        ImagePlus out = clijx.pull(output);

        clijx.release(input);
        clijx.release(output);

        out.setTitle(imp.getTitle() + "tXY");
        out.setLut(imp.getProcessor().getLut());
        out.show();
    }

    private void transposeXZ() {
        CLIJx clijx = CLIJx.getInstance();

        ImagePlus imp = IJ.getImage();
        ClearCLBuffer input = clijx.push(imp);
        ClearCLBuffer output = clijx.create(new long[]{input.getDepth(), input.getHeight(), input.getWidth()}, input.getNativeType());
        clijx.transposeXZ(input, output);
        ImagePlus out = clijx.pull(output);

        clijx.release(input);
        clijx.release(output);

        out.setTitle(imp.getTitle() + "tXZ");
        out.setLut(imp.getProcessor().getLut());
        out.show();
    }

    private void transposeYZ() {
        CLIJx clijx = CLIJx.getInstance();

        ImagePlus imp = IJ.getImage();
        ClearCLBuffer input = clijx.push(imp);
        ClearCLBuffer output = clijx.create(new long[]{input.getWidth(), input.getDepth(), input.getHeight()}, input.getNativeType());
        clijx.transposeYZ(input, output);
        ImagePlus out = clijx.pull(output);

        clijx.release(input);
        clijx.release(output);

        out.setTitle(imp.getTitle() + "tYZ");
        out.setLut(imp.getProcessor().getLut());
        out.show();
    }

    private void close() {
        killed = true;
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSED));
        dispose();
    }
}
