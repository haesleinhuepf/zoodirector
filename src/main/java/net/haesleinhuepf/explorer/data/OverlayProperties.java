package net.haesleinhuepf.explorer.data;

import ij.gui.Roi;

import java.awt.*;

public class OverlayProperties {
	public OverlayProperties()
	{
		color = Roi.getColor();
		visible = true;
	}
	public Color color;
	public boolean visible;
}
