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

public class ImagePlusTreeNode extends AbstractTreeNode implements ImageListener, PropertiesManipulatable {
	ImagePlus imp;

	public ImagePlusTreeNode(JTree tree, ImagePlus imp, AbstractTreeNode parent) {
		super(tree, imp.getTitle(), parent);
		this.imp = imp;

		imp.addImageListener(this);
	}

	@Override
	public void imageOpened(ImagePlus imp) {
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		System.out.println("image closed: " + imp);
		if (imp != this.imp) {
			return;
		}
		
		// Dirty workaround, because ImageJ vanishes the object even though I'm holding it :(
		this.imp = new Duplicator().run(imp);
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
		if (imp != this.imp) {
			return;
		}
		System.out.println("image changed: " + imp);
		somethingChanged(imp);
	}

	private int formerZ = -1;

	@Override
	public void somethingChanged(Object o) {
		System.out.println("something changed!");
		boolean pass = false;
		if (o == imp) {
			System.out.println("help, my image changed!");
			pass = true;
		}
		if (!pass && o instanceof AbstractTreeNode)
		{
			AbstractTreeNode tn = (AbstractTreeNode)o;
			for (int j = 0; j < getChildCount(); j++)
			{
				System.out.println("eq: " + getChildAt(j) + " == " + o);
				if (getChildAt(j) == tn)
				{
					System.out.println("help, onf of my children changed!");
					pass = true;
					break;
				}
			}
		}
		if (!pass)
		{
			return;
		}
		
		redrawOverLay();
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

	private void redrawOverLay() {
		Overlay ov = new Overlay();
		
		Interval interval = Intervals.createMinMax(0,0,imp.getZ()-1, imp.getWidth(), imp.getHeight(), imp.getZ()-1);
		
		for (int j = 0; j < getChildCount(); j++) {
			Object treeSubNode = getChildAt(j);
			if (treeSubNode instanceof OverlayUpdater) {
				((OverlayUpdater) treeSubNode).updateOverlay(ov, interval);
			}
		}
		imp.setOverlay(ov);
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
