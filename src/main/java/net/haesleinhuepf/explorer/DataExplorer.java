package net.haesleinhuepf.explorer;

import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.factories.*;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import ij.IJ;
import ij.ImageJ;
import ij.gui.NewImage;

public class DataExplorer {
	private DataExplorerWindow dew;
	AbstractTreeNode rootNode;
	TreeBuilder treeBuilder;
	
	
	public DataExplorer()
	{
		dew = new DataExplorerWindow();
		dew.setVisible(true);

		treeBuilder = new TreeBuilder();
		treeBuilder.addFactory(new RootTreeNodeFactory());
		treeBuilder.addFactory(new ImagePlusTreeNodeFactory());

		dew.setTreeBuilder(treeBuilder);
		dew.initializeTree();
	}
	

	public static void main(String... args) {
		new ImageJ();
		NewImage.createByteImage("Test volume", 100, 100, 100, NewImage.FILL_BLACK).show();
		NewImage.createByteImage("Test flat", 200, 200, 1, NewImage.FILL_BLACK).show();

		IJ.run("Blobs (25K)", "");

		
		new DataExplorer();
	}
	
	
	
	
	
}
