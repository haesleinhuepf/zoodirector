package net.haesleinhuepf.explorer.tree.nodes;

import javax.swing.*;
import java.nio.charset.Charset;

public class RootTreeNode extends AbstractTreeNode {
	public RootTreeNode(JTree tree) {
		super(tree, "Root", null);
	}

	@Override
	public void delete() {
		// haha, funny
	}
	

	public Icon getIcon() {
		return new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("node.png"));
	}

	@Override
	public byte[] getContent() {
		return new byte[0];
	}

	@Override
	public byte[] getHeader() {
		return ("" + this.getClass().getCanonicalName()).getBytes(Charset.forName("UTF-8"));
	}


}
