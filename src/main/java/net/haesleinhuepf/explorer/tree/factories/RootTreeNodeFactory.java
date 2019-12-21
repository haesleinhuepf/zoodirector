package net.haesleinhuepf.explorer.tree.factories;

import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import net.haesleinhuepf.explorer.tree.nodes.RootTreeNode;

public class RootTreeNodeFactory extends AbstractTreeNodeFactory{

	public RootTreeNodeFactory(TreeBuilder treeBuilder) {
		super(treeBuilder);
	}

	@Override
	public RootTreeNode createNew(AbstractTreeNode parent, Object object) {
		if (parent != null)
		{
			return null;
		}
		return new RootTreeNode(getTree());
	}

	@Override
	public AbstractTreeNode copyExisting(AbstractTreeNode parent, AbstractTreeNode treeNode) {
		return null;
	}

	@Override
	public boolean couldCreateNewWithParent(AbstractTreeNode parent, Object object) {
		return (parent == null); 
	}

	@Override
	public boolean couldCopyExistingWithParent(AbstractTreeNode parent, AbstractTreeNode treeNode) {
		return false;
	}

	@Override
	public String nodeName() {
		return "root of all evil";
	}
	
}
