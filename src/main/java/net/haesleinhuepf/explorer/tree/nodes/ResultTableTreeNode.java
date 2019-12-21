package net.haesleinhuepf.explorer.tree.nodes;

import ij.measure.ResultsTable;

import javax.swing.*;

public class ResultTableTreeNode extends AbstractTreeNode{
	ResultsTable resultsTable;
	
	public ResultTableTreeNode(JTree tree,  String title, AbstractTreeNode parent, ResultsTable resultsTable) {
		super(tree, title, parent);
		this.resultsTable = resultsTable;
	}
	
	@Override
	public void clicked() {
		resultsTable.show(title);
	}

	@Override
	public void delete() {
		System.out.println(" ResultTableTreeNode delete");
		resultsTable = null;
		this.removeAllChildren();
		this.removeFromParent();
		super.delete();
	}

	public Icon getIcon() {
		return new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("table.png"));
	}

	@Override
	public byte[] getContent() {
		return new byte[0];
	}
}
