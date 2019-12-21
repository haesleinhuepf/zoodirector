package net.haesleinhuepf.imagej.zoo.data.tree;

import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.factories.AbstractTreeNodeFactory;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.measurement.MeasurementTable;
import net.haesleinhuepf.imagej.zoo.visualisation.ClearControlInteractivePlot;

public class ClearControlPlotTreeNodeFactory extends AbstractTreeNodeFactory
{
    public ClearControlPlotTreeNodeFactory(TreeBuilder treeBuilder) {
        super(treeBuilder);
    }

    @Override
    public boolean couldCreateNewWithParent(AbstractTreeNode parent, Object object) {
        return object instanceof ClearControlInteractivePlot;
    }

    @Override
    public AbstractTreeNode createNew(AbstractTreeNode parent, Object object) {
        ClearControlInteractivePlot plot = (ClearControlInteractivePlot) object;
        return new ClearControlPlotTreeNode(treeBuilder, plot, parent);
    }

    @Override
    public String nodeName() {
        return "Plot";
    }
}
