package net.haesleinhuepf.explorer.data;

import ij.gui.Overlay;
import net.imglib2.Interval;

public interface OverlayUpdater {
	public void updateOverlay(Overlay ov, Interval interval);
}
