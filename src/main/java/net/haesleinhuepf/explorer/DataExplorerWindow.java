package net.haesleinhuepf.explorer;

import net.haesleinhuepf.explorer.tree.TreeBuilder;
import net.haesleinhuepf.explorer.tree.TreeModel;
import net.haesleinhuepf.explorer.tree.factories.AbstractTreeNodeFactory;
import net.haesleinhuepf.explorer.tree.manipulators.PropertiesManipulatable;
import net.haesleinhuepf.explorer.tree.nodes.*;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

public class DataExplorerWindow extends JFrame {

	private JPanel contentPane;
	JTree tree;
	private JScrollPane scrollPane;
	private JSplitPane splitPane;

	private TreeBuilder treeBuilder;

	Timer heartbeat = null;
	int delay = 1000; // milliseconds

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DataExplorerWindow frame = new DataExplorerWindow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public DataExplorerWindow() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 563);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.66);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		contentPane.add(splitPane, BorderLayout.CENTER);

		scrollPane = new JScrollPane();
		splitPane.setLeftComponent(scrollPane);

		tree = new JTree();
		tree.setCellRenderer(new CustomTreeCellRenderer());
		
		tree.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				System.out.println("mouse released");
				currentTreeNodeChanged();
			}
		});

		scrollPane.setViewportView(tree);

		splitPane.setRightComponent(new JPanel());

		createMenus(null);

		ActionListener taskPerformer = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				heartBeat();
			}
		};

		heartbeat = new Timer(delay, taskPerformer);
		System.out.println("starting heartbeat");
		heartbeat.start();
	}

	AbstractTreeNode currentTreeNode;
	AbstractTreeNode formerTreeNode;
	int formerTreeNodeChildCount;
	private TreeModel treeModel;

	private void heartBeat() {
		if (currentTreeNode != null && treeModel != null) {
			if (currentTreeNode == formerTreeNode && (formerTreeNodeChildCount != currentTreeNode.getChildCount())) {
				treeModel.reload(currentTreeNode);
				System.out.println("reloading...");
				if (formerTreeNodeChildCount == 0) {
					System.out.println("expanding...");
					tree.expandPath(new TreePath(currentTreeNode.getPath()));
					treeModel.reload(currentTreeNode.getLastChild());
				}
				formerTreeNodeChildCount = currentTreeNode.getChildCount();
			}
			formerTreeNode = currentTreeNode;
		}
		/*
		if (currentTreeNode != null && treeModel != null && IJ.getImage() != null
				&& (!(currentTreeNode instanceof RootTreeNode))
				&& (!(IJ.getImage().getWindow() instanceof PlotWindow))) {
			// DebugHelper.print(this, "checking selected image...");
			if (treeModel.getRoot() instanceof RootTreeNode) {
				// DebugHelper.print(this, "wrong parent...");
				if (currentTreeNode.getParentImage() != IJ.getImage()) {
					// DebugHelper.print(this, "checking another 1lvl...");
					RootTreeNode rtn = (RootTreeNode) treeModel.getRoot();
					AbstractTreeNode iptn = null;
					for (int i = 0; i < rtn.getChildCount(); i++) {
						TreeNode node = rtn.getChildAt(i);
						if (node instanceof ImagePlusTreeNode) {
							if (((ImagePlusTreeNode) node).getImagePlus() == IJ.getImage()) {
								iptn = (AbstractTreeNode) node;
								break;
							}
						}
					}
					if (iptn == null) {
						// DebugHelper.print(this, "nothing found, create one");
						// ImagePlusTreeNode
						AbstractTreeNodeFactory factory = treeBuilder
								.getFactoryToCreateNewTreeNode((AbstractTreeNode) (treeModel.getRoot()), IJ.getImage());
						if (factory != null) {
							iptn = factory.createNew((AbstractTreeNode) (treeModel.getRoot()), IJ.getImage());
						}
					}

					if (iptn != null) {
						tree.clearSelection();
						tree.setSelectionPath(new TreePath((iptn).getPath()));
					}
				}
				//
			}
		}*/
		// DebugHelper.print(this, "beat done...");
	}

	public void initializeTree(/* AbstractTreeNode tn */) {

		AbstractTreeNode rootNode = new RootTreeNode(tree);
		
		addImagePlussesFromWindowManager(rootNode);

		//addTestDataSet(rootNode);

		new NewImageListener(rootNode);
		treeModel = new TreeModel(rootNode, tree);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		tree.setModel(treeModel);
	}

	private void createMenus(final AbstractTreeNode treeNode) {
		JMenuBar menuBar = getJMenuBar();
		if (menuBar == null) {
			menuBar = new JMenuBar();
		}
		menuBar.removeAll();

		JMenu mainMenu = new JMenu("File");

		JMenu newMenu = new JMenu("New");
		if (treeBuilder != null) {
			ArrayList<AbstractTreeNodeFactory> factories = treeBuilder.getFactoriesToCreateNewTreeNode(treeNode, null);
			for (int i = 0; i < factories.size(); i++) {
				final AbstractTreeNodeFactory factory = factories.get(i);

				JMenuItem newItem = new JMenuItem(factory.nodeName());
				newItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						factory.createNew(treeNode, null);
					}
				});
				newMenu.add(newItem);
			}
			mainMenu.add(newMenu);
		}

		mainMenu.addSeparator();

		JMenuItem refreshTreeMenuItem = new JMenuItem("Refresh tree");
		refreshTreeMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				treeModel.reload();
			}

		});
		mainMenu.add(refreshTreeMenuItem);
		menuBar.add(mainMenu);

		// -------------

		JMenu editMenu = new JMenu("Edit");
		JMenuItem cutMenu = new JMenuItem("Cut");
		cutMenu.setAccelerator(KeyStroke.getKeyStroke('X', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		cutMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Menu Cut");
				ClipBoard.setClipboard(treeNode, false);
			}
		});

		editMenu.add(cutMenu);

		JMenuItem copyMenu = new JMenuItem("Copy");
		copyMenu.setAccelerator(KeyStroke.getKeyStroke('C', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		copyMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Menu Copy");
				ClipBoard.setClipboard(treeNode, true);
			}
		});

		editMenu.add(copyMenu);

		JMenuItem pasteMenu = new JMenuItem("Paste");
		pasteMenu.setAccelerator(KeyStroke.getKeyStroke('V', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		if (treeBuilder != null) {
			final AbstractTreeNodeFactory factory = treeBuilder.getFactoryToCopyTreeNode(treeNode,
					ClipBoard.getClipBoard());
			if (factory != null) {
				pasteMenu.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						System.out.println("Menu Paste");
						if (factory.copyExisting(treeNode, ClipBoard.getClipBoard()) != null) {
							System.out.println("copy worked");
							if (!ClipBoard.toCopy) {
								System.out.println("going to delete former one because it should be cut");
								ClipBoard.getClipBoard().delete();
								ClipBoard.setClipboard(null, false);
							}
						}
					}
				});
			} else {
				pasteMenu.setEnabled(false);
			}
		} else {
			pasteMenu.setEnabled(false);
		}
		editMenu.add(pasteMenu);

		JMenuItem deleteMenuItem = new JMenuItem("Delete");
		deleteMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Menu Delete");
				if (treeNode != null) {
					treeNode.delete();
				}
			}

		});
		editMenu.add(deleteMenuItem);
		editMenu.addSeparator();
		JMenuItem duplicateMenu = new JMenuItem("Duplicate");
		duplicateMenu.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Duplicate called via menu");
				if (treeNode != null) {
					AbstractTreeNodeFactory factory = treeBuilder.getFactoryToCopyTreeNode(treeNode.getParent(),
							treeNode);
					if (factory != null) {
						System.out.println("Factory found");
						if (factory.copyExisting(treeNode.getParent(), treeNode) != null) {
							// worked fine
							System.out.println("Duplicate worked");
						}
					}
				}
			}

		});
		editMenu.add(duplicateMenu);
		menuBar.add(editMenu);

		setJMenuBar(menuBar);
	}

	private void currentTreeNodeChanged() {
		TreePath[] path = tree.getSelectionPaths();

		if (!(tree == null || tree.getModel() == null || tree.getModel().getRoot() == null || path == null)) {

			AbstractTreeNode treeNode = TreeModel.getItemAt((AbstractTreeNode) (tree.getModel().getRoot()), path[0], 0);
			if (treeNode != null) {
				currentTreeNodeChanged(treeNode);
			}
		}
	}

	private void currentTreeNodeChanged(AbstractTreeNode treeNode) {
		currentTreeNode = treeNode;

		// Invoke click on item
		treeNode.clicked();

		// Show its edit-panel
		if (treeNode instanceof PropertiesManipulatable) {
			System.out.println("adding content pane");

			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setViewportView(((PropertiesManipulatable) treeNode).getManipulatorPanel());
			splitPane.setRightComponent(scrollPane);
		} else {
			splitPane.setRightComponent(new JPanel());
		}

		// Rebuild menus
		createMenus(treeNode);
	}

	public TreeBuilder getTreeBuilder() {
		return treeBuilder;
	}

	public void setTreeBuilder(TreeBuilder treeBuilder) {
		this.treeBuilder = treeBuilder;
		treeBuilder.setTree(tree);
	}

	private void addImagePlussesFromWindowManager(AbstractTreeNode rootNode) {
		int[] idList = WindowManager.getIDList();
		for (int i = 0; i < idList.length; i++) {
			ImagePlus imp = WindowManager.getImage(idList[i]);
			AbstractTreeNodeFactory factory = treeBuilder.getFactoryToCreateNewTreeNode(rootNode, imp);
			if (factory != null) {
				factory.createNew(rootNode, imp);
			}
		}
	}

	private static class CustomTreeCellRenderer extends DefaultTreeCellRenderer {
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {

			Component ret = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

			JLabel label = (JLabel) ret;
			if (value instanceof AbstractTreeNode) {
				label.setIcon(((AbstractTreeNode) value).getIcon());
			} else {
				System.out.println(" " + value);
			}
			return ret;
		}
	}
	

	NewImageListener newImageListener;
	
	class NewImageListener implements ImageListener
	{
		AbstractTreeNode rootNode;
		
		NewImageListener(AbstractTreeNode rootNode)
		{
			this.rootNode = rootNode;
			ImagePlus.addImageListener(this);
		}
		
		@Override
		public void imageOpened(ImagePlus imp) {
			System.out.println("new image found");
			/*new ImagePlusTreeNode(tree, imp, rootNode);
			DebugHelper.print(this, "root child count: " + rootNode.getChildCount());
			//rootNode.add(newChild);
			treeModel.reload();*/
		}
		@Override
		public void imageClosed(ImagePlus imp) {}
		@Override
		public void imageUpdated(ImagePlus imp) {}
	}
}
