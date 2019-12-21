package net.haesleinhuepf.imagej.zoo.data.tree;

import ij.ImagePlus;
import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlSession;
import net.haesleinhuepf.imagej.zoo.measurement.MeasurementTable;
import net.haesleinhuepf.imagej.zoo.measurement.Measurements;

public class ClearControlDataSetTreeNode extends AbstractTreeNode {

    private TreeBuilder treeBuilder;
    private ClearControlDataSet dataSet;

    public ClearControlDataSetTreeNode(TreeBuilder treeBuilder, ClearControlDataSet dataSet, AbstractTreeNode parent) {
        super(treeBuilder.getTree(), dataSet.getName(), parent);
        this.treeBuilder = treeBuilder;
        this.dataSet = dataSet;
    }

    @Override
    public byte[] getContent() {
        return new byte[0];
    }

    @Override
    public void clicked() {
        System.out.println("dataset clicked");

    }

    @Override
    public void doubleClicked() {
        System.out.println("dataset dbl clicked");
        if (this.children == null || this.children.size() == 0) {
            ImagePlus data = dataSet.getImageData();
            if (data != null) {
                treeBuilder.getFactoryToCreateNewTreeNode(this, data).createNew(this, data);
            }
            ImagePlus thumbnail = dataSet.getThumbnails();
            if (thumbnail != null) {
                treeBuilder.getFactoryToCreateNewTreeNode(this, thumbnail).createNew(this, thumbnail);
            }
            for (String name : dataSet.getMeasurementFiles()) {
                MeasurementTable table = new MeasurementTable(dataSet.getPath() + name);

                //System.out.println(name);
                //;
                treeBuilder.getFactoryToCreateNewTreeNode(this, table).createNew(this, table);
            }
        }
    }

    public ClearControlDataSet getDataSet() {
        return dataSet;
    }
}
