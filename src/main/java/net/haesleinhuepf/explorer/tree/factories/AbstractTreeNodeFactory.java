package net.haesleinhuepf.explorer.tree.factories;

import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;

import javax.swing.*;

public abstract class AbstractTreeNodeFactory {
	
	protected JTree tree;
	
	public void setTree(JTree tree)
	{
		this.tree = tree;
	}
	
	public abstract boolean couldCreateNewWithParent(AbstractTreeNode parent, Object object);
	
	public abstract AbstractTreeNode createNew(AbstractTreeNode parent, Object object);
	
	public abstract boolean couldCopyExistingWithParent(AbstractTreeNode parent, AbstractTreeNode treeNode);
	
	public abstract AbstractTreeNode copyExisting(AbstractTreeNode parent, AbstractTreeNode treeNode);

	public abstract String nodeName();
	
}
