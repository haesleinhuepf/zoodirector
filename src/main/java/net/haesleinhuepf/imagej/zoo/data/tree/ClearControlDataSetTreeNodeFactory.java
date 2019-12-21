package net.haesleinhuepf.imagej.zoo.data.tree;

import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.factories.AbstractTreeNodeFactory;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;

public class ClearControlDataSetTreeNodeFactory extends AbstractTreeNodeFactory
{
    public ClearControlDataSetTreeNodeFactory(TreeBuilder treeBuilder) {
        super(treeBuilder);
    }

    @Override
    public boolean couldCreateNewWithParent(AbstractTreeNode parent, Object object) {
        return object instanceof ClearControlDataSet;
    }

    @Override
    public AbstractTreeNode createNew(AbstractTreeNode parent, Object object) {
        ClearControlDataSet dataSet = (ClearControlDataSet) object;
        return new ClearControlDataSetTreeNode(treeBuilder, dataSet, parent);
    }

    @Override
    public String nodeName() {
        return "Data set";
    }
}
