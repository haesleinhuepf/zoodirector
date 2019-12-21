package net.haesleinhuepf.explorer;

import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;

public class ClipBoard {
	static boolean toCopy;
	static AbstractTreeNode clipboardNode;
	public static  void setClipboard(AbstractTreeNode tn, boolean toCopy)
	{
		clipboardNode = tn;
		ClipBoard.toCopy = toCopy;
	}
	
	public static AbstractTreeNode getClipBoard()
	{
		return clipboardNode;
	}
	
	public static boolean shouldBeCopied()
	{
		return toCopy;
	}
}
