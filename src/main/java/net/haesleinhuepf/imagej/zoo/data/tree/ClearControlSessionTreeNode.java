package net.haesleinhuepf.imagej.zoo.data.tree;

import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.nodes.AbstractTreeNode;
import net.haesleinhuepf.imagej.zoo.data.ClearControlDataSet;
import net.haesleinhuepf.imagej.zoo.data.ClearControlSession;

import javax.swing.*;
import java.io.File;

public class ClearControlSessionTreeNode extends AbstractTreeNode {

    private TreeBuilder treeBuilder;
    private ClearControlSession session;

    public ClearControlSessionTreeNode(TreeBuilder treeBuilder, ClearControlSession session, AbstractTreeNode parent) {
        super(treeBuilder.getTree(), session.getName(), parent);
        this.treeBuilder = treeBuilder;
        this.session = session;
    }

    @Override
    public byte[] getContent() {
        return new byte[0];
    }

    @Override
    public void clicked() {
        System.out.println("session clicked");

    }

    @Override
    public void doubleClicked() {
        System.out.println("session dbl clicked");
        if (this.children == null || this.children.size() == 0) {
            for (String name : session.getDataSetNames()) {
                ClearControlDataSet dataSet = session.getDataSet(name);
                treeBuilder.getFactoryToCreateNewTreeNode(this, dataSet).createNew(this, dataSet);
            }
            File folder = new File(session.getPath());
            treeBuilder.getFactoryToCreateNewTreeNode(this, folder).createNew(this, folder);
        }
    }

}
