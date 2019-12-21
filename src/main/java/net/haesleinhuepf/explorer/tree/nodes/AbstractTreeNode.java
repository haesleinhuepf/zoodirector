package net.haesleinhuepf.explorer.tree.nodes;

import net.haesleinhuepf.explorer.tree.TreeNodeClickListener;

import ij.ImagePlus;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public abstract class AbstractTreeNode extends DefaultMutableTreeNode implements TreeNodeClickListener {
	protected String title;
	private JTree tree;
	
	AbstractTreeNode parent = null;
	public AbstractTreeNode(JTree tree, String title, AbstractTreeNode parent)
	{
		super(title);
		this.tree = tree;
		this.title = title;
		
		if (parent != null)
		{
			parent.add(this);
			this.parent = parent;
			
		}
		
		
	}
	@Override
	public void clicked() {
		System.out.println("clicked");
	}
	@Override
	public void doubleClicked() {
		System.out.println("double clicked");
	}
	
	public void somethingChanged(Object o)
	{
		for (int j = 0; j < getChildCount(); j++)
		{
			Object treeSubNode = getChildAt(j);	
			if (treeSubNode instanceof AbstractTreeNode)
			{
				((AbstractTreeNode)treeSubNode).somethingChanged(o);
			}
		}
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public void delete()
	{
		System.out.println("abstractTreeNode deleting...");
		if (tree != null)
		{
			System.out.print("abstractTreeNode telling tree ...");
			JTree tree = this.tree;
			this.tree = null;
			
			if (parent != null)
			{
				System.out.print("abstractTreeNode telling parent ...");
				//tree.expandPath(new TreePath(parent.getPath()));
				((DefaultTreeModel)tree.getModel()).reload(parent);
				
				
				//this.tree.getModel().get
				//TreeNode[] nodes = ((DefaultTreeModel) tree.getModel()).getPathToRoot(parent);
				//tree.scrollPathToVisible(tpath);
                //tree.setSelectionPath(tpath);
				parent.somethingChanged(this);
				parent = null;
			}
		}
	}
	
	public ImagePlus getParentImage()
	{
		if (parent != null)
		{
			return parent.getParentImage();
		}
		else
		{
			return null;
		}
	}
	
	@Override
	public AbstractTreeNode getParent()
	{
		if (super.getParent() instanceof AbstractTreeNode)
		{
			return (AbstractTreeNode) super.getParent();
		}
		else
		{
			return null;
		}
	}
	
	@Override
	public void removeAllChildren()
	{
		for (int j = 0; j < getChildCount(); j++)
		{
			Object treeSubNode = getChildAt(j);	
			if (treeSubNode instanceof AbstractTreeNode)
			{
				((AbstractTreeNode)treeSubNode).delete();
			}
		}
	}
	
	public Icon getIcon() {
		return new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("node.png"));
	}
	
	@Override
	public String toString()
	{
		return title;
	}

	public abstract byte[] getContent();
	public byte[] getHeader()
	{
		return ("" + this.getClass().getCanonicalName()).getBytes(Charset.forName("UTF-8"));
	}

	class DataBlock
	{

		static final int formatHeaderLength = 1000;

		private byte[] formatHeader = new byte[0];

		private byte[] header = new byte[0];
		private byte[] content = new byte[0];
		private DataBlock[] children = new DataBlock[0];

		private void updateFormatHeader()
		{
			String fHeader = "" + header.length + ";" + content.length + ";" + children.length;
			while (fHeader.length() < formatHeaderLength)
			{
				fHeader.concat(" ");
			}
			//Todo: consider variable header length or alternative to concat
			formatHeader = fHeader.getBytes(Charset.forName("UTF-8"));
		}

		public void setHeader(byte[] header)
		{
			this.header = header;
			updateFormatHeader();
		}
		public void setContent(byte[] content)
		{
			this.content = content;
			updateFormatHeader();
		}
		public void setChildren(DataBlock[] children)
		{
			this.children = children;
			updateFormatHeader();
		}

		public long getSize()
		{
			int size = formatHeader.length + header.length + content.length;
			for (int i = 0; i < children.length; i ++)
			{
				size += children[i].getSize();
			}
			return size;
		}

		public boolean writeToStream(ByteArrayOutputStream outputStream)  {
			try {
				outputStream.write(formatHeader);
				outputStream.write(header);
				outputStream.write(content);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			for (int i = 0; i < children.length; i ++)
			{
				if (!children[i].writeToStream(outputStream))
				{
					return false;
				}
			}
			return true;
		}
	}

	protected DataBlock getDataBlock()
	{
		DataBlock block = new DataBlock();
		block.setHeader(getHeader());
		block.setContent(getContent());
		block.setChildren(getChildDataBlocks());
		return block;
	}

	protected DataBlock[] getChildDataBlocks()
	{
		DataBlock[] result = new DataBlock[getChildCount()];
		for (int j = 0; j < getChildCount(); j++)
		{
			Object treeSubNode = getChildAt(j);
			if (treeSubNode instanceof AbstractTreeNode)
			{
				result[j] = ((AbstractTreeNode)treeSubNode).getDataBlock();
			}
			else
			{
				System.out.println("Writing child " + treeSubNode + " to disc not implemented. Cancelling.");
				return null;
			}
		}
		return result;
	}
}
