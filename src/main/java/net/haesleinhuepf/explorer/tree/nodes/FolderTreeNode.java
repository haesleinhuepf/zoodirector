package net.haesleinhuepf.explorer.tree.nodes;

import ij.IJ;
import net.haesleinhuepf.explorer.tree.TreeBuilder;

import javax.swing.*;
import java.io.File;

public class FolderTreeNode extends AbstractTreeNode{

	private TreeBuilder treeBuilder;
	private File folder;

	public FolderTreeNode(TreeBuilder treeBuilder, File folder, AbstractTreeNode parent)
	{
		super(treeBuilder.getTree(), folder.getName(), parent);
		this.treeBuilder = treeBuilder;

		this.folder = folder;
	}

	@Override
	public void clicked()
	{
		System.out.println("folder click");
	}


	@Override
	public void doubleClicked() {
		System.out.println("folder dbl clicked");
		for (File file : folder.listFiles()) {
			treeBuilder.getFactoryToCreateNewTreeNode(this, file).createNew(this, file);
		}
	}

	public Icon getIcon() {
		return new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("node.png"));
	}

	@Override
	public byte[] getContent() {
		return new byte[0];
	}

}
