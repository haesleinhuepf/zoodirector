package net.haesleinhuepf.explorer.tree.nodes;

import net.haesleinhuepf.explorer.data.OverlayUpdater;
import net.haesleinhuepf.explorer.tree.manipulators.ImagePlusManipulator;
import net.haesleinhuepf.explorer.tree.manipulators.PropertiesManipulatable;

import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.plugin.Duplicator;
import net.imglib2.Interval;
import net.imglib2.util.Intervals;

import javax.swing.*;
import java.nio.charset.Charset;

public class ImagePlusTreeNode extends AbstractTreeNode implements PropertiesManipulatable {
	ImagePlus imp;

	public ImagePlusTreeNode(JTree tree, ImagePlus imp, AbstractTreeNode parent) {
		super(tree, imp.getTitle(), parent);
		this.imp = imp;


	}

	@Override
	public void clicked() {
		System.out.println("clicked");
		imp.show();
		if (imp.getWindow() != null) {
			imp.getWindow().toFront();
		}
	}

	@Override
	public void doubleClicked() {
	}

	public ImagePlus getImagePlus() {
		return imp;
	}

	@Override
	public JPanel getManipulatorPanel() {
		
		ImagePlusManipulator impm = new ImagePlusManipulator();
		impm.setImagePlus(imp);
		return impm;
	}

	@Override
	public void delete() {
		System.out.println(" ImagePlusTreeNode delete");
		imp = null;
		this.removeAllChildren();
		this.removeFromParent();
		super.delete();
	}
	
	@Override
	public ImagePlus getParentImage()
	{
		return imp;
	}
	

	public Icon getIcon() {
		return new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("image.png"));
	}

	@Override
	public byte[] getContent() {



		return (getImagePlus().getFileInfo().directory +
		getImagePlus().getFileInfo().fileName).getBytes(Charset.forName("UTF-8"));
	}
}
