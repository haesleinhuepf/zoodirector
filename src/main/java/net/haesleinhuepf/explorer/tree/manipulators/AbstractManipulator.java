package net.haesleinhuepf.explorer.tree.manipulators;


import javax.swing.*;
import javax.swing.event.ChangeListener;

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
		int rows = 15;
		/*RowSpec[] rowSpec = new RowSpec[rows * 2];
		for (int r = 0; r < rows * 2; r += 2) {
			rowSpec[r] = DEFAULT_ROWSPEC;
			rowSpec[r + 1] = DEFAULT_ROWSPEC;
		}
		
		int columns = 8;
		ColumnSpec[] colSpec = new ColumnSpec[columns * 2];
		for (int c = 0; c < columns * 2; c += 2) {
			colSpec[c] = RELATED_GAP_COLSPEC;
			colSpec[c + 1] = DEFAULT_COLSPEC;
		}
		setLayout(new FormLayout(colSpec, rowSpec));*/
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
