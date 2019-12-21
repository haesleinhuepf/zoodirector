package net.haesleinhuepf.explorer.tree.manipulators;

import ij.ImagePlus;


public class ImagePlusManipulator extends AbstractManipulator {

	ImagePlus imp;
	/**
	 * Create the panel.
	 */
	public ImagePlusManipulator() {

		super();
	}
	
	public void setImagePlus(ImagePlus imp)
	{
		this.imp = imp;
		
	}
	
	
}
