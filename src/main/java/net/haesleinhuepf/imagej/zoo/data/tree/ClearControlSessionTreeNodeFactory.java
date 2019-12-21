package net.haesleinhuepf.imagej.zoo.data.tree;

import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.factories.AbstractTreeNodeFactory;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import net.haesleinhuepf.imagej.zoo.data.ClearControlSession;

public class ClearControlSessionTreeNodeFactory extends AbstractTreeNodeFactory
{
    public ClearControlSessionTreeNodeFactory(TreeBuilder treeBuilder) {
        super(treeBuilder);
    }

    @Override
    public boolean couldCreateNewWithParent(AbstractTreeNode parent, Object object) {
        return object instanceof ClearControlSession;
    }

    @Override
    public AbstractTreeNode createNew(AbstractTreeNode parent, Object object) {
        ClearControlSession session = (ClearControlSession) object;
        return new ClearControlSessionTreeNode(treeBuilder, session, parent);
    }

    @Override
    public String nodeName() {
        return "Session";
    }
}
