package net.haesleinhuepf.explorer.tree.factories;

import ij.ImagePlus;
import ij.plugin.Commands;
import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import net.haesleinhuepf.explorer.tree.nodes.FileTreeNode;
import net.haesleinhuepf.explorer.tree.nodes.ImagePlusTreeNode;

import java.io.File;

public class FileTreeNodeFactory extends AbstractTreeNodeFactory{

	public FileTreeNodeFactory(TreeBuilder treeBuilder) {
		super(treeBuilder);
	}

	@Override
	public boolean couldCreateNewWithParent(AbstractTreeNode parent, Object object) {
		return (object instanceof File) && (!((File) object).isDirectory());
	}

	@Override
	public AbstractTreeNode createNew(AbstractTreeNode parent, Object object) {
		File file = (File) object;
		return new FileTreeNode(treeBuilder, file, parent);
	}

	@Override
	public String nodeName() {
		return "Image";
	}
}
