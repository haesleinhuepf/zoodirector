package net.haesleinhuepf.explorer.tree;

import net.haesleinhuepf.explorer.tree.factories.AbstractTreeNodeFactory;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import net.haesleinhuepf.explorer.tree.nodes.RootTreeNode;

import javax.swing.*;
import java.util.ArrayList;

public class TreeBuilder {

	private JTree tree;

	ArrayList<AbstractTreeNodeFactory> factories = new ArrayList<AbstractTreeNodeFactory>();
	private RootTreeNode rootNode;

	public void addFactory(AbstractTreeNodeFactory factory)
	{
		factories.add(factory);
	}
	
	public AbstractTreeNodeFactory getFactoryToCreateNewTreeNode(AbstractTreeNode parent, Object object)
	{
		for (int i = 0; i < factories.size(); i++)
		{
			AbstractTreeNodeFactory factory = factories.get(i);
			if (factory.couldCreateNewWithParent(parent, object))
			{
				//DebugHelper.print(this, "" + factory.getClass().getSimpleName() + " says it can create a " + object.getClass().getSimpleName());
				return factory;	
			}
		}
		return null;
	}
	

	public ArrayList<AbstractTreeNodeFactory> getFactoriesToCreateNewTreeNode(AbstractTreeNode parent, Object object)
	{
		ArrayList<AbstractTreeNodeFactory> potentFactories = new ArrayList<AbstractTreeNodeFactory>();
		for (int i = 0; i < factories.size(); i++)
		{
			AbstractTreeNodeFactory factory = factories.get(i);
			if (factory.couldCreateNewWithParent(parent, object))
			{
				potentFactories.add(factory);
			}
		}
		return potentFactories;
	}
	
	public AbstractTreeNodeFactory getFactoryToCopyTreeNode(AbstractTreeNode parent, AbstractTreeNode treeNode)
	{
		for (int i = 0; i < factories.size(); i++)
		{
			AbstractTreeNodeFactory factory = factories.get(i);
			if (factory.couldCopyExistingWithParent(parent, treeNode))
			{
				System.out.print("" + factory.getClass().getSimpleName() );
				if (treeNode != null)
				{
					System.out.println(" says it is able to create a " + treeNode.getClass().getSimpleName());
				}
				if (parent != null)
				{
					System.out.println(" and append it to " + parent.getClass().getSimpleName() );
				}
				return factory;	
			}
		}
		return null;
	}

	public void setTree(JTree tree) {
		this.tree = tree;
	}

	public JTree getTree() {
		return tree;
	}

	public RootTreeNode getRootNode() {
		return (RootTreeNode) tree.getModel().getRoot();
	}
}
