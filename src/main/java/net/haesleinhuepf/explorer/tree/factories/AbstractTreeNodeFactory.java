package net.haesleinhuepf.explorer.tree.factories;

import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;

import javax.swing.*;

public abstract class AbstractTreeNodeFactory {

	protected TreeBuilder treeBuilder;
	public AbstractTreeNodeFactory(TreeBuilder treeBuilder) {
		this.treeBuilder = treeBuilder;
	}

	public JTree getTree() { return treeBuilder.getTree(); }

	public abstract boolean couldCreateNewWithParent(AbstractTreeNode parent, Object object);
	
	public abstract AbstractTreeNode createNew(AbstractTreeNode parent, Object object);

	public boolean couldCopyExistingWithParent(AbstractTreeNode parent, AbstractTreeNode treeNode) {
		return false;
	}

	public AbstractTreeNode copyExisting(AbstractTreeNode parent, AbstractTreeNode treeNode) {
		return null;
	}

	public abstract String nodeName();
	
}
