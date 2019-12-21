package net.haesleinhuepf.explorer.tree.manipulators;

import net.haesleinhuepf.explorer.data.OverlayProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OverlayManipulator extends AbstractManipulator {
	protected OverlayProperties overlayProperties;

	JCheckBox cbVisible;
	JButton btnColor;
	
	public void setOverlayProperties(OverlayProperties overlayProperties)
	{
		this.overlayProperties = overlayProperties;
		buildUi();
	}
	
	
	protected void buildUi()
	{
		if (overlayProperties != null)
		{
			System.out.println("show ui");
			int formLine = newFormLine();
			JLabel lblC = new JLabel("Color");
			add(lblC, "2, " + formLine);
			
			btnColor = new JButton();
			btnColor.setOpaque(true);
			btnColor.setBorderPainted(false);
			btnColor.setBackground(overlayProperties.color);
			btnColor.addActionListener(new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent e) {
					Color initialBackground = btnColor.getBackground();
			        Color background = JColorChooser.showDialog(null, "Choose color", initialBackground);
			        if (background != null) {
			        	btnColor.setBackground(background);
			        	anyPropertyHasChanged();
			        }
				}
			});
			add(btnColor, "4, " + formLine);
			
			formLine = newFormLine();
			JLabel lblV = new JLabel("Visible");
			add(lblV, "2, " + formLine);
			
			cbVisible = new JCheckBox();
			cbVisible.setSelected(overlayProperties.visible);
			cbVisible.addActionListener(new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent evt) {
					System.out.println("property visible changed " + evt.toString());
					anyPropertyHasChanged();
				}
				
			});
	
			add(cbVisible, "4, " + formLine);
		}
	}
	
	@Override
	protected void anyPropertyHasChanged() {

		if (overlayProperties != null)
		{
			overlayProperties.color = btnColor.getBackground();
		
			overlayProperties.visible = cbVisible.isSelected();
		}
		super.anyPropertyHasChanged();
	}
}
