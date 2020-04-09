package net.haesleinhuepf.explorer.tree.manipulators;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

/*
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
*/

/*
import static com.jgoodies.forms.layout.FormSpecs.DEFAULT_COLSPEC;
import static com.jgoodies.forms.layout.FormSpecs.DEFAULT_ROWSPEC;
import static com.jgoodies.forms.layout.FormSpecs.RELATED_GAP_COLSPEC;
*/
public abstract class AbstractManipulator extends JPanel{

	private ChangeListener cl;
	
	public AbstractManipulator(){
		setLayout(new GridLayout(15, 2));
	}
	
	protected void anyPropertyHasChanged() {
		if (cl != null) {
			System.out.println("Calling stateChanged");
			cl.stateChanged(null);
		}
	}

	public void setChangeListener(ChangeListener cl) {
		this.cl = cl;
	}

	private int currentNumberOfFormLines = 0;

	protected int newFormLine() {
		currentNumberOfFormLines += 2;
		return currentNumberOfFormLines;
	}
}
