package net.haesleinhuepf.explorer.tree.factories;

import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import net.haesleinhuepf.explorer.tree.nodes.ImagePlusTreeNode;
import net.haesleinhuepf.explorer.tree.nodes.RootTreeNode;
import ij.ImagePlus;
import ij.plugin.Commands;

public class ImagePlusTreeNodeFactory extends AbstractTreeNodeFactory{
	
	@Override
	public AbstractTreeNode copyExisting(AbstractTreeNode parent, AbstractTreeNode treeNode) {
		if (couldCopyExistingWithParent(parent, treeNode))
		{
			if (treeNode instanceof ImagePlusTreeNode)
			{
				ImagePlusTreeNode iptn = new ImagePlusTreeNode(tree, ((ImagePlusTreeNode) treeNode).getImagePlus(), parent);
				parent.somethingChanged(iptn);
				return iptn;
			}
		}
		return null;
	}

	@Override
	public boolean couldCreateNewWithParent(AbstractTreeNode parent, Object object) {
		return (parent instanceof RootTreeNode) && (object == null || object instanceof ImagePlus);
	}

	@Override
	public boolean couldCopyExistingWithParent(AbstractTreeNode parent, AbstractTreeNode treeNode) {
		return (treeNode instanceof ImagePlusTreeNode) && couldCreateNewWithParent(parent,((ImagePlusTreeNode)treeNode).getImagePlus());
	}

	@Override
	public AbstractTreeNode createNew(AbstractTreeNode parent, Object object) {
		if (object == null)
		{
			new Commands().run("new");
		}
		else if (object instanceof ImagePlus)
		{
			return new ImagePlusTreeNode(tree, (ImagePlus)object, parent);
		}
		return null;
	}

	@Override
	public String nodeName() {
		return "Image";
	}
}
