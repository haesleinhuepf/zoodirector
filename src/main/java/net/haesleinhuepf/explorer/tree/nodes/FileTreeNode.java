package net.haesleinhuepf.explorer.tree.nodes;

import ij.IJ;
import ij.gui.Plot;
import net.haesleinhuepf.explorer.tree.TreeBuilder;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.Charset;

public class FileTreeNode extends AbstractTreeNode{

	private File file;

	public FileTreeNode(TreeBuilder treeBuilder, File file, AbstractTreeNode parent)
	{
		super(treeBuilder.getTree(), file.getName(), parent);

		this.file = file;
	}

	@Override
	public void clicked()
	{
		System.out.println("file click");
	}


	@Override
	public void doubleClicked() {
		System.out.println("file dbl clicked");
		IJ.open(file.getPath());
	}

	public Icon getIcon() {
		String icon = "node.png";
		if (file.getName().endsWith(".gif")) {
			icon = "image.png";
		}
		return new ImageIcon(Thread.currentThread().getContextClassLoader().getResource(icon));
	}

	@Override
	public byte[] getContent() {
		return new byte[0];
	}

}
