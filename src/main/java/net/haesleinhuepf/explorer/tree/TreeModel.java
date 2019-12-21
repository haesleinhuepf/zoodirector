package net.haesleinhuepf.explorer.tree;

import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class TreeModel extends DefaultTreeModel {

	private AbstractTreeNode root;

	public TreeModel(AbstractTreeNode tn, final JTree tree) {
		super(tn);
		
		tree.setModel(this);
		
		final TreeModel treeModel = this;

		tree.getModel().addTreeModelListener(new TreeModelListener() {

			@Override
			public void treeNodesChanged(TreeModelEvent arg0) {
				System.out.print("treeNodesChanged");
				//treeModel.reload();
			}

			@Override
			public void treeNodesInserted(TreeModelEvent e) {
				 // first option
				System.out.print("treeNodesInserted");
		        //tree.expandPath(e.getTreePath());  
				//tree.expandRow(e.getPath());
		          // second option
		       // tree.setSelectionPath(e.getTreePath());
		          //tree.addSelectionPath(e.getPath());
		        //  treeModel.reload();
			}

			@Override
			public void treeNodesRemoved(TreeModelEvent arg0) {
				System.out.print("treeNodesRemoved");
				//treeModel.reload();
			}

			@Override
			public void treeStructureChanged(TreeModelEvent arg0) {
				System.out.println("treeStructureChanged");
				//treeModel.reload();
				/*EventQueue.invokeLater(new Runnable() {
					public void run() {
						treeModel.reload();
					}
				});*/
			}
		});

		root = tn;

	}

	public static AbstractTreeNode getItemAt(AbstractTreeNode headNode, TreePath path, int level) {
		//DebugHelper.print("TreeNode", "a");
		if (headNode instanceof AbstractTreeNode) {
			if (path == null || path.getPathCount() == level + 1) {
				return (AbstractTreeNode) headNode;
			}
		}

		if (headNode instanceof AbstractTreeNode) {
			AbstractTreeNode treeNode = (AbstractTreeNode) headNode;
			for (int j = 0; j < treeNode.getChildCount(); j++) {
				Object treeSubNode = treeNode.getChildAt(j);

				if (treeSubNode instanceof AbstractTreeNode) {
					if (treeSubNode.toString().compareTo((path.getPathComponent(level + 1)).toString()) == 0) {
						return getItemAt((AbstractTreeNode) treeSubNode, path, level + 1);
					}
				}
			}
		}
		return null;
	}
}
