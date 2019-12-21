package net.haesleinhuepf.imagej.zoo.data.tree;

import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.factories.AbstractTreeNodeFactory;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.measurement.MeasurementTable;

public class MeasurementTableTreeNodeFactory extends AbstractTreeNodeFactory
{
    public MeasurementTableTreeNodeFactory(TreeBuilder treeBuilder) {
        super(treeBuilder);
    }

    @Override
    public boolean couldCreateNewWithParent(AbstractTreeNode parent, Object object) {
        return object instanceof MeasurementTable && parent instanceof ClearControlDataSetTreeNode;
    }

    @Override
    public AbstractTreeNode createNew(AbstractTreeNode parent, Object object) {
        MeasurementTable table = (MeasurementTable) object;
        ClearControlDataSet dataSet = ((ClearControlDataSetTreeNode) parent).getDataSet();
        return new MeasurementTableTreeNode(treeBuilder, table, parent, dataSet);
    }



    @Override
    public String nodeName() {
        return "Measurement table";
    }
}
