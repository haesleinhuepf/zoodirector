package net.haesleinhuepf.explorer;


import ij.plugin.PlugIn;

class DataExplorerPlugIn implements PlugIn
{
	
	@Override
	public void run(String arg) {
		new DataExplorer();
	}
	
}