package net.haesleinhuepf.imagej.zoo.data.tree;

import ij.ImagePlus;
import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.measurement.MeasurementTable;
import net.haesleinhuepf.imagej.zoo.visualisation.ClearControlInteractivePlot;

import javax.swing.*;

public class MeasurementTableTreeNode extends AbstractTreeNode {

    private TreeBuilder treeBuilder;
    private MeasurementTable table;
    private ClearControlDataSet dataSet;


    public MeasurementTableTreeNode(TreeBuilder treeBuilder, MeasurementTable table, AbstractTreeNode parent, ClearControlDataSet dataSet) {
        super(treeBuilder.getTree(), table.getName(), parent);
        this.treeBuilder = treeBuilder;
        this.table = table;
        this.dataSet = dataSet;
    }

    @Override
    public byte[] getContent() {
        return new byte[0];
    }

    @Override
    public void clicked() {
        System.out.println("table clicked");

    }

    @Override
    public void doubleClicked() {
        System.out.println("table dbl clicked");
        if (this.children == null || this.children.size() == 0) {
            for (String name : table.getColumnNames()) {
                System.out.println(name);
                //double[] yData = table.getColumn(name);
                //double[] xTimeData = dataSet.getTimesInMinutes();

                ClearControlInteractivePlot plot = new ClearControlInteractivePlot(dataSet, name, table);
                treeBuilder.getFactoryToCreateNewTreeNode(this, plot).createNew(this, plot);

                //;
                //treeBuilder.getFactoryToCreateNewTreeNode(this, dataSet).createNew(this, dataSet);
            }
        }
    }

    public Icon getIcon() {
        return new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("table.png"));
    }
}
