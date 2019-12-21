package net.haesleinhuepf.imagej.zoo.data.tree;

import ij.ImagePlus;
import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.measurement.MeasurementTable;
import net.haesleinhuepf.imagej.zoo.visualisation.ClearControlInteractivePlot;

import javax.swing.*;

public class ClearControlPlotTreeNode extends AbstractTreeNode {

    private TreeBuilder treeBuilder;
    private ClearControlInteractivePlot plot;


    public ClearControlPlotTreeNode(TreeBuilder treeBuilder, ClearControlInteractivePlot plot, AbstractTreeNode parent) {
        super(treeBuilder.getTree(), plot.getName(), parent);
        this.treeBuilder = treeBuilder;
        this.plot = plot;
    }

    @Override
    public byte[] getContent() {
        return new byte[0];
    }

    @Override
    public void clicked() {
        System.out.println("plot clicked");

    }

    @Override
    public void doubleClicked() {
        System.out.println("plot dbl clicked");
        plot.show();
    }

    public Icon getIcon() {
        return new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("table.png"));
    }
}
