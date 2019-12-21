package net.haesleinhuepf.explorer.tree.nodes;

import ij.gui.Plot;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.Charset;

public class PlotTreeNode extends AbstractTreeNode{
	
	Plot plot;
	
	private double[] xData;
	private double[] yData;
	private String xAxisText;
	private String yAxisText;

	public PlotTreeNode(JTree tree, String title, AbstractTreeNode parent, String xAxisText, String yAxisText, double[] xData, double[] yData)
	{
		super(tree, title, parent);
		this.xData = xData;
		this.yData = yData;
		this.xAxisText = xAxisText;
		this.yAxisText = yAxisText;
		
	}

	@Override
	public void clicked()
	{
		System.out.println("click");
		if (plot == null)
		{

			System.out.println("click?");
			plot = new Plot(title, xAxisText, yAxisText, xData, yData);
			plot.setLineWidth(0);
			System.out.println("click!");
			plot.setColor(new Color(254,254,254));
			plot.addPoints(xData, yData, Plot.LINE);
			plot.setColor(Color.blue);
			plot.addPoints(xData, yData, Plot.BOX);
			//plot.draw();
		}
		System.out.println("clicked");
		plot.show();
		System.out.println("clicked one");
	}
	

	@Override
	public void delete() {
		System.out.println(" PlotTreeNode delete");
		plot = null;
		this.removeAllChildren();
		this.removeFromParent();
		super.delete();
	}
	

	public Icon getIcon() {
		return new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("plot.png"));
	}

	@Override
	public byte[] getContent() {
		String result = "";
		result.concat(title + ";" + xAxisText + ";" + yAxisText + ";" + xData.length);
		for (int i = 0; i < xData.length; i++) {
			result.concat("" + xData[i] + ";" + yData[i]  +";");
		}

		return result.getBytes(Charset.forName("UTF-8"));
	}

}
