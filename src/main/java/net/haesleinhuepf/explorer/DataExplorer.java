package net.haesleinhuepf.explorer;

import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.factories.*;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import ij.IJ;
import ij.ImageJ;
import ij.gui.NewImage;
import net.haesleinhuepf.imagej.zoo.data.tree.ClearControlDataSetTreeNodeFactory;

import java.lang.reflect.InvocationTargetException;

public class DataExplorer {
	private DataExplorerWindow dew;
	TreeBuilder treeBuilder;
	
	
	public DataExplorer()
	{
		dew = new DataExplorerWindow();
		dew.setVisible(true);

		treeBuilder = new TreeBuilder();
		treeBuilder.addFactory(new RootTreeNodeFactory(treeBuilder));
		treeBuilder.addFactory(new ImagePlusTreeNodeFactory(treeBuilder));
		treeBuilder.addFactory(new FileTreeNodeFactory(treeBuilder));
		treeBuilder.addFactory(new FolderTreeNodeFactory(treeBuilder));

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
	
	public void addFactory(AbstractTreeNodeFactory factory) {
		treeBuilder.addFactory(factory);
	}

	public void addToRootNode(Object object) {
		AbstractTreeNodeFactory factory = treeBuilder.getFactoryToCreateNewTreeNode(treeBuilder.getRootNode(), object);
		factory.createNew(treeBuilder.getRootNode(), object);
	}


	public void addFactoryClass(Class<? extends AbstractTreeNodeFactory> factoryClass) {
		try {
			addFactory((AbstractTreeNodeFactory) factoryClass.getConstructors()[0].newInstance(treeBuilder));
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
